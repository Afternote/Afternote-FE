package com.afternote.konsist

import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.ext.list.withPackage
import org.junit.Test

/**
 * 응답 DTO 계약 회귀 가드.
 *
 * **보정형 기본값**(`= ""` · `= 0` · `= false` · `= emptyList()`)을 non-null 응답 프로퍼티에 두면,
 * 서버가 키를 빼거나 계약을 바꿔도 파싱이 **성공**한다. 빈 문자열·0·false·빈 목록이 정상값으로
 * 화면까지 흘러가 계약 변경이 은폐된다. 실제 사고 모양:
 *
 * - `isDraft = false` → 임시저장 글이 목록에 그대로 노출
 * - `hasNext = false` → 페이지네이션이 조용히 멈춤
 * - `diaries = emptyList()` → 목록 소실이 "기록 없음" 으로 보임
 *
 * 기본값을 두는 순간 "키 누락" 과 "서버가 보낸 정상 빈 값" 이 같은 결과로 수렴한다. 둘은 성격이
 * 다르다 — 키 누락은 계약이 바뀐 것이라 실패해야 드러나고, 값 확장은 관대해도 된다(전용 직렬화기).
 *
 * 전역 `coerceInputValues` 는 #1494 에서 걷어냈다. 그래서 이제 `null` 이 온 경우는 기본값 유무와
 * 무관하게 실패하고, 이 가드가 막는 것은 **키 누락**이 기본값으로 성공해 버리는 쪽이다.
 *
 * ### 검사 대상
 * `..dto..` 패키지의 `@Serializable data class` 중 **이름에 `Request` 가 없는 것**(= 응답).
 * 요청 DTO 의 기본값은 "미선택 = 빈 배열 전송" 처럼 의도된 것이라 제외한다.
 *
 * ### nullable 은 대상이 아니다
 * `val imageUrl: String? = null` 처럼 **값이 조건부인 필드**는 정당하다. 이 가드가 막는 것은
 * non-null 로 선언해 놓고 기본값으로 누락을 메우는, 서로 모순된 조합뿐이다.
 *
 * ### [KNOWN_COERCING_DEFAULTS]
 * 실측 시점에 남아 있던 잔여. 모듈별 적용 이슈가 해소하며 목록에서 뺀다.
 * 이미 고쳐진 항목이 목록에 남아 있으면 **경고만** 낸다 — 이유는 아래 「해소된 항목은 경고로 알린다」 주석 참고.
 */
class ResponseDtoContractKonsistTest {
    @Test
    fun `응답 DTO 의 non-null 프로퍼티는 보정형 기본값을 두지 않는다`() {
        val violations = coercingDefaults() - KNOWN_COERCING_DEFAULTS

        check(violations.isEmpty()) {
            buildString {
                appendLine("응답 DTO 의 non-null 프로퍼티에 보정형 기본값이 새로 추가됐다 (${violations.size}건).")
                appendLine("서버가 키를 빼도 파싱이 성공해 계약 누락이 은폐된다.")
                appendLine()
                violations.sorted().forEach { appendLine("  $it") }
                appendLine()
                appendLine("값이 조건부라 정말 없을 수 있으면 기본값이 아니라 nullable 로 선언한다.")
            }
        }
    }

    /**
     * 해소된 항목이 목록에 남아 있으면 **경고만** 낸다.
     *
     * 실패시키면 「항목을 해소하는 PR」 과 「목록에서 빼는 PR」 이 서로를 깨뜨린다. 둘은 다른
     * 브랜치라 충돌도 나지 않고, 어느 쪽이 먼저 머지되든 그 순간부터 develop 의 `:konsist:test`
     * 가 red 가 된다. 필수 체크가 `guard` 하나뿐이라 머지를 막지도 못한 채 red 만 남는다.
     * #933(#789 의 26건 해소)과 이 목록 사이에서 실제로 그렇게 됐다.
     *
     * 목록이 썩는 것을 막는 값어치는 「신규 추가 금지」 쪽이 이미 담당한다 — 순서와 무관하게
     * 작동하는 그쪽만 실패시키고, 이쪽은 갱신 시점을 알리는 데 그친다.
     */
    @Test
    fun `해소된 항목은 경고로 알린다`() {
        val stale = KNOWN_COERCING_DEFAULTS - coercingDefaults()
        if (stale.isEmpty()) return

        println(
            buildString {
                appendLine("[경고] KNOWN_COERCING_DEFAULTS 에 이미 해소된 항목이 남아 있다 (${stale.size}건).")
                appendLine("목록에서 지워야 다음 위반이 이 자리에 숨지 않는다.")
                appendLine()
                stale.sorted().forEach { appendLine("  $it") }
            },
        )
    }

    private fun coercingDefaults(): Set<String> =
        responseDtoClasses()
            .flatMap { dto ->
                dto.primaryConstructor
                    ?.parameters
                    .orEmpty()
                    .filter { !it.type.isNullable && it.defaultValue != null }
                    .map { "${dto.name}.${it.name}" }
            }.toSet()

    private fun responseDtoClasses(): List<KoClassDeclaration> =
        AfternoteKonsistScope
            .scope
            .classes()
            .withPackage("com.afternote..dto..")
            .filter { it.hasDataModifier }
            .filter { it.annotations.any { annotation -> annotation.name == SERIALIZABLE } }
            .filterNot { REQUEST_DTO in it.name }

    private companion object {
        const val SERIALIZABLE = "Serializable"

        /** 아웃바운드. `receivers = emptyList()`("미선택 = 빈 배열 전송") 같은 의도된 기본값이라 제외. */
        const val REQUEST_DTO = "Request"

        /**
         * 남은 잔여는 timeletter 1건뿐이다 — #790 이 추적하고, `ci-expected-failures.json` 에
         * 의도된 실패로 등록돼 있다. mindrecord 2건(#789)·afternote 11건(#957)은 해소돼 빠졌다.
         *
         * 서버는 이 키를 `null` 로 보내지 않는다(BE `ReceivedTimeLetterResponse` 가 미공개 구간에
         * `List.of()` 를 채운다). 그래서 `coerceInputValues` 없이도 파싱이 깨지지 않는다 — 이
         * 기본값이 실제로 가리는 것은 «키 자체가 빠지는» 경우다.
         */
        private val TIMELETTER = setOf("ReceivedTimeLetterDto.blocks")

        val KNOWN_COERCING_DEFAULTS = TIMELETTER
    }
}
