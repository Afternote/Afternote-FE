package com.afternote.feature.afternote.domain.port

import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle

fun interface ExportReceivedRepository {
    suspend fun saveToFile(bundle: ReceivedExportBundle): Result<Unit>
}
