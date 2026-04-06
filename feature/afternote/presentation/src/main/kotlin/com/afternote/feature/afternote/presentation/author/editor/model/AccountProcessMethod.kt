package com.afternote.feature.afternote.presentation.author.editor.model

/**
 * 계정 처리 방법 (소셜네트워크 카테고리).
 *
 * [clientName]은 클라이언트 내부에서 사용하는 이름,
 * [serverValue]는 서버 API에 전송하는 코드 문자열입니다.
 */
enum class AccountProcessMethod(
    val clientName: String,
    val serverValue: String,
) {
    MEMORIAL_ACCOUNT("MEMORIAL_ACCOUNT", "MEMORIAL"),
    PERMANENT_DELETE("PERMANENT_DELETE", "DELETE"),
    TRANSFER_TO_RECEIVER("TRANSFER_TO_RECEIVER", "TRANSFER"),
    ;

    companion object {
        private val byClientName = entries.associateBy { it.clientName }
        private val byServerValue =
            mapOf(
                "MEMORIAL" to MEMORIAL_ACCOUNT,
                "DELETE" to PERMANENT_DELETE,
                "TRANSFER" to TRANSFER_TO_RECEIVER,
                "RECEIVER" to TRANSFER_TO_RECEIVER,
            )

        fun fromClientName(name: String): AccountProcessMethod? = byClientName[name]

        fun fromServerValue(value: String): AccountProcessMethod? = byServerValue[value.uppercase()]

        val validClientNames: Set<String> = byClientName.keys
    }
}

/**
 * 정보 처리 방법 (갤러리 카테고리).
 */
enum class InfoProcessMethod(
    val clientName: String,
    val serverValue: String,
) {
    TRANSFER_TO_RECEIVER("TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER", "TRANSFER"),
    ;

    companion object {
        fun fromServerValue(value: String): InfoProcessMethod? =
            when (value.uppercase()) {
                "TRANSFER", "RECEIVER", "ADDITIONAL" -> TRANSFER_TO_RECEIVER
                else -> null
            }
    }
}
