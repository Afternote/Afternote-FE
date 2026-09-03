package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Test

/**
 * `mindRecordNavGraph` 의 «한 칸 뒤로» 매핑 회귀 가드 (#1562).
 *
 * #1311 이 단순 back 위임 5개를 `popBack` 하나로 합치면서, 자리마다 이름이 달라
 * (`onWriteSubmitSuccess`·`onWriteBack`·`onDraftListBack`) **이름 자체가 오배선을 드러내던 방어가
 * 사라졌다.** 이제 매핑 7줄이 모두 `() -> Unit` 이라 서로 바꿔 붙여도 컴파일이 통과한다.
 *
 * `onBackClick` 5줄은 계측이 실제로 태운다 — `MindRecordBackStackAndroidTest` 의
 * `screenBackButton_...` 이 각 화면의 뒤로가기를 눌러 `routeOf()` 로 복귀 route 를 대조한다.
 *
 * **`onSubmitSuccess` 2줄은 그 계측이 지나가지 않는다.** 제출 성공 경로라 서버 제출을 태워야 하고,
 * 그 비용이 이 한 줄의 값을 넘는다(#1562 판정). 그래서 계측 대신 **매핑 그 자체를 소스에서** 본다.
 *
 * ### 이 가드가 잡는 것과 못 잡는 것
 *
 * 잡는 것 — `onSubmitSuccess = actions::onNavigateToDraftList` 처럼 **다른 명령으로 바꿔 붙이는 것**.
 * 실제로 그렇게 바꿔도 계측 3건이 전부 초록이라는 것을 #1466 리뷰에서 실측했다.
 *
 * 못 잡는 것 — `popBack` **구현**이 잘못되는 경우. 그건 계측이 본다(다섯 목적지에서 홈으로 복귀 +
 * 백스택 잔재 0). 즉 이 가드는 「어느 명령에 붙었나」만, 계측은 「그 명령이 무엇을 하나」만 본다.
 *
 * 텍스트 검사라 `actions::popBack` 을 다른 표기(`{ actions.popBack() }`)로 적으면 헛짚는다.
 * 그때는 이 가드가 아니라 표기를 맞추는 편이 낫다 — 매핑 7줄이 같은 모양이어야 한 눈에 대조된다.
 */
class MindRecordNavGraphWiringKonsistTest {
    @Test
    fun `제출 성공은 한 칸 뒤로 명령에 붙는다`() {
        // 공유 진입점을 쓴다 — `Konsist.scopeFromProject()` 는 저장소 루트를 통째로 walk 해
        // 워크트리(`.claude/`·`.codex/`)의 다른 브랜치 사본까지 끌고 들어온다 (#1659).
        // 실제로 이 가드를 처음 돌렸을 때 그 사본의 옛 매핑(`actions::onWriteSubmitSuccess`)이
        // 걸려 헛짚었다. `ScanScopeKonsistTest` 가 그 규칙을 강제한다.
        val candidates =
            AfternoteKonsistScope
                .files
                .filter { it.normalizedProjectPath() == NAV_GRAPH_PATH }

        check(candidates.size == 1) {
            "$NAV_GRAPH_PATH 가 ${candidates.size}건 잡혔다 — 파일이 옮겨졌거나 사본이 섞였다.\n" +
                candidates.joinToString("\n") { "  " + it.path }
        }
        val source = candidates.single().text

        val bindings = SUBMIT_SUCCESS.findAll(source).map { it.groupValues[1].trim() }.toList()

        // **개수까지 못 박는다.** `isNotEmpty()` 면 두 줄 중 하나가 통째로 지워져도 남은 한 줄
        // 때문에 통과한다 — 두 화면의 콜백이 `= {}` 디폴트라 인자를 지워도 컴파일은 되고, 그
        // 화면의 «제출 후 이동» 만 조용히 사라진다 (#1562 리뷰).
        check(bindings.size == WRITE_SCREEN_COUNT) {
            "onSubmitSuccess 매핑이 ${bindings.size}건이다 — 데일리질문·일기 두 화면이라 $WRITE_SCREEN_COUNT 건이어야 한다.\n" +
                "화면이 늘거나 줄었으면 WRITE_SCREEN_COUNT 와 아래 화면별 단언을 함께 고칠 것."
        }

        // 화면별로도 본다 — 개수만 맞고 자리가 뒤바뀌는 경우를 가른다.
        WRITE_SCREENS.forEach { screen ->
            val screenBinding = screenBinding(source, screen)
            check(screenBinding == EXPECTED_BINDING) {
                "$screen 의 onSubmitSuccess 가 «$EXPECTED_BINDING» 이 아니라 «$screenBinding» 이다 (#1562)."
            }
        }
    }

    /** `<screen>(` 부터 그 호출의 닫는 괄호까지에서 `onSubmitSuccess` 바인딩을 뽑는다. */
    private fun screenBinding(
        source: String,
        screen: String,
    ): String? {
        val start = source.indexOf("$screen(").takeIf { it >= 0 } ?: return null
        var depth = 0
        var end = start
        for (i in start until source.length) {
            when (source[i]) {
                '(' -> {
                    depth++
                }

                ')' -> {
                    depth--
                    if (depth == 0) {
                        end = i
                        break
                    }
                }
            }
        }
        return SUBMIT_SUCCESS
            .find(source.substring(start, end))
            ?.groupValues
            ?.get(1)
            ?.trim()
    }

    // 다른 가드와 같은 관례 — konsist 가 주는 projectPath 를 OS 구분자·선행 슬래시 없이 맞춘다.
    private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

    private companion object {
        const val NAV_GRAPH_PATH =
            "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/" +
                "presentation/navigation/MindRecordNavGraph.kt"
        const val EXPECTED_BINDING = "actions::popBack"

        /** 제출 성공 경로를 갖는 작성 화면. 늘어나면 여기와 위 단언을 함께 고친다. */
        val WRITE_SCREENS = listOf("DailyQuestionWriteScreen", "DiaryWriteScreen")
        val WRITE_SCREEN_COUNT = WRITE_SCREENS.size

        val SUBMIT_SUCCESS = Regex("""onSubmitSuccess\s*=\s*([^,\n]+)""")
    }
}
