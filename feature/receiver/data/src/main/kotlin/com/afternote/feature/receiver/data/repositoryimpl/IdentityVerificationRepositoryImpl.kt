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
 * 발신자별 1회 검증 정책 (#597) — 발신자 A 인증이 발신자 B 의 관문을 열지 않도록 senderId 단위로
 * 격리해 보관한다.
 *
 * ## 왜 다시 영속인가 (#599)
 *
 * 키인 `senderId` 는 [SenderRegistryRepositoryImpl] 이 발급하는 로컬 카드 ID 다. #597 시점에는 그
 * 발급처가 in-memory stub 이라 앱을 재시작하면 같은 발신자도 새 UUID 를 받았고, 그래서 디스크에 남긴
 * 키는 «다시는 조회되지 않는 죽은 값» 으로 단조 누적되기만 했다 — 그 이유로 저장 수명을 프로세스에
 * 맞췄다.
 *
 * #599 가 그 전제를 없앴다. 카드 ID 가 재기동 뒤에도 그대로 복원되므로 같은 ID 로 저장한 확인 상태도
 * 재기동 뒤 유효하다. 그래서 SESSION scope 영속으로 되돌린다 — 로그아웃 시
 * [com.afternote.core.datastore.LocalStoreRegistry.clearScope] 가 카드와 함께 지운다.
 *
 * `@Singleton` — 열람 신청 흐름의 ViewModel 들이 같은 인스턴스를 공유해야 캐시가 의미를 갖는다.
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
