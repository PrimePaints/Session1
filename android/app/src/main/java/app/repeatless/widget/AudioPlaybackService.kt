package app.repeatless.widget

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import app.repeatless.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class AudioPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val ACTION_PLAY_PAD = "app.repeatless.action.PLAY_PAD"
        const val EXTRA_PAD_ID = "extra_pad_id"

        fun playPad(context: Context, padId: String) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_PLAY_PAD
                putExtra(EXTRA_PAD_ID, padId)
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PLAY_PAD) {
            val padId = intent.getStringExtra(EXTRA_PAD_ID)
            if (!padId.isNullOrBlank()) {
                playPadAudio(padId, startId)
            } else {
                stopSelf(startId)
            }
        } else {
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun playPadAudio(padId: String, startId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val pad = db.soundboardDao().getPadByIdSync(padId)
                if (pad != null) {
                    val audioDir = File(applicationContext.filesDir, "audio_clips")
                    val audioFile = File(audioDir, pad.audioFileName)

                    if (audioFile.exists() && audioFile.length() > 0) {
                        mediaPlayer?.let {
                            if (it.isPlaying) it.stop()
                            it.release()
                        }

                        val player = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build()
                            )
                            setDataSource(audioFile.absolutePath)
                            prepare()
                        }

                        mediaPlayer = player
                        player.setOnCompletionListener {
                            player.release()
                            mediaPlayer = null
                            stopSelf(startId)
                        }
                        player.setOnErrorListener { _, _, _ ->
                            player.release()
                            mediaPlayer = null
                            stopSelf(startId)
                            true
                        }
                        player.start()
                    } else {
                        stopSelf(startId)
                    }
                } else {
                    stopSelf(startId)
                }
            } catch (e: Exception) {
                Log.e("AudioPlaybackService", "Error playing pad audio $padId", e)
                stopSelf(startId)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }
}
