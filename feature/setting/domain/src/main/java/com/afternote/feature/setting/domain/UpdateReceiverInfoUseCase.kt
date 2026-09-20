package com.afternote.feature.setting.domain

import com.afternote.core.domain.repository.UserReceiverRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

/**
 * 수신자 저장 결과 (#1691).
 *
 * 기본정보와 인사말은 서로 다른 쓰기라 실패한 요청 단계에 따라 사용자에게 알릴 내용을 구분한다.
 * 인사말 요청 단계에서 실패하면 기본정보 요청은 이미 성공한 상태다.
 */
public sealed interface UpdateReceiverInfoResult {
    public data object Success : UpdateReceiverInfoResult

    /**
     * 기본정보 쓰기가 실패해 인사말 쓰기를 시도하지 않았다.
     *
     * [cause] 는 저장소가 던진 원인 그대로다. 소비자가 거절 예외를 가려 문구를 고르므로 감싸지 않는다.
     * `Exception` 이 아니라 `Throwable` 인 것은 이 정책을 옮겨 오기 전 ViewModel 이 쓰던
     * `runCatchingCancellable` 의 포착 범위(취소를 뺀 모든 `Throwable`)를 그대로 보존한 결과다.
     */
    public data class BasicInfoFailed(
        public val cause: Throwable,
    ) : UpdateReceiverInfoResult

    public data object MessageFailedAfterBasicInfoUpdated : UpdateReceiverInfoResult
}

/**
 * 수신자 기본정보를 저장한 뒤 인사말을 저장한다 (#1691).
 *
 * 순서를 고정한다. 기본정보가 실패하면 인사말은 시도하지 않는다. 두 쓰기 사이에서 취소되면
 * 인사말 쓰기를 시작하지 않고 취소를 그대로 전파한다.
 */
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
            } catch (failure: Throwable) {
                return UpdateReceiverInfoResult.BasicInfoFailed(failure)
            }
            currentCoroutineContext().ensureActive()
            return try {
                repository.updateReceiverMessage(receiverId, message)
                UpdateReceiverInfoResult.Success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                UpdateReceiverInfoResult.MessageFailedAfterBasicInfoUpdated
            }
        }
    }
