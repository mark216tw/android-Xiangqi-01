package com.example.xiangqi.rules

import com.example.xiangqi.model.Board
import com.example.xiangqi.model.Move
import com.example.xiangqi.model.PieceType
import com.example.xiangqi.model.Pieces
import com.example.xiangqi.model.Side

object MoveGenerator {
    const val MAX_MOVES = 192

    private val ORTHO = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
    private val DIAG = arrayOf(intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1))
    private val HORSE = arrayOf(
        intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(2, -1), intArrayOf(2, 1),
        intArrayOf(-1, -2), intArrayOf(1, -2), intArrayOf(-1, 2), intArrayOf(1, 2)
    )
    private val ELEPHANT = arrayOf(
        intArrayOf(-2, -2), intArrayOf(-2, 2), intArrayOf(2, -2), intArrayOf(2, 2)
    )
    private val PAWN_SIDES = intArrayOf(-1, 1)

    fun generate(board: IntArray, side: Side, capturesOnly: Boolean = false): IntArray {
        val out = IntArray(MAX_MOVES)
        val n = generateInto(board, side, capturesOnly, out)
        return out.copyOf(n)
    }

    fun generateInto(
        board: IntArray,
        side: Side,
        capturesOnly: Boolean = false,
        out: IntArray
    ): Int {
        var n = 0
        for (sq in 0 until Board.SIZE) {
            val piece = board[sq]
            if (Pieces.sideOf(piece) != side) continue
            val type = Pieces.typeOf(piece) ?: continue
            val row = Board.row(sq)
            val col = Board.col(sq)
            when (type) {
                PieceType.ROOK -> n = genRook(board, sq, row, col, side, capturesOnly, out, n)
                PieceType.CANNON -> n = genCannon(board, sq, row, col, side, capturesOnly, out, n)
                PieceType.HORSE -> n = genHorse(board, sq, row, col, side, capturesOnly, out, n)
                PieceType.ELEPHANT -> n = genElephant(board, sq, row, col, side, capturesOnly, out, n)
                PieceType.ADVISOR -> n = genAdvisor(board, sq, row, col, side, capturesOnly, out, n)
                PieceType.KING -> n = genKing(board, sq, row, col, side, capturesOnly, out, n)
                PieceType.PAWN -> n = genPawn(board, sq, row, col, side, capturesOnly, out, n)
            }
        }
        return n
    }

    fun generateLegal(board: IntArray, side: Side, capturesOnly: Boolean = false): IntArray {
        val out = IntArray(MAX_MOVES)
        val n = generateLegalInto(board, side, capturesOnly, out)
        return out.copyOf(n)
    }

    fun generateLegalInto(
        board: IntArray,
        side: Side,
        capturesOnly: Boolean = false,
        out: IntArray
    ): Int {
        val n = generateInto(board, side, capturesOnly, out)
        val king = kingSquare(board, side)
        var w = 0
        for (i in 0 until n) {
            val move = out[i]
            if (isLegalAfterMove(board, move, side, king)) {
                out[w++] = move
            }
        }
        return w
    }

    fun legalTargets(board: IntArray, side: Side, from: Int): IntArray {
        val all = generateLegal(board, side)
        val out = IntArray(all.size)
        var n = 0
        for (m in all) {
            if (Move.fromOf(m) == from) out[n++] = Move.toOf(m)
        }
        return out.copyOf(n)
    }

    fun isLegalAfterMove(board: IntArray, move: Int, side: Side): Boolean {
        return isLegalAfterMove(board, move, side, kingSquare(board, side))
    }

    private fun isLegalAfterMove(board: IntArray, move: Int, side: Side, king: Int): Boolean {
        val from = Move.fromOf(move)
        val to = Move.toOf(move)
        if (from == to) return false
        if (Pieces.sideOf(board[from]) != side) return false
        val captured = make(board, move)
        val kingAfter = if (from == king) to else king
        val legal = kingAfter >= 0 && !isAttacked(board, kingAfter, side.opposite())
        unmake(board, move, captured)
        return legal
    }

    fun make(board: IntArray, move: Int): Int {
        val from = Move.fromOf(move)
        val to = Move.toOf(move)
        val captured = board[to]
        board[to] = board[from]
        board[from] = Pieces.EMPTY
        return captured
    }

    fun unmake(board: IntArray, move: Int, captured: Int) {
        val from = Move.fromOf(move)
        val to = Move.toOf(move)
        board[from] = board[to]
        board[to] = captured
    }

    fun kingSquare(board: IntArray, side: Side): Int {
        val king = Pieces.code(side, PieceType.KING)
        for (i in board.indices) {
            if (board[i] == king) return i
        }
        return -1
    }

    fun isInCheck(board: IntArray, side: Side): Boolean {
        val king = kingSquare(board, side)
        if (king < 0) return true
        return isAttacked(board, king, side.opposite())
    }

    fun isAttacked(board: IntArray, square: Int, by: Side): Boolean {
        val row = Board.row(square)
        val col = Board.col(square)

        val enemyRook = Pieces.code(by, PieceType.ROOK)
        val enemyCannon = Pieces.code(by, PieceType.CANNON)
        val enemyHorse = Pieces.code(by, PieceType.HORSE)
        val enemyPawn = Pieces.code(by, PieceType.PAWN)
        val enemyKing = Pieces.code(by, PieceType.KING)
        val enemyAdvisor = Pieces.code(by, PieceType.ADVISOR)
        val enemyElephant = Pieces.code(by, PieceType.ELEPHANT)

        for (d in ORTHO) {
            var r = row + d[0]
            var c = col + d[1]
            while (r in 0 until Board.ROWS && c in 0 until Board.COLS) {
                val p = board[Board.sq(r, c)]
                if (p != Pieces.EMPTY) {
                    if (p == enemyRook) return true
                    break
                }
                r += d[0]
                c += d[1]
            }
        }

        for (d in ORTHO) {
            var r = row + d[0]
            var c = col + d[1]
            var screen = false
            while (r in 0 until Board.ROWS && c in 0 until Board.COLS) {
                val p = board[Board.sq(r, c)]
                if (p != Pieces.EMPTY) {
                    if (!screen) {
                        screen = true
                    } else {
                        if (p == enemyCannon) return true
                        break
                    }
                }
                r += d[0]
                c += d[1]
            }
        }

        for (d in HORSE) {
            val hr = row - d[0]
            val hc = col - d[1]
            if (hr !in 0 until Board.ROWS || hc !in 0 until Board.COLS) continue
            if (board[Board.sq(hr, hc)] != enemyHorse) continue
            val legR = hr + if (d[0] == 2) 1 else if (d[0] == -2) -1 else 0
            val legC = hc + if (d[1] == 2) 1 else if (d[1] == -2) -1 else 0
            if (board[Board.sq(legR, legC)] == Pieces.EMPTY) return true
        }

        for (d in ELEPHANT) {
            val er = row - d[0]
            val ec = col - d[1]
            if (er !in 0 until Board.ROWS || ec !in 0 until Board.COLS) continue
            if (board[Board.sq(er, ec)] != enemyElephant) continue
            if (!Board.onOwnHalf(er, by) || !Board.onOwnHalf(row, by)) continue
            val eyeR = er + d[0] / 2
            val eyeC = ec + d[1] / 2
            if (board[Board.sq(eyeR, eyeC)] == Pieces.EMPTY) return true
        }

        for (d in DIAG) {
            val ar = row + d[0]
            val ac = col + d[1]
            if (ar !in 0 until Board.ROWS || ac !in 0 until Board.COLS) continue
            val p = board[Board.sq(ar, ac)]
            if (p == enemyAdvisor && Board.inPalace(ar, ac, by)) return true
        }

        for (d in ORTHO) {
            val ar = row + d[0]
            val ac = col + d[1]
            if (ar !in 0 until Board.ROWS || ac !in 0 until Board.COLS) continue
            if (board[Board.sq(ar, ac)] == enemyKing) return true
        }

        when (by) {
            Side.RED -> {
                val behind = if (row + 1 < Board.ROWS) Board.sq(row + 1, col) else -1
                if (behind >= 0 && board[behind] == enemyPawn) return true
                if (row <= 4) {
                    if (col - 1 >= 0 && board[Board.sq(row, col - 1)] == enemyPawn) return true
                    if (col + 1 < Board.COLS && board[Board.sq(row, col + 1)] == enemyPawn) return true
                }
            }
            Side.BLACK -> {
                val behind = if (row - 1 >= 0) Board.sq(row - 1, col) else -1
                if (behind >= 0 && board[behind] == enemyPawn) return true
                if (row >= 5) {
                    if (col - 1 >= 0 && board[Board.sq(row, col - 1)] == enemyPawn) return true
                    if (col + 1 < Board.COLS && board[Board.sq(row, col + 1)] == enemyPawn) return true
                }
            }
        }

        var r = row - 1
        while (r >= 0) {
            val p = board[Board.sq(r, col)]
            if (p != Pieces.EMPTY) {
                if (p == enemyKing) return true
                break
            }
            r--
        }
        r = row + 1
        while (r < Board.ROWS) {
            val p = board[Board.sq(r, col)]
            if (p != Pieces.EMPTY) {
                if (p == enemyKing) return true
                break
            }
            r++
        }

        return false
    }

    private fun add(out: IntArray, n: Int, from: Int, to: Int): Int {
        out[n] = Move.encode(from, to)
        return n + 1
    }

    private fun canLand(board: IntArray, to: Int, side: Side, capturesOnly: Boolean): Boolean {
        val target = board[to]
        if (target == Pieces.EMPTY) return !capturesOnly
        return Pieces.sideOf(target) != side
    }

    private fun genRook(
        board: IntArray, from: Int, row: Int, col: Int, side: Side,
        capturesOnly: Boolean, out: IntArray, start: Int
    ): Int {
        var n = start
        for (d in ORTHO) {
            var r = row + d[0]
            var c = col + d[1]
            while (r in 0 until Board.ROWS && c in 0 until Board.COLS) {
                val to = Board.sq(r, c)
                if (board[to] == Pieces.EMPTY) {
                    if (!capturesOnly) n = add(out, n, from, to)
                } else {
                    if (Pieces.sideOf(board[to]) != side) n = add(out, n, from, to)
                    break
                }
                r += d[0]
                c += d[1]
            }
        }
        return n
    }

    private fun genCannon(
        board: IntArray, from: Int, row: Int, col: Int, side: Side,
        capturesOnly: Boolean, out: IntArray, start: Int
    ): Int {
        var n = start
        for (d in ORTHO) {
            var r = row + d[0]
            var c = col + d[1]
            var screen = false
            while (r in 0 until Board.ROWS && c in 0 until Board.COLS) {
                val to = Board.sq(r, c)
                val p = board[to]
                if (!screen) {
                    if (p == Pieces.EMPTY) {
                        if (!capturesOnly) n = add(out, n, from, to)
                    } else {
                        screen = true
                    }
                } else {
                    if (p != Pieces.EMPTY) {
                        if (Pieces.sideOf(p) != side) n = add(out, n, from, to)
                        break
                    }
                }
                r += d[0]
                c += d[1]
            }
        }
        return n
    }

    private fun genHorse(
        board: IntArray, from: Int, row: Int, col: Int, side: Side,
        capturesOnly: Boolean, out: IntArray, start: Int
    ): Int {
        var n = start
        for (d in HORSE) {
            val r = row + d[0]
            val c = col + d[1]
            if (r !in 0 until Board.ROWS || c !in 0 until Board.COLS) continue
            val legR = row + if (d[0] == 2) 1 else if (d[0] == -2) -1 else 0
            val legC = col + if (d[1] == 2) 1 else if (d[1] == -2) -1 else 0
            if (board[Board.sq(legR, legC)] != Pieces.EMPTY) continue
            val to = Board.sq(r, c)
            if (canLand(board, to, side, capturesOnly)) n = add(out, n, from, to)
        }
        return n
    }

    private fun genElephant(
        board: IntArray, from: Int, row: Int, col: Int, side: Side,
        capturesOnly: Boolean, out: IntArray, start: Int
    ): Int {
        var n = start
        for (d in ELEPHANT) {
            val r = row + d[0]
            val c = col + d[1]
            if (r !in 0 until Board.ROWS || c !in 0 until Board.COLS) continue
            if (!Board.onOwnHalf(r, side)) continue
            val eyeR = row + d[0] / 2
            val eyeC = col + d[1] / 2
            if (board[Board.sq(eyeR, eyeC)] != Pieces.EMPTY) continue
            val to = Board.sq(r, c)
            if (canLand(board, to, side, capturesOnly)) n = add(out, n, from, to)
        }
        return n
    }

    private fun genAdvisor(
        board: IntArray, from: Int, row: Int, col: Int, side: Side,
        capturesOnly: Boolean, out: IntArray, start: Int
    ): Int {
        var n = start
        for (d in DIAG) {
            val r = row + d[0]
            val c = col + d[1]
            if (!Board.inPalace(r, c, side)) continue
            val to = Board.sq(r, c)
            if (canLand(board, to, side, capturesOnly)) n = add(out, n, from, to)
        }
        return n
    }

    private fun genKing(
        board: IntArray, from: Int, row: Int, col: Int, side: Side,
        capturesOnly: Boolean, out: IntArray, start: Int
    ): Int {
        var n = start
        for (d in ORTHO) {
            val r = row + d[0]
            val c = col + d[1]
            if (!Board.inPalace(r, c, side)) continue
            val to = Board.sq(r, c)
            if (canLand(board, to, side, capturesOnly)) n = add(out, n, from, to)
        }
        return n
    }

    private fun genPawn(
        board: IntArray, from: Int, row: Int, col: Int, side: Side,
        capturesOnly: Boolean, out: IntArray, start: Int
    ): Int {
        var n = start
        val forward = if (side == Side.RED) -1 else 1
        val fr = row + forward
        if (fr in 0 until Board.ROWS) {
            val to = Board.sq(fr, col)
            if (canLand(board, to, side, capturesOnly)) n = add(out, n, from, to)
        }
        if (Board.crossedRiver(row, side)) {
            for (dc in PAWN_SIDES) {
                val c = col + dc
                if (c !in 0 until Board.COLS) continue
                val to = Board.sq(row, c)
                if (canLand(board, to, side, capturesOnly)) n = add(out, n, from, to)
            }
        }
        return n
    }
}
