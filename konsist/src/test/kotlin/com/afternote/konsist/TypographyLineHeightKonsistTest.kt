package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Test

/**
 * 타이포 토큰의 `fontSize` 만 덮는 패턴 재유입 가드 (#1444).
 *
 * `AfternoteDesign.typography.<토큰>.copy(fontSize = …)` 로 글자 크기만 덮으면 `lineHeight` 는
 * **원 토큰 값이 그대로 상속된다.** 원 토큰의 행간은 원 fontSize 기준이라, 크기를 바꾼 자리에서
 * 행간 배율이 조용히 어긋난다. 컴파일도 되고 화면도 뜨기 때문에 시안과 대조하기 전까지 아무도
 * 모른다 — 실제로 레포에서 4건이 이 방식으로 누적됐다.
 *
 * ### 적발 이력
 * - #1403 — `AfternoteButton` 라벨. `bodySmallB`(14/20) 를 13sp 로 덮어 시안 19.5 대신 20.
 * - #1486 — `SenderDetailScreen` 발신자 이름. `bodyLargeB`(18/24) 를 32sp 로 덮어 시안 32 대신 24.
 *   32sp 글자에 행간 24sp 라 이름이 접히면 줄이 겹쳤다.
 * - #1487(mindrecord) · #1488(setting) · #1580(home) — 담당자 몫으로 열려 있다.
 *
 * ### 검사 대상 — [GUARDED_MODULE_PREFIXES]
 * 위반이 0건이 된 area 만 본다. 남은 3건은 각각 담당자 이슈로 열려 있고 **모듈 담당자 몫**이라
 * 이 가드가 강제하지 않는다. 각 모듈이 시안 대조를 마치면 접두어를 여기에 더한다.
 *
 * ### 고치는 법
 * 시안에서 그 자리의 fontSize·lineHeight 를 실측해 둘 다 명시한다. 시안값이 소수점이면
 * 반올림하지 않는다(`lineHeight = 19.5.sp`). 시안값이 원 토큰과 같다면 `copy` 자체가 불필요하다.
 *
 * ### 경계 — 못 잡는 것과 헛짚는 것
 * 토큰을 지역 변수로 받아 두고 `copy` 하는 우회(`val base = …typography.h2; base.copy(fontSize = …)`)
 * 는 지나친다. 레포에 그런 자리는 없고, 잡으려면 타입 추론이 필요해 정적 텍스트 검사 범위를 넘는다.
 *
 * 반대로 **주석·문자열에 적힌 예시도 코드로 친다.** 이 레포는 설명을 코드 주석에 많이 담기 때문에
 * 「이렇게 쓰지 말 것」을 예시로 적으면 이 가드가 헛짚는다 — 그때는 예시에서 `copy(` 를 빼거나
 * 토큰명을 `<토큰>` 처럼 흐려 적는다. 주석을 걷어내고 보는 편이 정확하지만, 문자열 안의 `//`
 * (URL 등)까지 가려내야 해서 파서가 오히려 더 자주 틀린다. `ReceiverHomeResourceKonsistTest`
 * 도 같은 이유로 주석 오탐을 감수한다.
 */
class TypographyLineHeightKonsistTest {
    @Test
    fun `타이포 토큰의 fontSize 를 덮을 때 lineHeight 도 함께 지정한다`() {
        val violations =
            guardedFiles().flatMap { file ->
                file.typographyCopyCalls().mapNotNull { call ->
                    val overridesFontSize = FONT_SIZE_ARGUMENT.containsMatchIn(call.arguments)
                    val overridesLineHeight = LINE_HEIGHT_ARGUMENT.containsMatchIn(call.arguments)
                    if (overridesFontSize && !overridesLineHeight) {
                        "${file.normalizedProjectPath()}:${call.line} — typography.${call.token}.copy(fontSize = …)"
                    } else {
                        null
                    }
                }
            }

        check(violations.isEmpty()) {
            buildString {
                appendLine("타이포 토큰의 fontSize 만 덮은 자리가 있다 (${violations.size}건).")
                appendLine("lineHeight 는 원 토큰 값이 그대로 상속돼 행간 배율이 시안에서 어긋난다.")
                appendLine()
                violations.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("시안에서 fontSize·lineHeight 를 실측해 둘 다 명시한다 (소수점 유지). 시안값이 원 토큰과 같으면 copy 를 걷어낸다 (#1444).")
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

    /** `typography.<토큰>.copy( … )` 호출 하나. [arguments] 는 괄호 안쪽 원문이다. */
    private data class TypographyCopyCall(
        val line: Int,
        val token: String,
        val arguments: String,
    )

    private fun KoFileDeclaration.typographyCopyCalls(): List<TypographyCopyCall> {
        val source = text
        return TYPOGRAPHY_COPY
            .findAll(source)
            .mapNotNull { match ->
                val openIndex = match.range.last
                val closeIndex = source.matchingParenthesisIndex(openIndex) ?: return@mapNotNull null
                TypographyCopyCall(
                    line = source.take(openIndex).count { it == '\n' } + 1,
                    token = match.groupValues[1],
                    arguments = source.substring(openIndex + 1, closeIndex),
                )
            }.toList()
    }

    /**
     * [openIndex] 의 `(` 와 짝이 되는 `)` 위치. 문자열 리터럴 안의 괄호는 세지 않는다.
     * 짝을 못 찾으면 null — 그 호출은 건너뛴다.
     */
    private fun String.matchingParenthesisIndex(openIndex: Int): Int? {
        var depth = 0
        var index = openIndex
        var inString = false
        while (index < length) {
            val character = this[index]
            if (inString) {
                // 이스케이프 다음 글자는 통째로 건너뛴다 — `\"` 를 문자열 종료로 오인하지 않기 위해서다.
                if (character == '\\') {
                    index++
                } else if (character == '"') {
                    inString = false
                }
            } else if (character == '"') {
                inString = true
            } else if (character == '(') {
                depth++
            } else if (character == ')') {
                depth--
                if (depth == 0) {
                    return index
                }
            }
            index++
        }
        return null
    }

    private companion object {
        val TYPOGRAPHY_COPY = Regex("""typography\.([A-Za-z0-9_]+)\.copy\(""")
        val FONT_SIZE_ARGUMENT = Regex("""\bfontSize\s*=""")
        val LINE_HEIGHT_ARGUMENT = Regex("""\blineHeight\s*=""")

        /**
         * `copy(fontSize = …)` 누락이 0건인 area. 남은 자리는 담당자 이슈가 열려 있다 —
         * mindrecord(#1487) · setting(#1488) · home(#1580). 닫히는 대로 접두어를 더한다.
         */
        val GUARDED_MODULE_PREFIXES =
            listOf(
                "app/",
                "core/",
                "feature/afternote/",
                "feature/onboarding/",
                "feature/receiver/",
            )
    }
}
