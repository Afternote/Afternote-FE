package com.afternote.core.data.repoimpl.invitation

import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.ReceiverInvitationRepository
import com.afternote.core.model.user.ReceiverInvitationAccepted
import com.afternote.core.model.user.ReceiverInvitationCreated
import com.afternote.core.model.user.ReceiverInvitationLookup
import com.afternote.core.network.model.requireData
import com.afternote.core.network.service.ReceiverInvitationApiService
import javax.inject.Inject

/**
 * [ReceiverInvitationRepository] 구현 (#944).
 *
 * 모든 메서드가 `runCatchingCancellable { ... }.mapReceiverInvitationFailure()` 한 형태다 — 번역을
 * 빠뜨린 endpoint 가 인프라 예외를 그대로 흘리지 않게 한다(`ReceiverAuthRepositoryImpl` 과 같은 규약).
 */
internal class ReceiverInvitationRepositoryImpl
    @Inject
    constructor(
        private val api: ReceiverInvitationApiService,
    ) : ReceiverInvitationRepository {
        override suspend fun create(): Result<ReceiverInvitationCreated> =
            runCatchingCancellable {
                ReceiverInvitationCreated(token = api.createInvitation().requireData().invitationToken)
            }.mapReceiverInvitationFailure()

        override suspend fun lookup(token: String): Result<ReceiverInvitationLookup> =
            runCatchingCancellable {
                val dto = api.getInvitation(token).requireData()
                ReceiverInvitationLookup(inviterName = dto.inviterName, isExpired = dto.expired)
            }.mapReceiverInvitationFailure()

        override suspend fun accept(token: String): Result<ReceiverInvitationAccepted> =
            runCatchingCancellable {
                ReceiverInvitationAccepted(inviterName = api.acceptInvitation(token).requireData().inviterName)
            }.mapReceiverInvitationFailure()
    }
