package com.afternote.feature.timeletter.domain.testing

import com.afternote.feature.timeletter.domain.repository.FileMetadataRepository
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [FileMetadataRepository] fake 정본 (#1030, #1043).
 *
 * 기본 메타데이터를 반환하면서 모든 URI 조회를 기록한다. URI별 응답이나 실패가 필요한
 * 시나리오는 `onX` 람다로 갈아끼운다.
 */
class FakeFileMetadataRepository(
    var fileName: String = "fixture",
    var mimeType: String? = null,
    var onGetFileName: (suspend (String) -> String)? = null,
    var onGetMimeType: (suspend (String) -> String?)? = null,
) : FileMetadataRepository {
    val fileNameRequests = CopyOnWriteArrayList<String>()
    val mimeTypeRequests = CopyOnWriteArrayList<String>()

    override suspend fun getFileName(uriString: String): String {
        fileNameRequests += uriString
        onGetFileName?.let { return it(uriString) }
        return fileName
    }

    override suspend fun getMimeType(uriString: String): String? {
        mimeTypeRequests += uriString
        onGetMimeType?.let { return it(uriString) }
        return mimeType
    }

    companion object {
        /** 모든 호출을 닫고 테스트가 실제로 쓰는 경로만 `onX` 로 연다. */
        fun strict(): FakeFileMetadataRepository =
            FakeFileMetadataRepository(
                onGetFileName = { unexpectedCall("FileMetadataRepository.getFileName") },
                onGetMimeType = { unexpectedCall("FileMetadataRepository.getMimeType") },
            )
    }
}
