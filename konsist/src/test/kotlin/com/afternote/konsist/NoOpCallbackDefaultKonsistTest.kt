package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoParameterDeclaration
import org.junit.Test

/**
 * 컴포저블 no-op 콜백 디폴트 회귀 가드 (#1388).
 *
 * `onXxx: (...) -> Unit = {}` 는 배선을 빠뜨려도 컴파일이 통과해 조용히 no-op 이 된다 —
 * "눌러도 아무 일 없는 버튼" 실사고가 #582 · #618 · #722 · #777 로 반복된 패턴이다.
 * 규칙·처분 기준은 `docs/convention/composable-callback-defaults.md` 참고.
 *
 * ### 검사 대상
 * app · feature 모듈 `src/main` 의
 * 1. `@Composable` 함수 파라미터
 * 2. 클래스 주 생성자 파라미터 — `ReceiverHomeActions` 처럼 콜백을 프로퍼티로 묶은
 *    홀더 클래스도 같은 방식으로 미배선을 숨긴다.
 *
 * 이름이 `on`+대문자로 시작하고 기본값이 no-op 람다(`{}` · `{ }` · `{ _ -> }`)인 파라미터가
 * 위반이다. "함수 타입인가" 는 따로 검사하지 않는다 — no-op 람다 기본값은 함수 타입에만
 * 컴파일되므로 기본값 모양이 타입 검사를 대신하고, typealias 콜백까지 잡는다.
 *
 * ### 올바른 모델링
 * - 실호출부가 전부 실값을 넘기면: 디폴트를 없앤다(required). 프리뷰·테스트는 `{}` 명시.
 * - 상호작용이 진짜 선택적이면: nullable 핸들러(`onXxx: (() -> Unit)?`) + null 이면 UI 숨김,
 *   또는 오버로드 분리. `= {}` 로 죽은 버튼을 그리지 않는다.
 *
 * ### [LEGACY_NO_OP_DEFAULT_FILES]
 * 가드 도입 시점(#1388)에 남아 있던 잔여 파일. 모듈 담당별 후속 PR 이 청소하며 목록에서 뺀다.
 * afternote(#1388 본체) · receiver · **mindrecord·home(#1540)** 은 청소 완료로 빠졌고, 남은 것은 timeletter(#1541) 몫이다.
 * 목록에 있는 파일은 위반이 **있어도 없어도 통과**한다(관대 판정) — 파일을 청소하는 PR 과
 * 목록을 갱신하는 PR 의 머지 순서가 develop 을 red 로 만들지 않게 하기 위해서다
 * ([ResponseDtoContractKonsistTest] 의 #933 전례와 같은 구조). 이미 청소된 항목이 목록에
 * 남아 있으면 아래 「해소된 항목은 경고로 알린다」 가 경고만 낸다.
 */
class NoOpCallbackDefaultKonsistTest {
    @Test
    fun `컴포저블 on 콜백 파라미터는 no-op 디폴트를 두지 않는다`() {
        val violations = noOpCallbackDefaults().filterNot { it.isLegacyFile() }

        check(violations.isEmpty()) {
            buildString {
                appendLine("on* 콜백 파라미터에 no-op 디폴트가 새로 추가됐다 (${violations.size}건).")
                appendLine("배선을 빠뜨려도 컴파일이 통과해 조용히 no-op 이 된다 (#582 · #618 · #722 · #777 전례).")
                appendLine()
                violations.map(NoOpDefault::describe).sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("실호출부가 전부 실값을 넘기면 디폴트를 없애고(프리뷰·테스트는 {} 명시),")
                appendLine("진짜 선택적 상호작용이면 nullable 핸들러 + UI 숨김으로 모델링한다.")
                appendLine("기준: docs/convention/composable-callback-defaults.md")
            }
        }
    }

    /**
     * 해소된 항목이 목록에 남아 있으면 **경고만** 낸다.
     *
     * 실패시키면 「파일을 청소하는 PR」 과 「목록에서 빼는 PR」 이 서로를 깨뜨린다 —
     * [ResponseDtoContractKonsistTest] 의 동명 테스트와 같은 이유다. 신규 유입 차단은
     * 위 테스트가 담당하고, 이쪽은 목록 갱신 시점을 알리는 데 그친다.
     */
    @Test
    fun `해소된 항목은 경고로 알린다`() {
        val violatedPaths = noOpCallbackDefaults().map(NoOpDefault::path)
        val stale =
            LEGACY_NO_OP_DEFAULT_FILES.filter { suffix -> violatedPaths.none { it.endsWith(suffix) } }
        if (stale.isEmpty()) return

        println(
            buildString {
                appendLine("[경고] LEGACY_NO_OP_DEFAULT_FILES 에 이미 해소된 항목이 남아 있다 (${stale.size}건).")
                appendLine("목록에서 지워야 다음 위반이 이 자리에 숨지 않는다.")
                appendLine()
                stale.sorted().forEach { appendLine("  $it") }
            },
        )
    }

    private fun noOpCallbackDefaults(): List<NoOpDefault> =
        guardedFiles().flatMap { file ->
            val composableFunctionParams =
                file
                    .functions()
                    .filter { function -> function.annotations.any { it.name == COMPOSABLE } }
                    .flatMap { function -> function.parameters.noOpCallbacks(file, function.name) }
            val constructorParams =
                file
                    .classes()
                    .flatMap { cls ->
                        val parameters = cls.primaryConstructor?.parameters.orEmpty()
                        parameters.noOpCallbacks(file, cls.name)
                    }
            composableFunctionParams + constructorParams
        }

    private fun List<KoParameterDeclaration>.noOpCallbacks(
        file: KoFileDeclaration,
        owner: String,
    ): List<NoOpDefault> =
        filter { parameter -> CALLBACK_NAME.containsMatchIn(parameter.name) && parameter.hasNoOpDefault() }
            .map { parameter -> NoOpDefault(path = file.normalizedProjectPath(), owner = owner, parameter = parameter.name) }

    private fun KoParameterDeclaration.hasNoOpDefault(): Boolean {
        val normalized = defaultValue?.replace(WHITESPACE, "") ?: return false
        return NO_OP_LAMBDA.matches(normalized)
    }

    private fun guardedFiles(): List<KoFileDeclaration> =
        AfternoteKonsistScope
            .files
            .filter { file ->
                val path = file.normalizedProjectPath()
                (path.startsWith("app/") || path.startsWith("feature/")) && "/src/main/" in path
            }

    private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

    private data class NoOpDefault(
        val path: String,
        val owner: String,
        val parameter: String,
    ) {
        fun isLegacyFile(): Boolean = LEGACY_NO_OP_DEFAULT_FILES.any { path.endsWith(it) }

        fun describe(): String = "$path — $owner($parameter)"
    }

    private companion object {
        const val COMPOSABLE = "Composable"

        /** `on` + 대문자로 시작하는 콜백 이름. */
        val CALLBACK_NAME = Regex("""^on[A-Z]""")

        /** 공백 제거 후 `{}` · `{->}` · `{_->}` · `{_,_->}` 꼴 — 아무것도 하지 않는 람다. */
        val NO_OP_LAMBDA = Regex("""^\{(_(,_)*)?(->)?\}$""")

        val WHITESPACE = Regex("""\s""")

        /** home — #1540 에서 청소 완료. */
        private val HOME = emptySet<String>()

        /** mindrecord — #1540 에서 청소 완료. */
        private val MINDRECORD = emptySet<String>()

        /** timeletter — #1388 모듈 몫 후속 PR 이 청소한다 (TimeLetterWriteScreen 은 #778 과 조정). */
        private val TIMELETTER =
            setOf(
                "feature/timeletter/presentation/component/DraftLetterItem.kt",
                "feature/timeletter/presentation/component/TimeLetterBlockItem.kt",
                "feature/timeletter/presentation/component/TimeLetterContent.kt",
                "feature/timeletter/presentation/component/TimeletterListItem.kt",
                "feature/timeletter/presentation/screen/recipient/RecipientTimeletterScreen.kt",
                "feature/timeletter/presentation/screen/sender/TimeLetterWriteScreen.kt",
                "feature/timeletter/presentation/screen/sender/TimeletterScreen.kt",
            )

        val LEGACY_NO_OP_DEFAULT_FILES = HOME + MINDRECORD + TIMELETTER
    }
}
