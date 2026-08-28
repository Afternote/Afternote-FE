import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

internal fun Project.configureKtlint(isAndroid: Boolean) {
    val libs =
        extensions
            .getByType<VersionCatalogsExtension>()
            .named("libs")

    extensions.configure(KtlintExtension::class.java) {
        // ktlint binary 버전 정본은 카탈로그 하나다. CI도 같은 Gradle 태스크를 실행하며,
        // 추가 룰셋(compose-rules 등)은 적재하지 않는다(#1012).
        version.set(libs.findVersion("ktlint").get().requiredVersion)
        debug.set(false)
        verbose.set(true)
        android.set(isAndroid)
        outputToConsole.set(true)
        ignoreFailures.set(false)
        // PLAIN은 콘솔·로컬용, CHECKSTYLE은 CI job summary용 기계 판독 결과다.
        reporters {
            reporter(ReporterType.PLAIN)
            reporter(ReporterType.CHECKSTYLE)
        }
        filter {
            exclude { it.file.path.contains("build/") }
        }
    }
}
