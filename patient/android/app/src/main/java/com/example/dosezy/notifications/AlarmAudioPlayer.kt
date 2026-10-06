package com.example.dosezy.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.dosezy.data.model.AlarmSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

object AlarmAudioPlayer {
    private const val TAG = "AlarmAudioPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var autoSilenceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    @Synchronized
    fun isPlaying(): Boolean {
        val mpPlaying = try { mediaPlayer?.isPlaying == true } catch (_: Exception) { false }
        val rtPlaying = try { ringtone?.isPlaying == true } catch (_: Exception) { false }
        return mpPlaying || rtPlaying
    }

    @Synchronized
    fun play(
        context: Context,
        sound: AlarmSound,
        customPath: String? = null,
        autoSilenceSeconds: Int = 0
    ) {
        if (isPlaying()) {
            Log.d(TAG, "Alarm sound is already playing, refreshing auto-silence timeout if needed")
            if (autoSilenceSeconds > 0) {
                autoSilenceJob?.cancel()
                autoSilenceJob = scope.launch {
                    delay(autoSilenceSeconds * 1000L)
                    Log.d(TAG, "Auto-silencing alarm after $autoSilenceSeconds seconds")
                    stop()
                    AlarmActivity.stopActiveAlarm()
                }
            }
            return
        }

        stop()

        val shouldLoop = autoSilenceSeconds != -1

        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val lockDuration = when {
                autoSilenceSeconds > 0 -> (autoSilenceSeconds + 30) * 1000L
                autoSilenceSeconds == -1 -> 60 * 1000L
                else -> 10 * 60 * 1000L
            }
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Dosezy:AlarmAudioWakeLock")?.apply {
                acquire(lockDuration)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring audio wakelock", e)
        }

        startVibration(context, repeat = shouldLoop)

        var playbackStarted = false

        // 1. Try Custom Sound if configured
        if (sound == AlarmSound.CUSTOM && !customPath.isNullOrEmpty() && File(customPath).exists()) {
            try {
                Log.d(TAG, "Attempting custom alarm sound from: $customPath")
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(customPath)
                    isLooping = shouldLoop
                    if (!shouldLoop) {
                        setOnCompletionListener {
                            Log.d(TAG, "Single-play custom audio finished")
                            stop()
                            AlarmActivity.stopActiveAlarm()
                        }
                    }
                    prepare()
                    start()
                }
                playbackStarted = true
            } catch (e: Exception) {
                Log.e(TAG, "Custom audio playback failed, falling back to default alarm", e)
                try { mediaPlayer?.release() } catch (_: Exception) {}
                mediaPlayer = null
            }
        }

        // 2. Try Raw Resource Sound if configured
        if (!playbackStarted && sound.rawResId != null) {
            try {
                Log.d(TAG, "Attempting raw resource alarm sound: ${sound.name}")
                val afd = context.resources.openRawResourceFd(sound.rawResId)
                if (afd != null) {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        isLooping = shouldLoop
                        if (!shouldLoop) {
                            setOnCompletionListener {
                                Log.d(TAG, "Single-play raw audio finished")
                                stop()
                                AlarmActivity.stopActiveAlarm()
                            }
                        }
                        prepare()
                        start()
                    }
                    // Guard: Close AssetFileDescriptor only AFTER prepare() to prevent Bad file descriptor status=0x1 failure in MediaPlayer native layer
                    try { afd.close() } catch (_: Exception) {}
                    playbackStarted = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Raw resource playback failed, falling back to default alarm", e)
                try { mediaPlayer?.release() } catch (_: Exception) {}
                mediaPlayer = null
            }
        }

        // 3. Fallback: Always play system default alarm ringtone (never silent!)
        if (!playbackStarted) {
            try {
                Log.d(TAG, "Playing default system alarm ringtone")
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                var started = false
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        setDataSource(context, alarmUri)
                        isLooping = shouldLoop
                        if (!shouldLoop) {
                            setOnCompletionListener {
                                Log.d(TAG, "Single-play fallback ringtone finished")
                                stop()
                                AlarmActivity.stopActiveAlarm()
                            }
                        }
                        prepare()
                        start()
                    }
                    started = true
                } catch (e: Exception) {
                    Log.w(TAG, "MediaPlayer failed for default ringtone URI, falling back to RingtoneManager", e)
                    // Guard: Release and nullify failed MediaPlayer instance to prevent leaking native audio sink and hardware decoders upon RingtoneManager fallback
                    try { mediaPlayer?.release() } catch (_: Exception) {}
                    mediaPlayer = null
                }

                if (!started) {
                    ringtone = RingtoneManager.getRingtone(context.applicationContext, alarmUri)?.apply {
                        audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            isLooping = shouldLoop
                        }
                        play()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting fallback default ringtone", e)
            }
        }

        if (autoSilenceSeconds == -1) {
            autoSilenceJob?.cancel()
            autoSilenceJob = scope.launch {
                var elapsed = 0L
                while (isPlaying() && elapsed < 30_000L) {
                    delay(500L)
                    elapsed += 500L
                }
                Log.d(TAG, "Single-play alarm completed or timed out after $elapsed ms")
                stop()
                AlarmActivity.stopActiveAlarm()
            }
        } else if (autoSilenceSeconds > 0) {
            autoSilenceJob?.cancel()
            autoSilenceJob = scope.launch {
                delay(autoSilenceSeconds * 1000L)
                Log.d(TAG, "Auto-silencing alarm after $autoSilenceSeconds seconds")
                stop()
                AlarmActivity.stopActiveAlarm()
            }
        }
    }

    @Synchronized
    fun startVibration(context: Context, repeat: Boolean = true) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibrator = vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            val pattern = longArrayOf(0, 1000, 1000)
            val repeatIndex = if (repeat) 0 else -1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(pattern, repeatIndex), audioAttributes)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, repeatIndex)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration", e)
        }
    }

    @Synchronized
    fun stop() {
        autoSilenceJob?.cancel()
        autoSilenceJob = null

        // Guard: Catch and release MediaPlayer independently so an IllegalStateException during isPlaying or stop does not leak native mediaserver decoders
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) player.stop()
            } catch (_: Exception) {}
            try {
                player.release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null

        try {
            ringtone?.let {
                if (it.isPlaying) it.stop()
            }
        } catch (_: Exception) {}
        ringtone = null

        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
        vibrator = null

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null
    }
}
