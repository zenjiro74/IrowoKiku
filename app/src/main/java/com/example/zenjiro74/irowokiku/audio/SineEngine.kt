package com.example.zenjiro74.irowokiku.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.max

private const val TAG = "SineEngine"

/** 1 回の write で書き出すフレーム数。44.1kHz なら約 12ms。 */
private const val BLOCK_FRAMES = 512

private const val BYTES_PER_FLOAT = 4

/**
 * [SineOscillator] を [AudioTrack] に流し込む再生エンジン。
 *
 * 専用スレッドでブロック単位に生成し、ブロッキング write でカーネルのバッファに合わせて
 * ペースを取る。[setFrequency] はどのスレッドからでも呼べる。
 */
class SineEngine(
    private val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    private val peakAmplitude: Float = DEFAULT_PEAK_AMPLITUDE,
) {
    private val oscillator = SineOscillator(sampleRate)

    private var track: AudioTrack? = null
    private var playbackThread: Thread? = null

    @Volatile
    private var running = false

    @Volatile
    private var muted = false

    val isRunning: Boolean
        get() = running

    /** ミュート状態を織り込んだ、いま目指すべき振幅。 */
    private val activeAmplitude: Float
        get() = if (muted) 0f else peakAmplitude

    /** [initialFrequencyHz] から再生を開始する。既に再生中なら何もしない。 */
    @Synchronized
    fun start(initialFrequencyHz: Float) {
        if (running) return

        val minBufferBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )
        if (minBufferBytes <= 0) {
            Log.e(TAG, "getMinBufferSize failed: $minBufferBytes")
            return
        }
        // 数ブロック分の余裕を持たせて、write が間に合わなくても途切れないようにする。
        val bufferBytes = max(minBufferBytes * 2, BLOCK_FRAMES * BYTES_PER_FLOAT * 4)

        val newTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        oscillator.snapFrequency(initialFrequencyHz)
        // 0 から立ち上げることで開始時のプチッというクリックを避ける。
        oscillator.targetAmplitude = activeAmplitude

        track = newTrack
        running = true
        newTrack.play()

        playbackThread = Thread({ playbackLoop(newTrack) }, "SineEngine").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    /** 目標周波数を変える。実際の遷移はポルタメントで滑らかに行われる。 */
    fun setFrequency(hz: Float) {
        oscillator.targetFrequencyHz = hz
    }

    /** 再生を続けたまま音量だけ落とす。振幅ランプ越しに効くのでクリックは出ない。 */
    fun setMuted(muted: Boolean) {
        this.muted = muted
        // 停止処理中は playbackLoop がフェードアウト中なので触らない。
        if (running) {
            oscillator.targetAmplitude = activeAmplitude
        }
    }

    /** フェードアウトを鳴らし切ってから停止する。 */
    @Synchronized
    fun stop() {
        if (!running) return
        running = false

        playbackThread?.join(STOP_TIMEOUT_MILLIS)
        playbackThread = null

        track?.let {
            // MODE_STREAM の stop() は書き込み済みのデータを再生し切ってから止まる。
            runCatching { it.stop() }
            it.release()
        }
        track = null
    }

    private fun playbackLoop(track: AudioTrack) {
        val block = FloatArray(BLOCK_FRAMES)
        while (running) {
            oscillator.fill(block)
            if (track.write(block, 0, block.size, AudioTrack.WRITE_BLOCKING) < 0) return
        }

        // 停止要求後は無音まで落としてから抜ける。途中で切ると波形が跳ねてノイズになる。
        oscillator.targetAmplitude = 0f
        while (!oscillator.isSilent) {
            oscillator.fill(block)
            if (track.write(block, 0, block.size, AudioTrack.WRITE_BLOCKING) < 0) return
        }
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 44_100
        const val DEFAULT_PEAK_AMPLITUDE = 0.25f
        private const val STOP_TIMEOUT_MILLIS = 500L
    }
}
