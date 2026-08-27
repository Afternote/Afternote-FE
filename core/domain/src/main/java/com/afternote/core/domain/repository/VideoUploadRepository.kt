package com.afternote.core.domain.repository

import com.afternote.core.domain.model.UploadedFile

/**
 * Core video upload: content URI → presigned URL → S3 PUT → uploaded file.
 * Reusable for memorial video, and any other video upload that uses
 * POST /files/presigned-url and the same S3 flow.
 */
fun interface VideoUploadRepository {
    /**
     * @param uriString content URI of the selected video (e.g. from gallery / Photo Picker).
     * @param directory target directory for the file (e.g. "afternotes").
     * @return Success with the uploaded file — full URL and file key both — or failure.
     */
    suspend fun upload(
        uriString: String,
        directory: String,
    ): Result<UploadedFile>
}
