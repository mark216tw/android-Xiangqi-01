package com.example.xiangqi.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xiangqi.R
import com.example.xiangqi.ai.Difficulty
import com.example.xiangqi.data.AppSettings
import com.example.xiangqi.data.SavedGame
import com.example.xiangqi.data.TimeControl
import com.example.xiangqi.model.Side
import com.example.xiangqi.ui.difficultyOf
import com.example.xiangqi.ui.formatDate
import com.example.xiangqi.ui.theme.Crimson

@Composable
fun MenuScreen(
    hasUnfinished: Boolean,
    hasGame: Boolean,
    lastUnfinished: SavedGame?,
    settings: AppSettings,
    onNewGame: (Difficulty, Side, TimeControl) -> Unit,
    onContinue: () -> Unit,
    onRecords: () -> Unit,
    onSettings: () -> Unit
) {
    var showNewGame by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compact = maxHeight < 620.dp
        val scrollState = rememberScrollState()
        val logoSize = if (compact) 84.dp else 108.dp
        val logoTextSize = if (compact) 38.sp else 48.sp
        val topGap = if (compact) 24.dp else 56.dp
        val logoGap = if (compact) 14.dp else 20.dp
        val titleGap = if (compact) 4.dp else 6.dp
        val buttonsGap = if (compact) 24.dp else 44.dp
        val sectionGap = if (compact) 8.dp else 12.dp
        val newGameHeight = if (compact) 50.dp else 54.dp
        val continueHeight = if (compact) 48.dp else 52.dp
        val secondaryHeight = if (compact) 48.dp else 50.dp
        val footerGap = if (compact) 24.dp else 28.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (compact) Modifier.verticalScroll(scrollState) else Modifier)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(topGap))

            Box(
                modifier = Modifier
                    .size(logoSize)
                    .clip(CircleShape)
                    .background(Color(0xFFF6E7C8))
                    .border(if (compact) 4.dp else 5.dp, Crimson, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "帥",
                    color = Crimson,
                    fontSize = logoTextSize,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(logoGap))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(titleGap))
            Text(
                text = "人機對弈 · 標準規則 · 離線 AI",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(buttonsGap))

            Button(
                onClick = { showNewGame = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(newGameHeight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("新局", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(sectionGap))

            OutlinedButton(
                onClick = onContinue,
                enabled = hasUnfinished || hasGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(continueHeight),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("繼續棋局", fontSize = 17.sp)
            }

            if (lastUnfinished != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = continueSummary(lastUnfinished),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(sectionGap))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(sectionGap)
            ) {
                OutlinedButton(
                    onClick = onRecords,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(secondaryHeight),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("棋譜與回放", fontSize = 16.sp)
                }
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(secondaryHeight),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("設定", fontSize = 16.sp)
                }
            }

            if (compact) {
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }

            Text(
                text = "紅先黑後 · 將死或困斃即勝",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(footerGap))
        }
    }

    if (showNewGame) {
        NewGameDialog(
            settings = settings,
            hasUnfinishedGame = hasUnfinished || hasGame,
            onDismiss = { showNewGame = false },
            onStart = { difficulty, side, time ->
                showNewGame = false
                onNewGame(difficulty, side, time)
            }
        )
    }
}

private fun continueSummary(game: SavedGame): String {
    val sideLabel = if (game.humanSide == Side.RED.name) "紅" else "黑"
    return "${difficultyOf(game).label} · 執$sideLabel · ${game.moves.size} 步 · ${formatDate(game.updatedAt)}"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewGameDialog(
    settings: AppSettings,
    hasUnfinishedGame: Boolean,
    onDismiss: () -> Unit,
    onStart: (Difficulty, Side, TimeControl) -> Unit
) {
    var difficulty by remember { mutableStateOf(settings.difficulty) }
    var side by remember { mutableStateOf(settings.humanSide) }
    var timeControl by remember { mutableStateOf(settings.timeControl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新局設定") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (hasUnfinishedGame) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "目前有未完成棋局，開始新局後仍可在棋譜中繼續。",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                DialogSection("難度") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Difficulty.entries.forEach { level ->
                            FilterChip(
                                selected = difficulty == level,
                                onClick = { difficulty = level },
                                label = { Text(level.label) }
                            )
                        }
                    }
                }
                DialogSection("執子") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = side == Side.RED,
                            onClick = { side = Side.RED },
                            label = { Text("紅方先手") }
                        )
                        FilterChip(
                            selected = side == Side.BLACK,
                            onClick = { side = Side.BLACK },
                            label = { Text("黑方後手") }
                        )
                    }
                }
                DialogSection("時間") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeControl.entries.forEach { control ->
                            FilterChip(
                                selected = timeControl == control,
                                onClick = { timeControl = control },
                                label = { Text(control.label) }
                            )
                        }
                    }
                }
                Text(
                    text = if (side == Side.RED) "你執紅先行，電腦執黑。" else "電腦執紅先行，你執黑。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onStart(difficulty, side, timeControl) }) {
                Text("開始")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun DialogSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}
