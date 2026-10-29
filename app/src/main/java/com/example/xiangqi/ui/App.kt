package com.example.xiangqi.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.xiangqi.data.ThemeMode
import com.example.xiangqi.ui.game.AppScreen
import com.example.xiangqi.ui.game.GameScreen
import com.example.xiangqi.ui.game.GameViewModel
import com.example.xiangqi.ui.menu.MenuScreen
import com.example.xiangqi.ui.records.RecordsScreen
import com.example.xiangqi.ui.replay.ReplayScreen
import com.example.xiangqi.ui.settings.SettingsScreen
import com.example.xiangqi.ui.theme.MiniXiangqiTheme

@Composable
fun MiniXiangqiApp(viewModel: GameViewModel) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (state.settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MiniXiangqiTheme(darkTheme = darkTheme) {
        val view = LocalView.current
        DisposableEffect(state.screen) {
            view.keepScreenOn = state.screen == AppScreen.GAME || state.screen == AppScreen.REPLAY
            onDispose { view.keepScreenOn = false }
        }
        SystemBarsEffect(darkTheme)

        BackHandler(enabled = state.screen != AppScreen.MENU) {
            when (state.screen) {
                AppScreen.GAME -> viewModel.leaveGame()
                AppScreen.REPLAY -> viewModel.closeReplay()
                AppScreen.RECORDS, AppScreen.SETTINGS -> viewModel.goMenu()
                AppScreen.MENU -> Unit
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                when (state.screen) {
                    AppScreen.MENU -> MenuScreen(
                        hasUnfinished = state.hasUnfinished,
                        hasGame = state.hasGame,
                        lastUnfinished = state.games.firstOrNull { !it.finished },
                        settings = state.settings,
                        onNewGame = viewModel::startNewGame,
                        onContinue = viewModel::continueGame,
                        onRecords = viewModel::openRecords,
                        onSettings = viewModel::openSettings
                    )
                    AppScreen.GAME -> GameScreen(
                        state = state,
                        onTap = viewModel::onSquareTap,
                        onUndo = viewModel::undo,
                        onHint = viewModel::requestHint,
                        onResign = viewModel::resign,
                        onEndGame = viewModel::endGame,
                        onBack = viewModel::leaveGame,
                        onDismissResult = viewModel::dismissResult,
                        onRematch = {
                            viewModel.dismissResult()
                            viewModel.startNewGame(state.difficulty, state.humanSide, state.timeControl)
                        },
                        onMenu = viewModel::leaveGame
                    )
                    AppScreen.RECORDS -> RecordsScreen(
                        games = state.games,
                        onBack = viewModel::goMenu,
                        onContinue = viewModel::continueSaved,
                        onReplay = viewModel::openReplay,
                        onDelete = viewModel::deleteSaved,
                        onDeleteFinished = viewModel::deleteFinishedSaved
                    )
                    AppScreen.SETTINGS -> SettingsScreen(
                        settings = state.settings,
                        onBack = viewModel::goMenu,
                        onSound = viewModel::setSound,
                        onAnimation = viewModel::setAnimation,
                        onThemeMode = viewModel::setThemeMode,
                        onBoardTheme = viewModel::setBoardTheme,
                        onDifficulty = viewModel::setDefaultDifficulty
                    )
                    AppScreen.REPLAY -> ReplayScreen(
                        replay = state.replay,
                        boardTheme = state.settings.boardTheme,
                        onBack = viewModel::closeReplay,
                        onStep = viewModel::replayStep,
                        onSeek = viewModel::replayTo,
                        onTogglePlay = viewModel::toggleReplayPlay
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemBarsEffect(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme

        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
