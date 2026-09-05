package com.afternote.feature.afternote.presentation.editor

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.editor.message.EditorMessageTextBlock
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 신규 생성·수정 저장 공통으로 서버에 보낼 [RegisterAfternotePayload] 조립.
 * 날짜 포맷 등은 State Holder가 아닌 여기서 처리한다.
 *
 * Compose runtime(`TextFieldState` 등) 에 의존하지 않도록 모든 입력은 평범한 값으로 받는다 —
 * 호출자가 facade에서 텍스트를 추출해 넘긴다. 단위 테스트에서 facade 목 없이 바로 검증 가능하다.
 */
object SaveAfternotePayloadBuilder {
    /** MEMORIAL은 서비스명 입력이 없으므로 서버 title에 화면의 고정 카테고리명을 사용한다. */
    private const val MEMORIAL_DEFAULT_TITLE = "추억 노트"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    /**
     * @param form 폼 SSOT 스냅샷
     * @param messageBlocks 현재 남기실 말씀 입력에서 만든 저장용 값
     * @param accountId 작성자 계정 ID (UI [androidx.compose.foundation.text.input.TextFieldState] 에서 추출)
     * @param password 작성자 계정 비밀번호 (UI 텍스트에서 추출)
     * @param date 저장 날짜. 기본값 [LocalDate.now] — 테스트에선 결정적 값 주입 가능.
     */
    fun build(
        form: EditorFormState,
        messageBlocks: List<EditorMessageTextBlock>,
        accountId: String,
        password: String,
        date: LocalDate = LocalDate.now(),
    ): RegisterAfternotePayload {
        val methods = form.processingMethods.map { it.text }
        return RegisterAfternotePayload(
            serviceName =
                if (form.selectedType == AfternoteType.MEMORIAL) {
                    MEMORIAL_DEFAULT_TITLE
                } else {
                    // 미선택(null)이면 빈 문자열 → Validator 의 TITLE_REQUIRED 가 등록을 차단한다.
                    form.selectedService.orEmpty()
                },
            date = date.format(dateFormatter),
            accountId = accountId,
            password = password,
            messageBlocks = messageBlocks,
            processingMethods = methods,
        )
    }
}
