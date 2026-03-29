package com.afternote.feature.afternote.domain.usecase.receiver

import com.afternote.feature.afternote.domain.model.receiver.ReceivedExportBundle

fun interface DownloadAllReceivedUseCase {
    suspend operator fun invoke(authCode: String): Result<ReceivedExportBundle>
}
