package com.example.zenjiro74.irowokiku.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier

/** 画面はこの 3 つだけなので Navigation ライブラリは入れず、状態遷移で切り替える。 */
private enum class Screen { Camera, Help, Licenses }

@Composable
fun IrowoKikuApp(modifier: Modifier = Modifier) {
    var screen by rememberSaveable { mutableStateOf(Screen.Camera) }

    when (screen) {
        // カメラ画面は常に構成に残しておく。付け外しするとバインドし直しになり、
        // 戻るたびにプレビューが一瞬黒くなる。
        Screen.Camera -> CameraPermissionGate {
            ColorHearingScreen(
                modifier = modifier,
                onOpenHelp = { screen = Screen.Help },
                onOpenLicenses = { screen = Screen.Licenses },
            )
        }

        Screen.Help -> HelpScreen(onBack = { screen = Screen.Camera }, modifier = modifier)

        Screen.Licenses -> LicensesScreen(onBack = { screen = Screen.Camera }, modifier = modifier)
    }
}
