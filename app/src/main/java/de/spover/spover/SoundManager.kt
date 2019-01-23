package de.spover.spover

import android.content.Context
import android.media.SoundPool
import de.spover.spover.overlay.OverlayService


// in contrary to class objects may not have a constructor
object SoundManager {

    private var TAG = OverlayService::class.java.simpleName
    private var MIN_MILLISEC_DIFF_BETWEEN_REPLAY = 30000

    private var soundManager = SoundManager
    private var soundPool: SoundPool = SoundPool.Builder().setMaxStreams(1).build()
    private var warningSound: Int = -1

    private var lastTimePlayed = System.currentTimeMillis()

    fun getInstance(): SoundManager {
        return soundManager
    }

    fun loadSound(context: Context) {
        warningSound = soundPool.load(context, R.raw.beep, 1)
    }

    fun play() {
        val diff = System.currentTimeMillis() - lastTimePlayed

        if (diff > MIN_MILLISEC_DIFF_BETWEEN_REPLAY) {
            soundPool.play(warningSound, 1.0f, 1.0f, 0, 0, 1.0f)
            lastTimePlayed = System.currentTimeMillis()
        }
    }

    fun stop() {
        soundPool.stop(warningSound)
    }
}