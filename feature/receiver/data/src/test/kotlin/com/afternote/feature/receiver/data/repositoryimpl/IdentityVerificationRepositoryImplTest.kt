package com.afternote.feature.receiver.data.repositoryimpl

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 본인 확인 캐시의 발신자별 격리 (#597).
 *
 * 이전 구현은 전역 `identity_verified` boolean 하나라 발신자 A 인증만으로
 * 발신자 B 의 이메일 관문까지 열렸다 — 발신자 단위 격리가 실제 구현에서 지켜지는지 본다.
 *
 * 구 릴리스가 디스크에 남긴 전역 boolean · 옛 UUID 키 잔존값 가드는 별도 테스트가 없다 —
 * 본 구현은 디스크를 아예 읽지 않아 (프로세스 수명 in-memory, #597 리뷰 반영) 잔존값이
 * 관문에 닿을 경로 자체가 없다. 아래 «비어 시작한다» 가 그 전제(초기 상태에 어떤 발신자도
 * 열려 있지 않음)를 고정한다.
 */
class IdentityVerificationRepositoryImplTest {
    @Test
    fun `A 발신자 인증이 B 발신자 관문을 열지 않는다`() {
        val repository = IdentityVerificationRepositoryImpl()

        runBlocking { repository.markVerified("sender-a") }

        assertTrue(runBlocking { repository.isVerified("sender-a").first() })
        assertFalse(runBlocking { repository.isVerified("sender-b").first() })
    }

    @Test
    fun `발신자별 인증은 각자 독립적으로 누적된다`() {
        val repository = IdentityVerificationRepositoryImpl()

        runBlocking {
            repository.markVerified("sender-a")
            repository.markVerified("sender-b")
        }

        assertTrue(runBlocking { repository.isVerified("sender-a").first() })
        assertTrue(runBlocking { repository.isVerified("sender-b").first() })
        assertFalse(runBlocking { repository.isVerified("sender-c").first() })
    }

    @Test
    fun `새 저장소는 어떤 발신자의 관문도 미리 열어 두지 않는다`() {
        val repository = IdentityVerificationRepositoryImpl()

        assertFalse(runBlocking { repository.isVerified("sender-a").first() })
    }

    @Test
    fun `markVerified 이전에 얻어 둔 Flow 도 갱신된 값을 준다`() {
        // isVerified 반환 Flow 는 호출 시점 스냅숏이 아니다 — Intro 게이트가 stateIn 으로
        // 계속 물고 있는 실사용 형태와 같은 성질(시계열)을 값 수준에서 고정한다.
        val repository = IdentityVerificationRepositoryImpl()

        runBlocking {
            val flow = repository.isVerified("sender-a")
            assertFalse(flow.first())

            repository.markVerified("sender-a")

            assertTrue(flow.first())
        }
    }
}
