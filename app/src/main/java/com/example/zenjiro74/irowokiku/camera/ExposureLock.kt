package com.example.zenjiro74.irowokiku.camera

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera

/**
 * 自動露出 (AE) と自動ホワイトバランス (AWB) のロックを切り替える。
 *
 * AWB が動いたままだと、カメラを向けた先の色かぶりをカメラ自身が打ち消してしまい、
 * 「カラフルさ」が場面によらず一定に均されてしまう。AE も同様に明るさを追い込むので、
 * 指標を安定させたい場面ではロックする。
 *
 * CameraX の公開 API には露出/WB ロックが無いので Camera2 interop を使う。
 */
@OptIn(ExperimentalCamera2Interop::class)
fun Camera.setAutoExposureAndWhiteBalanceLocked(locked: Boolean) {
    Camera2CameraControl.from(cameraControl).setCaptureRequestOptions(
        CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, locked)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, locked)
            .build(),
    )
}
