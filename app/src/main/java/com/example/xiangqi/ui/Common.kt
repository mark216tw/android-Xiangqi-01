package com.example.xiangqi.ui

import com.example.xiangqi.ai.Difficulty
import com.example.xiangqi.data.SavedGame
import com.example.xiangqi.model.Outcome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

fun difficultyOf(saved: SavedGame): Difficulty =
    runCatching { Difficulty.valueOf(saved.difficulty) }.getOrDefault(Difficulty.MEDIUM)

fun savedOutcomeLabel(saved: SavedGame): String = when (saved.outcomeValue) {
    Outcome.ONGOING -> "進行中"
    Outcome.DRAW -> "和棋"
    Outcome.ABORTED -> "已結束"
    Outcome.RED_WIN -> "紅方勝"
    Outcome.BLACK_WIN -> "黑方勝"
}

fun savedResultText(saved: SavedGame): String {
    if (saved.outcomeValue == Outcome.ONGOING) return "進行中"
    val base = savedOutcomeLabel(saved)
    return if (saved.reason.isBlank()) base else "$base · ${saved.reason}"
}
