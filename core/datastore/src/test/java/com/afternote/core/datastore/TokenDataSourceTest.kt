package com.afternote.core.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * [TokenDataSource.sessionId] 의 세션 경계 계약 (#2135).
 *
 * 로그인 여부 Boolean 만으로는 "로그아웃 뒤 다른 계정 로그인" 이 관측자에게 `true → true` 로 뭉개진다.
 * 세션에 귀속된 캐시(현재 소비처는 `UserReceiverRepositoryImpl` 의 수신자 목록)는 이 식별자로 수명을
 * 가르므로, 여기서 고정하는 것은 세 가지다. 로그인마다 값이 달라질 것, 같은 세션의 토큰 회전에는 값이
 * 유지될 것, 식별자 키가 없던 시절의 저장분도 로그인 상태로 읽힐 것.
 *
 * 저장소는 실물 파일 DataStore 를 프로덕션과 같은 경로([LocalStoreRegistry])로 얻고, 로그아웃도
 * 프로덕션과 같은 [LocalStoreRegistry.clearScope] 로 한다.
 */
class TokenDataSourceTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val registryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val registry: LocalStoreRegistry =
        LocalStoreRegistryImpl(
            produceFile = { name -> File(tmp.root, "$name.preferences_pb") },
            registryScope = registryScope,
        )

    private val store = registry.store(name = "Token", scope = StoreScope.SESSION)

    private val tokenDataSource = TokenDataSource(store)

    @After
    fun tearDown() = registryScope.cancel()

    @Test
    fun `로그인은 세션 식별자를 함께 발급한다`() =
        runBlocking {
            tokenDataSource.saveTokens(accessToken = "access", refreshToken = "refresh")

            assertTrue(tokenDataSource.isLoggedIn.first())
            assertNotNullSessionId(tokenDataSource.sessionId.first())
        }

    /**
     * 같은 사용자가 같은 토큰 값을 다시 받아도 다른 세션이다. 식별자를 토큰에서 끌어오거나 저장소에
     * 남은 값을 증가시키는 방식은 로그아웃이 저장소를 비우는 순간 같은 값으로 되돌아온다.
     */
    @Test
    fun `로그아웃 뒤 같은 토큰 값으로 다시 로그인해도 세션 식별자는 달라진다`() =
        runBlocking {
            tokenDataSource.saveTokens(accessToken = "access", refreshToken = "refresh")
            val first = tokenDataSource.sessionId.first()

            registry.clearScope(StoreScope.SESSION)
            tokenDataSource.saveTokens(accessToken = "access", refreshToken = "refresh")
            val second = tokenDataSource.sessionId.first()

            assertNotNullSessionId(first)
            assertNotNullSessionId(second)
            assertNotEquals(first, second)
        }

    @Test
    fun `토큰 회전은 같은 세션 식별자를 유지한다`() =
        runBlocking {
            tokenDataSource.saveTokens(accessToken = "access", refreshToken = "refresh")
            val beforeRotation = tokenDataSource.sessionId.first()

            tokenDataSource.updateTokens(accessToken = "회전된 액세스", refreshToken = "회전된 리프레시")

            assertEquals(beforeRotation, tokenDataSource.sessionId.first())
            assertEquals("회전된 액세스", tokenDataSource.getAccessToken())
            assertEquals("회전된 리프레시", tokenDataSource.getRefreshToken())
        }

    @Test
    fun `세션 정리 뒤에는 세션 식별자가 없다`() =
        runBlocking {
            tokenDataSource.saveTokens(accessToken = "access", refreshToken = "refresh")

            registry.clearScope(StoreScope.SESSION)

            assertNull(tokenDataSource.sessionId.first())
            assertFalse(tokenDataSource.isLoggedIn.first())
        }

    /**
     * 식별자 키가 생기기 전 버전이 저장해 둔 토큰이다. 업데이트만으로 로그인이 풀리면 안 되고, 그 세션이
     * 끝날 때까지 식별자가 흔들려도 안 된다(흔들리면 세션에 귀속된 캐시가 이유 없이 버려진다).
     */
    @Test
    fun `세션 식별자 없이 저장된 기존 토큰도 로그인 상태이고 식별자가 고정된다`() =
        runBlocking {
            store.edit { prefs ->
                prefs[stringPreferencesKey("access_token")] = "기존 설치본 액세스"
                prefs[stringPreferencesKey("refresh_token")] = "기존 설치본 리프레시"
            }

            val legacySessionId = tokenDataSource.sessionId.first()
            assertTrue(tokenDataSource.isLoggedIn.first())
            assertNotNullSessionId(legacySessionId)

            tokenDataSource.updateTokens(accessToken = "회전된 액세스", refreshToken = "회전된 리프레시")

            assertEquals(legacySessionId, tokenDataSource.sessionId.first())
        }

    /** 재발급 응답은 그것을 시작한 세션에만 쓴다 (#2237). 로그아웃 뒤에는 식별자 없는 세션으로도 되살아나지 않는다. */
    @Test
    fun `세션 대조 회전은 다른 세션이나 로그아웃 상태에 쓰지 않는다`() =
        runBlocking {
            tokenDataSource.saveTokens(accessToken = "A-access", refreshToken = "A-refresh")
            val sessionA = checkNotNull(tokenDataSource.currentSessionId())

            assertTrue(tokenDataSource.updateTokensIfSession(sessionA, accessToken = "A-rotated", refreshToken = "A-refresh-rotated"))
            assertEquals("A-rotated", tokenDataSource.getAccessToken())
            assertEquals(sessionA, tokenDataSource.currentSessionId())

            registry.clearScope(StoreScope.SESSION)
            assertFalse(tokenDataSource.updateTokensIfSession(sessionA, accessToken = "A-late", refreshToken = "A-refresh-late"))
            assertNull(tokenDataSource.getAccessToken())
            assertNull(tokenDataSource.getRefreshToken())
            assertFalse(tokenDataSource.isLoggedIn.first())

            tokenDataSource.saveTokens(accessToken = "B-access", refreshToken = "B-refresh")
            assertFalse(tokenDataSource.updateTokensIfSession(sessionA, accessToken = "A-late", refreshToken = "A-refresh-late"))
            assertEquals("B-access", tokenDataSource.getAccessToken())
            assertNull(tokenDataSource.refreshTokenOf(sessionA))
            assertEquals("B-refresh", tokenDataSource.refreshTokenOf(checkNotNull(tokenDataSource.currentSessionId())))
        }

    /** 거절 정리는 그 거절을 받은 세션일 때만 한다 (#2238). */
    @Test
    fun `세션 대조 정리는 지금 세션일 때만 비운다`() =
        runBlocking {
            tokenDataSource.saveTokens(accessToken = "A-access", refreshToken = "A-refresh")
            val sessionA = checkNotNull(tokenDataSource.currentSessionId())
            registry.clearScope(StoreScope.SESSION)
            tokenDataSource.saveTokens(accessToken = "B-access", refreshToken = "B-refresh")
            val sessionB = checkNotNull(tokenDataSource.currentSessionId())

            assertFalse(tokenDataSource.clearIfSession(sessionA))
            assertEquals("B-access", tokenDataSource.getAccessToken())
            assertEquals(sessionB, tokenDataSource.currentSessionId())

            assertTrue(tokenDataSource.clearIfSession(sessionB))
            assertNull(tokenDataSource.getAccessToken())
            assertNull(tokenDataSource.currentSessionId())
        }

    private fun assertNotNullSessionId(sessionId: String?) {
        assertTrue("로그인된 세션에는 식별자가 있어야 한다 (실제=$sessionId)", !sessionId.isNullOrBlank())
    }
}
