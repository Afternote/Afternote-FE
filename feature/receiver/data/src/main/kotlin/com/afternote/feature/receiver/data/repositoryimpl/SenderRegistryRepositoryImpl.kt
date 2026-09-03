package com.afternote.feature.receiver.data.repositoryimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.feature.receiver.data.di.SenderRegistryDataStore
import com.afternote.feature.receiver.domain.model.DeliveryVerificationStatus
import com.afternote.feature.receiver.domain.model.ReceiverIdentity
import com.afternote.feature.receiver.domain.model.SenderEntry
import com.afternote.feature.receiver.domain.repository.SenderRegistryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private object SenderRegistryKeys {
    val SNAPSHOT = stringPreferencesKey("sender_registry_snapshot")
}

private const val CURRENT_SCHEMA_VERSION = 1

@Serializable
private data class SenderRegistrySnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val senders: List<PersistedSenderEntry> = emptyList(),
)

@Serializable
private data class PersistedSenderEntry(
    val id: String,
    val name: String,
    val masterKey: String? = null,
    val realSenderName: String? = null,
    val relation: String? = null,
    val verificationStatus: String? = null,
)

/**
 * Preferences DataStore에 발신자 카드의 순서 있는 단일 JSON 스냅샷을 저장한다.
 *
 * 이 저장소만 별도 암호화하지 않는다. [com.afternote.feature.receiver.data.local.ReceiverMasterKeyDataSource]와
 * 토큰 저장소도 같은 종류의 비밀을 평문 DataStore에 보관하므로, 여기만 암호화하면 수명·마이그레이션
 * 계약이 갈라진다. 저장소 전체를 아우르는 암호화 전환은 별도 시스템 마이그레이션으로 다룬다.
 */
@Singleton
class SenderRegistryRepositoryImpl
    @Inject
    constructor(
        @param:SenderRegistryDataStore private val dataStore: DataStore<Preferences>,
    ) : SenderRegistryRepository {
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        override val senders: Flow<List<SenderEntry>> =
            dataStore.data
                .map { preferences -> decode(preferences[SenderRegistryKeys.SNAPSHOT]) }
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    if (failure is Exception) {
                        emit(emptyList())
                    } else {
                        throw failure
                    }
                }

        override suspend fun register(name: String): Result<SenderEntry> {
            val entry = SenderEntry(id = UUID.randomUUID().toString(), name = name)
            return runCatchingCancellable {
                dataStore.edit { preferences ->
                    val current = decode(preferences[SenderRegistryKeys.SNAPSHOT])
                    preferences[SenderRegistryKeys.SNAPSHOT] = encode(current + entry)
                }
                entry
            }
        }

        override suspend fun findById(id: String): Result<SenderEntry?> =
            runCatchingCancellable {
                val preferences = dataStore.data.first()
                decode(preferences[SenderRegistryKeys.SNAPSHOT]).firstOrNull { it.id == id }
            }

        override suspend fun attachIdentity(
            id: String,
            masterKey: String,
            identity: ReceiverIdentity,
        ): Result<SenderEntry?> =
            updateById(id) { entry ->
                entry.copy(
                    masterKey = masterKey,
                    realSenderName = identity.senderName,
                    relation = identity.relation,
                )
            }

        override suspend fun updateVerificationStatus(
            id: String,
            status: DeliveryVerificationStatus,
        ): Result<SenderEntry?> = updateById(id) { it.copy(verificationStatus = status) }

        private suspend fun updateById(
            id: String,
            transform: (SenderEntry) -> SenderEntry,
        ): Result<SenderEntry?> =
            runCatchingCancellable {
                var updated: SenderEntry? = null
                dataStore.edit { preferences ->
                    val current = decode(preferences[SenderRegistryKeys.SNAPSHOT])
                    val next =
                        current.map { entry ->
                            if (entry.id == id) {
                                transform(entry).also { updated = it }
                            } else {
                                entry
                            }
                        }
                    if (updated != null) {
                        preferences[SenderRegistryKeys.SNAPSHOT] = encode(next)
                    }
                }
                updated
            }

        private fun decode(raw: String?): List<SenderEntry> {
            if (raw == null) return emptyList()
            val snapshot = json.decodeFromString<SenderRegistrySnapshot>(raw)
            if (snapshot.schemaVersion != CURRENT_SCHEMA_VERSION) {
                throw UnsupportedSenderRegistrySchemaException(snapshot.schemaVersion)
            }
            return snapshot.senders.map(PersistedSenderEntry::toDomain)
        }

        private fun encode(entries: List<SenderEntry>): String =
            json.encodeToString(
                SenderRegistrySnapshot(
                    senders = entries.map(SenderEntry::toPersisted),
                ),
            )
    }

private class UnsupportedSenderRegistrySchemaException(
    version: Int,
) : IllegalStateException("지원하지 않는 sender registry schemaVersion=$version")

private fun PersistedSenderEntry.toDomain(): SenderEntry =
    SenderEntry(
        id = id,
        name = name,
        masterKey = masterKey,
        realSenderName = realSenderName,
        relation = relation,
        // 우리가 [SenderEntry.toPersisted] 로 직접 쓴 `name` 을 되읽는다. 모르는 값이면 «캐시 없음» 으로
        // 낮춘다 — 옛 릴리스가 남긴 이름이 사라졌을 때 [DeliveryVerificationStatus.UNKNOWN] 으로 흡수하면
        // 「아직 신청 안 함」으로 그려져 버린다 (#1554 가 `fromRaw` 를 걷어낸 것과 같은 이유).
        verificationStatus = verificationStatus?.let(DeliveryVerificationStatus::fromWireOrNull),
    )

private fun SenderEntry.toPersisted(): PersistedSenderEntry =
    PersistedSenderEntry(
        id = id,
        name = name,
        masterKey = masterKey,
        realSenderName = realSenderName,
        relation = relation,
        verificationStatus = verificationStatus?.name,
    )
