package com.example.xiangqi.ui.game

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xiangqi.ai.Difficulty
import com.example.xiangqi.ai.SearchEngine
import com.example.xiangqi.audio.SoundManager
import com.example.xiangqi.data.AppSettings
import com.example.xiangqi.data.BoardTheme
import com.example.xiangqi.data.GameStore
import com.example.xiangqi.data.SavedGame
import com.example.xiangqi.data.SavedMove
import com.example.xiangqi.data.SettingsStore
import com.example.xiangqi.data.ThemeMode
import com.example.xiangqi.data.TimeControl
import com.example.xiangqi.game.Game
import com.example.xiangqi.model.Board
import com.example.xiangqi.model.GameResult
import com.example.xiangqi.model.Move
import com.example.xiangqi.model.Outcome
import com.example.xiangqi.model.Pieces
import com.example.xiangqi.model.Side
import com.example.xiangqi.rules.MoveGenerator
import com.example.xiangqi.rules.Notation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.max

enum class AppScreen { MENU, GAME, RECORDS, SETTINGS, REPLAY }

data class MoveAnimation(
    val from: Int,
    val to: Int,
    val piece: Int,
    val captured: Int,
    val key: Long
)

data class ReplayUi(
    val saved: SavedGame,
    val boards: List<IntArray>,
    val sides: List<Side>,
    val notations: List<String>,
    val index: Int,
    val playing: Boolean
) {
    val lastIndex: Int get() = boards.size - 1
    val board: IntArray get() = boards[index]
    val sideToMove: Side get() = sides[index]
    val lastMove: SavedMove? get() = if (index > 0) saved.moves.getOrNull(index - 1) else null
    val check: Boolean get() = MoveGenerator.isInCheck(board, sideToMove)
    val atEnd: Boolean get() = index == lastIndex
}

data class UiState(
    val screen: AppScreen = AppScreen.MENU,
    val board: IntArray = IntArray(Board.SIZE),
    val sideToMove: Side = Side.RED,
    val humanSide: Side = Side.RED,
    val selected: Int = -1,
    val targets: IntArray = IntArray(0),
    val lastFrom: Int = -1,
    val lastTo: Int = -1,
    val lastMoveByAi: Boolean = false,
    val inCheck: Boolean = false,
    val result: GameResult = GameResult(),
    val moveNotations: List<String> = emptyList(),
    val thinking: Boolean = false,
    val hintComputing: Boolean = false,
    val hintMove: Int = -1,
    val hintNotation: String = "",
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val timeControl: TimeControl = TimeControl.TEN,
    val redTimeMs: Long = TimeControl.TEN.baseMs,
    val blackTimeMs: Long = TimeControl.TEN.baseMs,
    val settings: AppSettings = AppSettings(),
    val games: List<SavedGame> = emptyList(),
    val hasUnfinished: Boolean = false,
    val hasGame: Boolean = false,
    val currentGameId: String? = null,
    val replay: ReplayUi? = null,
    val animation: MoveAnimation? = null,
    val resultDialog: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)
    private val gameStore = GameStore(application)
    private val sound = SoundManager(application)
    private val engine = SearchEngine()
    private val searchMutex = Mutex()
    private val saveMutex = Mutex()

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    private var game: Game? = null
    private var gameId: String? = null
    private var createdAt: Long = 0L
    private var difficulty = Difficulty.MEDIUM
    private var humanSide = Side.RED
    private var timeControl = TimeControl.TEN
    private var redTimeMs = TimeControl.TEN.baseMs
    private var blackTimeMs = TimeControl.TEN.baseMs
    private var lastShownRedSec = -1L
    private var lastShownBlackSec = -1L
    private var selected = -1
    private var targets = IntArray(0)
    private var hintMove = -1
    private var hintNotation = ""
    private var hintComputing = false
    private var thinking = false
    private var paused = false
    private var lastTick = 0L
    private var animation: MoveAnimation? = null

    @Volatile
    private var aiCancel = false

    private var aiJob: Job? = null
    private var hintJob: Job? = null
    private var tickerJob: Job? = null
    private var replayJob: Job? = null

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                sound.enabled = settings.sound
                _ui.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch { refreshGamesInternal() }
    }

    private suspend fun refreshGamesInternal() {
        saveMutex.withLock {
            val games = gameStore.list()
            _ui.update { it.copy(games = games, hasUnfinished = games.any { g -> !g.finished }) }
        }
    }

    private fun refresh() {
        val g = game
        if (g == null) {
            _ui.update {
                it.copy(
                    board = IntArray(Board.SIZE),
                    selected = -1,
                    targets = IntArray(0),
                    lastFrom = -1,
                    lastTo = -1,
                    lastMoveByAi = false,
                    hasGame = false
                )
            }
            return
        }
        val last = g.lastMove()
        _ui.update {
            it.copy(
                board = g.board.copyOf(),
                sideToMove = g.sideToMove,
                humanSide = humanSide,
                selected = selected,
                targets = targets.copyOf(),
                lastFrom = last?.from ?: -1,
                lastTo = last?.to ?: -1,
                lastMoveByAi = last != null && last.mover != humanSide,
                inCheck = g.inCheck(),
                result = g.result,
                moveNotations = g.moves.map { m -> m.notation },
                thinking = thinking,
                hintComputing = hintComputing,
                hintMove = hintMove,
                hintNotation = if (hintMove >= 0) hintNotation else "",
                difficulty = difficulty,
                timeControl = timeControl,
                redTimeMs = redTimeMs,
                blackTimeMs = blackTimeMs,
                hasGame = !g.result.isOver,
                currentGameId = gameId,
                animation = animation,
                resultDialog = _ui.value.resultDialog
            )
        }
    }

    private fun updateTimes() {
        val redSec = if (redTimeMs <= 0) 0L else (redTimeMs + 999) / 1000
        val blackSec = if (blackTimeMs <= 0) 0L else (blackTimeMs + 999) / 1000
        if (redSec == lastShownRedSec && blackSec == lastShownBlackSec) return
        lastShownRedSec = redSec
        lastShownBlackSec = blackSec
        _ui.update { it.copy(redTimeMs = redTimeMs, blackTimeMs = blackTimeMs) }
    }

    fun onSquareTap(sq: Int) {
        val g = game ?: return
        if (_ui.value.screen != AppScreen.GAME || g.result.isOver || thinking || paused) return
        if (g.sideToMove != humanSide) return

        if (selected >= 0) {
            if (sq == selected) {
                selected = -1
                targets = IntArray(0)
                refresh()
                return
            }
            if (targets.contains(sq)) {
                val from = selected
                selected = -1
                targets = IntArray(0)
                hintMove = -1
                playHuman(from, sq)
                return
            }
            if (Pieces.sideOf(g.board[sq]) == humanSide) {
                select(sq)
                return
            }
            selected = -1
            targets = IntArray(0)
            refresh()
            return
        }

        if (Pieces.sideOf(g.board[sq]) == humanSide) {
            select(sq)
        }
    }

    private fun select(sq: Int) {
        val g = game ?: return
        selected = sq
        targets = g.legalTargets(sq)
        hintMove = -1
        refresh()
    }

    private fun playHuman(from: Int, to: Int) {
        val g = game ?: return
        val record = g.play(from, to) ?: return
        sound.playMove(record)
        animation = MoveAnimation(record.from, record.to, record.piece, record.captured, System.nanoTime())
        addIncrement(record.mover)
        refresh()
        saveGame()
        if (g.result.isOver) {
            handleGameOver()
        } else {
            triggerAi(300)
        }
    }

    private fun addIncrement(mover: Side) {
        if (timeControl == TimeControl.NONE) return
        if (mover == Side.RED) redTimeMs += timeControl.incrementMs
        else blackTimeMs += timeControl.incrementMs
    }

    fun startNewGame(
        difficulty: Difficulty,
        humanSide: Side,
        timeControl: TimeControl
    ) {
        cancelAllJobs()
        this.difficulty = difficulty
        this.humanSide = humanSide
        this.timeControl = timeControl
        this.redTimeMs = timeControl.baseMs
        this.blackTimeMs = timeControl.baseMs
        this.game = Game(humanSide)
        this.gameId = UUID.randomUUID().toString()
        this.createdAt = System.currentTimeMillis()
        this.selected = -1
        this.targets = IntArray(0)
        this.hintMove = -1
        this.hintComputing = false
        this.thinking = false
        this.animation = null
        this.paused = false
        engine.newGame()

        _ui.update {
            it.copy(
                screen = AppScreen.GAME,
                resultDialog = false,
                replay = null
            )
        }
        refresh()
        saveGame()
        viewModelScope.launch {
            settingsStore.setDefaults(difficulty, humanSide, timeControl)
        }
        startTicker()
        if (humanSide == Side.BLACK) triggerAi(550)
    }

    fun continueGame() {
        val existing = game
        if (existing != null && !existing.result.isOver) {
            reopenGame()
            return
        }
        viewModelScope.launch {
            val saved = gameStore.latestUnfinished() ?: return@launch
            loadSavedGame(saved)
        }
    }

    fun continueSaved(id: String) {
        viewModelScope.launch {
            val saved = gameStore.load(id) ?: return@launch
            saveGame()
            loadSavedGame(saved)
        }
    }

    private fun reopenGame() {
        val g = game ?: return
        selected = -1
        targets = IntArray(0)
        hintMove = -1
        hintComputing = false
        thinking = false
        animation = null
        paused = false
        _ui.update {
            it.copy(
                screen = AppScreen.GAME,
                resultDialog = g.result.isOver
            )
        }
        refresh()
        startTicker()
        if (!g.result.isOver && g.sideToMove != humanSide) triggerAi(400)
    }

    private suspend fun loadSavedGame(saved: SavedGame) {
        val side = runCatching { Side.valueOf(saved.humanSide) }.getOrDefault(Side.RED)
        val diff = runCatching { Difficulty.valueOf(saved.difficulty) }.getOrDefault(Difficulty.MEDIUM)
        val tc = runCatching { TimeControl.valueOf(saved.timeControl) }.getOrDefault(TimeControl.TEN)
        val restored = Game(side)
        for (move in saved.moves) {
            if (restored.play(move.from, move.to) == null) break
        }

        cancelAllJobs()
        game = restored
        gameId = saved.id
        createdAt = saved.createdAt
        humanSide = side
        difficulty = diff
        timeControl = tc
        redTimeMs = if (saved.redTimeMs > 0) saved.redTimeMs else tc.baseMs
        blackTimeMs = if (saved.blackTimeMs > 0) saved.blackTimeMs else tc.baseMs
        selected = -1
        targets = IntArray(0)
        hintMove = -1
        hintComputing = false
        thinking = false
        animation = null
        engine.newGame()

        _ui.update {
            it.copy(
                screen = AppScreen.GAME,
                resultDialog = restored.result.isOver,
                replay = null
            )
        }
        refresh()
        startTicker()
        if (!restored.result.isOver && restored.sideToMove != humanSide) triggerAi(450)
    }

    private fun triggerAi(delayMs: Long = 280) {
        val g = game ?: return
        if (g.result.isOver || g.sideToMove == humanSide || thinking) return

        aiCancel = false
        thinking = true
        refresh()

        val boardCopy = g.board.copyOf()
        val side = g.sideToMove
        val level = difficulty

        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            try {
                delay(delayMs)
                val result = withContext(Dispatchers.Default) {
                    searchMutex.withLock {
                        engine.search(boardCopy, side, level) { aiCancel || !isActive }
                    }
                }
                if (aiCancel || !isActive) return@launch
                thinking = false
                if (result == null) {
                    refresh()
                    return@launch
                }
                val current = game
                if (current == null || current.result.isOver || current.sideToMove != side) {
                    refresh()
                    return@launch
                }
                val from = Move.fromOf(result.move)
                val to = Move.toOf(result.move)
                val record = current.play(from, to)
                if (record == null) {
                    refresh()
                    return@launch
                }
                sound.playMove(record)
                animation = MoveAnimation(record.from, record.to, record.piece, record.captured, System.nanoTime())
                addIncrement(record.mover)
                refresh()
                saveGame()
                if (current.result.isOver) handleGameOver()
            } finally {
                if (!aiCancel) {
                    thinking = false
                }
            }
        }
    }

    fun requestHint() {
        val g = game ?: return
        if (thinking || g.result.isOver || g.sideToMove != humanSide) return
        thinking = true
        hintComputing = true
        hintMove = -1
        refresh()

        val boardCopy = g.board.copyOf()
        val side = g.sideToMove
        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                searchMutex.withLock {
                    engine.search(boardCopy, side, Difficulty.MEDIUM) { !isActive }
                }
            }
            thinking = false
            hintComputing = false
            hintMove = result?.move ?: -1
            hintNotation = if (hintMove >= 0) {
                Notation.move(boardCopy, side, Move.fromOf(hintMove), Move.toOf(hintMove))
            } else {
                ""
            }
            refresh()
        }
    }

    fun undo() {
        val g = game ?: return
        if (g.result.isOver || g.moves.isEmpty()) return
        aiCancel = true
        aiJob?.cancel()
        hintJob?.cancel()
        thinking = false
        hintComputing = false
        hintMove = -1
        animation = null
        _ui.update { it.copy(resultDialog = false) }

        var count = 0
        while (count < 2 && g.canUndo()) {
            g.undo(1)
            count++
            if (g.sideToMove == humanSide) break
        }
        if (g.sideToMove != humanSide && g.canUndo()) g.undo(1)

        selected = -1
        targets = IntArray(0)
        refresh()
        saveGame()
        if (g.result.isOver) {
            handleGameOver()
        } else {
            startTicker()
            if (g.sideToMove != humanSide) triggerAi(450)
        }
    }

    fun resign() {
        val g = game ?: return
        if (g.result.isOver) return
        g.resign(humanSide)
        handleGameOver()
    }

    fun endGame() {
        val g = game ?: return
        if (g.result.isOver) return
        aiCancel = true
        aiJob?.cancel()
        hintJob?.cancel()
        tickerJob?.cancel()
        thinking = false
        hintComputing = false
        hintMove = -1
        hintNotation = ""
        animation = null
        selected = -1
        targets = IntArray(0)
        g.abort()
        saveGame()
        game = null
        gameId = null
        _ui.update {
            it.copy(
                screen = AppScreen.MENU,
                resultDialog = false,
                replay = null,
                hasGame = false,
                currentGameId = null,
                animation = null
            )
        }
        viewModelScope.launch { refreshGamesInternal() }
    }

    fun dismissResult() {
        _ui.update { it.copy(resultDialog = false) }
    }

    private fun handleGameOver() {
        thinking = false
        hintComputing = false
        aiCancel = true
        aiJob?.cancel()
        hintJob?.cancel()
        hintMove = -1
        hintNotation = ""
        tickerJob?.cancel()
        val result = game?.result ?: return
        _ui.update { it.copy(resultDialog = true) }
        when {
            result.outcome == Outcome.DRAW -> Unit
            result.winner == humanSide -> sound.playWin()
            else -> sound.playLose()
        }
        saveGame()
        refresh()
    }

    fun leaveGame() {
        val g = game
        if (g != null) saveGame()
        aiCancel = true
        aiJob?.cancel()
        hintJob?.cancel()
        tickerJob?.cancel()
        thinking = false
        hintComputing = false
        selected = -1
        targets = IntArray(0)
        hintMove = -1
        animation = null
        paused = false
        _ui.update { it.copy(screen = AppScreen.MENU, animation = null) }
    }

    fun goMenu() {
        aiCancel = true
        aiJob?.cancel()
        hintJob?.cancel()
        tickerJob?.cancel()
        replayJob?.cancel()
        thinking = false
        hintComputing = false
        _ui.update { it.copy(screen = AppScreen.MENU, replay = null) }
    }

    fun openRecords() {
        _ui.update { it.copy(screen = AppScreen.RECORDS) }
        viewModelScope.launch { refreshGamesInternal() }
    }

    fun openSettings() {
        _ui.update { it.copy(screen = AppScreen.SETTINGS) }
    }

    fun openReplay(saved: SavedGame) {
        replayJob?.cancel()
        val build = buildReplay(saved)
        _ui.update {
            it.copy(
                screen = AppScreen.REPLAY,
                replay = ReplayUi(
                    saved = saved,
                    boards = build.first,
                    sides = build.second,
                    notations = build.third,
                    index = 0,
                    playing = false
                )
            )
        }
    }

    private fun buildReplay(saved: SavedGame): Triple<List<IntArray>, List<Side>, List<String>> {
        val start = Pieces.initialBoard()
        val boards = mutableListOf(start.copyOf())
        val sides = mutableListOf(Side.RED)
        val notations = mutableListOf<String>()
        for (move in saved.moves) {
            if (move.from !in 0 until Board.SIZE || move.to !in 0 until Board.SIZE) break
            val side = sides.last()
            notations.add(Notation.move(start, side, move.from, move.to))
            MoveGenerator.make(start, Move.encode(move.from, move.to))
            boards.add(start.copyOf())
            sides.add(side.opposite())
        }
        return Triple(boards, sides, notations)
    }

    fun replayStep(delta: Int) {
        val replay = _ui.value.replay ?: return
        replayJob?.cancel()
        val next = (replay.index + delta).coerceIn(0, replay.lastIndex)
        _ui.update { it.copy(replay = replay.copy(index = next, playing = false)) }
    }

    fun replayTo(index: Int) {
        val replay = _ui.value.replay ?: return
        replayJob?.cancel()
        val next = index.coerceIn(0, replay.lastIndex)
        _ui.update { it.copy(replay = replay.copy(index = next, playing = false)) }
    }

    fun toggleReplayPlay() {
        val replay = _ui.value.replay ?: return
        if (replay.playing) {
            replayJob?.cancel()
            _ui.update { it.copy(replay = replay.copy(playing = false)) }
            return
        }
        val startIndex = if (replay.atEnd) 0 else replay.index
        _ui.update { it.copy(replay = replay.copy(index = startIndex, playing = startIndex < replay.lastIndex)) }
        if (startIndex >= replay.lastIndex) return

        replayJob = viewModelScope.launch {
            while (isActive) {
                delay(900)
                val current = _ui.value.replay ?: break
                if (!current.playing) break
                if (current.atEnd) {
                    _ui.update { it.copy(replay = current.copy(playing = false)) }
                    break
                }
                val next = current.index + 1
                _ui.update {
                    it.copy(replay = current.copy(index = next, playing = next < current.lastIndex))
                }
            }
        }
    }

    fun closeReplay() {
        replayJob?.cancel()
        _ui.update { it.copy(screen = AppScreen.RECORDS, replay = null) }
        viewModelScope.launch { refreshGamesInternal() }
    }

    fun deleteSaved(id: String) {
        viewModelScope.launch {
            saveMutex.withLock { gameStore.delete(id) }
            refreshGamesInternal()
        }
    }

    fun deleteFinishedSaved() {
        viewModelScope.launch {
            saveMutex.withLock {
                for (saved in gameStore.list()) {
                    if (saved.finished) gameStore.delete(saved.id)
                }
            }
            refreshGamesInternal()
        }
    }

    fun setSound(enabled: Boolean) {
        sound.enabled = enabled
        _ui.update { it.copy(settings = it.settings.copy(sound = enabled)) }
        viewModelScope.launch { settingsStore.setSound(enabled) }
    }

    fun setAnimation(enabled: Boolean) {
        _ui.update { it.copy(settings = it.settings.copy(animation = enabled)) }
        viewModelScope.launch { settingsStore.setAnimation(enabled) }
    }

    fun setBoardTheme(theme: BoardTheme) {
        _ui.update { it.copy(settings = it.settings.copy(boardTheme = theme)) }
        viewModelScope.launch { settingsStore.setBoardTheme(theme) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _ui.update { it.copy(settings = it.settings.copy(themeMode = mode)) }
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setDefaultDifficulty(level: Difficulty) {
        _ui.update { it.copy(settings = it.settings.copy(difficulty = level)) }
        viewModelScope.launch {
            val s = _ui.value.settings
            settingsStore.setDefaults(level, s.humanSide, s.timeControl)
        }
    }

    fun onPause() {
        paused = true
        tickerJob?.cancel()
    }

    fun onResume() {
        paused = false
        lastTick = SystemClock.elapsedRealtime()
        val g = game
        if (g != null && !g.result.isOver && _ui.value.screen == AppScreen.GAME) {
            startTicker()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        lastTick = SystemClock.elapsedRealtime()
        if (timeControl == TimeControl.NONE) return
        tickerJob = viewModelScope.launch {
            while (isActive) {
                delay(250)
                val now = SystemClock.elapsedRealtime()
                val g = game
                if (g == null || g.result.isOver || _ui.value.screen != AppScreen.GAME || paused) {
                    lastTick = now
                    continue
                }
                val delta = now - lastTick
                lastTick = now
                if (delta <= 0) continue
                if (g.sideToMove == Side.RED) {
                    redTimeMs = max(0L, redTimeMs - delta)
                } else {
                    blackTimeMs = max(0L, blackTimeMs - delta)
                }
                updateTimes()
                if (redTimeMs <= 0 || blackTimeMs <= 0) {
                    val loser = if (redTimeMs <= 0) Side.RED else Side.BLACK
                    g.timeout(loser)
                    handleGameOver()
                }
            }
        }
    }

    private fun saveGame() {
        val g = game ?: return
        val id = gameId ?: UUID.randomUUID().toString().also {
            gameId = it
            createdAt = System.currentTimeMillis()
        }
        if (createdAt == 0L) createdAt = System.currentTimeMillis()
        val saved = SavedGame(
            id = id,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis(),
            difficulty = difficulty.name,
            humanSide = humanSide.name,
            timeControl = timeControl.name,
            moves = g.moves.map { SavedMove(it.from, it.to) },
            outcome = g.result.outcome.name,
            reason = g.result.reason,
            finished = g.result.isOver,
            redTimeMs = redTimeMs,
            blackTimeMs = blackTimeMs
        )
        viewModelScope.launch {
            saveMutex.withLock {
                if (gameStore.save(saved)) upsertSavedGame(saved)
            }
        }
    }

    private fun upsertSavedGame(saved: SavedGame) {
        _ui.update { state ->
            val games = (state.games.filterNot { it.id == saved.id } + saved)
                .sortedByDescending { it.updatedAt }
            state.copy(
                games = games,
                hasUnfinished = games.any { !it.finished }
            )
        }
    }

    private fun cancelAllJobs() {
        aiCancel = true
        aiJob?.cancel()
        hintJob?.cancel()
        tickerJob?.cancel()
        replayJob?.cancel()
    }

    override fun onCleared() {
        cancelAllJobs()
        sound.release()
        super.onCleared()
    }
}

fun formatClock(ms: Long): String {
    if (ms <= 0) return "00:00"
    val total = (ms + 999) / 1000
    val minutes = total / 60
    val seconds = total % 60
    return "%02d:%02d".format(minutes, seconds)
}

fun resultTitle(result: GameResult, humanSide: Side): String = when (result.outcome) {
    Outcome.ONGOING -> "對局進行中"
    Outcome.DRAW -> "和棋"
    Outcome.ABORTED -> "對局結束"
    Outcome.RED_WIN -> if (humanSide == Side.RED) "紅方勝 · 你贏了" else "紅方勝 · 你輸了"
    Outcome.BLACK_WIN -> if (humanSide == Side.BLACK) "黑方勝 · 你贏了" else "黑方勝 · 你輸了"
}

fun resultSubtitle(result: GameResult): String {
    if (!result.isOver) return ""
    return result.reason
}

