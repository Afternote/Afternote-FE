package com.afternote.core.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * [LocalStoreRegistryImpl] 의 수명(scope) 계약 가드 (#912).
 *
 * 핵심은 재기동 시나리오다 — Hilt provider 는 최초 주입 때에야 실행되므로, 이전 프로세스에서
 * 기록된 저장소를 이번 프로세스에서 한 번도 안 연 채 로그아웃하면 in-memory 등록만으로는
 * 잔존이 재현된다. 매니페스트 영속이 그 구멍을 막는 것을 레지스트리 재생성(= 프로세스 재시작
 * 시뮬레이션)으로 검증한다. 실물 파일 DataStore 를 쓰며, 프로세스 종료는 registryScope 취소로
 * 흉내 낸다 (DataStore 의 "파일당 활성 인스턴스 1개" 등록이 scope 종료로 풀린다).
 *
 * 매니페스트 기록은 비동기라 종료 전에 디스크까지 내려보내야 한다. 그 flush 는 공개 operation
 * [LocalStoreRegistry.clearScope] 로 한다 — clearScope 는 계약상 «대기 중인 등록을 먼저 반영한
 * 뒤» 지우므로, 아직 값을 쓰기 전에 한 번 부르면 등록만 확정되고 지울 것은 없다 (#1672).
 */
class LocalStoreRegistryImplTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val key = stringPreferencesKey("value")

    private fun newRegistry(scope: CoroutineScope) =
        LocalStoreRegistryImpl(
            produceFile = { name -> File(tmp.root, "$name.preferences_pb") },
            registryScope = scope,
        )

    private fun newScope() = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 프로세스 종료 시뮬레이션 — 이 레지스트리의 모든 DataStore 활성 등록을 푼다. */
    private fun shutdown(scope: CoroutineScope) =
        runBlocking {
            val job = scope.coroutineContext.job
            job.cancel()
            job.join()
        }

    @Test
    fun `store - 같은 name 재요청은 같은 인스턴스 (파일당 1개 강제)`() {
        val scope = newScope()
        try {
            val registry = newRegistry(scope)

            val first = registry.store("alpha", StoreScope.SESSION)
            val second = registry.store("alpha", StoreScope.SESSION)

            assertSame(first, second)
        } finally {
            shutdown(scope)
        }
    }

    @Test
    fun `store - 같은 name 을 다른 scope 로 재요청하면 즉시 실패`() {
        val scope = newScope()
        try {
            val registry = newRegistry(scope)
            registry.store("alpha", StoreScope.SESSION)

            val thrown = runCatching { registry.store("alpha", StoreScope.DEVICE) }.exceptionOrNull()

            assertTrue(thrown is IllegalStateException)
        } finally {
            shutdown(scope)
        }
    }

    @Test
    fun `store - 매니페스트 예약 이름은 거부`() {
        val scope = newScope()
        try {
            val registry = newRegistry(scope)

            // 레지스트리 자신의 매니페스트 파일 이름. 값은 디스크 계약이라 리터럴로 고정한다 —
            // 상수를 공유하려고 프로덕션 visibility 를 넓히지 않는다 (#1672).
            val thrown =
                runCatching {
                    registry.store("local_store_registry", StoreScope.DEVICE)
                }.exceptionOrNull()

            assertTrue(thrown is IllegalArgumentException)
        } finally {
            shutdown(scope)
        }
    }

    @Test
    fun `clearScope(SESSION) - SESSION 저장소만 비우고 DEVICE 는 유지`() {
        val scope = newScope()
        try {
            val registry = newRegistry(scope)
            runBlocking {
                registry.store("session_store", StoreScope.SESSION).edit { it[key] = "세션 값" }
                registry.store("device_store", StoreScope.DEVICE).edit { it[key] = "기기 값" }

                registry.clearScope(StoreScope.SESSION)

                assertNull(registry.store("session_store", StoreScope.SESSION).data.first()[key])
                assertEquals("기기 값", registry.store("device_store", StoreScope.DEVICE).data.first()[key])
            }
        } finally {
            shutdown(scope)
        }
    }

    @Test
    fun `clearScope - 이번 프로세스에서 획득된 적 없는 저장소도 매니페스트 기록으로 지운다 (재기동 시뮬레이션)`() {
        // 프로세스 1 — 저장소를 획득해 값을 쓰고 종료한다.
        val scope1 = newScope()
        val registry1 = newRegistry(scope1)
        runBlocking {
            registry1.store("session_store", StoreScope.SESSION)
            registry1.store("device_store", StoreScope.DEVICE)
            // 아직 값이 없을 때 부르는 clearScope — 등록만 디스크로 확정시키는 flush 지점이다.
            registry1.clearScope(StoreScope.SESSION)

            registry1.store("session_store", StoreScope.SESSION).edit { it[key] = "잔존 후보" }
            registry1.store("device_store", StoreScope.DEVICE).edit { it[key] = "기기 값" }
        }
        shutdown(scope1)

        // 프로세스 2 — 어떤 store() 호출도 없이 곧장 로그아웃 (Hilt lazy 주입으로 미획득인 상황).
        val scope2 = newScope()
        try {
            val registry2 = newRegistry(scope2)
            runBlocking {
                registry2.clearScope(StoreScope.SESSION)

                assertNull(registry2.store("session_store", StoreScope.SESSION).data.first()[key])
                assertEquals("기기 값", registry2.store("device_store", StoreScope.DEVICE).data.first()[key])
            }
        } finally {
            shutdown(scope2)
        }
    }

    @Test
    fun `clearScope 가 연 저장소도 이후 store() 와 같은 인스턴스를 공유한다`() {
        // 재기동 후 clearScope 가 매니페스트 기록만으로 먼저 연 저장소를, 뒤늦게 feature 가
        // 획득해도 두 번째 인스턴스가 만들어지면 안 된다 (DataStore 파일당 1개 강제 조건).
        val scope1 = newScope()
        val registry1 = newRegistry(scope1)
        runBlocking {
            registry1.store("session_store", StoreScope.SESSION)
            registry1.clearScope(StoreScope.SESSION)

            registry1.store("session_store", StoreScope.SESSION).edit { it[key] = "값" }
        }
        shutdown(scope1)

        val scope2 = newScope()
        try {
            val registry2 = newRegistry(scope2)
            runBlocking {
                registry2.clearScope(StoreScope.SESSION)

                val store = registry2.store("session_store", StoreScope.SESSION)
                store.edit { it[key] = "새 세션 값" }
                assertEquals("새 세션 값", store.data.first()[key])
            }
        } finally {
            shutdown(scope2)
        }
    }
}
