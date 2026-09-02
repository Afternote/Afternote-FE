package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.Test

/**
 * 라디오 단품 직접 사용 차단 (#649).
 *
 * `core:ui` 의 단품 `CustomRadioButton` 은 `selected`/`onClick` 만 받는다 — 선택값의 소유권이
 * 호출부에 남아 **단일 선택을 구조로 강제하지 못한다.** 라디오 비주얼을 그대로 둔 채 `Set`
 * 토글 상태에 물리면 "라디오인데 복수 선택" 이 되고, 이 오용은 컴파일 타임에 걸리지 않는다
 * (#648 실사고). 그룹 semantics(`Modifier.selectableGroup`)도 호출부 책임으로 새어 나가
 * 빠뜨리면 스크린리더가 상호배타 관계를 읽지 못한다.
 *
 * 정본은 선택값 하나를 소유하는 `AfternoteRadioGroup`(`selectedValue: T?` + `onSelect(T)`)이다.
 * 단일 선택·그룹 semantics·48dp 상호작용 경계가 컴포넌트 안에서 닫힌다.
 *
 * ### 검사 대상
 * `app` · `core` · `feature` 모듈의 모든 소스셋에서 `CustomRadioButton` 을 이름으로든 FQN 으로든
 * 참조하는 파일. 선언 자체가 있는 `core:ui` 의 `button` 패키지는 제외한다 — 단품은 이제
 * `core:ui` 안에서만 존재할 수 있는 잔재이고, 삭제 시점까지 선언·존치 근거가 그 자리에 있다.
 *
 * main 소스셋만 보지 않는 이유는 테스트도 사용처이기 때문이다 — 계측·단위 테스트가 단품을
 * 붙들고 있으면 잔여 범위가 그만큼 남는다.
 *
 * ### [LEGACY_STANDALONE_RADIO_FILES]
 * 가드 도입 시점(#649)에 남아 있던 잔여 사용처. 모듈 담당별 후속 이슈가 이관하며 목록에서 뺀다.
 * 목록에 있는 파일은 위반이 **있어도 없어도 통과**한다(관대 판정) — 파일을 이관하는 PR 과
 * 목록을 갱신하는 PR 의 머지 순서가 develop 을 red 로 만들지 않게 하기 위해서다
 * ([NoOpCallbackDefaultKonsistTest] · [ResponseDtoContractKonsistTest] 와 같은 구조).
 * 목록이 비면 `CustomRadioButton` 선언을 삭제하고 이 테스트도 함께 걷는다.
 */
class SingleSelectionRadioKonsistTest {
    @Test
    fun `라디오 단품을 core ui 밖에서 직접 쓰지 않는다`() {
        val violations = standaloneRadioUsages().filterNot(::isLegacy)

        check(violations.isEmpty()) {
            buildString {
                appendLine("core:ui 밖에서 단품 CustomRadioButton 을 직접 쓴 자리가 새로 생겼다 (${violations.size}건).")
                appendLine("단품은 선택값을 소유하지 않아 단일 선택이 구조로 강제되지 않는다 (#648 실사고).")
                appendLine()
                violations.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("AfternoteRadioGroup(options, selectedValue, onSelect) 로 옮겨라 —")
                appendLine("단일 선택·selectableGroup semantics·48dp 상호작용 경계가 컴포넌트 안에서 닫힌다.")
                appendLine("기준: core/ui/src/main/kotlin/com/afternote/core/ui/button/AfternoteRadioGroup.kt")
            }
        }
    }

    /**
     * 해소된 항목이 목록에 남아 있으면 **경고만** 낸다.
     *
     * 실패시키면 「사용처를 이관하는 PR」 과 「목록에서 빼는 PR」 이 서로를 깨뜨린다.
     * 신규 유입 차단은 위 테스트가 담당하고, 이쪽은 목록 갱신 시점을 알리는 데 그친다.
     */
    @Test
    fun `해소된 항목은 경고로 알린다`() {
        val usedPaths = standaloneRadioUsages()
        val stale = LEGACY_STANDALONE_RADIO_FILES.filter { suffix -> usedPaths.none { it.endsWith(suffix) } }
        if (stale.isEmpty()) return

        println(
            buildString {
                appendLine("[경고] LEGACY_STANDALONE_RADIO_FILES 에 이미 해소된 항목이 남아 있다 (${stale.size}건).")
                appendLine("목록에서 지워야 다음 위반이 이 자리에 숨지 않는다.")
                appendLine("목록이 비면 CustomRadioButton 선언과 이 가드를 함께 걷는다 (#649).")
                appendLine()
                stale.sorted().forEach { appendLine("  $it") }
            },
        )
    }

    private fun isLegacy(path: String): Boolean = LEGACY_STANDALONE_RADIO_FILES.any { path.endsWith(it) }

    /** `core:ui` 의 선언 자리를 뺀, 단품을 참조하는 파일들의 프로젝트 상대 경로. */
    private fun standaloneRadioUsages(): List<String> =
        guardedFiles()
            .filter { file -> STANDALONE_RADIO_REFERENCE.containsMatchIn(file.text) }
            .map { it.normalizedProjectPath() }

    private fun guardedFiles(): List<KoFileDeclaration> =
        AfternoteKonsistScope.files
            .filter { file ->
                val path = file.normalizedProjectPath()
                GUARDED_MODULE_PREFIXES.any { path.startsWith(it) } && !path.startsWith(DECLARATION_PACKAGE_PREFIX)
            }

    private fun KoFileDeclaration.normalizedProjectPath(): String = projectPath.replace('\\', '/').trimStart('/')

    private companion object {
        /**
         * 단품 참조 — import·호출·FQN 을 한 벌로 잡는다.
         *
         * import 만 보면 `com.afternote.core.ui.button.CustomRadioButton(...)` 처럼 FQN 으로 쓴
         * 자리가 빠져나간다 ([ReceiverHomeResourceKonsistTest] 와 같은 손버릇). 본문 전체를 본다.
         * 주석·문자열 안의 언급까지 잡히는 오탐은 감수한다 — 이관을 권하는 문구는 그룹 API 이름을
         * 쓰면 되고, 단품 이름을 적어야 할 곳은 `core:ui` 안(제외 대상)뿐이다.
         */
        val STANDALONE_RADIO_REFERENCE = Regex("""\bCustomRadioButton\b""")

        val GUARDED_MODULE_PREFIXES = listOf("app/", "core/", "feature/")

        /** 단품 선언과 그 존치 근거가 사는 자리 — 유일한 예외. */
        const val DECLARATION_PACKAGE_PREFIX = "core/ui/src/main/kotlin/com/afternote/core/ui/button/"

        /**
         * setting — #1396 이 `AfternoteRadioGroup` 으로 이관하며 목록에서 뺀다.
         *
         * `RadioGroupCard` 는 카드 테두리에 `selectable` 을 두고 단품을 비인터랙티브 인디케이터로만
         * 쓰고 있어 오용은 아니지만, 단품 선언을 붙들고 있는 마지막 사용처다.
         */
        private val SETTING: Set<String> =
            setOf(
                "feature/setting/presentation/component/RadioGroupCard.kt",
            )

        /** 목록이 비면 `setOf()` 의 타입을 못 잡는 일이 없도록 원소 타입을 명시해 둔다. */
        val LEGACY_STANDALONE_RADIO_FILES: Set<String> = SETTING
    }
}
