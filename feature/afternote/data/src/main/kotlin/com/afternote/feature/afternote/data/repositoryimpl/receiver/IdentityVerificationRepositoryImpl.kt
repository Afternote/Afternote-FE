package com.afternote.feature.afternote.data.repositoryimpl.receiver

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.afternote.feature.afternote.data.di.IdentityVerificationDataStore
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private object IdentityVerificationKeys {
    val VERIFIED = booleanPreferencesKey("identity_verified")
}

/**
 * [IdentityVerificationRepository] 의 DataStore Preferences 기반 구현.
 *
 * 사람 단위 1회 검증 정책 → 단일 boolean 키 (`identity_verified`).
 * process death · 앱 재시작 후에도 값 유지.
 */
@Singleton
class IdentityVerificationRepositoryImpl
    @Inject
    constructor(
        @param:IdentityVerificationDataStore private val dataStore: DataStore<Preferences>,
    ) : IdentityVerificationRepository {
        override val isVerified: Flow<Boolean> =
            // dataStore.data 가 Flow<Preferences> source — 디스크 파일 변경 감지해 새 Preferences 흘림.
            // 왜 Flow? 디스크 값이 시간에 따라 변할 수 있고 (verify 완료 시 false → true),
            // UI 가 그 변화를 자동 reactive 하게 받아 화면 갱신해야 해서.
            // "한 번 읽고 끝" 이면 suspend 함수로 충분했지만 본 케이스는 시계열.
            // 이하 .catch / .map 은 그 Flow 의 operator (변환·예외 처리).
            dataStore.data
                .catch { exception ->
                    // 람다의 implicit `this` = FlowCollector<Preferences> (Flow.catch 가 람다 호출 시
                    // 숨겨서 주입 — "lambda with receiver" 패턴). 본인 코드엔 안 적혀있지만 컴파일러가 인식.
                    //
                    // IOException = 디스크 손상·권한·storage 풀 등 *환경* 문제 (코드 버그 아님).
                    //   → 빈 Preferences 흘림 → 아래 .map 이 `null ?: false` 로 폴백 → consumer 는 `false` 받음.
                    //   앱 크래시 회피 + "verify 안 된 상태" 로 정상 작동 (사용자가 재시도 가능).
                    // 그 외 예외 = 보통 코드 버그 → 숨기면 디버깅 불가 → 그대로 throw.
                    // (DataStore 공식 가이드 권장 패턴 — ReceiverAuthCodeDataSource 와 동일.)
                    if (exception is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { preferences ->
                    // `preferences` = typed Map<Preferences.Key<*>, ...> (immutable). subscript 접근 = Map 처럼.
                    // SharedPreferences 레거시 이름 그대로 가져옴 — "settings UI" 어감이지만 실체는 단순 키-값 컨테이너.
                    preferences[IdentityVerificationKeys.VERIFIED] ?: false
                }

        override suspend fun markVerified() {
            dataStore.edit { preferences ->
                preferences[IdentityVerificationKeys.VERIFIED] = true
            }
        }
    }
