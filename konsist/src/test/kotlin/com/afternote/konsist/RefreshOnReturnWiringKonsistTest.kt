package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Test

/**
 * 재진입 갱신(`refreshOnReturn`) 결선 회귀 가드 (#701).
 *
 * `refreshOnReturn()` 은 ViewModel 에 함수 하나, 화면에 `LifecycleEventEffect(ON_RESUME)` 한 줄로
 * 갈라져 있다. **화면 쪽 한 줄이 사라져도 컴파일은 통과한다** — ViewModel 의 함수는 그대로 남아
 * 테스트도 계속 초록이고, 사용자에게만 「수정하고 돌아왔는데 옛 값이 그대로」로 나타난다.
 * 이 이슈가 애초에 그 상태를 고친 것이고, 실제로 두 번 재현됐다.
 *
 * 1. 수신 애프터노트 화면 15파일을 afternote 모듈로 옮기며(#1461) `ReceiverNavGraph` 의 결선이
 *    빠져나갔다. 옮겨간 자리에 다시 결선돼 동작은 살았지만, 남겨진 `LifecycleEventEffect` import 는
 *    아무도 눈치채지 못한 채 develop 에 남았다 — ktlint 도 이 import 를 잡지 못한다.
 * 2. 수신자 홈 담당 이관으로 결선을 도려낼 때(#1452)는 `refreshOnReturn()` 까지 함께 되돌려
 *    짝이 맞았다. 함수만 남기고 결선만 지웠다면 아무 신호도 없었을 것이다.
 *
 * ### 규칙
 * main 소스셋의 어느 클래스가 `refreshOnReturn` 을 선언하면, **자기 파일이 아닌** main 소스셋
 * 파일 중 `Lifecycle.Event.ON_RESUME` 과 `refreshOnReturn(` 을 함께 갖고 그 클래스 이름을
 * 참조하는 파일이 최소 하나 있어야 한다.
 *
 * 모듈을 가로지르는 결선(홈 탭은 `feature:home:presentation` 의 ViewModel 을 app 모듈
 * `AppNavigation` 에서 건다)도 통과하도록 검사는 프로젝트 전역이다. 화면 하나가 여러
 * ViewModel 을 거는 자리(마인드레코드 홈의 카테고리 분기)도 같은 파일이 세 이름을 모두
 * 참조하므로 그대로 잡힌다.
 *
 * 「ON_RESUME 에서 정말 그 ViewModel 의 것을 부르는가」까지는 보지 않는다 — 텍스트로 그 이상을
 * 단언하면 오탐이 늘고, 이 가드가 막으려는 것은 「결선이 통째로 사라진」 경우다. 계약 자체
 * (로딩 미방출·실패 시 화면 유지·첫 resume 스킵)는 각 ViewModel 의 단위 테스트가 지킨다.
 */
class RefreshOnReturnWiringKonsistTest {
    @Test
    fun `refreshOnReturn 을 선언한 ViewModel 은 ON_RESUME 결선을 갖는다`() {
        val mainFiles = mainSourceFiles()
        val wiringSites = mainFiles.filter { ON_RESUME in it.text && REFRESH_CALL in it.text }

        val unwired =
            mainFiles
                .flatMap { file ->
                    file
                        .classes()
                        .filter { declaration -> declaration.functions().any { it.name == REFRESH_ON_RETURN } }
                        .map { declaration -> file to declaration.name }
                }.filter { (declaringFile, className) ->
                    wiringSites.none { site ->
                        site.projectPath != declaringFile.projectPath && site.references(className)
                    }
                }.map { (declaringFile, className) -> "${declaringFile.normalizedProjectPath()} — $className" }

        check(unwired.isEmpty()) {
            buildString {
                appendLine("refreshOnReturn() 은 선언돼 있는데 ON_RESUME 결선이 없는 ViewModel 이 있다 (${unwired.size}건).")
                appendLine("컴파일도 단위 테스트도 통과하지만, 화면은 재진입해도 옛 값을 그대로 보여준다 (#701).")
                appendLine()
                unwired.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("화면(Route·Screen·NavGraph)에서 다음 한 줄을 되살린다:")
                appendLine("  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshOnReturn() }")
                appendLine("갱신이 더는 필요 없다면 ViewModel 의 refreshOnReturn() 도 함께 지운다 — 짝을 맞춘다.")
            }
        }
    }

    private fun mainSourceFiles(): List<KoFileDeclaration> =
        AfternoteKonsistScope.files.filter { "/src/main/" in it.normalizedProjectPath() }

    /** 클래스 이름을 «식별자로» 참조하는지 — 부분 일치(`SenderDetailViewModelFactory`)를 배제한다. */
    private fun KoFileDeclaration.references(className: String): Boolean =
        Regex("""(?<![A-Za-z0-9_])${Regex.escape(className)}(?![A-Za-z0-9_])""").containsMatchIn(text)

    private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

    private companion object {
        const val REFRESH_ON_RETURN = "refreshOnReturn"
        const val REFRESH_CALL = "refreshOnReturn("
        const val ON_RESUME = "Lifecycle.Event.ON_RESUME"
    }
}
