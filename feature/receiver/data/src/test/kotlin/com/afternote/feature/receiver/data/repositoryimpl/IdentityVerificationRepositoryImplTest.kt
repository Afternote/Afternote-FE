package com.afternote.feature.receiver.data.repositoryimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 본인 확인 캐시의 발신자별 격리 (#597).
 *
 * 이전 구현은 전역 `identity_verified` boolean 하나라 발신자 A 인증만으로
 * 발신자 B 의 이메일 관문까지 열렸다 — 키를 `identity_verified_<senderId>` 로 쪼갠
 * 격리가 실제 DataStore 경로에서 지켜지는지 본다.
 */
class IdentityVerificationRepositoryImplTest {
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
    fun `구 전역 키 잔존값은 어떤 발신자의 관문도 열지 않는다`() {
        // #597 이전 릴리스가 남긴 전역 boolean 이 디스크에 있어도 발신자별 조회에는 잡히지 않는다.
        val dataStore = InMemoryPreferencesDataStore()
        runBlocking {
            dataStore.edit { preferences ->
                preferences[booleanPreferencesKey("identity_verified")] = true
            }
        }
        val repository = IdentityVerificationRepositoryImpl(dataStore)

        assertFalse(runBlocking { repository.isVerified("sender-a").first() })
    }
}

/** 파일 IO 없이 [DataStore] 계약만 흉내 내는 인메모리 구현 — 단위 테스트 전용. */
private class InMemoryPreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
