package com.example.xiangqi.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xiangqi.data.TimeControl
import com.example.xiangqi.model.Board
import com.example.xiangqi.model.PieceType
import com.example.xiangqi.model.Pieces
import com.example.xiangqi.model.Side
import com.example.xiangqi.ui.board.XiangqiBoard
import com.example.xiangqi.ui.theme.Crimson
import com.example.xiangqi.ui.theme.Ink
import com.example.xiangqi.ui.theme.Jade

@Composable
fun GameScreen(
    state: UiState,
    onTap: (Int) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onResign: () -> Unit,
    onEndGame: () -> Unit,
    onBack: () -> Unit,
    onDismissResult: () -> Unit,
    onRematch: () -> Unit,
    onMenu: () -> Unit
) {
    var showResignConfirm by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showEndGameConfirm by remember { mutableStateOf(false) }
    val canUndo = !state.result.isOver &&
        state.moveNotations.isNotEmpty() &&
        !(state.humanSide == Side.BLACK && state.moveNotations.size == 1)
    val checkSquare = remember(state.board, state.inCheck, state.sideToMove) {
        if (state.inCheck) {
            val king = Pieces.code(state.sideToMove, com.example.xiangqi.model.PieceType.KING)
            var found = -1
            for (i in state.board.indices) if (state.board[i] == king) found = i
            found
        } else {
            -1
        }
    }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state.animation?.key) {
        val move = state.animation ?: return@LaunchedEffect
        if (state.lastMoveByAi) return@LaunchedEffect
        if (System.nanoTime() - move.key > 1_000_000_000L) return@LaunchedEffect
        val type = if (state.inCheck || move.captured != Pieces.EMPTY) {
            HapticFeedbackType.LongPress
        } else {
            HapticFeedbackType.TextHandleMove
        }
        haptic.performHapticFeedback(type)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "對局",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${state.difficulty.label} · 你執${if (state.humanSide == Side.RED) "紅" else "黑"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.thinking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "更多選項")
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    if (state.result.isOver) {
                        DropdownMenuItem(
                            text = { Text("再來一局") },
                            onClick = {
                                showMoreMenu = false
                                onRematch()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("結束對局") },
                            onClick = {
                                showMoreMenu = false
                                showEndGameConfirm = true
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimerCard(
                label = "紅方",
                isHuman = state.humanSide == Side.RED,
                timeMs = state.redTimeMs,
                timeControl = state.timeControl,
                active = !state.result.isOver && state.sideToMove == Side.RED,
                accent = Crimson,
                modifier = Modifier.weight(1f)
            )
            TimerCard(
                label = "黑方",
                isHuman = state.humanSide == Side.BLACK,
                timeMs = state.blackTimeMs,
                timeControl = state.timeControl,
                active = !state.result.isOver && state.sideToMove == Side.BLACK,
                accent = Jade,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val boardWidth = minOf(maxWidth, maxHeight * 9f / 10f)
            XiangqiBoard(
                board = state.board,
                selected = state.selected,
                targets = state.targets,
                lastFrom = state.lastFrom,
                lastTo = state.lastTo,
                hintMove = state.hintMove,
                checkSquare = checkSquare,
                animation = state.animation,
                animationEnabled = state.settings.animation,
                theme = state.settings.boardTheme,
                interactive = !state.result.isOver && !state.thinking && state.sideToMove == state.humanSide,
                flipped = state.humanSide == Side.BLACK,
                lastMoveByAi = state.lastMoveByAi,
                onTap = onTap,
                modifier = Modifier.width(boardWidth)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val status = when {
                    state.result.isOver -> resultTitle(state.result, state.humanSide)
                    state.hintComputing -> "計算提示中…"
                    state.thinking -> "電腦思考中…"
                    state.sideToMove == state.humanSide -> "輪到你 · ${sideName(state.sideToMove)}"
                    else -> "輪到${sideName(state.sideToMove)}"
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.inCheck && !state.result.isOver) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground
                )
                val subtitle = when {
                    state.result.isOver -> resultSubtitle(state.result)
                    state.inCheck -> "將軍！"
                    state.hintMove >= 0 -> "依亮色箭頭落子"
                    else -> if (state.timeControl == TimeControl.NONE) "不限時" else "用時：${state.timeControl.label}"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.inCheck && !state.result.isOver) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (state.hintMove >= 0 && !state.thinking) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = if (state.hintNotation.isNotEmpty()) "建議：${state.hintNotation}" else "建議走法",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        CapturedMaterialRow(board = state.board)
        Spacer(Modifier.height(4.dp))
        MoveStrip(notations = state.moveNotations)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("悔棋")
            }
            OutlinedButton(
                onClick = onHint,
                enabled = !state.thinking && !state.result.isOver && state.sideToMove == state.humanSide,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("提示")
            }
            OutlinedButton(
                onClick = { showResignConfirm = true },
                enabled = !state.result.isOver,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("認輸")
            }
        }
    }

    if (showResignConfirm) {
        AlertDialog(
            onDismissRequest = { showResignConfirm = false },
            title = { Text("確認認輸？") },
            text = { Text("認輸後本局將結束並保存棋譜。") },
            confirmButton = {
                TextButton(onClick = {
                    showResignConfirm = false
                    onResign()
                }) {
                    Text("認輸")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResignConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showEndGameConfirm) {
        AlertDialog(
            onDismissRequest = { showEndGameConfirm = false },
            title = { Text("結束對局？") },
            text = { Text("結束後會保存目前棋譜並返回主選單，之後可在棋譜中回放。") },
            confirmButton = {
                TextButton(onClick = {
                    showEndGameConfirm = false
                    onEndGame()
                }) {
                    Text("結束對局")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndGameConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (state.resultDialog && state.result.isOver) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(resultTitle(state.result, state.humanSide)) },
            text = {
                Text(
                    text = buildString {
                        append(resultSubtitle(state.result))
                        append("\n")
                        append("共 ${state.moveNotations.size} 步")
                    }
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = onDismissResult) {
                        Text("查看棋盤")
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onRematch) {
                        Text("再來一局")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onMenu) {
                    Text("回主選單")
                }
            }
        )
    }
}

private val CAPTURE_TYPES = listOf(
    PieceType.ROOK to 2,
    PieceType.CANNON to 2,
    PieceType.HORSE to 2,
    PieceType.ADVISOR to 2,
    PieceType.ELEPHANT to 2,
    PieceType.PAWN to 5
)

private fun capturedPieces(board: IntArray, side: Side): List<Int> {
    val result = ArrayList<Int>(16)
    for ((type, initial) in CAPTURE_TYPES) {
        val code = Pieces.code(side, type)
        var count = 0
        for (piece in board) {
            if (piece == code) count++
        }
        repeat((initial - count).coerceAtLeast(0)) { result.add(code) }
    }
    return result
}

private fun materialValue(code: Int): Int = when (Pieces.typeOf(code)) {
    PieceType.ROOK -> 9
    PieceType.CANNON -> 5
    PieceType.HORSE -> 4
    PieceType.ADVISOR, PieceType.ELEPHANT -> 2
    PieceType.PAWN -> 1
    else -> 0
}

@Composable
private fun CapturedMaterialRow(board: IntArray) {
    val redCaptured = remember(board) { capturedPieces(board, Side.BLACK) }
    val blackCaptured = remember(board) { capturedPieces(board, Side.RED) }
    val redScore = redCaptured.sumOf { materialValue(it) }
    val blackScore = blackCaptured.sumOf { materialValue(it) }
    val diff = redScore - blackScore
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CapturedSide(
            label = "紅方吃",
            pieces = redCaptured,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = when {
                    diff > 0 -> "紅 +$diff"
                    diff < 0 -> "黑 +${-diff}"
                    else -> "均勢"
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    diff > 0 -> MaterialTheme.colorScheme.primary
                    diff < 0 -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Spacer(Modifier.width(8.dp))
        CapturedSide(
            label = "黑方吃",
            pieces = blackCaptured,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CapturedSide(
    label: String,
    pieces: List<Int>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        if (pieces.isEmpty()) {
            Text(
                text = "—",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        } else {
            pieces.forEach { piece ->
                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Pieces.name(piece),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            color = if (piece <= 7) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.width(2.dp))
            }
        }
    }
}

@Composable
private fun TimerCard(
    label: String,
    isHuman: Boolean,
    timeMs: Long,
    timeControl: TimeControl,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val hasClock = timeControl != TimeControl.NONE
    val warning = hasClock && timeMs <= 30_000L
    val critical = hasClock && timeMs <= 10_000L
    val containerColor = when {
        critical -> MaterialTheme.colorScheme.errorContainer
        active -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        critical -> MaterialTheme.colorScheme.onErrorContainer
        active -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val timeColor = when {
        critical -> MaterialTheme.colorScheme.onErrorContainer
        warning -> MaterialTheme.colorScheme.error
        active -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = if (critical) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .padding(0.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(9.dp),
                    shape = CircleShape,
                    color = accent
                ) {}
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$label${if (isHuman) " · 你" else " · 電腦"}",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (timeControl == TimeControl.NONE) "不限時" else formatClock(timeMs),
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (warning) FontWeight.Bold else FontWeight.SemiBold,
                color = timeColor
            )
        }
    }
}

@Composable
private fun MoveStrip(notations: List<String>) {
    if (notations.isEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "尚未落子 · 點選棋子開始",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(notations.size) {
        listState.animateScrollToItem(notations.size - 1)
    }
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(notations) { index, notation ->
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = if (index == notations.size - 1) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${index + 1}. $notation",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index == notations.size - 1) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun sideName(side: Side): String = if (side == Side.RED) "紅方" else "黑方"

