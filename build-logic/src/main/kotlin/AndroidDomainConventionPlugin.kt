import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidDomainConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("afternote.android.library")
            pluginManager.apply("afternote.android.hilt")

            // 도메인 계약의 fake 정본은 계약 옆(src/testFixtures)에 둔다 — 유닛 테스트와
            // androidTest 가 각자 복사본을 들고 있으면 계약이 바뀔 때 고칠 곳이 갈라지고,
            // 그중 일부만 고친 채로 초록을 보게 된다 (#1030).
            extensions.configure<LibraryExtension> {
                testFixtures {
                    enable = true
                }
            }

            afterNoteDependencies {
                project(":core:model")
            }
        }
    }
}
