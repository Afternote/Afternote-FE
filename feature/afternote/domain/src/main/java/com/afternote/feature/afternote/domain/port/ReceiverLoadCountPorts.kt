package com.afternote.feature.afternote.domain.port

import com.afternote.feature.afternote.domain.model.receiver.LoadCountResult

fun interface LoadMindRecordsByAuthCodePort {
    suspend operator fun invoke(authCode: String): Result<LoadCountResult>
}

fun interface LoadTimeLettersByAuthCodePort {
    suspend operator fun invoke(authCode: String): Result<LoadCountResult>
}

fun interface LoadSenderMessageByAuthCodePort {
    suspend operator fun invoke(authCode: String): Result<String?>
}
