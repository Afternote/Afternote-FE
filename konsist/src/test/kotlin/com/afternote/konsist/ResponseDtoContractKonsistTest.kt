package com.afternote.konsist

import com.lemonappdev.konsist.api.Konsist
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
 * `coerceInputValues` 는 **기본값이 있어야** 동작하므로, 기본값을 두는 순간 "키 누락" 과
 * "서버가 보낸 잘못된 값" 이 같은 결과로 수렴한다. 둘은 성격이 다르다 —
 * 키 누락은 계약이 바뀐 것이라 실패해야 드러나고, 값 확장은 관대해도 된다(전용 직렬화기).
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
 * #676 실측 시점에 남아 있던 38건. 모듈별 적용 이슈가 해소하며 목록에서 뺀다.
 * **목록에 있는데 이미 고쳐진 항목도 실패**시킨다 — 그래야 목록이 썩지 않는다.
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

    @Test
    fun `해소된 항목은 예외 목록에서 뺀다`() {
        val stale = KNOWN_COERCING_DEFAULTS - coercingDefaults()

        check(stale.isEmpty()) {
            buildString {
                appendLine("KNOWN_COERCING_DEFAULTS 에 이미 해소된 항목이 남아 있다 (${stale.size}건).")
                appendLine("목록에서 지워야 다음 위반이 이 자리에 숨지 않는다.")
                appendLine()
                stale.sorted().forEach { appendLine("  $it") }
            }
        }
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
        Konsist
            .scopeFromProject()
            .classes()
            .withPackage("com.afternote..dto..")
            .filter { it.hasDataModifier }
            .filter { it.annotations.any { annotation -> annotation.name == SERIALIZABLE } }
            .filterNot { REQUEST_DTO in it.name }

    private companion object {
        const val SERIALIZABLE = "Serializable"

        /** 아웃바운드. `receivers = emptyList()`("미선택 = 빈 배열 전송") 같은 의도된 기본값이라 제외. */
        const val REQUEST_DTO = "Request"

        /** #789 — mindrecord (PR #933 진행 중, 26건) */
        private val MINDRECORD =
            setOf(
                "WeeklyReportDto.dailyQuestionAmount",
                "WeeklyReportDto.diaryAmount",
                "WeeklyReportDto.summaryText",
                "WeeklyReportDto.week",
                "WeeklyReportDto.dailyQuestions",
                "WeeklyReportDto.emotions",
                "WeeklyReportDailyQuestionDto.title",
                "WeeklyReportDailyQuestionDto.content",
                "WeeklyReportDailyQuestionDto.date",
                "WeeklyReportEmotionDto.keyword",
                "WeeklyReportEmotionDto.percentage",
                "DiaryListItemDto.title",
                "DiaryListItemDto.content",
                "DiaryListItemDto.createdAt",
                "DiaryListItemDto.isDraft",
                "DiaryListDto.diaries",
                "DiaryListDto.monthDiaryCount",
                "ReceiverDailyQuestionListDto.dailyQuestions",
                "ReceiverDiaryListDto.diaries",
                "ReceiverDiaryItemDto.isDraft",
                "ReceiverDiaryItemDto.date",
                "ReceiverDiaryItemDto.createdAt",
                "ReceiverDiaryItemDto.updatedAt",
                "DailyQuestionListItemDto.isDraft",
                "TodayDailyQuestionDto.isAnswered",
                "TodayDailyQuestionDto.isDraft",
            )

        /** #957 — afternote (11건). `AfternoteDetailDto.createdAt` 은 #676 에서 해소(BE 응답에 없는 필드라 삭제). */
        private val AFTERNOTE =
            setOf(
                "AfternoteDetailDto.updatedAt",
                "AfternotePlaylistDto.songs",
                "AfternotePageDto.content",
                "AfternotePageDto.page",
                "AfternotePageDto.size",
                "AfternotePageDto.hasNext",
                "ReceivedAfternoteListDto.afternotes",
                "ReceivedAfternoteListDto.totalCount",
                "ReceivedAfternoteDetailDto.processingMethods",
                "ReceivedPlaylistDto.songs",
                "MusicSearchResponseDto.tracks",
            )

        /** #790 — timeletter (1건) */
        private val TIMELETTER = setOf("ReceivedTimeLetterDto.blocks")

        val KNOWN_COERCING_DEFAULTS = MINDRECORD + AFTERNOTE + TIMELETTER
    }
}
