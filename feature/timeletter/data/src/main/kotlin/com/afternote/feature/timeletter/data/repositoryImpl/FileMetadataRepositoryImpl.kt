package com.afternote.feature.timeletter.data.repositoryImpl

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.afternote.feature.timeletter.domain.repository.FileMetadataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FileMetadataRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : FileMetadataRepository {
        override suspend fun getFileName(uriString: String): String {
            val uri = Uri.parse(uriString)
            return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            } ?: uri.lastPathSegment ?: "파일"
        }

        override suspend fun getMimeType(uriString: String): String? =
            context.contentResolver.getType(Uri.parse(uriString))
    }
