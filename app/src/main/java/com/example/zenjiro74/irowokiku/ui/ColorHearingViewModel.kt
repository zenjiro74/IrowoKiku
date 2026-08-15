package com.example.zenjiro74.irowokiku.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.zenjiro74.irowokiku.audio.SineEngine
import com.example.zenjiro74.irowokiku.camera.ColorfulnessAnalyzer
import com.example.zenjiro74.irowokiku.camera.FrameStats
import com.example.zenjiro74.irowokiku.mapping.ExponentialSmoother
import com.example.zenjiro74.irowokiku.mapping.FrequencyMapping
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ColorHearingUiState(
    val isRunning: Boolean = false,
    val colorfulness: Float = 0f,
    val meanLuma: Float = 0f,
    val frequencyHz: Float = FrequencyMapping.DEFAULT_MIN_FREQUENCY_HZ,
    /** 周波数レンジ内の位置 (0..1)。HUD のバー表示用。 */
    val pitchPosition: Float = 0f,
    /** 暗すぎて指標が信用できない状態。周波数を保持して音をミュートする。 */
    val isTooDark: Boolean = false,
    /** AE/AWB をロックしているか。 */
    val isExposureLocked: Boolean = true,
)

/**
 * 解析結果を周波数へ写してサイン波エンジンに流し込む。
 *
 * [analyzer] は CameraX の解析スレッドから呼ばれ、[uiState] は Compose から購読される。
 * 両者の受け渡しはスレッドセーフな [MutableStateFlow] と [SineEngine] の volatile
 * フィールド越しに行うのでロックを持たない。
 */
class ColorHearingViewModel : ViewModel() {

    private val engine = SineEngine()
    private val mapping = FrequencyMapping()
    private val smoother = ExponentialSmoother()

    private val _uiState = MutableStateFlow(ColorHearingUiState())
    val uiState: StateFlow<ColorHearingUiState> = _uiState.asStateFlow()

    val analyzer = ColorfulnessAnalyzer(onResult = ::onFrame)

    private var frameCount = 0L

    private fun onFrame(stats: FrameStats) {
        // 暗所ではセンサノイズが rg/yb の分散を押し上げ、無彩色の被写体でも
        // 指標が跳ね上がる。信用できないので周波数を据え置いて音を止める。
        val isTooDark = stats.meanLuma < DARK_LUMA_THRESHOLD
        engine.setMuted(isTooDark)

        if (isTooDark) {
            logMeasurement(stats, _uiState.value.colorfulness, _uiState.value.frequencyHz)
            _uiState.update { it.copy(meanLuma = stats.meanLuma, isTooDark = true) }
            return
        }

        val colorfulness = smoother.update(stats.colorfulness)
        val frequencyHz = mapping.toFrequency(colorfulness)

        engine.setFrequency(frequencyHz)
        logMeasurement(stats, colorfulness, frequencyHz)

        _uiState.update {
            it.copy(
                colorfulness = colorfulness,
                meanLuma = stats.meanLuma,
                frequencyHz = frequencyHz,
                pitchPosition = mapping.normalizedPosition(frequencyHz),
                isTooDark = false,
            )
        }
    }

    /** レンジ調整のための実測ログ。数フレームに 1 回だけ出す。 */
    private fun logMeasurement(stats: FrameStats, smoothed: Float, frequencyHz: Float) {
        if (frameCount++ % LOG_INTERVAL_FRAMES != 0L) return
        Log.d(
            TAG,
            "#$frameCount raw=%.1f smoothed=%.1f luma=%.1f -> %.1fHz".format(
                stats.colorfulness,
                smoothed,
                stats.meanLuma,
                frequencyHz,
            ),
        )
    }

    fun setExposureLocked(locked: Boolean) {
        _uiState.update { it.copy(isExposureLocked = locked) }
    }

    fun toggle() {
        if (engine.isRunning) stop() else start()
    }

    fun start() {
        // 直近のフレームで決まっている周波数から鳴らし始める。
        engine.start(_uiState.value.frequencyHz)
        _uiState.update { it.copy(isRunning = engine.isRunning) }
    }

    fun stop() {
        engine.stop()
        _uiState.update { it.copy(isRunning = false) }
    }

    override fun onCleared() {
        engine.stop()
    }

    private companion object {
        const val TAG = "IrowoKiku"
        const val LOG_INTERVAL_FRAMES = 15L

        /** これを下回る平均輝度 (0-255) では指標をノイズとみなす。 */
        const val DARK_LUMA_THRESHOLD = 30f
    }
}
