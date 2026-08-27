import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * 커버리지 규약 — kover 를 적용하고 «ci» 변형과 리포트 필터를 건다.
 *
 * 예전에는 루트 `build.gradle.kts` 가 `subprojects` 를 configuration time 에 열거하며 각
 * 프로젝트의 `pluginManager`·`extensions` 를 직접 조작했다. 이 저장소에서 모듈 공통 설정이
 * convention plugin 으로 들어가지 않는 유일한 자리였고, Gradle project isolation 과
 * 비호환이라 그 기능을 켜는 시점의 첫 걸림돌이었다(#918 — #864 승인 리뷰의 유예분).
 *
 * 루트도 이 플러그인을 적용한다. 루트는 소스가 없어 변형에 아무것도 싣지 않고 병합 리포트만
 * 소유하며, 무엇을 합칠지는 루트의 `dependencies { kover(project(...)) }` 가 정한다.
 */
class AfternoteKoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<KoverProjectExtension> {
                reports {
                    filters {
                        excludes {
                            // BuildConfig, R, Manifest and other Android-generated classes.
                            androidGeneratedClasses()
                            classes(*COVERAGE_CLASS_EXCLUSIONS)
                        }
                    }
                }
            }

            if (path == AGGREGATE_ROOT_PATH) {
                // The root owns only the merged report; source/test variants come from its dependencies.
                extensions.configure<KoverProjectExtension> {
                    currentProject { createVariant(CI_VARIANT) {} }
                }
                return
            }

            // 변형에 실을 소스는 모듈 타입이 정한다. 세 플러그인은 한 모듈에 함께 오지 않으므로
            // createVariant 는 모듈당 한 번만 불린다.
            MODULE_VARIANT_SOURCES.forEach { (pluginId, source) ->
                pluginManager.withPlugin(pluginId) {
                    extensions.configure<KoverProjectExtension> {
                        currentProject { createVariant(CI_VARIANT) { add(source) } }
                    }
                }
            }
        }
    }
}

private const val AGGREGATE_ROOT_PATH = ":"

private const val CI_VARIANT = "ci"

private val MODULE_VARIANT_SOURCES =
    listOf(
        "com.android.application" to "debug",
        "com.android.library" to "debug",
        "org.jetbrains.kotlin.jvm" to "jvm",
    )

private val COVERAGE_CLASS_EXCLUSIONS =
    arrayOf(
        // Hilt/Dagger and Compose compiler output contains no hand-written product decisions.
        "*Dagger*",
        "*Hilt_*",
        "*HiltWrapper_*",
        "hilt_aggregated_deps.*",
        "*_ComponentTreeDeps*",
        "*_*Factory*",
        "*_GeneratedInjector*",
        "*_HiltModules*",
        "*_MembersInjector*",
        "*ComposableSingletons*",
    )
