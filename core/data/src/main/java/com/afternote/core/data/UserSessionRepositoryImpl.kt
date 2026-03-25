package com.afternote.core.data

import com.afternote.core.domain.usecase.UserSessionRepository
import javax.inject.Inject

class UserSessionRepositoryImpl
@Inject
constructor(
    val tokenManager
) : UserSessionRepository {

}