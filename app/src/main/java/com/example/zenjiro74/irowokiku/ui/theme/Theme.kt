package com.example.zenjiro74.irowokiku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * ビューファインダの上に HUD を重ねる画面なので、常にダーク配色で固定する。
 * 端末のライト/ダーク設定に追従させると、明るい背景の上に白文字が乗って読めなくなる。
 */
private val ColorScheme = darkColorScheme(
    primary = Color(0xFF7FD1FF),
    onPrimary = Color(0xFF00344B),
    secondary = Color(0xFFFFB870),
    background = Color(0xFF000000),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF141414),
    onSurface = Color(0xFFECECEC),
)

@Composable
fun IrowoKikuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content,
    )
}
