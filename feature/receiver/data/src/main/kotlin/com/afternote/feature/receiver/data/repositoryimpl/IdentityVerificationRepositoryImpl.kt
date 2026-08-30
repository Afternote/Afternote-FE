package com.afternote.feature.receiver.data.repositoryimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.afternote.feature.receiver.data.di.IdentityVerificationDataStore
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private object IdentityVerificationKeys {
    val VERIFIED_SENDER_IDS = stringSetPreferencesKey("identity_verified_sender_ids")
}

/**
 * 발신자별 본인 확인 완료 상태를 SESSION Preferences DataStore에 보관한다.
 *
 * [SenderRegistryRepositoryImpl]이 로컬 카드 ID를 프로세스 재시작 뒤에도 그대로 복원하므로,
 * 같은 ID로 저장한 확인 상태도 재기동 뒤 유효하다. 발신자 A의 확인이 B의 관문을 열지 않도록
 * 한 집합 안에서도 ID별로 격리하며, 로그아웃 시 SESSION 저장소와 함께 지워진다.
 */
@Singleton
class IdentityVerificationRepositoryImpl
    @Inject
    constructor(
        @param:IdentityVerificationDataStore private val dataStore: DataStore<Preferences>,
    ) : IdentityVerificationRepository {
        override fun isVerified(senderId: String): Flow<Boolean> =
            dataStore.data
                .map { preferences -> senderId in preferences[IdentityVerificationKeys.VERIFIED_SENDER_IDS].orEmpty() }
                .catch { failure ->
                    if (failure is CancellationException) throw failure
                    if (failure is Exception) {
                        emit(false)
                    } else {
                        throw failure
                    }
                }.distinctUntilChanged()

        override suspend fun markVerified(senderId: String) {
            dataStore.edit { preferences ->
                val verified = preferences[IdentityVerificationKeys.VERIFIED_SENDER_IDS].orEmpty()
                preferences[IdentityVerificationKeys.VERIFIED_SENDER_IDS] = verified + senderId
            }
        }
    }
