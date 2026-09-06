import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 컨벤션 밖에서 java-library 를 직접 쓰는 JVM 모듈에 ktlint 만 붙인다.
 *
 * [JvmLibraryConventionPlugin] 은 Java/Kotlin 타깃까지 통일하므로, 타깃을 자기 사정으로 고정해야
 * 하는 모듈(현재 `:konsist`·`:feature:setting:domain` 이 Java 11)은 그것을 탈 수 없다. 그렇다고
 * ktlint 설정을 모듈마다 복붙하면 카탈로그 버전·리포터 설정이 갈라진다 — `afternote.android.lint`
 * 의 JVM 짝이다.
 */
class JvmLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")
            configureKtlint(isAndroid = false)
        }
    }
}
