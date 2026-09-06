import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

/**
 * explicit API strict 전환 전까지 warning inventory 로 관리하는 기존 프로덕션 모듈이다.
 *
 * 여기에 없는 새 컨벤션 모듈은 자동으로 strict 를 적용한다. 기존 모듈을 strict 로 전환할 때는
 * 해당 경로를 제거하고 [docs/convention/production-visibility.md]의 순서도 함께 갱신한다.
 */
private val explicitApiWarningInventory: Set<String> =
    setOf(
        ":app",
        ":core:common",
        ":core:data",
        ":core:datastore",
        ":core:domain",
        ":core:model",
        ":core:network",
        ":core:ui",
        ":feature:afternote:data",
        ":feature:afternote:domain",
        ":feature:afternote:presentation",
        ":feature:home:presentation",
        ":feature:mindrecord:data",
        ":feature:mindrecord:domain",
        ":feature:mindrecord:presentation",
        ":feature:onboarding:presentation",
        ":feature:receiver:data",
        ":feature:receiver:domain",
        ":feature:receiver:presentation",
        ":feature:setting:presentation",
        ":feature:timeletter:data",
        ":feature:timeletter:domain",
        ":feature:timeletter:presentation",
    )

private fun productionExplicitApiMode(projectPath: String): ExplicitApiMode =
    if (projectPath in explicitApiWarningInventory) {
        ExplicitApiMode.Warning
    } else {
        ExplicitApiMode.Strict
    }

internal fun KotlinBaseExtension.configureProductionExplicitApi(projectPath: String) {
    explicitApi = productionExplicitApiMode(projectPath)
}
