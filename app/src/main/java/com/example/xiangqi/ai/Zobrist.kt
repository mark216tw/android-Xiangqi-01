package com.example.xiangqi.ai

import com.example.xiangqi.model.Board
import com.example.xiangqi.model.Side
import java.util.Random

object Zobrist {
    private val pieceKeys = Array(16) { LongArray(Board.SIZE) }
    private val sideKey: Long

    init {
        val random = Random(0x5EED5EEDL)
        for (p in 1 until 16) {
            for (s in 0 until Board.SIZE) {
                pieceKeys[p][s] = random.nextLong()
            }
        }
        sideKey = random.nextLong()
    }

    fun hash(board: IntArray, side: Side): Long {
        var h = 0L
        for (i in board.indices) {
            val p = board[i]
            if (p != 0) h = h xor pieceKeys[p][i]
        }
        if (side == Side.BLACK) h = h xor sideKey
        return h
    }

    fun togglePiece(hash: Long, piece: Int, square: Int): Long = hash xor pieceKeys[piece][square]

    fun toggleSide(hash: Long): Long = hash xor sideKey
}
