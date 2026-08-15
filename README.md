# IrowoKiku

色を聞く。

カメラ映像の「カラフルさ」を測り、サイン波の周波数にマッピングして連続再生する Android アプリ。
カメラを向けると、映っているものの彩度の豊かさが音程として聞こえる。

## 仕組み

| 段階 | 内容 |
| --- | --- |
| 取得 | CameraX `ImageAnalysis` / RGBA_8888 / 640x480 / `KEEP_ONLY_LATEST` |
| 解析 | Hasler–Süsstrunk colorfulness metric。4x4 ボックス平均でサブサンプル |
| 写像 | 対数マッピングで 110Hz (A2) 〜 1760Hz (A6) の 4 オクターブ |
| 発音 | `AudioTrack` (MODE_STREAM / PCM_FLOAT / 44.1kHz / mono)。サンプル単位ポルタメントで位相連続 |

指標は Hasler–Süsstrunk 論文の官能スケール (15 = slightly colorful, 82 = highly colorful) を
レンジの両端に割り当てている。

暗所ではセンサノイズが指標を押し上げて偽の「カラフルさ」を生むため、平均輝度が閾値を
下回る間は周波数を保持して音をミュートする。AE/AWB は画面から手動でロックできる
(オートのままだとカメラ自身が色かぶりを打ち消し、指標が場面によらず均されてしまう)。

## 構成

```
audio/     SineOscillator (純 DSP) / SineEngine (AudioTrack ラッパ)
camera/    Colorfulness (純関数) / ColorfulnessAnalyzer / CameraViewfinder / ExposureLock
mapping/   FrequencyMapping / ExponentialSmoother / noteNameOf
ui/        ColorHearingScreen (Compose) / ColorHearingViewModel
```

DSP と指標計算は Android API に依存させず、JVM 単体テストで検証している。

## ビルドと実行

```bash
./gradlew :app:installDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

エミュレータでも動くが、仮想シーンのカメラはフレームレートが極端に低く指標がほとんど
動かないため、実際の挙動確認には実機を推奨。
