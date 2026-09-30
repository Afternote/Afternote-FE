package com.afternote.core.data.repoimpl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.afternote.core.datastore.TokenDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * 실제 파일 Preferences DataStore + [TokenDataSource] 실물로 세션 경계를 세우는 테스트 하네스 (#2135).
 *
 * fake 의 `MutableStateFlow` 를 토글하는 방식으로는 로그아웃 방출이 수집자에게 뭉개지는 구간이 재현되지
 * 않는다. 이 결함이 정확히 그 구멍으로 살아남았다. 저장소 생성 경로는 `LocalStoreRegistryImpl.createStore`
 * 와 같고, [clearSession] 은 로그아웃·탈퇴·401 이 쓰는 `LocalStoreRegistry.clearScope(StoreScope.SESSION)`
 * 과 같이 키를 통째로 비운다.
 */
internal class TestTokenSessionStore(
    directory: File,
) {
    private val storeScope = CoroutineScope(SupervisorJob())

    private val storedPreferences: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = storeScope) { File(directory, STORE_FILE_NAME) }

    private val observationHeld = MutableStateFlow(false)

    private val store: DataStore<Preferences> = ObservationGatedDataStore(storedPreferences, observationHeld)

    val tokenDataSource = TokenDataSource(store)

    /**
     * 이 시점에 이미 열려 있는 관측 스트림에 한해 새 저장값의 전달을 멈춘다 (#2135).
     *
     * 쓰기는 그대로 커밋되고, 멈춘 동안 새로 여는 읽기(`sessionId.first()` 같은 즉시 재조회)는 현재 저장값을
     * 본다. 실제 앱에서 수집자의 디스패처가 바빠 세션 전환 통지를 아직 처리하지 못한 구간이 이 모양이다.
     * 멈춘 구간에 쌓인 중간 상태는 [releaseObservation] 때 마지막 값 하나로 합쳐져 전달된다.
     */
    fun holdObservation() {
        observationHeld.value = true
    }

    fun releaseObservation() {
        observationHeld.value = false
    }

    /** 로그인. 프로덕션에서 `AuthRepository.saveSession` 이 도달하는 쓰기와 같다. */
    suspend fun login(
        accessToken: String = DEFAULT_ACCESS_TOKEN,
        refreshToken: String = DEFAULT_REFRESH_TOKEN,
    ) = tokenDataSource.saveTokens(accessToken = accessToken, refreshToken = refreshToken)

    /** 같은 세션의 토큰 회전. `AuthRepository.rotateToken` 이 도달하는 쓰기와 같다. */
    suspend fun rotateTokens(
        accessToken: String,
        refreshToken: String,
    ) = tokenDataSource.updateTokens(accessToken = accessToken, refreshToken = refreshToken)

    suspend fun clearSession() {
        store.edit { it.clear() }
    }

    /**
     * 세션 식별자 키가 생기기 전 버전이 저장해 둔 토큰. 키 이름을 여기에 다시 적는 것이 이 helper 의 일이다.
     * 기존 설치본의 디스크 내용을 흉내 내는 것이라 [TokenDataSource] 의 키 상수를 빌려 쓰면 "기존 저장분을
     * 계속 읽는가" 를 못 본다.
     */
    suspend fun writeLegacyTokens(
        accessToken: String = DEFAULT_ACCESS_TOKEN,
        refreshToken: String = DEFAULT_REFRESH_TOKEN,
    ) {
        store.edit { prefs ->
            prefs[stringPreferencesKey("access_token")] = accessToken
            prefs[stringPreferencesKey("refresh_token")] = refreshToken
        }
    }

    fun close() = storeScope.cancel()

    private companion object {
        const val STORE_FILE_NAME = "Token.preferences_pb"
        const val DEFAULT_ACCESS_TOKEN = "access"
        const val DEFAULT_REFRESH_TOKEN = "refresh"
    }
}

/**
 * 실제 [DataStore] 를 감싸, 쓰기는 이미 커밋됐지만 이어지던 관측 스트림에는 아직 전달되지 않은 구간을
 * 결정적으로 만드는 테스트 장치 (#2135).
 *
 * 붙잡는 대상은 [held] 가 켜진 시점에 이미 열려 있던 수집뿐이다. 그 뒤에 새로 여는 읽기는 통과해 현재
 * 저장값을 본다. 실제 DataStore 도 커밋된 쓰기를 새 읽기에는 곧바로 주고, 이어지던 수집자에게는 그
 * 코루틴이 재개될 때 전달한다.
 *
 * 붙잡는 동안에는 위임 저장소의 수집 자체를 끊고, 풀릴 때 현재 저장값부터 다시 수집한다. 그래서 붙잡힌
 * 구간의 중간 상태(로그아웃)는 수집자에게 도달할 수 없고, 풀린 뒤 첫 값은 반드시 그때의 저장값이다.
 * 수집자가 중간 상태를 못 보고 다음 세션만 보는 구간이 이 결함의 전제이므로 타이밍 운에 맡기지 않는다.
 */
private class ObservationGatedDataStore(
    private val delegate: DataStore<Preferences>,
    private val held: StateFlow<Boolean>,
) : DataStore<Preferences> {
    @OptIn(ExperimentalCoroutinesApi::class)
    override val data: Flow<Preferences> =
        flow {
            if (held.value) {
                emitAll(delegate.data)
            } else {
                emitAll(held.flatMapLatest { holding -> if (holding) emptyFlow() else delegate.data })
            }
        }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = delegate.updateData(transform)
}
