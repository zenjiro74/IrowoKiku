import org.gradle.api.artifacts.component.ModuleComponentIdentifier

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.zenjiro74.irowokiku"
    // Compose BOM 2026.08.00 / core-ktx 1.19.0 が API 37 でのコンパイルを要求する。
    // targetSdk は新しい実行時挙動を取り込まないよう 36 のまま据え置く。
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.zenjiro74.irowokiku"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    testImplementation(libs.junit)
}

/**
 * APK に同梱される依存を解決済みグラフから拾い、`licenses/known-licenses.json` の対応表と
 * 突き合わせて `assets/oss_licenses.json` を書き出す。
 *
 * 収録物の列挙は自動なので依存を足しても取りこぼさない。一方でライセンスの判定は
 * 対応表に固定してある (POM の <licenses> は親 POM 頼みのものがあり機械的には決まらない)。
 * 対応表に無いグループが現れたらビルドを失敗させ、更新漏れを気付けるようにしている。
 */
abstract class GenerateOssLicensesTask : DefaultTask() {

    /** "group:artifact:version" の列。解決済み依存グラフから流し込まれる。 */
    @get:Input
    abstract val artifactCoordinates: ListProperty<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val knownLicenses: RegularFileProperty

    /** ライセンス全文を置いてある assets のルート。全文の実在確認に使う。 */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val assetsRoot: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        @Suppress("UNCHECKED_CAST")
        val known = groovy.json.JsonSlurper()
            .parse(knownLicenses.get().asFile) as Map<String, Any>
        val licenses = known["licenses"] as Map<String, Map<String, String>>
        val groups = known["groups"] as Map<String, Map<String, String>>

        // 長いグループ名から先に照合する。"org.jetbrains.kotlin" が "org.jetbrains" に
        // 吸われないようにするため。
        val groupKeys = groups.keys.sortedByDescending { it.length }

        val unmapped = sortedSetOf<String>()
        val byProject = sortedMapOf<String, MutableSet<String>>()
        val metaByProject = mutableMapOf<String, Map<String, String>>()

        for (coordinate in artifactCoordinates.get()) {
            val group = coordinate.substringBefore(':')
            val key = groupKeys.firstOrNull { group.startsWith(it) }
            if (key == null) {
                unmapped += group
                continue
            }
            val meta = groups.getValue(key)
            val project = meta.getValue("project")
            metaByProject[project] = meta
            byProject.getOrPut(project) { sortedSetOf() } += coordinate
        }

        if (unmapped.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("ライセンス対応表に無い依存グループがあります:")
                    unmapped.forEach { appendLine("  - $it") }
                    appendLine()
                    appendLine("${knownLicenses.get().asFile} の groups に追記してください。")
                    appendLine("ライセンスは POM の <licenses> で確認できますが、親 POM にしか")
                    appendLine("書かれていない場合があるので実際の配布物で裏を取ること。")
                },
            )
        }

        val assetsDir = assetsRoot.get().asFile
        val brokenLicenses = metaByProject.values.mapNotNull { meta ->
            val licenseId = meta.getValue("license")
            val license = licenses[licenseId]
                ?: return@mapNotNull "${meta["project"]}: 未定義のライセンス id '$licenseId'"
            val text = assetsDir.resolve(license.getValue("asset"))
            if (text.isFile) null else "${meta["project"]}: 全文が見つかりません ($text)"
        }
        if (brokenLicenses.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("ライセンス定義に不備があります:")
                    brokenLicenses.forEach { appendLine("  - $it") }
                },
            )
        }

        val entries = byProject.map { (project, artifacts) ->
            val meta = metaByProject.getValue(project)
            mapOf(
                "project" to project,
                "url" to meta["url"],
                "license" to meta["license"],
                "artifacts" to artifacts.toList(),
            )
        }

        val output = mapOf("licenses" to licenses, "entries" to entries)
        val file = outputDirectory.get().asFile.resolve("oss_licenses.json")
        file.parentFile.mkdirs()
        file.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(output)))

        logger.lifecycle(
            "oss_licenses.json: ${entries.size} プロジェクト / " +
                "${artifactCoordinates.get().size} アーティファクト",
        )
    }
}

androidComponents {
    onVariants { variant ->
        val coordinates = variant.runtimeConfiguration.incoming.artifacts.resolvedArtifacts
            .map { artifacts ->
                artifacts
                    .mapNotNull { it.id.componentIdentifier as? ModuleComponentIdentifier }
                    .map { "${it.group}:${it.module}:${it.version}" }
                    .distinct()
                    .sorted()
            }

        val task = tasks.register<GenerateOssLicensesTask>(
            "generate${variant.name.replaceFirstChar(Char::uppercase)}OssLicenses",
        ) {
            artifactCoordinates.set(coordinates)
            knownLicenses.set(layout.projectDirectory.file("licenses/known-licenses.json"))
            assetsRoot.set(layout.projectDirectory.dir("src/main/assets"))
        }

        variant.sources.assets?.addGeneratedSourceDirectory(
            task,
            GenerateOssLicensesTask::outputDirectory,
        )
    }
}
