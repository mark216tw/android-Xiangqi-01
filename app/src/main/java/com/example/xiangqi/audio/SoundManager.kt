package com.example.xiangqi.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.xiangqi.R
import com.example.xiangqi.model.MoveRecord
import com.example.xiangqi.model.Pieces

class SoundManager(context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val moveId = pool.load(context, R.raw.move, 1)
    private val captureId = pool.load(context, R.raw.capture, 1)
    private val checkId = pool.load(context, R.raw.check, 1)
    private val winId = pool.load(context, R.raw.win, 1)
    private val loseId = pool.load(context, R.raw.lose, 1)

    var enabled: Boolean = true

    fun playMove(record: MoveRecord) {
        if (!enabled) return
        val id = when {
            record.gaveCheck -> checkId
            record.captured != Pieces.EMPTY -> captureId
            else -> moveId
        }
        pool.play(id, 0.9f, 0.9f, 1, 0, 1f)
    }

    fun playWin() {
        if (enabled) pool.play(winId, 1f, 1f, 1, 0, 1f)
    }

    fun playLose() {
        if (enabled) pool.play(loseId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}
