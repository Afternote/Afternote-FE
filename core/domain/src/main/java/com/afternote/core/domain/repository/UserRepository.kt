package com.afternote.core.domain.repository

/** 기존 소비자의 프로필·수신자 계약. 새 소비자는 필요한 좁은 계약을 직접 사용한다. */
interface UserRepository :
    UserReceiverRepository,
    MyProfileRepository
