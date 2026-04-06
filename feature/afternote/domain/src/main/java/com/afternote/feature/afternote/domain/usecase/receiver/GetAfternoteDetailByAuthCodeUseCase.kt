package com.afternote.feature.afternote.domain.usecase.receiver

import com.afternote.feature.afternote.domain.model.receiver.ReceivedAfternoteDetail

fun interface GetAfternoteDetailByAuthCodeUseCase {
    suspend operator fun invoke(
        authCode: String,
        afternoteId: Long,
    ): Result<ReceivedAfternoteDetail>
}
