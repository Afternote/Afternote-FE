package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Test

/**
 * main 소스셋 `@Preview` 재유입 가드 (#1434).
 *
 * PR #1435 가 담당 area 의 main `@Preview` 148건을 전량 삭제했는데, 그 뒤로 2건이 다시 들어왔다.
 * 이 가드가 없으면 같은 청소를 주기적으로 되풀이하게 된다.
 *
 * ### 왜 main 프리뷰를 두지 않는가
 * 1. **CI 가 검증하지 않는다** — 골든이 없어 렌더가 깨져도 아무도 모른다. 같은 그림을 검증받는
 *    `screenshotTest` 쪽이 정본이다.
 * 2. 같은 그림을 두 소스셋에서 관리하면 시안이 바뀔 때 **한쪽만 고쳐진다**.
 * 3. 프리뷰용 더미 데이터와 no-op 배선이 프로덕션 소스셋에 남는다 — #1388 이 막으려던
 *    「전 액션을 한 줄로 죽이는 우회로」가 `ReceiverHomeActions.Noop` 으로 프로덕션 API 에
 *    새어 나온 것이 그 실사례다.
 *
 * ### 검사 대상 — [GUARDED_MODULE_PREFIXES]
 * 삭제를 마친 담당 area 만 본다. 그 밖(timeletter · setting · mindrecord · home)에는 110건이
 * 남아 있고 **모듈 담당자 몫**이라 이 가드가 강제하지 않는다. 각 모듈이 청소를 마치면 접두어를
 * 여기에 더한다.
 *
 * 프리뷰가 필요하면 `src/screenshotTest` 에 `@PreviewTest` + `@Preview` 로 둔다 — 골든이 붙어
 * CI 가 렌더를 지킨다.
 */
class MainSourcePreviewKonsistTest {
    @Test
    fun `청소를 마친 모듈의 main 소스셋에는 Preview 를 두지 않는다`() {
        val violations =
            guardedFiles().flatMap { file ->
                file
                    .functions()
                    .filter { function -> function.annotations.any { it.name == PREVIEW } }
                    .map { function -> "${file.normalizedProjectPath()} — ${function.name}" }
            }

        check(violations.isEmpty()) {
            buildString {
                appendLine("main 소스셋에 @Preview 가 다시 들어왔다 (${violations.size}건).")
                appendLine("골든이 없어 CI 가 렌더를 검증하지 못하고, 프리뷰용 더미·no-op 배선이 프로덕션 소스에 남는다.")
                appendLine()
                violations.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("프리뷰가 필요하면 src/screenshotTest 에 @PreviewTest + @Preview 로 둔다 (#1434).")
            }
        }
    }

    private fun guardedFiles(): List<KoFileDeclaration> =
        AfternoteKonsistScope
            .files
            .filter { file ->
                val path = file.normalizedProjectPath()
                "/src/main/" in path && GUARDED_MODULE_PREFIXES.any { path.startsWith(it) }
            }

    private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

    private companion object {
        const val PREVIEW = "Preview"

        /** PR #1435 로 main `@Preview` 가 0건이 된 모듈들. 다른 모듈은 담당자가 청소를 마치면 더한다. */
        val GUARDED_MODULE_PREFIXES =
            listOf(
                "core/ui/",
                "feature/afternote/presentation/",
                "feature/onboarding/presentation/",
                "feature/receiver/presentation/",
            )
    }
}
