package com.afternote.core.data.repoimpl

import com.afternote.core.domain.repository.MyProfileRepository
import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.domain.repository.UserRepository
import javax.inject.Inject

/** 기존 합본 소비자도 좁은 계약과 같은 프로필·수신자 인스턴스를 사용한다. */
internal class UserRepositoryImpl
    @Inject
    constructor(
        receiverRepository: UserReceiverRepository,
        myProfileRepository: MyProfileRepository,
    ) : UserRepository,
        UserReceiverRepository by receiverRepository,
        MyProfileRepository by myProfileRepository
