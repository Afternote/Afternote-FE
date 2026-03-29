package com.afternote.feature.afternote.presentation.receiver.wiring

import com.afternote.feature.afternote.presentation.receiver.wiring.model.AfterNotesListResult
import com.afternote.feature.afternote.presentation.receiver.wiring.model.LoadCountResult
import com.afternote.feature.afternote.presentation.receiver.wiring.model.ReceivedAfternoteDetail
import com.afternote.feature.afternote.presentation.receiver.wiring.model.ReceivedExportBundle
import javax.inject.Inject
import javax.inject.Singleton

fun interface ReceiverAuthCodeProvider {
    fun currentAuthCode(): String?
}

@Singleton
class StubReceiverAuthCodeProvider
    @Inject
    constructor() : ReceiverAuthCodeProvider {
        override fun currentAuthCode(): String? = null
    }

@Singleton
class GetAfterNotesByAuthCodeUseCase
    @Inject
    constructor() {
        suspend operator fun invoke(authCode: String): Result<AfterNotesListResult> =
            Result.success(
                AfterNotesListResult(
                    items = emptyList(),
                    totalCount = 0,
                ),
            )
    }

@Singleton
class GetAfternoteDetailByAuthCodeUseCase
    @Inject
    constructor() {
        suspend operator fun invoke(
            authCode: String,
            afternoteId: Long,
        ): Result<ReceivedAfternoteDetail> = Result.failure(IllegalStateException("Receiver afternote detail not wired"))
    }

@Singleton
class DownloadAllReceivedUseCase
    @Inject
    constructor() {
        suspend operator fun invoke(authCode: String): Result<ReceivedExportBundle> = Result.success(ReceivedExportBundle())
    }

fun interface ExportReceivedRepository {
    suspend fun saveToFile(bundle: ReceivedExportBundle): Result<Unit>
}

@Singleton
class StubExportReceivedRepository
    @Inject
    constructor() : ExportReceivedRepository {
        override suspend fun saveToFile(bundle: ReceivedExportBundle): Result<Unit> = Result.success(Unit)
    }

fun interface LoadMindRecordsByAuthCodePort {
    suspend operator fun invoke(authCode: String): Result<LoadCountResult>
}

fun interface LoadTimeLettersByAuthCodePort {
    suspend operator fun invoke(authCode: String): Result<LoadCountResult>
}

fun interface LoadSenderMessageByAuthCodePort {
    suspend operator fun invoke(authCode: String): Result<String?>
}

@Singleton
class StubLoadMindRecordsByAuthCodePort
    @Inject
    constructor() : LoadMindRecordsByAuthCodePort {
        override suspend fun invoke(authCode: String): Result<LoadCountResult> = Result.success(LoadCountResult(0))
    }

@Singleton
class StubLoadTimeLettersByAuthCodePort
    @Inject
    constructor() : LoadTimeLettersByAuthCodePort {
        override suspend fun invoke(authCode: String): Result<LoadCountResult> = Result.success(LoadCountResult(0))
    }

@Singleton
class StubLoadSenderMessageByAuthCodePort
    @Inject
    constructor() : LoadSenderMessageByAuthCodePort {
        override suspend fun invoke(authCode: String): Result<String?> = Result.success(null)
    }
