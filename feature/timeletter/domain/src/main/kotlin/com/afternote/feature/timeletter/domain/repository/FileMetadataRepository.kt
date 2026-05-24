package com.afternote.feature.timeletter.domain.repository

interface FileMetadataRepository {
    suspend fun getFileName(uriString: String): String

    suspend fun getMimeType(uriString: String): String?
}
