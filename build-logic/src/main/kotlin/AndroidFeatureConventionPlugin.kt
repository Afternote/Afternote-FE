import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                // 1. 우리가 만든 플러그인들을 조합하여 'Feature' 성격 정의
                apply("afternote.android.library.compose") // 기본 SDK + UI
                apply("afternote.android.hilt")            // DI
                // apply("afternote.android.navigation")   // (아직 안 만들었다면 생성 필요)
            }

            afterNoteDependencies {
                // 2. 모든 피처 모듈이 공통으로 의존하는 내부 모듈 연결
                project(":core:ui")
                project(":core:model")

                // 3. 피처 모듈 전용 라이브러리 (Hilt Navigation 등)
                implementation("androidx-hilt-navigation-compose")
            }
        }
    }
}