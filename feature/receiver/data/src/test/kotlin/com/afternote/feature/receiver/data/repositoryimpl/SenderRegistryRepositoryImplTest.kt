package com.afternote.feature.receiver.data.repositoryimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class SenderRegistryRepositoryImplTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val snapshotKey = stringPreferencesKey("sender_registry_snapshot")

    @Test
    fun `실물 파일 DataStore 재생성 후 모든 카드 필드와 등록 순서를 복원한다`() {
        val file = File(tmp.root, "sender-registry.preferences_pb")
        val firstScope = newStoreScope()
        val firstStore = newFileDataStore(file, firstScope)
        val expected =
            try {
                runBlocking {
                    val repository = SenderRegistryRepositoryImpl(firstStore)
                    val first = repository.register("첫 번째 별칭").getOrThrow()
                    repository
                        .attachIdentity(
                            id = first.id,
                            masterKey = "master-key-1",
                            identity =
                                ReceiverIdentity(
                                    receiverId = 11L,
                                    receiverName = "수신자",
                                    senderName = "실제 발신자 1",
                                    relation = "부",
                                ),
                        ).getOrThrow()!!
                    val completedFirst =
                        repository
                            .updateVerificationStatus(first.id, DeliveryVerificationStatus.APPROVED)
                            .getOrThrow()!!

                    val second = repository.register("두 번째 별칭").getOrThrow()
                    val completedSecond =
                        repository
                            .attachIdentity(
                                id = second.id,
                                masterKey = "master-key-2",
                                identity =
                                    ReceiverIdentity(
                                        receiverId = 22L,
                                        receiverName = "다른 수신자",
                                        senderName = "실제 발신자 2",
                                        relation = "친구",
                                    ),
                            ).getOrThrow()!!

                    listOf(completedFirst, completedSecond)
                }
            } finally {
                shutdown(firstScope)
            }

        val secondScope = newStoreScope()
        try {
            val recreated = SenderRegistryRepositoryImpl(newFileDataStore(file, secondScope))

            assertEquals(expected, runBlocking { recreated.senders.first() })
        } finally {
            shutdown(secondScope)
        }
    }

    @Test
    fun `손상 JSON 읽기는 빈 목록이며 후속 쓰기 실패 시 원문 payload를 보존한다`() {
        val scope = newStoreScope()
        try {
            val dataStore = newFileDataStore(File(tmp.root, "malformed.preferences_pb"), scope)
            val malformed = "{ not-valid-json"
            runBlocking { dataStore.edit { it[snapshotKey] = malformed } }
            val repository = SenderRegistryRepositoryImpl(dataStore)

            assertEquals(emptyList<SenderEntry>(), runBlocking { repository.senders.first() })

            val write = runBlocking { repository.register("덮어쓰면 안 됨") }
            assertTrue(write.isFailure)
            assertEquals(malformed, runBlocking { dataStore.data.first()[snapshotKey] })
        } finally {
            shutdown(scope)
        }
    }

    @Test
    fun `DataStore 읽기 실패는 빈 목록을 방출한다`() {
        val repository = SenderRegistryRepositoryImpl(ReadFailingDataStore(IOException("read failed")))

        assertEquals(emptyList<SenderEntry>(), runBlocking { repository.senders.first() })
    }

    @Test
    fun `알 수 없는 저장 상태는 캐시 없음으로 낮추고 새 필드는 무시한다`() {
        val scope = newStoreScope()
        try {
            val dataStore = newFileDataStore(File(tmp.root, "future-status.preferences_pb"), scope)
            runBlocking {
                dataStore.edit {
                    it[snapshotKey] =
                        """
                        {
                          "schemaVersion": 1,
                          "futureSnapshotField": true,
                          "senders": [
                            {
                              "id": "sender-1",
                              "name": "별칭",
                              "verificationStatus": "WAITING_FOR_FUTURE",
                              "futureEntryField": 123
                            }
                          ]
                        }
                        """.trimIndent()
                }
            }
            val repository = SenderRegistryRepositoryImpl(dataStore)

            assertEquals(
                listOf(
                    SenderEntry(
                        // 모르는 상태 이름은 «캐시 없음»(null) 이다. UNKNOWN 으로 흡수하면 화면이
                        // 「아직 신청 안 함」으로 그려진다 (#1554 가 `fromRaw` 를 걷어낸 이유).
                        id = "sender-1",
                        name = "별칭",
                        verificationStatus = null,
                    ),
                ),
                runBlocking { repository.senders.first() },
            )
        } finally {
            shutdown(scope)
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

private class ReadFailingDataStore(
    private val failure: Exception,
) : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw failure }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = emptyPreferences()
}
