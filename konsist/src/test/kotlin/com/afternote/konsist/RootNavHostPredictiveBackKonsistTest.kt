package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Test

/**
 * 루트 `FeatureNavDisplay` 의 predictive back pop 전환 결선 가드 (#1869, #1702).
 *
 * Navigation 3 기본값과 #1869 공용 전환은 같은 모양이지만, 루트도 공용 전환을 명시적으로 전달해
 * 로컬 스택과 단일 출처를 유지한다.
 *
 * 어긋난 축이 「화면이 어떻게 보이는가」 하나라서 기존 자동 검사가 전부 지나간다.
 * `PredictiveBackNavigationTest`(5건)는 백스택 상태만 보고, Robolectric 은 프레임을 돌리지 않는다.
 * `predictivePopTransitionSpec` 은 기본값이 있어 지워도 컴파일이 통과한다. 그 되돌림을 소스에서 본다.
 *
 * ### 이 가드가 잡는 것과 못 잡는 것
 *
 * 잡는 것 — 공용 전환 둘 중 하나라도 사라지는 것, 다른 값으로 갈아 끼우는 것, core/ui 의 공용 상수를
 * 두고 app 안에서 따로 정의한 값을 넘기는 것(import 까지 본다), 그리고 core/ui 쪽 상수가
 * 이름을 바꾸거나 사라지는 것.
 *
 * 못 잡는 것 — 상수의 **값**이 Nav3 기본값에서 벗어나는 것. 픽셀 판정이라 텍스트로는 셀 수 없고,
 * 그 몫은 `docs/qa/predictive-back.md` 의 수동 절차가 받는다.
 *
 * 전진(push) 쪽 `enterTransition`·`exitTransition` 은 보지 않는다 — #1869 의 완료 조건 밖이라
 * 손대지 않았고, 없는 결선을 요구하면 이 가드가 그 자체로 오탐이 된다.
 */
class RootNavHostPredictiveBackKonsistTest {
    @Test
    fun `루트 FeatureNavDisplay 는 core ui 의 predictive back pop 전환을 넘긴다`() {
        // 공유 진입점을 쓴다 — 워크트리 사본이 섞이면 남의 브랜치 소스로 헛짚는다 (#1659, ScanScopeKonsistTest).
        val source = singleFile(APP_NAVIGATION_PATH).text
        val arguments = rootNavDisplayArguments(source)

        check(arguments != null) {
            "$APP_NAVIGATION_PATH 에서 루트 «FeatureNavDisplay(» 호출을 찾지 못했다."
        }

        val binding = Regex("""(?s)predictivePopTransitionSpec\s*=\s*(\{[^}]*})""").find(arguments)?.groupValues?.get(1)
        check(binding != null) { "루트 FeatureNavDisplay 에 predictivePopTransitionSpec 인자가 없다 (#1869)." }
        EXPECTED_BINDINGS.forEach { constant ->
            check(binding.references(constant)) {
                "루트 FeatureNavDisplay 의 predictivePopTransitionSpec 에 «$constant» 가 없다 (#1869)."
            }
            // 같은 이름을 app 안에서 새로 정의해도 위 단언은 통과한다 — 출처가 core/ui 인지 import 로 본다.
            check(source.contains("import $CONSTANT_PACKAGE.$constant")) {
                "$APP_NAVIGATION_PATH 가 $CONSTANT_PACKAGE.$constant 을 import 하지 않는다 — " +
                    "루트와 로컬이 같은 값을 쓰도록 상수는 core/ui 한 곳에 둔다 (#1869)."
            }
        }
        val featureDisplay = singleFile(FEATURE_NAV_DISPLAY_PATH).text
        check(Regex("""predictivePopTransitionSpec\s*=\s*predictivePopTransitionSpec""").containsMatchIn(featureDisplay)) {
            "FeatureNavDisplay 가 predictivePopTransitionSpec 을 NavDisplay 에 전달하지 않는다 (#1869)."
        }
    }

    @Test
    fun `predictive back pop 전환은 core ui 가 소유한다`() {
        val declared = singleFile(TRANSITIONS_PATH).properties().map { it.name }.toSet()
        val missing = EXPECTED_BINDINGS.filterNot(declared::contains)

        check(missing.isEmpty()) {
            "$TRANSITIONS_PATH 에 ${missing.joinToString()} 이 없다 — 루트와 로컬 NavDisplay 가 " +
                "같은 값을 넘기려면 이 파일이 단일 출처여야 한다 (#1869)."
        }
    }

    private fun singleFile(projectPath: String): KoFileDeclaration {
        val candidates = AfternoteKonsistScope.files.filter { it.normalizedProjectPath() == projectPath }

        check(candidates.size == 1) {
            "$projectPath 가 ${candidates.size}건 잡혔다 — 파일이 옮겨졌거나 사본이 섞였다.\n" +
                candidates.joinToString("\n") { "  " + it.path }
        }
        return candidates.single()
    }

    /**
     * 루트 `NavHost(` 의 여는 괄호부터 짝이 맞는 닫는 괄호까지를 돌려준다. 후행 람다(destination 등록부
     * 40여 개)는 괄호 밖이라 들어오지 않는다.
     *
     * 이름은 식별자 경계로 찾는다 — 같은 파일에 `AfternoteNavHost(`·`ReceiverNavHost(` 처럼 끝이
     * 겹치는 호출이 여럿이라, 단순 `indexOf` 는 그중 무엇이 먼저 오느냐에 결과가 달라진다.
     */
    private fun rootNavDisplayArguments(source: String): String? {
        val open = NAV_HOST_CALL.find(source)?.range?.last ?: return null
        var depth = 0
        for (index in open until source.length) {
            when (source[index]) {
                '(' -> {
                    depth++
                }

                ')' -> {
                    depth--
                    if (depth == 0) return source.substring(open + 1, index)
                }
            }
        }
        return null
    }

    /** 이름을 «식별자로» 참조하는지 — 부분 일치(`PredictiveBackPopEnterOverride`)를 배제한다. */
    private fun String.references(name: String): Boolean =
        Regex("""(?<![A-Za-z0-9_])${Regex.escape(name)}(?![A-Za-z0-9_])""").containsMatchIn(this)

    // 다른 가드와 같은 관례 — konsist 가 주는 projectPath 를 OS 구분자·선행 슬래시 없이 맞춘다.
    private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

    private companion object {
        const val APP_NAVIGATION_PATH = "app/src/main/java/com/afternote/afternote_fe/navigation/AppNavigation.kt"
        const val TRANSITIONS_PATH =
            "core/ui/src/main/kotlin/com/afternote/core/ui/navigation/PredictiveBackTransitions.kt"
        const val FEATURE_NAV_DISPLAY_PATH = "core/ui/src/main/kotlin/com/afternote/core/ui/navigation/FeatureNavDisplay.kt"
        const val CONSTANT_PACKAGE = "com.afternote.core.ui.navigation"

        /** 루트 `FeatureNavDisplay(` — `AfternoteNavHost(` 처럼 이름 끝만 겹치는 호출을 배제한다. */
        val NAV_HOST_CALL = Regex("""(?<![A-Za-z0-9_])FeatureNavDisplay\(""")

        /** 루트 `FeatureNavDisplay` 가 넘기는 core/ui 상수. */
        val EXPECTED_BINDINGS = listOf("PredictiveBackPopEnter", "PredictiveBackPopExit")
    }
}
