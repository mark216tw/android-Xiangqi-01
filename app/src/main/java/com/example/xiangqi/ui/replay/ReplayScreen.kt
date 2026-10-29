package com.example.xiangqi.ui.replay

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xiangqi.data.BoardTheme
import com.example.xiangqi.model.PieceType
import com.example.xiangqi.model.Pieces
import com.example.xiangqi.model.Side
import com.example.xiangqi.ui.board.XiangqiBoard
import com.example.xiangqi.ui.difficultyOf
import com.example.xiangqi.ui.formatDate
import com.example.xiangqi.ui.game.ReplayUi
import com.example.xiangqi.ui.savedResultText
import kotlin.math.roundToInt

@Composable
fun ReplayScreen(
    replay: ReplayUi?,
    boardTheme: BoardTheme,
    onBack: () -> Unit,
    onStep: (Int) -> Unit,
    onSeek: (Int) -> Unit,
    onTogglePlay: () -> Unit
) {
    if (replay == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("沒有可回放的棋譜")
        }
        return
    }

    val checkSquare = remember(replay.board, replay.check, replay.sideToMove) {
        if (replay.check) {
            val king = Pieces.code(replay.sideToMove, PieceType.KING)
            var found = -1
            for (i in replay.board.indices) if (replay.board[i] == king) found = i
            found
        } else {
            -1
        }
    }
    val lastMove = replay.lastMove

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
                    text = "棋譜回放",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatDate(replay.saved.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "第 ${replay.index} / ${replay.lastIndex} 步",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "難度 ${difficultyOf(replay.saved).label} · " +
                        "執${if (replay.saved.humanSide == Side.RED.name) "紅" else "黑"} · " +
                        "${replay.saved.moves.size} 步",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = savedResultText(replay.saved),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                board = replay.board,
                selected = -1,
                targets = IntArray(0),
                lastFrom = lastMove?.from ?: -1,
                lastTo = lastMove?.to ?: -1,
                hintMove = -1,
                checkSquare = checkSquare,
                animation = null,
                animationEnabled = false,
                theme = boardTheme,
                interactive = false,
                flipped = replay.saved.humanSide == Side.BLACK.name,
                onTap = {},
                modifier = Modifier.width(boardWidth)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        replay.atEnd && replay.saved.finished -> "終局"
                        replay.check -> "將軍！"
                        replay.sideToMove == Side.RED -> "輪到紅方"
                        else -> "輪到黑方"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (replay.check) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (replay.index > 0) {
                        "上一手：${replay.notations.getOrNull(replay.index - 1) ?: ""}"
                    } else {
                        "開局局面"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (replay.lastIndex > 0) {
            Slider(
                value = replay.index.toFloat(),
                onValueChange = { onSeek(it.roundToInt()) },
                valueRange = 0f..replay.lastIndex.toFloat()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onSeek(0) }, enabled = replay.index > 0) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "開局")
            }
            IconButton(onClick = { onStep(-1) }, enabled = replay.index > 0) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "上一步")
            }
            Spacer(Modifier.width(10.dp))
            FilledIconButton(
                onClick = onTogglePlay,
                enabled = replay.lastIndex > 0,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (replay.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (replay.playing) "暫停" else "播放"
                )
            }
            Spacer(Modifier.width(10.dp))
            IconButton(onClick = { onStep(1) }, enabled = replay.index < replay.lastIndex) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "下一步")
            }
            IconButton(onClick = { onSeek(replay.lastIndex) }, enabled = replay.index < replay.lastIndex) {
                Icon(Icons.Filled.SkipNext, contentDescription = "終局")
            }
        }

        Spacer(Modifier.height(4.dp))
        ReplayMoveStrip(replay, onSeek)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ReplayMoveStrip(replay: ReplayUi, onSeek: (Int) -> Unit) {
    if (replay.notations.isEmpty()) {
        Text(
            text = "此棋譜沒有走法",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(replay.index) {
        val target = (replay.index - 1).coerceAtLeast(0)
        if (replay.index > 0) listState.animateScrollToItem(target)
    }
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(replay.notations) { index, notation ->
            val isCurrent = replay.index > 0 && index == replay.index - 1
            Surface(
                onClick = { onSeek(index + 1) },
                shape = RoundedCornerShape(9.dp),
                color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${index + 1}. $notation",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


