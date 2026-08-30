package com.afternote.feature.receiver.data.repositoryimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/** 발신자별 본인 확인 격리와 SESSION DataStore 재기동 복원 계약. */
class IdentityVerificationRepositoryImplTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `A 발신자 인증이 B 발신자 관문을 열지 않는다`() {
        val repository = IdentityVerificationRepositoryImpl(InMemoryPreferencesDataStore())

        runBlocking { repository.markVerified("sender-a") }

        assertTrue(runBlocking { repository.isVerified("sender-a").first() })
        assertFalse(runBlocking { repository.isVerified("sender-b").first() })
    }

    @Test
    fun `발신자별 인증은 각자 독립적으로 누적된다`() {
        val repository = IdentityVerificationRepositoryImpl(InMemoryPreferencesDataStore())

        runBlocking {
            repository.markVerified("sender-a")
            repository.markVerified("sender-b")
        }

        assertTrue(runBlocking { repository.isVerified("sender-a").first() })
        assertTrue(runBlocking { repository.isVerified("sender-b").first() })
        assertFalse(runBlocking { repository.isVerified("sender-c").first() })
    }

    @Test
    fun `실물 파일 DataStore 재생성 후 같은 발신자 ID의 인증 상태를 복원한다`() {
        val file = File(tmp.root, "identity-verification.preferences_pb")
        val firstScope = newStoreScope()
        try {
            runBlocking {
                val first = IdentityVerificationRepositoryImpl(newFileDataStore(file, firstScope))
                first.markVerified("persistent-sender-id")
            }
        } finally {
            shutdown(firstScope)
        }

        val secondScope = newStoreScope()
        try {
            val recreated = IdentityVerificationRepositoryImpl(newFileDataStore(file, secondScope))

            assertTrue(runBlocking { recreated.isVerified("persistent-sender-id").first() })
            assertFalse(runBlocking { recreated.isVerified("different-sender-id").first() })
        } finally {
            shutdown(secondScope)
        }
    }

    @Test
    fun `DataStore 읽기 실패는 미인증으로 방출한다`() {
        val repository = IdentityVerificationRepositoryImpl(ReadFailingIdentityDataStore())

        assertFalse(runBlocking { repository.isVerified("sender-a").first() })
    }

    @Test
    fun `markVerified 이전에 얻어 둔 Flow도 갱신된 값을 준다`() {
        val repository = IdentityVerificationRepositoryImpl(InMemoryPreferencesDataStore())

        runBlocking {
            val flow = repository.isVerified("sender-a")
            assertFalse(flow.first())

            repository.markVerified("sender-a")

            assertTrue(flow.first())
        }
    }

    private fun newStoreScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun newFileDataStore(
        file: File,
        scope: CoroutineScope,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope) { file }

    private fun shutdown(scope: CoroutineScope) {
        runBlocking {
            val job = scope.coroutineContext.job
            job.cancel()
            job.join()
        }
    }
}

private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

private class ReadFailingIdentityDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw IOException("read failed") }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = emptyPreferences()
}
