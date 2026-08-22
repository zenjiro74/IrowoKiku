# IrowoKiku

色を聞く。

カメラ映像の「カラフルさ」を測り、サイン波の周波数にマッピングして連続再生する Android アプリ。
カメラを向けると、映っているものの彩度の豊かさが音程として聞こえる。

AI駆動

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
about/     OssLicenses (生成済み一覧の読み込み)
ui/        ColorHearingScreen / HelpScreen / LicensesScreen (Compose)
```

画面はカメラ・使い方・ライセンスの 3 つだけなので、Navigation ライブラリは入れず
Compose の状態遷移で切り替えている。

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

## オープンソースライセンス表示

アプリ内の「オープンソースライセンス」画面の元データは、ビルド時に
`generateOssLicenses` タスクが `assets/oss_licenses.json` へ生成する。

- **収録物の列挙は自動**。解決済みの依存グラフから APK に入るアーティファクトを
  拾うので、依存を足しても一覧が古くならない
- **ライセンスの判定は対応表に固定**。`app/licenses/known-licenses.json` に
  グループ単位で書いている。POM の `<licenses>` を機械的に読む方式は取れない
  (guava と auto-value は親 POM にしか記載が無い) ため、人手で確認した対応を置く
- **対応表に無いグループが入るとビルドが落ちる**。依存追加時に更新漏れで
  ライセンス表示が不正確になるのを防ぐため

現状は 106 アーティファクト / 14 プロジェクト。`org.checkerframework` だけが MIT で、
残りは Apache-2.0。両方の全文を `assets/licenses/` に同梱しオフラインでも読める。

依存を追加してビルドが落ちたら、`app/licenses/known-licenses.json` の `groups` に
グループと `project` / `url` / `license` を足す。新しいライセンス種別なら `licenses` に
定義を足し、全文を `app/src/main/assets/licenses/` に置く。

## アイコン

`ic_launcher_foreground.xml` は生成物で、元は
`scripts/generate_launcher_icon.py`。色相環の扇形と正弦波のパスを計算して吐く。
形を変えるときは XML を直接いじらずスクリプト側を編集して再生成する。
