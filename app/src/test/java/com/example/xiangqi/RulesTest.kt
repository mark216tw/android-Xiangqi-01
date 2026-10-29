package com.example.xiangqi

import com.example.xiangqi.ai.Difficulty
import com.example.xiangqi.ai.Evaluator
import com.example.xiangqi.ai.SearchEngine
import com.example.xiangqi.ai.Zobrist
import com.example.xiangqi.model.Board
import com.example.xiangqi.model.Move
import com.example.xiangqi.model.Outcome
import com.example.xiangqi.model.Pieces
import com.example.xiangqi.model.Side
import com.example.xiangqi.rules.MoveGenerator
import com.example.xiangqi.rules.Notation
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
    private fun emptyBoard(): IntArray = IntArray(Board.SIZE)

    private fun put(board: IntArray, row: Int, col: Int, piece: Int) {
        board[Board.sq(row, col)] = piece
    }

    private fun perft(board: IntArray, side: Side, depth: Int): Long {
        if (depth == 0) return 1L
        val moves = MoveGenerator.generateLegal(board, side)
        if (depth == 1) return moves.size.toLong()
        var total = 0L
        for (move in moves) {
            val captured = MoveGenerator.make(board, move)
            total += perft(board, side.opposite(), depth - 1)
            MoveGenerator.unmake(board, move, captured)
        }
        return total
    }

    @Test
    fun initialPositionPerft() {
        val board = Pieces.initialBoard()
        assertEquals(44L, perft(board, Side.RED, 1))
        assertEquals(1920L, perft(board, Side.RED, 2))
        assertEquals(79666L, perft(board, Side.RED, 3))
    }

    @Test
    fun flyingGeneralIsCheck() {
        val board = emptyBoard()
        put(board, 9, 4, Pieces.RED_KING)
        put(board, 0, 4, Pieces.BLACK_KING)
        assertTrue(MoveGenerator.isInCheck(board, Side.RED))
        assertTrue(MoveGenerator.isInCheck(board, Side.BLACK))
    }

    @Test
    fun cannotExposeFlyingGeneral() {
        val board = emptyBoard()
        put(board, 9, 4, Pieces.RED_KING)
        put(board, 0, 4, Pieces.BLACK_KING)
        put(board, 5, 4, Pieces.RED_ROOK)
        val targets = MoveGenerator.legalTargets(board, Side.RED, Board.sq(5, 4))
        assertFalse(targets.contains(Board.sq(5, 3)))
        assertTrue(targets.contains(Board.sq(4, 4)))
        assertTrue(targets.contains(Board.sq(6, 4)))
    }

    @Test
    fun horseLegBlocksMove() {
        val board = emptyBoard()
        put(board, 9, 4, Pieces.RED_KING)
        put(board, 0, 3, Pieces.BLACK_KING)
        put(board, 9, 1, Pieces.RED_HORSE)
        put(board, 8, 1, Pieces.RED_PAWN)
        val targets = MoveGenerator.legalTargets(board, Side.RED, Board.sq(9, 1))
        assertFalse(targets.contains(Board.sq(7, 0)))
        assertFalse(targets.contains(Board.sq(7, 2)))
        assertTrue(targets.contains(Board.sq(8, 3)))
    }

    @Test
    fun elephantEyeAndRiver() {
        val board = emptyBoard()
        put(board, 9, 4, Pieces.RED_KING)
        put(board, 0, 3, Pieces.BLACK_KING)
        put(board, 9, 2, Pieces.RED_ELEPHANT)
        put(board, 8, 1, Pieces.RED_PAWN)
        val targets = MoveGenerator.legalTargets(board, Side.RED, Board.sq(9, 2))
        assertFalse(targets.contains(Board.sq(7, 0)))
        assertTrue(targets.contains(Board.sq(7, 4)))

        val board2 = emptyBoard()
        put(board2, 9, 4, Pieces.RED_KING)
        put(board2, 0, 3, Pieces.BLACK_KING)
        put(board2, 5, 4, Pieces.RED_ELEPHANT)
        val targets2 = MoveGenerator.legalTargets(board2, Side.RED, Board.sq(5, 4))
        assertTrue(targets2.isNotEmpty())
        for (t in targets2) assertTrue(Board.row(t) >= 5)
    }

    @Test
    fun cannonNeedsScreenToCapture() {
        val board = emptyBoard()
        put(board, 9, 4, Pieces.RED_KING)
        put(board, 0, 3, Pieces.BLACK_KING)
        put(board, 7, 1, Pieces.RED_CANNON)
        put(board, 5, 1, Pieces.RED_PAWN)
        put(board, 3, 1, Pieces.BLACK_ROOK)
        val targets = MoveGenerator.legalTargets(board, Side.RED, Board.sq(7, 1))
        assertFalse(targets.contains(Board.sq(5, 1)))
        assertTrue(targets.contains(Board.sq(3, 1)))
        assertTrue(targets.contains(Board.sq(6, 1)))
    }

    @Test
    fun checkmatePositionHasNoMoves() {
        val board = emptyBoard()
        put(board, 0, 4, Pieces.BLACK_KING)
        put(board, 1, 4, Pieces.RED_ROOK)
        put(board, 2, 4, Pieces.RED_PAWN)
        put(board, 0, 0, Pieces.RED_ROOK)
        put(board, 0, 8, Pieces.RED_ROOK)
        put(board, 9, 4, Pieces.RED_KING)
        assertTrue(MoveGenerator.isInCheck(board, Side.BLACK))
        assertEquals(0, MoveGenerator.generateLegal(board, Side.BLACK).size)
    }

    @Test
    fun notationUsesChineseFiles() {
        val board = Pieces.initialBoard()
        assertEquals("炮二平五", Notation.move(board, Side.RED, Board.sq(7, 7), Board.sq(7, 4)))
        assertEquals("馬八進七", Notation.move(board, Side.RED, Board.sq(9, 1), Board.sq(7, 2)))
        assertEquals("卒七進一", Notation.move(board, Side.BLACK, Board.sq(3, 6), Board.sq(4, 6)))
    }

    @Test
    fun engineFindsMateInOne() {
        val board = emptyBoard()
        put(board, 0, 4, Pieces.BLACK_KING)
        put(board, 1, 3, Pieces.RED_ROOK)
        put(board, 0, 0, Pieces.RED_ROOK)
        put(board, 0, 8, Pieces.RED_ROOK)
        put(board, 2, 4, Pieces.RED_PAWN)
        put(board, 9, 4, Pieces.RED_KING)

        val engine = SearchEngine()
        val result = engine.search(
            board,
            Side.RED,
            Difficulty.MEDIUM,
            maxNodes = 200_000,
            maxTimeMs = 5_000
        )
        assertNotNull(result)
        assertTrue(result!!.score > Evaluator.MATE - 1000)

        val after = board.copyOf()
        MoveGenerator.make(after, result.move)
        assertEquals(0, MoveGenerator.generateLegal(after, Side.BLACK).size)
        assertTrue(MoveGenerator.isInCheck(after, Side.BLACK))
    }

    @Test
    fun gamePlaysAndUndoes() {
        val game = com.example.xiangqi.game.Game(Side.RED)
        val record = game.play(Board.sq(7, 7), Board.sq(7, 4))
        assertNotNull(record)
        assertEquals("炮二平五", record!!.notation)
        assertEquals(Side.BLACK, game.sideToMove)
        assertTrue(game.undo())
        assertEquals(Side.RED, game.sideToMove)
        assertEquals(0, game.moves.size)
        assertEquals(Pieces.RED_CANNON, game.board[Board.sq(7, 7)])
        assertEquals(Pieces.EMPTY, game.board[Board.sq(7, 4)])
    }

    @Test
    fun threefoldRepetitionIsDraw() {
        val game = com.example.xiangqi.game.Game(Side.RED)
        val cycle = listOf(
            Board.sq(9, 1) to Board.sq(7, 2),
            Board.sq(0, 1) to Board.sq(2, 2),
            Board.sq(7, 2) to Board.sq(9, 1),
            Board.sq(2, 2) to Board.sq(0, 1)
        )
        repeat(2) {
            for ((from, to) in cycle) {
                assertNotNull(game.play(from, to))
            }
        }
        assertEquals(Outcome.DRAW, game.result.outcome)
        assertEquals("三次重複局面", game.result.reason)
    }

    @Test
    fun abortGameRecordsAbortedOutcome() {
        val game = com.example.xiangqi.game.Game(Side.RED)
        assertNotNull(game.play(Board.sq(7, 7), Board.sq(7, 4)))
        game.abort()
        assertEquals(Outcome.ABORTED, game.result.outcome)
        assertEquals("手動結束", game.result.reason)
        assertTrue(game.result.isOver)
        assertEquals(null, game.result.winner)
    }

    @Test
    fun engineReturnsLegalMoveFromInitialPosition() {
        val board = Pieces.initialBoard()
        val engine = SearchEngine()
        val result = engine.search(
            board,
            Side.RED,
            Difficulty.MEDIUM,
            maxNodes = 50_000,
            maxTimeMs = 5_000
        )
        assertNotNull(result)
        val legal = MoveGenerator.generateLegal(board, Side.RED).toSet()
        assertTrue(legal.contains(result!!.move))
    }

    @Test
    fun generateLegalIntoMatchesAllocatingVersion() {
        val board = Pieces.initialBoard()
        val expected = MoveGenerator.generateLegal(board, Side.RED)
        val out = IntArray(MoveGenerator.MAX_MOVES)
        val count = MoveGenerator.generateLegalInto(board, Side.RED, false, out)
        assertEquals(expected.size, count)
        for (i in 0 until count) assertEquals(expected[i], out[i])
    }

    @Test
    fun generateLegalIntoMatchesCapturesOnlyVersion() {
        val board = emptyBoard()
        put(board, 9, 4, Pieces.RED_KING)
        put(board, 0, 3, Pieces.BLACK_KING)
        put(board, 7, 1, Pieces.RED_CANNON)
        put(board, 5, 1, Pieces.RED_PAWN)
        put(board, 3, 1, Pieces.BLACK_ROOK)
        val expected = MoveGenerator.generateLegal(board, Side.RED, capturesOnly = true)
        val out = IntArray(MoveGenerator.MAX_MOVES)
        val count = MoveGenerator.generateLegalInto(board, Side.RED, true, out)
        assertEquals(expected.size, count)
        for (i in 0 until count) assertEquals(expected[i], out[i])
    }

    @Test
    fun incrementalHashRestoredAfterSearch() {
        val board = Pieces.initialBoard()
        val engine = SearchEngine()
        assertNotNull(
            engine.search(
                board,
                Side.RED,
                Difficulty.EASY,
                maxNodes = 50_000,
                maxTimeMs = 5_000
            )
        )
        assertEquals(Zobrist.hash(board, Side.RED), engine.currentHash())
        assertArrayEquals(Pieces.initialBoard(), board)
    }

    @Test
    fun zobristToggleMatchesAfterCapture() {
        val board = emptyBoard()
        put(board, 9, 4, Pieces.RED_KING)
        put(board, 0, 3, Pieces.BLACK_KING)
        put(board, 7, 1, Pieces.RED_CANNON)
        put(board, 5, 1, Pieces.RED_PAWN)
        put(board, 3, 1, Pieces.BLACK_ROOK)

        val move = Move.encode(Board.sq(7, 1), Board.sq(3, 1))
        val from = Move.fromOf(move)
        val to = Move.toOf(move)
        val moving = board[from]
        var hash = Zobrist.hash(board, Side.RED)
        val captured = MoveGenerator.make(board, move)
        hash = Zobrist.togglePiece(hash, moving, from)
        hash = Zobrist.togglePiece(hash, moving, to)
        if (captured != Pieces.EMPTY) {
            hash = Zobrist.togglePiece(hash, captured, to)
        }
        hash = Zobrist.toggleSide(hash)
        assertEquals(Zobrist.hash(board, Side.BLACK), hash)
    }
}
