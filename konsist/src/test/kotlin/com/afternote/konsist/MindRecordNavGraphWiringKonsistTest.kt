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

        check(bindings.isNotEmpty()) {
            "$NAV_GRAPH_PATH 에서 onSubmitSuccess 매핑을 찾지 못했다 — 화면이 빠졌거나 표기가 바뀌었다."
        }
        check(bindings.all { it == EXPECTED_BINDING }) {
            buildString {
                appendLine("onSubmitSuccess 가 «한 칸 뒤로»($EXPECTED_BINDING) 가 아닌 명령에 붙었다.")
                appendLine("이 자리는 계측이 지나가지 않아 오배선해도 전부 초록이다 (#1562).")
                appendLine()
                bindings.filterNot { it == EXPECTED_BINDING }.forEach { appendLine("  $it") }
                appendLine()
                appendLine("의도한 변경이라면 이 가드의 EXPECTED_BINDING 과 KDoc 을 함께 고칠 것.")
            }
        }
    }

    // 다른 가드와 같은 관례 — konsist 가 주는 projectPath 를 OS 구분자·선행 슬래시 없이 맞춘다.
    private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

    private companion object {
        const val NAV_GRAPH_PATH =
            "feature/mindrecord/presentation/src/main/kotlin/com/afternote/feature/mindrecord/" +
                "presentation/navigation/MindRecordNavGraph.kt"
        const val EXPECTED_BINDING = "actions::popBack"

        val SUBMIT_SUCCESS = Regex("""onSubmitSuccess\s*=\s*([^,\n]+)""")
    }
}
