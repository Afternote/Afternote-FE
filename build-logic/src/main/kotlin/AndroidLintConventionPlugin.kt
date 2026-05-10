import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class AndroidLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")

            // VersionCatalog 명시적 접근
            val libs =
                extensions
                    .getByType<VersionCatalogsExtension>()
                    .named("libs")

            extensions.configure(KtlintExtension::class.java) {
                // ktlint-gradle 의 default 버전 대신 libs.versions.toml 의 ktlint 버전을
                // 명시적으로 사용해 CI(ScaCap/action-ktlint) 와 룰셋을 일치시킨다.
                version.set(libs.findVersion("ktlint").get().requiredVersion)
                debug.set(false)
                verbose.set(true)
                android.set(true)
                outputToConsole.set(true)
                ignoreFailures.set(false)
                filter {
                    exclude { it.file.path.contains("build/") }
                }
            }

            dependencies.add(
                "ktlintRuleset",
                libs.findLibrary("compose-rules").get()
            )
        }
    }
}