package com.afternote.feature.receiver.domain.testing

import com.afternote.feature.receiver.domain.repository.ReceiverDeliveryDocumentUploadRepository
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [ReceiverDeliveryDocumentUploadRepository] fake 정본.
 *
 * 기본 동작은 업로드 호출을 [uploadCalls]에 기록하고 [defaultFileUrl]을 돌려준다.
 * 실패·게이트 등 저장소 동작으로 표현할 수 없는 시나리오만 [onUpload]로 교체한다.
 */
class FakeReceiverDeliveryDocumentUploadRepository(
    var defaultFileUrl: String = "memory://receiver-document",
    var onUpload: (suspend (ByteArray, String) -> Result<String>)? = null,
) : ReceiverDeliveryDocumentUploadRepository {
    val uploadCalls = CopyOnWriteArrayList<UploadCall>()

    data class UploadCall(
        val bytes: ByteArray,
        val extension: String,
    )

    override suspend fun upload(
        bytes: ByteArray,
        extension: String,
    ): Result<String> {
        uploadCalls += UploadCall(bytes.copyOf(), extension)
        onUpload?.let { return it(bytes, extension) }
        return Result.success(defaultFileUrl)
    }

    companion object {
        /** 모든 호출을 실패시키고, 시나리오가 실제로 쓰는 것만 [onUpload]로 연다. */
        fun strict(): FakeReceiverDeliveryDocumentUploadRepository =
            FakeReceiverDeliveryDocumentUploadRepository(
                onUpload = { _, _ ->
                    unexpectedCall("ReceiverDeliveryDocumentUploadRepository.upload")
                },
            )
    }
}
