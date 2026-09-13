package com.afternote.feature.setting.domain

import com.afternote.core.domain.testing.FakeUserReceiverRepository
import com.afternote.core.model.delivery.ConditionState
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiverSavePolicyTest {
    @Test
    fun `basic info failure skips message and preserves cause`() =
        runTest {
            val error = IllegalStateException("save failed")
            val repository = FakeUserReceiverRepository(onUpdateReceiver = { _, _, _, _, _ -> throw error })
            val result = save(repository)
            assertSame(error, (result as UpdateReceiverInfoResult.BasicInfoFailed).cause)
            assertTrue(repository.receiverMessageCalls.isEmpty())
        }

    @Test
    fun `message failure reports already saved basic info`() =
        runTest {
            val repository = FakeUserReceiverRepository(onUpdateReceiverMessage = { _, _ -> error("message failed") })
            assertEquals(UpdateReceiverInfoResult.MessageFailedAfterBasicInfoUpdated, save(repository))
            assertEquals(
                "updated",
                repository.receiverState.value
                    .single()
                    .name,
            )
            assertEquals(1, repository.receiverMessageCalls.size)
        }

    @Test
    fun `success saves basic info before message`() =
        runTest {
            val repository = FakeUserReceiverRepository()
            repository.onUpdateReceiverMessage = { _, _ ->
                assertEquals(
                    "updated",
                    repository.receiverState.value
                        .single()
                        .name,
                )
            }
            assertEquals(UpdateReceiverInfoResult.Success, save(repository))
        }

    @Test
    fun `cancellation between writes does not save message`() =
        runTest {
            val repository = FakeUserReceiverRepository()
            repository.onUpdateReceiver = { _, _, _, _, _ ->
                currentCoroutineContext().cancel()
                repository.receiverState.value.single()
            }
            var completed = false
            val job =
                launch {
                    save(repository)
                    completed = true
                }
            job.join()
            assertTrue(job.isCancelled)
            assertTrue(repository.receiverMessageCalls.isEmpty())
            assertEquals(false, completed)
        }

    @Test
    fun `message cancellation propagates`() =
        runTest {
            val cancelled = CancellationException("cancelled")
            val repository = FakeUserReceiverRepository(onUpdateReceiverMessage = { _, _ -> throw cancelled })
            try {
                save(repository)
                error("Expected cancellation")
            } catch (actual: CancellationException) {
                assertSame(cancelled, actual)
            }
        }

    @Test
    fun `time letter patch preserves other conditions and removes request period`() =
        runTest {
            val otherType = DeliveryContentType.entries.first { it != DeliveryContentType.TIME_LETTER }
            val other = condition(otherType)
            val timeLetter = condition(DeliveryContentType.TIME_LETTER)
            val repository = FakeUserReceiverRepository()
            val result =
                UpdateTimeLetterDeliveryConditionUseCase(repository)(
                    7L,
                    listOf(other, timeLetter),
                    DeliveryConditionType.RECEIVER_REQUEST,
                    InactivityPeriod.ONE_YEAR,
                )
            assertEquals(
                listOf(other, timeLetter.copy(conditionType = DeliveryConditionType.RECEIVER_REQUEST, inactivityPeriod = null)),
                result.conditions,
            )
            assertEquals(result.conditions, repository.deliveryUpdateCalls.single().conditions)
        }

    @Test
    fun `missing time letter is synthesized with selected inactivity period`() =
        runTest {
            val repository = FakeUserReceiverRepository()
            val result =
                UpdateTimeLetterDeliveryConditionUseCase(repository)(
                    7L,
                    emptyList(),
                    DeliveryConditionType.INACTIVITY,
                    InactivityPeriod.ONE_YEAR,
                )
            assertEquals(listOf(condition(DeliveryContentType.TIME_LETTER)), result.conditions)
        }

    private suspend fun save(repository: FakeUserReceiverRepository): UpdateReceiverInfoResult =
        UpdateReceiverInfoUseCase(repository)(7L, "updated", "01012345678", "가족", "receiver@test.com", "hello")

    private fun condition(type: DeliveryContentType) =
        DeliveryConditionItem(
            contentType = type,
            conditionType = DeliveryConditionType.INACTIVITY,
            inactivityPeriod = InactivityPeriod.ONE_YEAR,
            state = ConditionState.ACTIVE,
            fulfilled = false,
            gracePeriodStartedAt = null,
            fulfilledAt = null,
        )
}
