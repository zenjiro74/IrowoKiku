package com.example.zenjiro74.irowokiku.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zenjiro74.irowokiku.R
import com.example.zenjiro74.irowokiku.camera.CameraViewfinder
import com.example.zenjiro74.irowokiku.mapping.noteNameOf

/**
 * ビューファインダの上に、いま鳴っている音と指標を出す HUD を重ねた画面。
 */
@Composable
fun ColorHearingScreen(
    modifier: Modifier = Modifier,
    viewModel: ColorHearingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 音を聞きながら被写体を探すので、画面が消えると使い物にならない。
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReadoutCard(uiState)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { viewModel.setExposureLocked(!uiState.isExposureLocked) },
                ) {
                    Text(
                        stringResource(
                            if (uiState.isExposureLocked) {
                                R.string.action_exposure_locked
                            } else {
                                R.string.action_exposure_auto
                            },
                        ),
                    )
                }
                Button(onClick = viewModel::toggle) {
                    Text(
                        stringResource(
                            if (uiState.isRunning) R.string.action_stop else R.string.action_start,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadoutCard(uiState: ColorHearingUiState) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(
                    R.string.hud_pitch,
                    noteNameOf(uiState.frequencyHz),
                    uiState.frequencyHz,
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            PitchBar(position = uiState.pitchPosition)

            Text(
                text = stringResource(R.string.hud_colorfulness, uiState.colorfulness) +
                    "   " + stringResource(R.string.hud_luma, uiState.meanLuma),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (uiState.isTooDark) {
                Text(
                    text = stringResource(R.string.hud_too_dark),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

/** 110Hz〜1760Hz のどこにいるかを示す横バー。位置は対数スケール。 */
@Composable
private fun PitchBar(position: Float) {
    // 生の値は 30fps で細かく動くので、表示だけ少し慣性を持たせる。
    val animated by animateFloatAsState(targetValue = position, label = "pitch")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                // fillMaxWidth は 0 を受け付けないので、下限を僅かに持たせる。
                .fillMaxWidth(animated.coerceIn(0.001f, 1f))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
