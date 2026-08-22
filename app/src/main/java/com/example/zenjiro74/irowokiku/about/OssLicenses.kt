package com.example.zenjiro74.irowokiku.about

import android.content.res.AssetManager
import org.json.JSONObject

/** 1 つのライセンス種別。全文は assets に同梱している。 */
data class OssLicense(
    val id: String,
    val name: String,
    val textAsset: String,
)

/** 同じライセンスで配布されているプロジェクトと、そこから同梱しているアーティファクト。 */
data class OssEntry(
    val project: String,
    val url: String,
    val license: OssLicense,
    val artifacts: List<String>,
)

/**
 * ビルド時に生成された `oss_licenses.json` を読む。
 *
 * 生成側は app/build.gradle.kts の GenerateOssLicensesTask。収録物の列挙は解決済みの
 * 依存グラフから自動で行われるので、この一覧は実際に APK に入っているものと一致する。
 *
 * 中身はビルド時に確定していて実行中に変わらないため、一度読んだら保持する。
 * 画面を開き直すたびに asset の読み直しと JSON パースが走るのを避ける。
 */
object OssLicenses {

    private const val MANIFEST_ASSET = "oss_licenses.json"

    @Volatile
    private var cachedEntries: List<OssEntry>? = null

    private val cachedTexts = mutableMapOf<String, String>()

    fun load(assets: AssetManager): List<OssEntry> =
        cachedEntries ?: parse(assets).also { cachedEntries = it }

    @Synchronized
    fun readLicenseText(assets: AssetManager, license: OssLicense): String =
        cachedTexts.getOrPut(license.id) {
            assets.open(license.textAsset).bufferedReader().use { it.readText() }
        }

    private fun parse(assets: AssetManager): List<OssEntry> {
        val root = JSONObject(assets.open(MANIFEST_ASSET).bufferedReader().use { it.readText() })

        val licenses = root.getJSONObject("licenses").let { json ->
            json.keys().asSequence().associateWith { id ->
                val license = json.getJSONObject(id)
                OssLicense(
                    id = id,
                    name = license.getString("name"),
                    textAsset = license.getString("asset"),
                )
            }
        }

        val entries = root.getJSONArray("entries")
        return (0 until entries.length()).map { index ->
            val entry = entries.getJSONObject(index)
            val artifacts = entry.getJSONArray("artifacts")
            OssEntry(
                project = entry.getString("project"),
                url = entry.getString("url"),
                license = licenses.getValue(entry.getString("license")),
                artifacts = (0 until artifacts.length()).map { artifacts.getString(it) },
            )
        }
    }
}
