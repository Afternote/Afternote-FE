package com.afternote.feature.setting.domain

import com.afternote.core.domain.repository.UserReceiverRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

public sealed interface UpdateReceiverInfoResult {
    public data object Success : UpdateReceiverInfoResult

    public data class BasicInfoFailed(
        public val cause: Exception,
    ) : UpdateReceiverInfoResult

    public data object MessageFailedAfterBasicInfoUpdated : UpdateReceiverInfoResult
}

public class UpdateReceiverInfoUseCase
    @Inject
    constructor(
        private val repository: UserReceiverRepository,
    ) {
        public suspend operator fun invoke(
            receiverId: Long,
            name: String,
            phone: String,
            relation: String,
            email: String,
            message: String,
        ): UpdateReceiverInfoResult {
            try {
                repository.updateReceiver(receiverId, name, phone, relation, email)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                return UpdateReceiverInfoResult.BasicInfoFailed(failure)
            }
            currentCoroutineContext().ensureActive()
            return try {
                repository.updateReceiverMessage(receiverId, message)
                UpdateReceiverInfoResult.Success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                UpdateReceiverInfoResult.MessageFailedAfterBasicInfoUpdated
            }
        }
    }
