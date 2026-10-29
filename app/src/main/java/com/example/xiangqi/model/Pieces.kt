package com.example.xiangqi.model

enum class Side {
    RED,
    BLACK;

    fun opposite(): Side = if (this == RED) BLACK else RED
}

enum class PieceType {
    KING,
    ADVISOR,
    ELEPHANT,
    HORSE,
    ROOK,
    CANNON,
    PAWN
}

object Pieces {
    const val EMPTY = 0

    const val RED_KING = 1
    const val RED_ADVISOR = 2
    const val RED_ELEPHANT = 3
    const val RED_HORSE = 4
    const val RED_ROOK = 5
    const val RED_CANNON = 6
    const val RED_PAWN = 7

    const val BLACK_KING = 9
    const val BLACK_ADVISOR = 10
    const val BLACK_ELEPHANT = 11
    const val BLACK_HORSE = 12
    const val BLACK_ROOK = 13
    const val BLACK_CANNON = 14
    const val BLACK_PAWN = 15

    fun sideOf(code: Int): Side? = when (code) {
        in RED_KING..RED_PAWN -> Side.RED
        in BLACK_KING..BLACK_PAWN -> Side.BLACK
        else -> null
    }

    fun typeOf(code: Int): PieceType? = when (code) {
        RED_KING, BLACK_KING -> PieceType.KING
        RED_ADVISOR, BLACK_ADVISOR -> PieceType.ADVISOR
        RED_ELEPHANT, BLACK_ELEPHANT -> PieceType.ELEPHANT
        RED_HORSE, BLACK_HORSE -> PieceType.HORSE
        RED_ROOK, BLACK_ROOK -> PieceType.ROOK
        RED_CANNON, BLACK_CANNON -> PieceType.CANNON
        RED_PAWN, BLACK_PAWN -> PieceType.PAWN
        else -> null
    }

    fun code(side: Side, type: PieceType): Int = when (side) {
        Side.RED -> when (type) {
            PieceType.KING -> RED_KING
            PieceType.ADVISOR -> RED_ADVISOR
            PieceType.ELEPHANT -> RED_ELEPHANT
            PieceType.HORSE -> RED_HORSE
            PieceType.ROOK -> RED_ROOK
            PieceType.CANNON -> RED_CANNON
            PieceType.PAWN -> RED_PAWN
        }
        Side.BLACK -> when (type) {
            PieceType.KING -> BLACK_KING
            PieceType.ADVISOR -> BLACK_ADVISOR
            PieceType.ELEPHANT -> BLACK_ELEPHANT
            PieceType.HORSE -> BLACK_HORSE
            PieceType.ROOK -> BLACK_ROOK
            PieceType.CANNON -> BLACK_CANNON
            PieceType.PAWN -> BLACK_PAWN
        }
    }

    fun name(code: Int): String = when (code) {
        RED_KING -> "帥"
        RED_ADVISOR -> "仕"
        RED_ELEPHANT -> "相"
        RED_HORSE -> "馬"
        RED_ROOK -> "車"
        RED_CANNON -> "炮"
        RED_PAWN -> "兵"
        BLACK_KING -> "將"
        BLACK_ADVISOR -> "士"
        BLACK_ELEPHANT -> "象"
        BLACK_HORSE -> "馬"
        BLACK_ROOK -> "車"
        BLACK_CANNON -> "砲"
        BLACK_PAWN -> "卒"
        else -> ""
    }

    fun initialBoard(): IntArray {
        val b = IntArray(Board.SIZE)
        val back = intArrayOf(
            RED_ROOK, RED_HORSE, RED_ELEPHANT, RED_ADVISOR, RED_KING,
            RED_ADVISOR, RED_ELEPHANT, RED_HORSE, RED_ROOK
        )
        val blackBack = intArrayOf(
            BLACK_ROOK, BLACK_HORSE, BLACK_ELEPHANT, BLACK_ADVISOR, BLACK_KING,
            BLACK_ADVISOR, BLACK_ELEPHANT, BLACK_HORSE, BLACK_ROOK
        )
        for (c in 0 until Board.COLS) {
            b[Board.sq(0, c)] = blackBack[c]
            b[Board.sq(9, c)] = back[c]
        }
        b[Board.sq(2, 1)] = BLACK_CANNON
        b[Board.sq(2, 7)] = BLACK_CANNON
        b[Board.sq(7, 1)] = RED_CANNON
        b[Board.sq(7, 7)] = RED_CANNON
        for (c in 0 until Board.COLS step 2) {
            b[Board.sq(3, c)] = BLACK_PAWN
            b[Board.sq(6, c)] = RED_PAWN
        }
        return b
    }
}

object Board {
    const val ROWS = 10
    const val COLS = 9
    const val SIZE = ROWS * COLS

    fun sq(row: Int, col: Int): Int = row * COLS + col
    fun row(square: Int): Int = square / COLS
    fun col(square: Int): Int = square % COLS

    fun inPalace(row: Int, col: Int, side: Side): Boolean {
        if (col !in 3..5) return false
        return when (side) {
            Side.RED -> row in 7..9
            Side.BLACK -> row in 0..2
        }
    }

    fun crossedRiver(row: Int, side: Side): Boolean = when (side) {
        Side.RED -> row <= 4
        Side.BLACK -> row >= 5
    }

    fun onOwnHalf(row: Int, side: Side): Boolean = when (side) {
        Side.RED -> row >= 5
        Side.BLACK -> row <= 4
    }
}
