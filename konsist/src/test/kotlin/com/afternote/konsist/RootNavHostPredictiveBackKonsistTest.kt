package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Test

/**
 * 루트 `NavHost` 의 predictive back pop 전환 결선 가드 (#1869).
 *
 * 이 저장소는 네비게이션 엔진을 둘 쓴다. Nav3 로컬 스택은 `predictivePopTransitionSpec` 을 안 넘겨도
 * 라이브러리 기본값이 앞 화면을 줄여 주지만, **Nav2 루트 `NavHost` 의 pop 기본값은 fade 라 앞 화면이
 * 제자리에서 흐려지기만 한다** — 그래서 진행 중 두 화면이 같은 크기로 겹쳐 보였다(#1869 실기기 재현).
 *
 * 어긋난 축이 「화면이 어떻게 보이는가」 하나라서 기존 자동 검사가 전부 지나간다.
 * `PredictiveBackNavigationTest`(5건)는 백스택 상태만 보고, Robolectric 은 프레임을 돌리지 않는다.
 * `popEnterTransition`·`popExitTransition` 두 인자는 **기본값이 있어 지워도 컴파일이 통과하고**,
 * 그대로 #1869 이전 상태로 되돌아간다. 그 되돌림을 소스에서 본다.
 *
 * ### 이 가드가 잡는 것과 못 잡는 것
 *
 * 잡는 것 — 두 인자 중 하나라도 사라지는 것, 다른 값으로 갈아 끼우는 것, core/ui 의 공용 상수를
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
    fun `루트 NavHost 는 core ui 의 predictive back pop 전환을 넘긴다`() {
        // 공유 진입점을 쓴다 — 워크트리 사본이 섞이면 남의 브랜치 소스로 헛짚는다 (#1659, ScanScopeKonsistTest).
        val source = singleFile(APP_NAVIGATION_PATH).text
        val arguments = rootNavHostArguments(source)

        check(arguments != null) {
            "$APP_NAVIGATION_PATH 에서 루트 «NavHost(» 호출을 찾지 못했다 — 루트가 NavDisplay 로 바뀌었다면(#1702) " +
                "이 가드를 predictivePopTransitionSpec 기준으로 옮긴다."
        }

        EXPECTED_BINDINGS.forEach { (parameter, constant) ->
            val binding =
                Regex("""$parameter\s*=\s*([^,\n]+)""")
                    .find(arguments)
                    ?.groupValues
                    ?.get(1)
                    ?.trim()

            check(binding != null) {
                "루트 NavHost 에 $parameter 인자가 없다 (#1869).\n" +
                    "기본값은 fade 라, 지우면 predictive back 진행 중 앞 화면이 줄지 않고 뒤 화면과 겹쳐 그려진다.\n" +
                    "  $parameter = { $constant },"
            }
            check(binding.references(constant)) {
                "루트 NavHost 의 $parameter 가 «$constant» 이 아니라 «$binding» 을 넘긴다 (#1869)."
            }
            // 같은 이름을 app 안에서 새로 정의해도 위 단언은 통과한다 — 출처가 core/ui 인지 import 로 본다.
            check(source.contains("import $CONSTANT_PACKAGE.$constant")) {
                "$APP_NAVIGATION_PATH 가 $CONSTANT_PACKAGE.$constant 을 import 하지 않는다 — " +
                    "두 엔진이 같은 값을 쓰도록 상수는 core/ui 한 곳에 둔다 (#1869)."
            }
        }
    }

    @Test
    fun `predictive back pop 전환은 core ui 가 소유한다`() {
        val declared = singleFile(TRANSITIONS_PATH).properties().map { it.name }.toSet()
        val missing = EXPECTED_BINDINGS.values.filterNot(declared::contains)

        check(missing.isEmpty()) {
            "$TRANSITIONS_PATH 에 ${missing.joinToString()} 이 없다 — 루트 NavHost 와 #1702 의 NavDisplay 가 " +
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
    private fun rootNavHostArguments(source: String): String? {
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
        const val CONSTANT_PACKAGE = "com.afternote.core.ui.navigation"

        /** 루트 `NavHost(` — `AfternoteNavHost(` 처럼 끝만 겹치는 호출을 배제한다. */
        val NAV_HOST_CALL = Regex("""(?<![A-Za-z0-9_])NavHost\(""")

        /** 루트 `NavHost` 인자 → 넘어가야 하는 core/ui 상수. */
        val EXPECTED_BINDINGS =
            mapOf(
                "popEnterTransition" to "PredictiveBackPopEnter",
                "popExitTransition" to "PredictiveBackPopExit",
            )
    }
}
