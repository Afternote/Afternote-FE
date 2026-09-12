package com.afternote.feature.setting.domain

import com.afternote.core.domain.repository.UserReceiverRepository
import com.afternote.core.model.delivery.ConditionState
import com.afternote.core.model.delivery.DeliveryConditionItem
import com.afternote.core.model.delivery.DeliveryConditionType
import com.afternote.core.model.delivery.DeliveryContentType
import com.afternote.core.model.delivery.InactivityPeriod
import com.afternote.core.model.delivery.ReceiverDeliveryConditions
import javax.inject.Inject

public class UpdateTimeLetterDeliveryConditionUseCase
    @Inject
    constructor(
        private val repository: UserReceiverRepository,
    ) {
        public suspend operator fun invoke(
            receiverId: Long,
            conditions: List<DeliveryConditionItem>,
            conditionType: DeliveryConditionType,
            inactivityPeriod: InactivityPeriod,
        ): ReceiverDeliveryConditions {
            val period = inactivityPeriod.takeIf { conditionType == DeliveryConditionType.INACTIVITY }
            val updated =
                conditions
                    .map { condition ->
                        if (condition.contentType == DeliveryContentType.TIME_LETTER) {
                            condition.copy(conditionType = conditionType, inactivityPeriod = period)
                        } else {
                            condition
                        }
                    }.let { items ->
                        if (items.any { it.contentType == DeliveryContentType.TIME_LETTER }) {
                            items
                        } else {
                            items +
                                DeliveryConditionItem(
                                    contentType = DeliveryContentType.TIME_LETTER,
                                    conditionType = conditionType,
                                    inactivityPeriod = period,
                                    state = ConditionState.ACTIVE,
                                    fulfilled = false,
                                    gracePeriodStartedAt = null,
                                    fulfilledAt = null,
                                )
                        }
                    }
            return repository.updateReceiverDeliveryConditions(receiverId, updated)
        }
    }
