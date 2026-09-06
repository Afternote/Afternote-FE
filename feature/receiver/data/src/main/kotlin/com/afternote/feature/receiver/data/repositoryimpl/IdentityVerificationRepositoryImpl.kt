package com.afternote.feature.receiver.data.repositoryimpl

import com.afternote.feature.receiver.domain.repository.IdentityVerificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [IdentityVerificationRepository] 의 프로세스 수명 in-memory 구현.
 *
 * 발신자별 1회 검증 정책 (#597) — 발신자 A 인증이 발신자 B 의 관문을 열지 않도록 senderId 단위로
 * 격리해 보관한다.
 *
 * ## 왜 in-memory 인가 (#597 리뷰 반영)
 *
 * 키인 `senderId` 는 presentation 의 `SenderRegistry` 가 `UUID.randomUUID()` 로 발급하는
 * **프로세스 수명 값**이다 (registry 자체가 in-memory stub, #215).
 * 앱 재시작 시 같은 발신자라도 재등록으로 새 UUID 를 받으므로,
 * - 디스크에 남긴 이전 UUID 키는 **다시는 조회되지 않고** (영속의 실효 없음),
 * - 인증 1회마다 죽은 키가 preferences 파일에 **단조 누적**되어 읽기 비용만 키운다.
 *
 * 그래서 저장 수명을 키 발급처와 같은 프로세스 수명으로 맞춘다. #912 가 도입했던 DataStore
 * (SESSION scope) 는 전역 boolean 시절의 「process death 후 유지」가 목적이었으나, 발신자별
 * UUID 키에서는 그 유지가 도달 불가능해 디스크 비용만 남는다. 프로세스를 넘는 발신자 식별자
 * (예: BE 발급 sender id, 또는 마스터 키의 비가역 파생값) 가 생기면 그때 영속을 되살린다.
 *
 * 구 릴리스가 디스크에 남긴 전역 `identity_verified` boolean · `identity_verified_<UUID>` 잔존값은
 * 본 구현이 디스크를 아예 읽지 않으므로 구조적으로 어떤 발신자의 관문도 열 수 없다.
 * (잔존 파일 `afternote_identity_verification` 은 SESSION scope 매니페스트 등록이 디스크에 남아 있어
 * 다음 로그아웃 때 [com.afternote.core.datastore.LocalStoreRegistry.clearScope] 가 마저 비운다.)
 *
 * `@Singleton` — 열람 신청 흐름의 ViewModel 들이 같은 인스턴스를 공유해야 캐시가 의미를 갖는다.
 */
@Singleton
class IdentityVerificationRepositoryImpl
    @Inject
    constructor() : IdentityVerificationRepository {
        private val verifiedSenderIds = MutableStateFlow<Set<String>>(emptySet())

        override fun isVerified(senderId: String): Flow<Boolean> =
            // 왜 Flow? 값이 시간에 따라 변하고 (verify 완료 시 false → true),
            // UI 가 그 변화를 reactive 하게 받아 화면 갱신해야 해서. 단발 조회면 suspend 로 충분했지만 시계열.
            verifiedSenderIds
                .map { senderId in it }
                // 다른 발신자의 markVerified 로 Set 이 바뀌어도 이 발신자의 boolean 이 그대로면 재방출하지 않는다.
                .distinctUntilChanged()

        override suspend fun markVerified(senderId: String) {
            verifiedSenderIds.update { it + senderId }
        }
    }
