package com.example.xiangqi.ui.board

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.example.xiangqi.data.BoardTheme
import com.example.xiangqi.model.Board
import com.example.xiangqi.model.Pieces
import com.example.xiangqi.model.Move
import com.example.xiangqi.ui.game.MoveAnimation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private data class Palette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val line: Color,
    val pieceFill: Color,
    val pieceBorder: Color,
    val pieceInner: Color,
    val redText: Color,
    val blackText: Color,
    val highlight: Color,
    val target: Color,
    val check: Color,
    val aiMove: Color
)

private fun paletteFor(theme: BoardTheme): Palette = when (theme) {
    BoardTheme.WOOD -> Palette(
        backgroundTop = Color(0xFFE9CB92),
        backgroundBottom = Color(0xFFCFA263),
        line = Color(0xFF6E4B2A),
        pieceFill = Color(0xFFF6E7C8),
        pieceBorder = Color(0xFF8A5A32),
        pieceInner = Color(0xFFC9A227),
        redText = Color(0xFFB23A2E),
        blackText = Color(0xFF2B2118),
        highlight = Color(0xFFC9A227),
        target = Color(0xFF2F6B5B),
        check = Color(0xFFD3402F),
        aiMove = Color(0xFF2E86AB)
    )
    BoardTheme.LIGHT -> Palette(
        backgroundTop = Color(0xFFFBF6EA),
        backgroundBottom = Color(0xFFEDE0C8),
        line = Color(0xFFA08B6C),
        pieceFill = Color(0xFFFFFFFF),
        pieceBorder = Color(0xFFB9A98F),
        pieceInner = Color(0xFFC9A227),
        redText = Color(0xFFB23A2E),
        blackText = Color(0xFF2B2118),
        highlight = Color(0xFFB78A2E),
        target = Color(0xFF2F6B5B),
        check = Color(0xFFD3402F),
        aiMove = Color(0xFF2E86AB)
    )
    BoardTheme.INK_JADE -> Palette(
        backgroundTop = Color(0xFF2C3E39),
        backgroundBottom = Color(0xFF182521),
        line = Color(0xFF8FB3A4),
        pieceFill = Color(0xFFEFE3CC),
        pieceBorder = Color(0xFF6E8B7B),
        pieceInner = Color(0xFF8FB3A4),
        redText = Color(0xFFE06A55),
        blackText = Color(0xFF17201D),
        highlight = Color(0xFFE8C15A),
        target = Color(0xFF64C3A3),
        check = Color(0xFFFF6B57),
        aiMove = Color(0xFF7FD6E8)
    )
    BoardTheme.CELADON -> Palette(
        backgroundTop = Color(0xFFE1EFE7),
        backgroundBottom = Color(0xFFC3DACC),
        line = Color(0xFF4F7A66),
        pieceFill = Color(0xFFFFFFFF),
        pieceBorder = Color(0xFF86A996),
        pieceInner = Color(0xFFB78A2E),
        redText = Color(0xFFB23A2E),
        blackText = Color(0xFF21332B),
        highlight = Color(0xFFB78A2E),
        target = Color(0xFF2F6B5B),
        check = Color(0xFFD3402F),
        aiMove = Color(0xFF2E86AB)
    )
}

@Composable
fun XiangqiBoard(
    board: IntArray,
    selected: Int,
    targets: IntArray,
    lastFrom: Int,
    lastTo: Int,
    hintMove: Int,
    checkSquare: Int,
    animation: MoveAnimation?,
    animationEnabled: Boolean,
    theme: BoardTheme,
    interactive: Boolean,
    flipped: Boolean = false,
    lastMoveByAi: Boolean = false,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = paletteFor(theme)
    val hintPulse = rememberInfiniteTransition(label = "hint")
    val hintAlpha = if (hintMove >= 0) {
        hintPulse.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "hintAlpha"
        ).value
    } else {
        1f
    }
    val progress = remember(animation?.key) {
        Animatable(if (animation == null || !animationEnabled) 1f else 0f)
    }
    val textPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
    }

    LaunchedEffect(animation?.key, animationEnabled) {
        if (animation != null && animationEnabled) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 230, easing = FastOutSlowInEasing))
        } else {
            progress.snapTo(1f)
        }
    }

    val animProgress = if (animation != null && animationEnabled) progress.value else 1f
    val animating = animation != null && animProgress < 0.999f

    Box(
        modifier = modifier
            .aspectRatio(9f / 10f)
            .pointerInput(interactive, flipped) {
                if (!interactive) return@pointerInput
                detectTapGestures { offset ->
                    val cell = size.width / 9f
                    val displayCol = ((offset.x / cell) - 0.5f).roundToInt()
                    val displayRow = ((offset.y / cell) - 0.5f).roundToInt()
                    if (displayRow !in 0 until Board.ROWS || displayCol !in 0 until Board.COLS) {
                        return@detectTapGestures
                    }
                    val cx = (displayCol + 0.5f) * cell
                    val cy = (displayRow + 0.5f) * cell
                    val distance = hypot(offset.x - cx, offset.y - cy)
                    if (distance <= cell * 0.72f) {
                        val col = if (flipped) Board.COLS - 1 - displayCol else displayCol
                        val row = if (flipped) Board.ROWS - 1 - displayRow else displayRow
                        onTap(Board.sq(row, col))
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = size.width / 9f
            val radius = cell * 0.445f
            val lineWidth = cell * 0.028f
            val corner = cell * 0.42f

            fun displayCol(col: Int): Int = if (flipped) Board.COLS - 1 - col else col
            fun displayRow(row: Int): Int = if (flipped) Board.ROWS - 1 - row else row
            fun xOf(col: Int): Float = (displayCol(col) + 0.5f) * cell
            fun yOf(row: Int): Float = (displayRow(row) + 0.5f) * cell

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(palette.backgroundTop, palette.backgroundBottom),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                size = size,
                cornerRadius = CornerRadius(corner, corner)
            )

            for (row in 0 until Board.ROWS) {
                drawLine(
                    color = palette.line.copy(alpha = 0.85f),
                    start = Offset(xOf(0), yOf(row)),
                    end = Offset(xOf(8), yOf(row)),
                    strokeWidth = lineWidth
                )
            }
            for (col in 0 until Board.COLS) {
                if (col == 0 || col == 8) {
                    drawLine(
                        color = palette.line.copy(alpha = 0.85f),
                        start = Offset(xOf(col), yOf(0)),
                        end = Offset(xOf(col), yOf(9)),
                        strokeWidth = lineWidth
                    )
                } else {
                    drawLine(
                        color = palette.line.copy(alpha = 0.85f),
                        start = Offset(xOf(col), yOf(0)),
                        end = Offset(xOf(col), yOf(4)),
                        strokeWidth = lineWidth
                    )
                    drawLine(
                        color = palette.line.copy(alpha = 0.85f),
                        start = Offset(xOf(col), yOf(5)),
                        end = Offset(xOf(col), yOf(9)),
                        strokeWidth = lineWidth
                    )
                }
            }

            drawLine(palette.line, Offset(xOf(3), yOf(0)), Offset(xOf(5), yOf(2)), lineWidth)
            drawLine(palette.line, Offset(xOf(5), yOf(0)), Offset(xOf(3), yOf(2)), lineWidth)
            drawLine(palette.line, Offset(xOf(3), yOf(7)), Offset(xOf(5), yOf(9)), lineWidth)
            drawLine(palette.line, Offset(xOf(5), yOf(7)), Offset(xOf(3), yOf(9)), lineWidth)

            drawRoundRect(
                color = palette.line.copy(alpha = 0.55f),
                topLeft = Offset(xOf(0), yOf(0)),
                size = Size(cell * 8f, cell * 9f),
                cornerRadius = CornerRadius(cell * 0.06f, cell * 0.06f),
                style = Stroke(width = lineWidth * 1.5f)
            )

            textPaint.textSize = cell * 0.36f
            textPaint.typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            textPaint.color = palette.line.copy(alpha = 0.7f).toArgb()
            textPaint.alpha = 200
            drawContext.canvas.nativeCanvas.drawText(
                "楚 河", xOf(2), yOf(4) + cell * 0.62f, textPaint
            )
            drawContext.canvas.nativeCanvas.drawText(
                "漢 界", xOf(6), yOf(4) + cell * 0.62f, textPaint
            )
            textPaint.alpha = 255

            fun marker(square: Int, color: Color, alpha: Float) {
                if (square < 0) return
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius * 0.98f,
                    center = Offset(xOf(Board.col(square)), yOf(Board.row(square)))
                )
            }
            if (lastMoveByAi) {
                if (lastFrom >= 0) {
                    drawCircle(
                        color = palette.aiMove.copy(alpha = 0.75f),
                        radius = cell * 0.17f,
                        center = Offset(xOf(Board.col(lastFrom)), yOf(Board.row(lastFrom))),
                        style = Stroke(width = cell * 0.028f)
                    )
                }
                if (lastTo >= 0) {
                    drawCircle(
                        color = palette.aiMove.copy(alpha = 0.14f),
                        radius = radius * 1.22f,
                        center = Offset(xOf(Board.col(lastTo)), yOf(Board.row(lastTo)))
                    )
                }
            } else {
                marker(lastFrom, palette.highlight, 0.22f)
                marker(lastTo, palette.highlight, 0.22f)
            }

            if (checkSquare >= 0) {
                drawCircle(
                    color = palette.check.copy(alpha = 0.22f),
                    radius = radius * 1.2f,
                    center = Offset(xOf(Board.col(checkSquare)), yOf(Board.row(checkSquare)))
                )
            }

            for (square in targets) {
                if (square !in 0 until Board.SIZE) continue
                val center = Offset(xOf(Board.col(square)), yOf(Board.row(square)))
                if (board[square] == Pieces.EMPTY) {
                    drawCircle(
                        color = palette.target.copy(alpha = 0.55f),
                        radius = cell * 0.105f,
                        center = center
                    )
                } else {
                    drawCircle(
                        color = palette.target.copy(alpha = 0.8f),
                        radius = radius * 1.08f,
                        center = center,
                        style = Stroke(width = cell * 0.05f)
                    )
                }
            }

            fun drawPiece(center: Offset, code: Int, alpha: Float = 1f) {
                if (code == Pieces.EMPTY) return
                drawCircle(
                    color = Color.Black.copy(alpha = 0.14f * alpha),
                    radius = radius,
                    center = Offset(center.x, center.y + cell * 0.045f)
                )
                drawCircle(
                    color = palette.pieceFill.copy(alpha = alpha),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = palette.pieceBorder.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = cell * 0.034f)
                )
                drawCircle(
                    color = palette.pieceInner.copy(alpha = 0.65f * alpha),
                    radius = radius * 0.86f,
                    center = center,
                    style = Stroke(width = cell * 0.012f)
                )
                textPaint.textSize = cell * 0.55f
                textPaint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textPaint.color = (if (code <= 7) palette.redText else palette.blackText).toArgb()
                textPaint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
                val baseline = center.y - (textPaint.ascent() + textPaint.descent()) / 2f
                drawContext.canvas.nativeCanvas.drawText(
                    Pieces.name(code),
                    center.x,
                    baseline,
                    textPaint
                )
                textPaint.alpha = 255
            }

            if (animating && animation.captured != Pieces.EMPTY) {
                drawPiece(
                    Offset(xOf(Board.col(animation.to)), yOf(Board.row(animation.to))),
                    animation.captured,
                    1f - animProgress
                )
            }

            for (square in 0 until Board.SIZE) {
                val piece = board[square]
                if (piece == Pieces.EMPTY) continue
                if (animating && square == animation.to) continue
                drawPiece(
                    Offset(xOf(Board.col(square)), yOf(Board.row(square))),
                    piece
                )
            }

            if (selected >= 0) {
                drawCircle(
                    color = palette.highlight.copy(alpha = 0.95f),
                    radius = radius * 1.1f,
                    center = Offset(xOf(Board.col(selected)), yOf(Board.row(selected))),
                    style = Stroke(width = cell * 0.055f)
                )
            }

            if (animating) {
                val fx = xOf(Board.col(animation.from))
                val fy = yOf(Board.row(animation.from))
                val tx = xOf(Board.col(animation.to))
                val ty = yOf(Board.row(animation.to))
                val cx = fx + (tx - fx) * animProgress
                val cy = fy + (ty - fy) * animProgress
                drawPiece(Offset(cx, cy), animation.piece)
            }

            if (lastMoveByAi && lastTo >= 0) {
                drawCircle(
                    color = palette.aiMove.copy(alpha = 0.9f),
                    radius = radius * 1.15f,
                    center = Offset(xOf(Board.col(lastTo)), yOf(Board.row(lastTo))),
                    style = Stroke(width = cell * 0.05f)
                )
            }

            if (hintMove >= 0) {
                val hintFrom = Move.fromOf(hintMove)
                val hintTo = Move.toOf(hintMove)
                val fromX = xOf(Board.col(hintFrom))
                val fromY = yOf(Board.row(hintFrom))
                val toX = xOf(Board.col(hintTo))
                val toY = yOf(Board.row(hintTo))
                val dx = toX - fromX
                val dy = toY - fromY
                val length = hypot(dx, dy)
                if (length > 1f) {
                    val ux = dx / length
                    val uy = dy / length
                    val start = Offset(
                        fromX + ux * radius * 0.95f,
                        fromY + uy * radius * 0.95f
                    )
                    val end = Offset(
                        toX - ux * radius * 1.05f,
                        toY - uy * radius * 1.05f
                    )
                    val hintColor = palette.highlight.copy(alpha = hintAlpha)

                    drawCircle(
                        color = palette.highlight.copy(alpha = 0.22f * hintAlpha),
                        radius = cell * 0.62f,
                        center = Offset(fromX, fromY)
                    )
                    drawCircle(
                        color = palette.highlight.copy(alpha = 0.22f * hintAlpha),
                        radius = cell * 0.62f,
                        center = Offset(toX, toY)
                    )
                    drawCircle(
                        color = hintColor,
                        radius = cell * 0.5f,
                        center = Offset(fromX, fromY),
                        style = Stroke(width = cell * 0.055f)
                    )
                    drawCircle(
                        color = hintColor,
                        radius = cell * 0.5f,
                        center = Offset(toX, toY),
                        style = Stroke(width = cell * 0.055f)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.38f),
                        start = start,
                        end = end,
                        strokeWidth = cell * 0.15f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = hintColor,
                        start = start,
                        end = end,
                        strokeWidth = cell * 0.075f,
                        cap = StrokeCap.Round
                    )

                    val angle = atan2(uy, ux)
                    val headLength = cell * 0.26f
                    val left = Offset(
                        end.x - headLength * cos(angle - 0.55f),
                        end.y - headLength * sin(angle - 0.55f)
                    )
                    val right = Offset(
                        end.x - headLength * cos(angle + 0.55f),
                        end.y - headLength * sin(angle + 0.55f)
                    )
                    val arrowHead = Path().apply {
                        moveTo(end.x, end.y)
                        lineTo(left.x, left.y)
                        lineTo(right.x, right.y)
                        close()
                    }
                    drawPath(arrowHead, hintColor)
                }
            }
        }
    }
}




