package com.afternote.feature.receiver.data.repositoryimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.afternote.feature.receiver.data.di.IdentityVerificationDataStore
import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private object IdentityVerificationKeys {
    /**
     * 발신자별 본인 확인 키 (#597). 이전의 단일 `identity_verified` 키는 발신자 구분 없이 전역이라
     * 발신자 A 인증만으로 발신자 B 의 이메일 관문까지 열렸다. 키를 발신자 단위로 쪼개 격리한다.
     *
     * 구 전역 키에 남은 값은 조회 대상에서 빠져 자연 무효화된다 — 발신자 카드가 in-memory 라
     * 앱 재시작 시 재등록(새 senderId)이 전제라 마이그레이션 실익이 없다.
     */
    fun verified(senderId: String): Preferences.Key<Boolean> = booleanPreferencesKey("identity_verified_$senderId")
}

/**
 * [IdentityVerificationRepository] 의 DataStore Preferences 기반 구현.
 *
 * 발신자별 1회 검증 정책 → 발신자별 boolean 키 (`identity_verified_<senderId>`).
 * process death · 앱 재시작 후에도 값 유지.
 */
@Singleton
class IdentityVerificationRepositoryImpl
    @Inject
    constructor(
        @param:IdentityVerificationDataStore private val dataStore: DataStore<Preferences>,
    ) : IdentityVerificationRepository {
        override fun isVerified(senderId: String): Flow<Boolean> =
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
                    preferences[IdentityVerificationKeys.verified(senderId)] ?: false
                }

        override suspend fun markVerified(senderId: String) {
            dataStore.edit { preferences ->
                preferences[IdentityVerificationKeys.verified(senderId)] = true
            }
        }
    }
