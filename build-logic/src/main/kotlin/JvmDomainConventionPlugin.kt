import org.gradle.api.Plugin
import org.gradle.api.Project

/** Android SDK 없이 도메인 계약과 그 계약의 공유 fake 를 제공하는 JVM 모듈 규약이다. */
class JvmDomainConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("afternote.jvm.library")
            // 계약 fake 정본은 계약 옆 src/testFixtures 에 두고 JVM/Android 소비자가 함께 쓴다.
            pluginManager.apply("java-test-fixtures")

            afterNoteDependencies {
                // 생성자 주입 표식만 컴파일한다. Hilt/KSP 는 최종 Android 소비 모듈이 담당한다.
                implementation("javax-inject")
            }
        }
    }
}
