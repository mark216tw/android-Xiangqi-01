package com.example.xiangqi.data

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import com.example.xiangqi.ai.Difficulty
import com.example.xiangqi.model.Side
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class TimeControl(val label: String, val baseMs: Long, val incrementMs: Long) {
    NONE("不限時", 0L, 0L),
    TEN("10 分鐘", 600_000L, 0L),
    TEN_INC("10 分 + 5 秒", 600_000L, 5_000L)
}

enum class BoardTheme(val label: String) {
    WOOD("經典木紋"),
    LIGHT("素雅淺色"),
    INK_JADE("墨玉"),
    CELADON("青瓷")
}

enum class ThemeMode(val label: String) {
    SYSTEM("系統"),
    LIGHT("淺色"),
    DARK("深色")
}

data class AppSettings(
    val sound: Boolean = true,
    val animation: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val boardTheme: BoardTheme = BoardTheme.WOOD,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val humanSide: Side = Side.RED,
    val timeControl: TimeControl = TimeControl.TEN
)

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val sound = booleanPreferencesKey("sound")
        val animation = booleanPreferencesKey("animation")
        val themeMode = stringPreferencesKey("theme_mode")
        val boardTheme = stringPreferencesKey("board_theme")
        val difficulty = stringPreferencesKey("difficulty")
        val humanSide = stringPreferencesKey("human_side")
        val timeControl = stringPreferencesKey("time_control")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            sound = prefs[Keys.sound] ?: true,
            animation = prefs[Keys.animation] ?: true,
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.themeMode] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            boardTheme = runCatching {
                BoardTheme.valueOf(prefs[Keys.boardTheme] ?: BoardTheme.WOOD.name)
            }.getOrDefault(BoardTheme.WOOD),
            difficulty = runCatching {
                Difficulty.valueOf(prefs[Keys.difficulty] ?: Difficulty.MEDIUM.name)
            }.getOrDefault(Difficulty.MEDIUM),
            humanSide = runCatching {
                Side.valueOf(prefs[Keys.humanSide] ?: Side.RED.name)
            }.getOrDefault(Side.RED),
            timeControl = runCatching {
                TimeControl.valueOf(prefs[Keys.timeControl] ?: TimeControl.TEN.name)
            }.getOrDefault(TimeControl.TEN)
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setSound(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.sound] = enabled }
    }

    suspend fun setAnimation(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.animation] = enabled }
    }

    suspend fun setBoardTheme(theme: BoardTheme) {
        context.settingsDataStore.edit { it[Keys.boardTheme] = theme.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setDefaults(difficulty: Difficulty, humanSide: Side, timeControl: TimeControl) {
        context.settingsDataStore.edit {
            it[Keys.difficulty] = difficulty.name
            it[Keys.humanSide] = humanSide.name
            it[Keys.timeControl] = timeControl.name
        }
    }
}
