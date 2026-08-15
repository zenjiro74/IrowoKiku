package com.example.zenjiro74.irowokiku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zenjiro74.irowokiku.camera.CameraViewfinder

/**
 * メイン画面。ステップ6で HUD の見た目を仕上げる。
 */
@Composable
fun ColorHearingScreen(
    modifier: Modifier = Modifier,
    viewModel: ColorHearingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // バックグラウンドに回ったら鳴らし続けない。
    LifecycleResumeEffect(viewModel) {
        onPauseOrDispose { viewModel.stop() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        CameraViewfinder(
            analyzer = viewModel.analyzer,
            modifier = Modifier.fillMaxSize(),
            lockExposureAndWhiteBalance = uiState.isExposureLocked,
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("colorfulness %.1f".format(uiState.colorfulness))
            Text("%.1f Hz".format(uiState.frequencyHz))
            if (uiState.isTooDark) Text("暗すぎます")
            Button(onClick = { viewModel.setExposureLocked(!uiState.isExposureLocked) }) {
                Text(if (uiState.isExposureLocked) "AE/AWB ロック中" else "AE/AWB 自動")
            }
            Button(onClick = viewModel::toggle) {
                Text(if (uiState.isRunning) "停止" else "開始")
            }
        }
    }
}
