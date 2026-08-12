package com.afternote.feature.setting.presentation.model

enum class InquiryStatus { RECEIVED, ANSWERED }

data class InquiryUiModel(
    val id: Long,
    val status: InquiryStatus,
    val date: String,
    val title: String,
    val content: String,
    val answer: String?,
)

val sampleInquiries =
    listOf(
        InquiryUiModel(
            id = 1L,
            status = InquiryStatus.ANSWERED,
            date = "2025.08.09.",
            title = "타임레터 발송일 변경이 안 돼요",
            content =
                "타임레터를 작성한 뒤 발송일을 변경하려고 하는데 날짜를 선택해도 기존 날짜로 계속 표시됩니다. " +
                    "앱을 종료했다가 다시 실행해도 동일하고, 수정 버튼을 눌러 저장해도 변경사항이 반영되지 않아요.\n" +
                    "현재 발송 예정일은 8월 20일로 설정되어 있고, 9월 15일로 변경하려고 합니다. 확인 부탁드립니다.",
            answer =
                "안녕하세요 애프터노트입니다.\n\n타임레터 발신인은 마이페이지에서 변경하실 수 있습니다.\n" +
                    "마이페이지 > 타임레터 관리에서 변경을 원하시는 타임레터를 선택한 후, 발송예정일을 변경해 주세요.\n" +
                    "변경된 날짜는 저장 후 정상적으로 반영됩니다.\n\n" +
                    "추가로 이용에 어려움이 있으실 경우 언제든지 문의해 주세요.\n\n감사합니다.",
        ),
        InquiryUiModel(
            id = 2L,
            status = InquiryStatus.RECEIVED,
            date = "2025.08.09.",
            title = "타임레터 발송일 변경이 안 돼요",
            content = "발송 예약일은 마이페이지에서 변경이 가능한지 문의드립니다.",
            answer = null,
        ),
    )
