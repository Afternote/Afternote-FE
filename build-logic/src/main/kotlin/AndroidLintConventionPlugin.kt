import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

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
                // ktlint binary 버전 정본은 이 카탈로그 하나다 — CI 도 같은 Gradle 태스크를
                // 돌리므로 워크플로에 손으로 적은 버전이 없다 (#1012).
                // 추가 룰셋(compose-rules 등)은 적재하지 않는다.
                version.set(libs.findVersion("ktlint").get().requiredVersion)
                debug.set(false)
                verbose.set(true)
                android.set(true)
                outputToConsole.set(true)
                ignoreFailures.set(false)
                // PLAIN 은 콘솔·로컬용, CHECKSTYLE 은 CI 가 job summary 로 렌더할 기계 판독용.
                reporters {
                    reporter(ReporterType.PLAIN)
                    reporter(ReporterType.CHECKSTYLE)
                }
                filter {
                    exclude { it.file.path.contains("build/") }
                }
            }
        }
    }
}
