package com.afternote.feature.timeletter.data.repositoryImpl

import com.afternote.core.network.model.ApiException
import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.timeletter.data.api.TimeLetterApiService
import com.afternote.feature.timeletter.data.dto.TimeLetterCreateRequestDto
import com.afternote.feature.timeletter.data.dto.TimeLetterDeleteRequestDto
import com.afternote.feature.timeletter.data.dto.TimeLetterUpdateRequestDto
import com.afternote.feature.timeletter.data.mapper.toDomain
import com.afternote.feature.timeletter.data.mapper.toDto
import com.afternote.feature.timeletter.domain.error.TimeLetterServerRejectionException
import com.afternote.feature.timeletter.domain.model.NewTimeLetterBlock
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterDeliveryMode
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.domain.repository.TimeLetterRepository
import javax.inject.Inject

class TimeLetterRepositoryImpl
    @Inject
    constructor(
        private val timeLetterApiService: TimeLetterApiService,
    ) : TimeLetterRepository {
        override suspend fun getTimeLetters(): TimeLetterList =
            timeLetterApiService
                .getTimeLetters()
                .requireData()
                .toDomain()

        override suspend fun getTemporaryTimeLetters(): TimeLetterList =
            timeLetterApiService
                .getTemporaryTimeLetters()
                .requireData()
                .toDomain()

        override suspend fun getTimeLetter(timeLetterId: Long): TimeLetter =
            mapTimeLetterRequest {
                timeLetterApiService
                    .getTimeLetter(timeLetterId)
                    .requireData()
                    .toDomain()
            }

        override suspend fun createTimeLetter(
            title: String?,
            blocks: List<NewTimeLetterBlock>,
            sendAt: String?,
            deliveryMode: TimeLetterDeliveryMode,
            status: TimeLetterStatus,
            receiverIds: List<Long>,
        ): TimeLetter =
            mapTimeLetterRequest {
                timeLetterApiService
                    .createTimeLetter(
                        TimeLetterCreateRequestDto(
                            title = title,
                            sendAt = sendAt,
                            deliveryMode = deliveryMode.toDto(),
                            status = status.toDto(),
                            blocks = blocks.map { it.toDto() },
                            receiverIds = receiverIds,
                        ),
                    ).requireData()
                    .toDomain()
            }

        override suspend fun updateTimeLetter(
            timeLetterId: Long,
            title: String?,
            blocks: List<NewTimeLetterBlock>,
            sendAt: String?,
            deliveryMode: TimeLetterDeliveryMode?,
            status: TimeLetterStatus?,
        ): TimeLetter =
            mapTimeLetterRequest {
                timeLetterApiService
                    .updateTimeLetter(
                        timeLetterId = timeLetterId,
                        request =
                            TimeLetterUpdateRequestDto(
                                title = title,
                                sendAt = sendAt,
                                deliveryMode = deliveryMode?.toDto(),
                                status = status?.toDto(),
                                blocks = blocks.map { it.toDto() },
                            ),
                    ).requireData()
                    .toDomain()
            }

        override suspend fun deleteTimeLetters(timeLetterIds: List<Long>) {
            timeLetterApiService
                .deleteTimeLetters(TimeLetterDeleteRequestDto(timeLetterIds = timeLetterIds))
                .requireStatus()
        }

        override suspend fun deleteAllTemporary() {
            timeLetterApiService
                .deleteAllTemporary()
                .requireStatus()
        }

        private suspend inline fun <T> mapTimeLetterRequest(crossinline request: suspend () -> T): T =
            try {
                request()
            } catch (error: ApiException) {
                val serverMessage = error.serverMessage?.takeIf { it.isNotBlank() }
                if (error.status in 400..499 && serverMessage != null) {
                    throw TimeLetterServerRejectionException(error.status, serverMessage, error)
                }
                throw error
            }
    }
