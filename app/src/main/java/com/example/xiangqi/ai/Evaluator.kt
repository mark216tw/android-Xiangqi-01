package com.example.xiangqi.ai

import com.example.xiangqi.model.Board
import com.example.xiangqi.model.PieceType
import com.example.xiangqi.model.Pieces
import com.example.xiangqi.model.Side

object Evaluator {
    const val MATE = 30000
    const val INF = 1_000_000

    private val values = IntArray(16)
    private val pst = Array(16) { IntArray(Board.SIZE) }

    init {
        values[Pieces.RED_KING] = 0
        values[Pieces.RED_ADVISOR] = 200
        values[Pieces.RED_ELEPHANT] = 200
        values[Pieces.RED_HORSE] = 400
        values[Pieces.RED_ROOK] = 900
        values[Pieces.RED_CANNON] = 450
        values[Pieces.RED_PAWN] = 100
        for (i in 1..7) values[8 + i] = values[i]

        for (p in 1 until 16) {
            val side = Pieces.sideOf(p) ?: continue
            val type = Pieces.typeOf(p) ?: continue
            val redBase = p <= 7
            for (row in 0 until Board.ROWS) {
                for (col in 0 until Board.COLS) {
                    val normalizedRow = if (redBase) row else 9 - row
                    pst[p][Board.sq(row, col)] = positional(type, normalizedRow, col)
                }
            }
        }
    }

    fun value(code: Int): Int = if (code in values.indices) values[code] else 0

    private fun positional(type: PieceType, row: Int, col: Int): Int {
        val center = 4 - kotlin.math.abs(col - 4)
        return when (type) {
            PieceType.KING -> if (row >= 8) 10 else 0
            PieceType.ADVISOR -> 0
            PieceType.ELEPHANT -> 0
            PieceType.HORSE -> center * 5 + if (row in 2..7) 10 else 0
            PieceType.ROOK -> center * 3 + if (row <= 6) (6 - row) * 3 else 0
            PieceType.CANNON -> center * 4 + if (row <= 6) (6 - row) * 2 else 0
            PieceType.PAWN -> {
                var score = center * 2
                if (row <= 4) {
                    score += 40 + (4 - row) * 8
                } else {
                    score += (6 - row) * 3
                }
                score
            }
        }
    }

    fun evaluate(board: IntArray, side: Side): Int {
        var red = 0
        var black = 0
        for (i in board.indices) {
            val p = board[i]
            if (p == Pieces.EMPTY) continue
            val score = values[p] + pst[p][i]
            if (p <= 7) red += score else black += score
        }
        val diff = red - black
        return if (side == Side.RED) diff else -diff
    }
}
