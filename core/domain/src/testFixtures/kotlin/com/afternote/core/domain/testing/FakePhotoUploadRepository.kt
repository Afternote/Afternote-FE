package com.afternote.core.domain.testing

import com.afternote.core.domain.model.UploadedFile
import com.afternote.core.domain.repository.PhotoUploadRepository
import java.util.concurrent.CopyOnWriteArrayList

/** [PhotoUploadRepository] fake 정본. 업로드 요청을 기록하고 기본 업로드 결과를 돌려준다. */
class FakePhotoUploadRepository(
    var uploadedUrl: String = DEFAULT_UPLOADED_URL,
    var uploadedKey: String = DEFAULT_UPLOADED_KEY,
    var onUpload: (suspend (String, String) -> Result<UploadedFile>)? = null,
) : PhotoUploadRepository {
    val uploads = CopyOnWriteArrayList<Pair<String, String>>()

    override suspend fun upload(
        uriString: String,
        directory: String,
    ): Result<UploadedFile> {
        uploads += uriString to directory
        onUpload?.let { return it(uriString, directory) }
        return Result.success(UploadedFile(fileUrl = uploadedUrl, fileKey = uploadedKey))
    }

    companion object {
        const val DEFAULT_UPLOADED_URL = "https://cdn.test/uploaded"
        const val DEFAULT_UPLOADED_KEY = "uploads/test/uploaded"

        fun strict(): FakePhotoUploadRepository =
            FakePhotoUploadRepository(
                onUpload = { _, _ -> unexpectedCall("PhotoUploadRepository.upload") },
            )
    }
}
