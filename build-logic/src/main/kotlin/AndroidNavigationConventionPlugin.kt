import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 네비게이션을 쓰는 모듈의 공통 의존성.
 *
 * Navigation 2 와 Navigation 3 를 함께 얹는다 — 로컬 스택 이관(#1698)이 진행되는 동안 루트
 * `NavHost` 는 Nav2 를 계속 쓰고, 각 피처는 그 아래에서 Nav3 `NavDisplay` 로 동작한다.
 * Nav2 제거는 루트를 `NavDisplay` 로 바꾸는 #1702 가 소유한다.
 */
class AndroidNavigationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            afterNoteDependencies {
                implementation("android-navigation-compose")
                implementation("androidx-navigation3-runtime")
                implementation("androidx-navigation3-ui")
                // NavEntry 범위 ViewModel 스코프 — rememberViewModelStoreNavEntryDecorator()
                implementation("androidx-lifecycle-viewmodel-navigation3")
                implementation("kotlinx-serialization-json")
            }
        }
    }
}
