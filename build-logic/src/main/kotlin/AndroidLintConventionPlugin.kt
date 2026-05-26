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
                // ktlint binary 버전을 CI(ScaCap/action-ktlint) 와 동일하게 핀.
                // 추가 룰셋(compose-rules 등)은 CI 가 적재하지 않으므로 로컬도 적재하지 않는다.
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
        }
    }
}
