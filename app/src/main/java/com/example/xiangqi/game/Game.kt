package com.example.xiangqi.game

import com.example.xiangqi.ai.Zobrist
import com.example.xiangqi.model.Board
import com.example.xiangqi.model.GameResult
import com.example.xiangqi.model.Move
import com.example.xiangqi.model.MoveRecord
import com.example.xiangqi.model.Outcome
import com.example.xiangqi.model.PieceType
import com.example.xiangqi.model.Pieces
import com.example.xiangqi.model.Side
import com.example.xiangqi.rules.MoveGenerator
import com.example.xiangqi.rules.Notation

class Game(val humanSide: Side = Side.RED) {
    var board: IntArray = Pieces.initialBoard()
        private set
    var sideToMove: Side = Side.RED
        private set
    var result: GameResult = GameResult()
        private set
    var halfMoveClock: Int = 0
        private set
    var fullMoveNumber: Int = 1
        private set

    val moves = mutableListOf<MoveRecord>()
    private val snapshots = mutableListOf<Snapshot>()
    private var positionCounts = mutableMapOf<Long, Int>()

    init {
        positionCounts[currentHash()] = 1
    }

    data class Snapshot(
        val board: IntArray,
        val sideToMove: Side,
        val result: GameResult,
        val halfMoveClock: Int,
        val fullMoveNumber: Int,
        val movesSize: Int,
        val positionCounts: Map<Long, Int>
    )

    fun currentHash(): Long = Zobrist.hash(board, sideToMove)

    fun legalTargets(from: Int): IntArray {
        if (result.isOver) return IntArray(0)
        return MoveGenerator.legalTargets(board, sideToMove, from)
    }

    fun legalMoves(): IntArray {
        if (result.isOver) return IntArray(0)
        return MoveGenerator.generateLegal(board, sideToMove)
    }

    fun hasLegalMove(): Boolean = legalMoves().isNotEmpty()

    fun inCheck(side: Side = sideToMove): Boolean = MoveGenerator.isInCheck(board, side)

    fun lastMove(): MoveRecord? = moves.lastOrNull()

    fun play(from: Int, to: Int): MoveRecord? {
        if (result.isOver) return null
        if (from !in 0 until Board.SIZE || to !in 0 until Board.SIZE) return null
        if (Pieces.sideOf(board[from]) != sideToMove) return null
        if (!legalTargets(from).contains(to)) return null

        val mover = sideToMove
        val piece = board[from]
        val notation = Notation.move(board, mover, from, to)
        val captured = board[to]
        snapshots.add(
            Snapshot(
                board = board.copyOf(),
                sideToMove = sideToMove,
                result = result,
                halfMoveClock = halfMoveClock,
                fullMoveNumber = fullMoveNumber,
                movesSize = moves.size,
                positionCounts = positionCounts.toMap()
            )
        )

        val encoded = Move.encode(from, to)
        MoveGenerator.make(board, encoded)
        halfMoveClock = if (captured != Pieces.EMPTY) 0 else halfMoveClock + 1
        if (mover == Side.BLACK) fullMoveNumber++
        sideToMove = sideToMove.opposite()

        val hash = currentHash()
        val count = (positionCounts[hash] ?: 0) + 1
        positionCounts[hash] = count

        val checking = MoveGenerator.isInCheck(board, sideToMove)
        val record = MoveRecord(
            move = encoded,
            mover = mover,
            piece = piece,
            captured = captured,
            notation = notation,
            gaveCheck = checking
        )
        moves.add(record)

        val replies = MoveGenerator.generateLegal(board, sideToMove)
        when {
            replies.isEmpty() -> {
                val winnerOutcome = if (mover == Side.RED) Outcome.RED_WIN else Outcome.BLACK_WIN
                result = GameResult(winnerOutcome, if (checking) "將死" else "困斃")
            }
            count >= 3 -> result = GameResult(Outcome.DRAW, "三次重複局面")
            halfMoveClock >= 120 -> result = GameResult(Outcome.DRAW, "60 回合無吃子")
        }
        return record
    }

    fun undo(count: Int = 1): Boolean {
        var undone = 0
        while (undone < count && snapshots.isNotEmpty()) {
            val snapshot = snapshots.removeAt(snapshots.size - 1)
            board = snapshot.board
            sideToMove = snapshot.sideToMove
            result = snapshot.result
            halfMoveClock = snapshot.halfMoveClock
            fullMoveNumber = snapshot.fullMoveNumber
            positionCounts = snapshot.positionCounts.toMutableMap()
            while (moves.size > snapshot.movesSize) moves.removeAt(moves.size - 1)
            undone++
        }
        return undone > 0
    }

    fun canUndo(): Boolean = snapshots.isNotEmpty()

    fun resign(side: Side) {
        if (result.isOver) return
        val outcome = if (side == Side.RED) Outcome.BLACK_WIN else Outcome.RED_WIN
        result = GameResult(outcome, "認輸")
    }

    fun timeout(side: Side) {
        if (result.isOver) return
        val outcome = if (side == Side.RED) Outcome.BLACK_WIN else Outcome.RED_WIN
        result = GameResult(outcome, "超時")
    }

    fun abort() {
        if (result.isOver) return
        result = GameResult(Outcome.ABORTED, "手動結束")
    }

    fun pieceTypeOf(code: Int): PieceType? = Pieces.typeOf(code)
}
