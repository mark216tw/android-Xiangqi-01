package com.example.xiangqi.model

data class Move(val from: Int, val to: Int) {
    val encoded: Int get() = from * Board.SIZE + to

    companion object {
        const val NONE = -1

        fun encode(from: Int, to: Int): Int = from * Board.SIZE + to
        fun fromOf(move: Int): Int = move / Board.SIZE
        fun toOf(move: Int): Int = move % Board.SIZE
    }
}

enum class Outcome { ONGOING, RED_WIN, BLACK_WIN, DRAW, ABORTED }

data class GameResult(val outcome: Outcome = Outcome.ONGOING, val reason: String = "") {
    val isOver: Boolean get() = outcome != Outcome.ONGOING
    val winner: Side?
        get() = when (outcome) {
            Outcome.RED_WIN -> Side.RED
            Outcome.BLACK_WIN -> Side.BLACK
            else -> null
        }
}

data class MoveRecord(
    val move: Int,
    val mover: Side,
    val piece: Int,
    val captured: Int,
    val notation: String,
    val gaveCheck: Boolean
) {
    val from: Int get() = Move.fromOf(move)
    val to: Int get() = Move.toOf(move)
}
