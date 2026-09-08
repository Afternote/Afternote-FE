package com.afternote.feature.receiver.domain.repository

/**
 * 수신자 열람 신청 흐름의 증빙 서류 업로드 전용 Repository (이슈 #215, 디자인 6·7·8).
 *
 * `receiver-auth/presigned-url` 응답으로 받은 S3 URL 에 바이트를 PUT 한 뒤 파일의 공개 URL 을 반환한다.
 * 호출자(ViewModel) 는 Android 의 `ContentResolver` 로 Uri → `ByteArray` 변환을 마친 뒤 본 함수를
 * 호출한다 (도메인 레이어가 안드로이드 API 에 의존하지 않도록).
 *
 * 업로드 + presigned URL 요청은 모두 `receiver-auth/` 인증 컨텍스트가 필요하므로, 호출 시점에
 * [ReceiverRepository.saveMasterKey] 로 해당 발신자의 masterKey 가 세팅돼 있어야 한다 (인터셉터가 헤더 부착).
 */
interface ReceiverDeliveryDocumentUploadRepository {
    /**
     * @param bytes 업로드할 파일 바이트 (이미지·PDF 등).
     * @param extension 확장자 (예: `jpg`, `png`, `pdf`). presigned URL 요청 페이로드에 그대로 전달된다.
     * @return 업로드 성공 시 [Result.success] 와 함께 공개 fileUrl. 실패 시 [Result.failure].
     */
    suspend fun upload(
        bytes: ByteArray,
        extension: String,
    ): Result<String>
}
