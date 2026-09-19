package com.afternote.core.domain.error

/**
 * 파일 업로드(presigned URL 발급 → S3 PUT) 실패 중 **사유가 확인된 것**의 공통 루트.
 *
 * 사진·영상 두 업로드가 같은 `POST /files/presigned-url` 로 크기를 실어 보내고 같은 서버 코드를
 * 맞으므로 매체별로 가르지 않는다 — 실패가 걸리는 대상이 사진이나 영상이 아니라 파일이다.
 *
 * data 계층이 `ApiException` 을 이 계열로 번역하고, presentation 은 이 루트로 좁힌 뒤 `when` 으로
 * 가른다 — 하위 타입이 늘면 컴파일러가 소비처를 잡아준다(`sealed`). 사유를 확인하지 못한 실패는
 * 번역하지 않고 원본 그대로 흘려보내므로, 소비처의 폴백 분기는 계속 필요하다.
 *
 * 표시 문구는 호출처 리소스가 갖는다(BE#92 — 서버 `message` 는 사용자 노출용이라는 규정이 없다).
 * 서버 코드값은 cause 인 `ApiException` 이 갖는다. `message` 는 그 둘 어느 쪽도 아닌 **리포팅
 * 콘솔용 정적 진단 문구**다 — [CoreAuthFailure] 가 세운 규약을 그대로 따른다.
 */
sealed class FileUploadFailure(
    message: String,
    cause: Throwable,
) : Exception(message, cause) {
    /**
     * 업로드하려는 파일이 서버가 허용하는 크기를 넘었다는 사실(서버 code 1803 · FILE_SIZE_EXCEEDED).
     *
     * 재시도로 풀리지 않는 유일한 업로드 실패라 따로 둔다 — 같은 파일을 다시 보내면 서버가 같은
     * 코드로 다시 거절한다. 두 저장소 구현 모두 원본 파일 크기를 그대로 실어 보내므로
     * (`contentLength = tempFile.length()`) 사진·영상 어느 경로에서든 온다.
     */
    class FileSizeExceeded(
        cause: Throwable,
    ) : FileUploadFailure("upload file size exceeded", cause)
}
