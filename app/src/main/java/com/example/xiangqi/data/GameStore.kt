package com.example.xiangqi.data

import android.content.Context
import com.example.xiangqi.model.Outcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SavedMove(val from: Int, val to: Int)

@Serializable
data class SavedGame(
    val schemaVersion: Int = 1,
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val difficulty: String,
    val humanSide: String,
    val timeControl: String,
    val moves: List<SavedMove>,
    val outcome: String,
    val reason: String,
    val finished: Boolean,
    val redTimeMs: Long = 0L,
    val blackTimeMs: Long = 0L
) {
    val outcomeValue: Outcome
        get() = runCatching { Outcome.valueOf(outcome) }.getOrDefault(Outcome.ONGOING)
}

class GameStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val dir: File
        get() = File(context.filesDir, "games").also { if (!it.exists()) it.mkdirs() }

    suspend fun list(): List<SavedGame> = withContext(Dispatchers.IO) {
        val files = dir.listFiles { f -> f.isFile && f.extension == "json" } ?: return@withContext emptyList()
        files.mapNotNull { file ->
            runCatching {
                json.decodeFromString<SavedGame>(file.readText())
            }.getOrNull()
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun latestUnfinished(): SavedGame? = withContext(Dispatchers.IO) {
        list().firstOrNull { !it.finished }
    }

    suspend fun load(id: String): SavedGame? = withContext(Dispatchers.IO) {
        val file = File(dir, "$id.json")
        if (!file.exists()) return@withContext null
        runCatching { json.decodeFromString<SavedGame>(file.readText()) }.getOrNull()
    }

    suspend fun save(game: SavedGame): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(dir, "${game.id}.json")
            val temp = File(dir, "${game.id}.json.tmp")
            temp.writeText(json.encodeToString(SavedGame.serializer(), game))
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
        }.isSuccess
    }

    suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { File(dir, "$id.json").delete() }.getOrDefault(false)
    }
}
