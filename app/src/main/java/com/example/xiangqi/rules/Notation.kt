package com.example.xiangqi.rules

import com.example.xiangqi.model.Board
import com.example.xiangqi.model.PieceType
import com.example.xiangqi.model.Pieces
import com.example.xiangqi.model.Side

object Notation {
    private val DIGITS = charArrayOf('一', '二', '三', '四', '五', '六', '七', '八', '九')

    fun digit(n: Int): String = if (n in 1..9) DIGITS[n - 1].toString() else n.toString()

    fun fileNumber(side: Side, col: Int): Int = when (side) {
        Side.RED -> 9 - col
        Side.BLACK -> col + 1
    }

    fun move(board: IntArray, side: Side, from: Int, to: Int): String {
        val piece = board[from]
        val type = Pieces.typeOf(piece) ?: return ""
        val fromRow = Board.row(from)
        val fromCol = Board.col(from)
        val toRow = Board.row(to)
        val toCol = Board.col(to)

        val sameColumn = (0 until Board.ROWS)
            .map { Board.sq(it, fromCol) }
            .filter { board[it] == piece }
        val prefix = when {
            sameColumn.size <= 1 -> digit(fileNumber(side, fromCol))
            sameColumn.size == 2 -> {
                val ordered = sameColumn.sortedBy { if (side == Side.RED) Board.row(it) else -Board.row(it) }
                if (ordered[0] == from) "前" else "後"
            }
            sameColumn.size == 3 -> {
                val ordered = sameColumn.sortedBy { if (side == Side.RED) Board.row(it) else -Board.row(it) }
                when (ordered.indexOf(from)) {
                    0 -> "前"
                    1 -> "中"
                    else -> "後"
                }
            }
            else -> {
                val ordered = sameColumn.sortedBy { if (side == Side.RED) Board.row(it) else -Board.row(it) }
                digit(ordered.indexOf(from) + 1)
            }
        }

        val name = Pieces.name(piece)
        val head = if (prefix in setOf("前", "中", "後") || prefix.length == 1 && prefix[0].isDigit()) {
            prefix + name
        } else {
            name + prefix
        }
        if (fromRow == toRow) {
            return head + "平" + digit(fileNumber(side, toCol))
        }

        val forward = if (side == Side.RED) toRow < fromRow else toRow > fromRow
        val direction = if (forward) "進" else "退"
        val value = when (type) {
            PieceType.HORSE, PieceType.ELEPHANT, PieceType.ADVISOR ->
                digit(fileNumber(side, toCol))
            else -> digit(kotlin.math.abs(toRow - fromRow))
        }
        return head + direction + value
    }
}
