import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

/**
 * 모듈 의존성이 layer 룰 (UI → Domain → Data) 을 위반하면 *configuration time* 에
 * 빌드를 실패시킨다. AI/사람 판단에 의존하지 않는 결정론적 차단.
 *
 * Android Architecture Guide 권고: Data Layer 진입점은 Repository 로 한정.
 * ViewModel / UseCase 는 DataSource (네트워크 / DB / 센서) 모듈에 직접 의존 X.
 *
 * **Opt-in plugin** — 본인 책임 영역 모듈만 `id("afternote.layer.rules")` 명시.
 * 다른 영역 (mindrecord, timeletter, setting) 모듈에는 적용되지 않는다 — 각 owner 가
 * 도입 여부 결정. 첫 도입 범위 (Phase 1) 는 `:feature:afternote:` 와 `:feature:onboarding:`
 * 산하 presentation·domain·data 모듈.
 *
 * **Phase 2 (별도 PR)**: `:core:` 내부 layer 룰 (역방향 의존 차단 등) 확장 예정.
 *
 * 룰 매트릭스는 [forbiddenFor] 에 정의. 위반 발견 시 메시지에 위반 경로와 정공법 안내 포함.
 */
class AfternoteLayerRulesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.afterEvaluate {
            val forbidden = forbiddenFor(target.path) ?: return@afterEvaluate
            val violations =
                target.configurations
                    .flatMap { it.dependencies.filterIsInstance<ProjectDependency>() }
                    .filter { dep ->
                        forbidden.any { pattern -> matchesPattern(dep.path, pattern) }
                    }.distinctBy { it.path }

            if (violations.isNotEmpty()) {
                throw GradleException(
                    buildString {
                        appendLine("[Layer 룰 위반] ${target.path} 가 다음 모듈에 직접 의존:")
                        violations.forEach { appendLine("  - ${it.path}") }
                        appendLine()
                        appendLine("금지 패턴: ${forbidden.joinToString()}")
                        appendLine("정공법: data 레이어가 도메인 예외/모델로 변환해 노출하거나 적합한 layer 통과.")
                        appendLine("룰 정의: build-logic/src/main/kotlin/AfternoteLayerRulesPlugin.kt")
                    },
                )
            }
        }
    }
}

/**
 * 모듈 path 에 대한 금지 의존성 패턴 목록. `null` 이면 룰 적용 안 함 (= 위반 검사 통과).
 *
 * 패턴 문법: `*` 는 `[^:]+` 매칭 (한 단계 path 세그먼트).
 */
private fun forbiddenFor(path: String): List<String>? {
    val coreInfraModules = listOf(":core:network", ":core:database", ":core:datastore", ":core:data")
    val anyFeatureData = ":feature:*:data"
    val anyFeaturePresentation = ":feature:*:presentation"

    return when {
        // feature presentation: data 모듈 (자기 영역 포함) + core 인프라 의존 금지
        path.startsWith(":feature:") && path.endsWith(":presentation") -> {
            coreInfraModules + anyFeatureData
        }

        // feature domain: data + presentation + core 인프라 모두 금지 (가장 엄격)
        path.startsWith(":feature:") && path.endsWith(":domain") -> {
            coreInfraModules + anyFeatureData + anyFeaturePresentation
        }

        // feature data: presentation 의존 금지 (역방향 차단)
        path.startsWith(":feature:") && path.endsWith(":data") -> {
            listOf(anyFeaturePresentation)
        }

        else -> {
            null
        }
    }
}

private fun matchesPattern(
    path: String,
    pattern: String,
): Boolean {
    if (!pattern.contains("*")) return path == pattern
    val regex = pattern.replace(":", "\\:").replace("*", "[^:]+").toRegex()
    return regex.matches(path)
}
