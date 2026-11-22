package com.wordlight.game.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class Sfx(val assetPath: String) {
    SELECT("sfx/select.ogg"),
    WORD("sfx/word.ogg"),
    BONUS("sfx/bonus.ogg"),
    INVALID("sfx/invalid.ogg"),
    COMPLETE("sfx/complete.ogg"),
}

interface SoundPlayer {
    /** [pitch] in 0.5..2.0 — used to make each extra selected letter sound a little higher. */
    fun play(sfx: Sfx, pitch: Float = 1f)
}

/**
 * Low-latency SoundPool player. Sound files are optional: any missing asset is
 * simply skipped, so the game builds and runs without audio files.
 */
@Singleton
class SoundPoolPlayer @Inject constructor(
    @ApplicationContext context: Context,
) : SoundPlayer {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds: Map<Sfx, Int> = Sfx.entries.mapNotNull { sfx ->
        runCatching { sfx to pool.load(context.assets.openFd(sfx.assetPath), 1) }.getOrNull()
    }.toMap()

    override fun play(sfx: Sfx, pitch: Float) {
        val id = soundIds[sfx] ?: return
        pool.play(id, 0.8f, 0.8f, 1, 0, pitch.coerceIn(0.5f, 2f))
    }
}
