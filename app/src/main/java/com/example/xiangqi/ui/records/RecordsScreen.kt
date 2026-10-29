package com.example.xiangqi.ui.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.xiangqi.data.SavedGame
import com.example.xiangqi.model.Outcome
import com.example.xiangqi.model.Side
import com.example.xiangqi.ui.difficultyOf
import com.example.xiangqi.ui.formatDate
import com.example.xiangqi.ui.savedOutcomeLabel
import com.example.xiangqi.ui.savedResultText
import com.example.xiangqi.ui.theme.Crimson

@Composable
fun RecordsScreen(
    games: List<SavedGame>,
    onBack: () -> Unit,
    onContinue: (String) -> Unit,
    onReplay: (SavedGame) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteFinished: () -> Unit
) {
    var deleteTarget by remember { mutableStateOf<SavedGame?>(null) }
    var showDeleteFinishedConfirm by remember { mutableStateOf(false) }
    val finishedCount = games.count { it.finished }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            Text(
                text = "棋譜與回放",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "共 ${games.size} 局",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = if (finishedCount > 0) 2.dp else 12.dp)
            )
            if (finishedCount > 0) {
                TextButton(
                    onClick = { showDeleteFinishedConfirm = true },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("刪除已結束", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (games.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "還沒有棋譜",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "完成或保存的對局會出現在這裡",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(games, key = { it.id }) { game ->
                RecordCard(
                    game = game,
                    onOpen = {
                        if (game.finished) onReplay(game) else onContinue(game.id)
                    },
                    onContinue = { onContinue(game.id) },
                    onReplay = { onReplay(game) },
                    onDelete = { deleteTarget = game }
                )
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("刪除棋譜？") },
            text = { Text("${formatDate(target.updatedAt)} 的對局將被永久刪除。") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) {
                    Text("刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDeleteFinishedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteFinishedConfirm = false },
            title = { Text("刪除已結束棋譜？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("將永久刪除全部 $finishedCount 局已結束棋譜。")
                    Text("未完成的對局會保留。")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteFinishedConfirm = false
                    onDeleteFinished()
                }) {
                    Text("刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFinishedConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun RecordCard(
    game: SavedGame,
    onOpen: () -> Unit,
    onContinue: () -> Unit,
    onReplay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDate(game.updatedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (!game.finished) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "進行中",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "刪除",
                        modifier = Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = savedResultText(game),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = when (game.outcomeValue) {
                    Outcome.RED_WIN -> Crimson
                    Outcome.BLACK_WIN -> MaterialTheme.colorScheme.onSurface
                    Outcome.DRAW -> MaterialTheme.colorScheme.tertiary
                    Outcome.ABORTED -> MaterialTheme.colorScheme.onSurfaceVariant
                    Outcome.ONGOING -> MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = "難度 ${difficultyOf(game).label} · " +
                    "執${if (game.humanSide == Side.RED.name) "紅" else "黑"} · " +
                    "${game.moves.size} 步",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (game.finished) {
                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Text("回放")
                    }
                } else {
                    OutlinedButton(
                        onClick = onContinue,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Text("繼續")
                    }
                }
            }
        }
    }
}
