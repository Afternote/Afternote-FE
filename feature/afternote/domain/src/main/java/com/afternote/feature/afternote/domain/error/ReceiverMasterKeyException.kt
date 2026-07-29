package com.afternote.feature.afternote.domain.error

/**
 * 마스터 키 검증(`receiver-auth/verify`) API 가 거절한 실패.
 *
 * 계약(문구·code 의 의미, 왜 흐름별로 나누는지)은 상위 [ReceiverServerRejectionException] 참고.
 * 이 흐름의 거절은 대부분 키 오타라 서버가 안내 문구를 함께 내려준다.
 */
class ReceiverMasterKeyException(
    status: Int,
    serverMessage: String?,
    serverCode: Int,
) : ReceiverServerRejectionException(
        status = status,
        serverMessage = serverMessage,
        serverMessageFallback = "master key verify failed (serverCode=$serverCode)",
    )
