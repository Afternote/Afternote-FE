package com.afternote.feature.afternote.data.repositoryimpl

import com.afternote.core.domain.repository.PhotoUploadRepository
import com.afternote.feature.afternote.domain.repository.MemorialPhotoUploadRepository
import javax.inject.Inject

private const val DIRECTORY_AFTERNOTES = "afternotes"

class MemorialPhotoUploadRepositoryImpl
    @Inject
    constructor(
        private val photoUploadRepository: PhotoUploadRepository,
    ) : MemorialPhotoUploadRepository {
        override suspend fun upload(uriString: String): Result<String> = photoUploadRepository.upload(uriString, DIRECTORY_AFTERNOTES)
    }
