package com.example.xiangqi.ai

import com.example.xiangqi.model.Board
import com.example.xiangqi.model.Move
import com.example.xiangqi.model.Pieces
import com.example.xiangqi.model.Side
import com.example.xiangqi.rules.MoveGenerator

enum class Difficulty(val label: String, val maxDepth: Int, val timeMs: Long, val randomness: Int) {
    EASY("簡單", 2, 320L, 110),
    MEDIUM("中等", 4, 800L, 0),
    HARD("困難", 8, 1800L, 0)
}

data class SearchResult(
    val move: Int,
    val score: Int,
    val depth: Int,
    val nodes: Long,
    val rootScores: List<RootScore> = emptyList()
)

data class RootScore(val move: Int, val score: Int)

class SearchEngine {
    private var board: IntArray = IntArray(Board.SIZE)
    private var sideToMove: Side = Side.RED
    private var hash: Long = 0L
    private var deadline: Long = 0L
    private var nodeLimit: Long = Long.MAX_VALUE
    private var nodes: Long = 0L
    private var aborted: Boolean = false
    private var cancelCheck: () -> Boolean = { false }

    private var ttKey = LongArray(0)
    private var ttScore = IntArray(0)
    private var ttDepth = ByteArray(0)
    private var ttFlag = ByteArray(0)
    private var ttMove = IntArray(0)
    private val killers = IntArray(MAX_PLY * 2)
    private val history = IntArray(Board.SIZE * Board.SIZE)
    private val orderScores = IntArray(MoveGenerator.MAX_MOVES)
    private val moveBuffers = Array(MAX_PLY) { IntArray(MoveGenerator.MAX_MOVES) }

    @Volatile
    private var ttDirty = true

    fun newGame() {
        ttDirty = true
    }

    private fun ensureTt() {
        if (ttKey.size != TT_SIZE) {
            ttKey = LongArray(TT_SIZE)
            ttScore = IntArray(TT_SIZE)
            ttDepth = ByteArray(TT_SIZE)
            ttFlag = ByteArray(TT_SIZE)
            ttMove = IntArray(TT_SIZE)
            ttDirty = false
            return
        }
        if (ttDirty) {
            ttKey.fill(0)
            ttDepth.fill(0)
            killers.fill(0)
            history.fill(0)
            ttDirty = false
        }
    }

    fun search(
        board: IntArray,
        side: Side,
        difficulty: Difficulty,
        maxNodes: Long = Long.MAX_VALUE,
        maxTimeMs: Long = difficulty.timeMs,
        cancelled: () -> Boolean = { false }
    ): SearchResult? {
        val moves = moveBuffers[0]
        val moveCount = MoveGenerator.generateLegalInto(board, side, false, moves)
        if (moveCount == 0) return null

        this.board = board.copyOf()
        this.sideToMove = side
        this.hash = Zobrist.hash(this.board, side)
        this.cancelCheck = cancelled
        this.nodeLimit = maxNodes
        this.aborted = false
        this.nodes = 0L
        val start = System.nanoTime()
        this.deadline = start + maxTimeMs * 1_000_000L
        ensureTt()

        orderMoves(moves, moveCount, -1, 0)
        var bestMove = moves[0]
        var bestScore = 0
        var completedDepth = 0
        var rootScores: List<RootScore> = emptyList()

        for (depth in 1..difficulty.maxDepth) {
            val scores = ArrayList<RootScore>(moveCount)
            var alpha = -Evaluator.INF
            var localBest = -1
            var localScore = -Evaluator.INF
            var depthAborted = false

            for (i in 0 until moveCount) {
                val move = moves[i]
                val captured = makeMove(move)
                sideToMove = sideToMove.opposite()
                val score = if (difficulty.randomness > 0) {
                    -negamax(depth - 1, -Evaluator.INF, Evaluator.INF, 1)
                } else {
                    -negamax(depth - 1, -Evaluator.INF, -alpha, 1)
                }
                sideToMove = sideToMove.opposite()
                unmakeMove(move, captured)

                if (aborted) {
                    depthAborted = true
                    break
                }
                scores.add(RootScore(move, score))
                if (score > localScore) {
                    localScore = score
                    localBest = move
                }
                if (score > alpha) alpha = score
            }

            if (depthAborted || localBest < 0) break
            bestMove = localBest
            bestScore = localScore
            completedDepth = depth
            rootScores = scores

            if (difficulty.randomness <= 0) {
                val scoreMap = HashMap<Int, Int>(moveCount)
                for (score in scores) scoreMap[score.move] = score.score
                val order = (0 until moveCount).sortedByDescending {
                    scoreMap[moves[it]] ?: -Evaluator.INF
                }
                val reordered = IntArray(moveCount) { moves[order[it]] }
                for (i in 0 until moveCount) moves[i] = reordered[i]
            }

            if (kotlin.math.abs(bestScore) > Evaluator.MATE - 500) break
            if (System.nanoTime() >= deadline) break
        }

        if (aborted && completedDepth == 0) {
            return SearchResult(moves[0], 0, 0, nodes)
        }

        val chosen = if (difficulty.randomness > 0 && rootScores.isNotEmpty()) {
            chooseWithNoise(rootScores, difficulty.randomness)
        } else {
            bestMove
        }
        return SearchResult(chosen, bestScore, completedDepth, nodes, rootScores)
    }

    internal fun currentHash(): Long = hash

    private fun makeMove(move: Int): Int {
        val from = Move.fromOf(move)
        val to = Move.toOf(move)
        val moving = board[from]
        val captured = MoveGenerator.make(board, move)
        hash = Zobrist.togglePiece(hash, moving, from)
        hash = Zobrist.togglePiece(hash, moving, to)
        if (captured != Pieces.EMPTY) {
            hash = Zobrist.togglePiece(hash, captured, to)
        }
        hash = Zobrist.toggleSide(hash)
        return captured
    }

    private fun unmakeMove(move: Int, captured: Int) {
        val from = Move.fromOf(move)
        val to = Move.toOf(move)
        val moving = board[to]
        hash = Zobrist.togglePiece(hash, moving, to)
        hash = Zobrist.togglePiece(hash, moving, from)
        if (captured != Pieces.EMPTY) {
            hash = Zobrist.togglePiece(hash, captured, to)
        }
        hash = Zobrist.toggleSide(hash)
        MoveGenerator.unmake(board, move, captured)
    }

    private fun chooseWithNoise(scores: List<RootScore>, noise: Int): Int {
        val best = scores.maxOf { it.score }
        val pool = scores.filter { it.score >= best - noise }.take(4)
        return if (pool.isEmpty()) scores.first().move else pool.random().move
    }

    private fun negamax(depth: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        if (aborted) return 0
        nodes++
        if ((nodes and 1023L) == 0L && checkAbort()) return 0
        if (ply >= MAX_PLY) return Evaluator.evaluate(board, sideToMove)

        var searchDepth = depth
        if (searchDepth <= 0) {
            if (MoveGenerator.isInCheck(board, sideToMove)) {
                searchDepth = 1
            } else {
                return quiesce(alphaIn, beta, ply)
            }
        }

        val key = hash
        val idx = (key and TT_MASK).toInt()
        var ttBest = -1
        if (ttKey[idx] == key) {
            ttBest = ttMove[idx]
            if (ttDepth[idx].toInt() >= searchDepth) {
                val score = ttScore[idx]
                when (ttFlag[idx].toInt()) {
                    TT_EXACT -> return score
                    TT_LOWER -> if (score >= beta) return score
                    TT_UPPER -> if (score <= alphaIn) return score
                }
            }
        }

        val moves = moveBuffers[ply]
        val moveCount = MoveGenerator.generateLegalInto(board, sideToMove, false, moves)
        if (moveCount == 0) return -Evaluator.MATE + ply

        orderMoves(moves, moveCount, ttBest, ply)
        var best = -Evaluator.INF
        var bestMove = -1
        var alpha = alphaIn
        var flag = TT_UPPER

        for (i in 0 until moveCount) {
            val move = moves[i]
            val captured = makeMove(move)
            sideToMove = sideToMove.opposite()
            val score = -negamax(searchDepth - 1, -beta, -alpha, ply + 1)
            sideToMove = sideToMove.opposite()
            unmakeMove(move, captured)
            if (aborted) return 0

            if (score > best) {
                best = score
                bestMove = move
            }
            if (score > alpha) {
                alpha = score
                flag = TT_EXACT
            }
            if (alpha >= beta) {
                flag = TT_LOWER
                recordKiller(ply, move)
                val bonus = searchDepth * searchDepth
                history[move] = minOf(history[move] + bonus, 200_000)
                break
            }
        }

        storeTt(key, idx, searchDepth, best, flag, bestMove)
        return best
    }

    private fun quiesce(alphaIn: Int, beta: Int, ply: Int): Int {
        if (aborted) return 0
        nodes++
        if ((nodes and 1023L) == 0L && checkAbort()) return 0
        if (ply >= MAX_PLY) return Evaluator.evaluate(board, sideToMove)

        val inCheck = MoveGenerator.isInCheck(board, sideToMove)
        var alpha = alphaIn
        if (!inCheck) {
            val standPat = Evaluator.evaluate(board, sideToMove)
            if (standPat >= beta) return beta
            if (standPat > alpha) alpha = standPat
        }

        val moves = moveBuffers[ply]
        val moveCount = MoveGenerator.generateLegalInto(board, sideToMove, !inCheck, moves)
        if (moveCount == 0) {
            return if (inCheck) -Evaluator.MATE + ply else alpha
        }
        orderMoves(moves, moveCount, -1, ply)

        for (i in 0 until moveCount) {
            val move = moves[i]
            val captured = makeMove(move)
            sideToMove = sideToMove.opposite()
            val score = -quiesce(-beta, -alpha, ply + 1)
            sideToMove = sideToMove.opposite()
            unmakeMove(move, captured)
            if (aborted) return 0
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }
        return alpha
    }

    private fun orderMoves(moves: IntArray, count: Int, ttMove: Int, ply: Int) {
        if (count <= 1) return
        val scores = orderScores
        val killerA = if (ply < MAX_PLY) killers[ply * 2] else -1
        val killerB = if (ply < MAX_PLY) killers[ply * 2 + 1] else -1
        for (i in 0 until count) {
            val move = moves[i]
            val from = move / Board.SIZE
            val to = move % Board.SIZE
            val target = board[to]
            val attacker = board[from]
            scores[i] = when {
                move == ttMove -> 1_000_000
                target != Pieces.EMPTY ->
                    100_000 + Evaluator.value(target) * 16 - Evaluator.value(attacker)
                move == killerA -> 90_000
                move == killerB -> 80_000
                else -> history[move]
            }
        }
        for (i in 1 until count) {
            val move = moves[i]
            val score = scores[i]
            var j = i - 1
            while (j >= 0 && scores[j] < score) {
                moves[j + 1] = moves[j]
                scores[j + 1] = scores[j]
                j--
            }
            moves[j + 1] = move
            scores[j + 1] = score
        }
    }

    private fun recordKiller(ply: Int, move: Int) {
        if (ply >= MAX_PLY) return
        val index = ply * 2
        if (killers[index] != move) {
            killers[index + 1] = killers[index]
            killers[index] = move
        }
    }

    private fun storeTt(key: Long, idx: Int, depth: Int, score: Int, flag: Int, move: Int) {
        ttKey[idx] = key
        ttScore[idx] = score
        ttDepth[idx] = depth.coerceIn(0, 127).toByte()
        ttFlag[idx] = flag.toByte()
        ttMove[idx] = move
    }

    private fun checkAbort(): Boolean {
        if (!aborted && (nodes >= nodeLimit || System.nanoTime() >= deadline || cancelCheck())) {
            aborted = true
        }
        return aborted
    }

    companion object {
        private const val MAX_PLY = 64
        private const val TT_SIZE = 1 shl 19
        private const val TT_MASK = (TT_SIZE - 1).toLong()
        private const val TT_EXACT = 0
        private const val TT_LOWER = 1
        private const val TT_UPPER = 2
    }
}
