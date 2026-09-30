package com.afternote.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.afternote.core.datastore.di.TokenDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore 기반 액세스·리프레시 토큰 로컬 저장.
 * 읽기 스트림은 [IOException] 시 [emptyPreferences]로 복구합니다.
 *
 * 토큰과 함께 [sessionId] 를 보관한다. 로그인 여부(Boolean)만으로는 "로그아웃 뒤 다른 계정 로그인" 을
 * 관측자가 구분할 수 없기 때문이다 (#2135).
 */
@Singleton
class TokenDataSource
    @Inject
    constructor(
        @param:TokenDataStore private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val ACCESS_TOKEN = stringPreferencesKey("access_token")
            val REFRESH_TOKEN = stringPreferencesKey("refresh_token")

            // 키 이름은 저장 스키마 계약이다. 바꾸면 기존 세션이 식별자 없는 세션으로 떨어진다.
            val SESSION_ID = stringPreferencesKey("session_id")
        }

        private val preferencesFlow: Flow<Preferences> =
            dataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }

        /**
         * 지금 저장된 로그인 세션의 식별자. 세션이 없으면 `null` 이다.
         *
         * 세션마다 **값이 다른 것** 이 이 흐름의 계약이다. 로그인 여부 Boolean 은 로그아웃과 새 로그인이
         * 한 구간에 겹치면 관측자에게 `true → true` 로 뭉개져, 세션이 바뀐 줄 모르는 구독자가 이전 계정의
         * 상태를 이어 쓴다 (#2135). 식별자는 [saveTokens] 가 토큰과 **같은 `edit` 블록** 에서 새로 발급하므로
         * (원자적 기록) 토큰과 식별자가 어긋난 중간 상태가 관측되지 않고, 새 로그인이 이전 세션과 같은 토큰
         * 값을 받아도 값이 같아지지 않는다.
         *
         * 식별자 키가 없던 시절에 저장된 세션은 [LEGACY_SESSION_ID] 로 읽는다. 로그인 상태는 그대로 유지되고,
         * 다음 로그인부터 발급된 식별자를 쓴다.
         */
        val sessionId: Flow<String?> =
            preferencesFlow.map { prefs ->
                if (prefs[Keys.ACCESS_TOKEN] == null) {
                    null
                } else {
                    prefs[Keys.SESSION_ID] ?: LEGACY_SESSION_ID
                }
            }

        val isLoggedIn: Flow<Boolean> = sessionId.map { it != null }

        /** 로그인으로 세션을 새로 연다. 세션 식별자를 토큰과 같은 쓰기로 함께 발급한다. */
        suspend fun saveTokens(
            accessToken: String,
            refreshToken: String,
        ) {
            dataStore.edit { prefs ->
                prefs[Keys.ACCESS_TOKEN] = accessToken
                prefs[Keys.REFRESH_TOKEN] = refreshToken
                prefs[Keys.SESSION_ID] = UUID.randomUUID().toString()
            }
        }

        /**
         * 같은 세션 안에서 토큰만 회전한다 ([saveTokens] 와 달리 [sessionId] 를 새로 발급하지 않는다).
         * 회전은 계정이 바뀌는 사건이 아니므로, 세션에 귀속된 캐시를 버리게 만들면 안 된다.
         */
        suspend fun updateTokens(
            accessToken: String,
            refreshToken: String,
        ) {
            dataStore.edit { prefs ->
                prefs[Keys.ACCESS_TOKEN] = accessToken
                prefs[Keys.REFRESH_TOKEN] = refreshToken
            }
        }

        suspend fun getAccessToken(): String? = preferencesFlow.first()[Keys.ACCESS_TOKEN]

        suspend fun getRefreshToken(): String? = preferencesFlow.first()[Keys.REFRESH_TOKEN]

        private companion object {
            /** 세션 식별자 키가 생기기 전에 저장된 세션의 식별자. 그 세션이 끝날 때까지 이 값 하나로 고정된다. */
            const val LEGACY_SESSION_ID = "legacy-session"
        }
    }
