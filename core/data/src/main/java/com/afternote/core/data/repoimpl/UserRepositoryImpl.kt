package com.afternote.core.data.repoimpl

import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.repository.UserRepository
import javax.inject.Inject

/**
 * [UserRepository] 전환기 구현 — 좁은 계약 2종에 위임만 한다 (#1282).
 *
 * 수신자는 [UserReceiverRepository], 서버 정본 프로필은 [MyProfileRepository] 의 구현이 소유하고
 * 여기서는 생성자로 주입받은 그 바인딩에 위임한다. 수신자 구현은 `@Singleton` 이라 이 합본을 거치든
 * 좁은 계약을 직접 주입받든 같은 인스턴스를 본다 — 좁은 계약으로 만든 수신자가 합본 구독자의 목록도
 * 갱신한다.
 *
 * 계정·푸시 8멤버는 `feature:setting:data` 로 내려갔다 (#1429).
 */
internal class UserRepositoryImpl
    @Inject
    constructor(
        receiverRepository: UserReceiverRepository,
        myProfileRepository: MyProfileRepository,
    ) : UserRepository,
        UserReceiverRepository by receiverRepository,
        MyProfileRepository by myProfileRepository
