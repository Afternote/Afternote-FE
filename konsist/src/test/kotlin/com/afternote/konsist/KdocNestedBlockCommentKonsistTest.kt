package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Test

/**
 * KDoc 안에 블록 주석 여는 토큰(슬래시 뒤 별)을 두지 않는다 (#1889).
 *
 * Kotlin 의 블록 주석은 **중첩된다.** KDoc 본문에 «별 둘·슬래시·별» 로 이어지는 경로 글롭을 적으면
 * 그 안의 «슬래시·별» 이 새 주석을 열고, 처음 만나는 닫는 토큰은 안쪽 것만 닫는다 — 바깥 KDoc 은
 * 파일 끝까지 열린 채라 **그 아래 선언이 전부 사라진다.** 컴파일러는 «Unclosed comment» 한 줄만 내고,
 * 다른 모듈에선 Dagger 가 「type could not be resolved」 로만 말해 증상과 원인이 파일 하나만큼 떨어진다.
 *
 * 이 파일 자체가 첫 실측이다 — 초판은 이 KDoc 에 글롭을 그대로 적었다가 그 에러로 컴파일이 죽었다.
 *
 * 문자열 리터럴 안의 토큰은 주석이 아니므로 먼저 걷어 낸 뒤 KDoc 만 훑는다.
 */
class KdocNestedBlockCommentKonsistTest {
    @Test
    fun `KDoc 안에서 블록 주석이 다시 열리지 않는다`() {
        var kdocBlocks = 0
        val violations =
            AfternoteKonsistScope.files.flatMap { file ->
                val source = file.text.withoutStringLiterals()
                kdocBlocks += countKdocBlocks(source)
                nestedOpeners(source).map { line -> "${file.normalizedProjectPath()}:$line" }
            }

        check(kdocBlocks > MINIMUM_KDOC_BLOCKS) {
            "KDoc 블록이 ${kdocBlocks}개뿐이다 — 스캔 범위가 비었거나 파서가 깨졌다. 이 가드는 초록이어도 아무것도 안 본 것이다."
        }
        check(violations.isEmpty()) {
            buildString {
                appendLine("KDoc 안에서 블록 주석이 다시 열렸다 (${violations.size}건).")
                appendLine("Kotlin 블록 주석은 중첩되므로 바깥 KDoc 이 닫히지 않아 그 아래 선언이 통째로 사라진다 — Dagger 는 «could not be resolved» 로만 말한다.")
                appendLine()
                violations.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("경로 글롭이면 별과 슬래시가 «슬래시·별» 순으로 붙지 않게 풀어 쓰거나 코드 스팬 밖으로 뺀다 (#1889).")
            }
        }
    }

    /**
     * KDoc 여는 토큰마다 깊이를 세며 걷는다. 안에서 여는 토큰을 또 만나 깊이가 2가 되는 순간이 위반이고,
     * 그 줄 번호를 돌려준다. 바깥이 닫힌 뒤엔 다음 KDoc 부터 다시 센다.
     */
    private fun nestedOpeners(source: String): List<Int> {
        val lines = mutableListOf<Int>()
        var index = source.indexOf(KDOC_OPEN)
        while (index >= 0) {
            var depth = 1
            var cursor = index + KDOC_OPEN.length
            while (cursor < source.length - 1 && depth > 0) {
                when {
                    source.startsWith(BLOCK_OPEN, cursor) -> {
                        depth += 1
                        if (depth == 2) lines += source.lineNumberAt(cursor)
                        cursor += BLOCK_OPEN.length
                    }

                    source.startsWith(BLOCK_CLOSE, cursor) -> {
                        depth -= 1
                        cursor += BLOCK_CLOSE.length
                    }

                    else -> {
                        cursor += 1
                    }
                }
            }
            index = source.indexOf(KDOC_OPEN, maxOf(cursor, index + KDOC_OPEN.length))
        }
        return lines
    }

    private fun countKdocBlocks(source: String): Int = Regex(Regex.escape(KDOC_OPEN)).findAll(source).count()

    private fun String.lineNumberAt(offset: Int): Int = 1 + substring(0, offset).count { it == '\n' }

    /** 원시 문자열·일반 문자열 리터럴을 공백으로 바꾼다 — 그 안의 토큰은 주석이 아니다. */
    private fun String.withoutStringLiterals(): String = replace(RAW_STRING, " ").replace(STRING, " ")

    private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

    private companion object {
        val KDOC_OPEN = "/" + "**"
        val BLOCK_OPEN = "/" + "*"
        val BLOCK_CLOSE = "*" + "/"

        /** 저장소의 KDoc 블록은 수천 개다 — 이 아래면 스캔이 비었다고 본다. */
        const val MINIMUM_KDOC_BLOCKS = 500

        val RAW_STRING = Regex("\"\"\"[\\s\\S]*?\"\"\"")
        val STRING = Regex("\"(?:[^\"\\\\\\n]|\\\\.)*\"")
    }
}
