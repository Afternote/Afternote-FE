package com.afternote.feature.receiver.data.repositoryimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.network.model.BaseResponse
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteDetailDto
import com.afternote.feature.receiver.data.dto.ReceivedAfternoteListDto
import com.afternote.feature.receiver.data.local.ReceiverMasterKeyDataSource
import com.afternote.feature.receiver.data.service.ReceiverAfternoteApiService
import com.afternote.feature.receiver.domain.error.ReceiverFailure
import com.afternote.feature.receiver.domain.model.ReceivedExportBundle
import com.afternote.feature.receiver.domain.testing.FakeReceiverAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiverRepositoryImplExportTest {
    private val repository =
        ReceiverRepositoryImpl(
            masterKeyDataSource = ReceiverMasterKeyDataSource(InMemoryPreferencesDataStore()),
            api = UnusedReceiverAfternoteApiService,
            receiverAuthRepository = FakeReceiverAuthRepository.strict(),
            errorReporter = UnusedErrorReporter,
        )

    @Test
    fun `downloadReceivedExport - 구현 전에는 ExportNotSupported 실패로 닫힌다`() {
        val failure = runBlocking { repository.downloadReceivedExport() }.exceptionOrNull()

        assertTrue(failure is ReceiverFailure.ExportNotSupported)
    }

    @Test
    fun `saveReceivedExportToFile - 구현 전에는 ExportNotSupported 실패로 닫힌다`() {
        val failure =
            runBlocking {
                repository.saveReceivedExportToFile(ReceivedExportBundle(payloadJson = "not-written"))
            }.exceptionOrNull()

        assertTrue(failure is ReceiverFailure.ExportNotSupported)
    }
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> get() = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val transformed = transform(state.value)
        state.value = transformed
        return transformed
    }
}

private object UnusedReceiverAfternoteApiService : ReceiverAfternoteApiService {
    override suspend fun getReceiverAfternotes(): BaseResponse<ReceivedAfternoteListDto> = error("unused")

    override suspend fun getReceiverAfternoteDetail(afternoteId: Long): BaseResponse<ReceivedAfternoteDetailDto> = error("unused")
}

private object UnusedErrorReporter : ErrorReporter {
    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) = error("unused")
}
