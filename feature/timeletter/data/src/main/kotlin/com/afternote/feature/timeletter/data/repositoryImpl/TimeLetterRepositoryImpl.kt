package com.afternote.feature.timeletter.data.repositoryImpl

import com.afternote.core.network.model.requireData
import com.afternote.core.network.model.requireStatus
import com.afternote.feature.timeletter.data.api.TimeLetterApiService
import com.afternote.feature.timeletter.data.dto.TimeLetterCreateRequest
import com.afternote.feature.timeletter.data.dto.TimeLetterDeleteRequest
import com.afternote.feature.timeletter.data.dto.TimeLetterMediaRequest
import com.afternote.feature.timeletter.data.dto.TimeLetterUpdateRequest
import com.afternote.feature.timeletter.data.mapper.toDomain
import com.afternote.feature.timeletter.data.mapper.toDto
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterList
import com.afternote.feature.timeletter.domain.model.TimeLetterMediaType
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
            timeLetterApiService
                .getTimeLetter(timeLetterId)
                .requireData()
                .toDomain()

        override suspend fun createTimeLetter(
            title: String?,
            content: String?,
            sendAt: String?,
            status: TimeLetterStatus,
            mediaList: List<Pair<TimeLetterMediaType, String>>?,
            receiverIds: List<Long>?,
            deliveredAt: String?,
        ): TimeLetter =
            timeLetterApiService
                .createTimeLetter(
                    TimeLetterCreateRequest(
                        title = title,
                        content = content,
                        sendAt = sendAt,
                        status = status.toDto(),
                        mediaList =
                            mediaList?.map { (type, url) ->
                                TimeLetterMediaRequest(
                                    mediaType = type.toDto(),
                                    mediaUrl = url,
                                )
                            } ?: emptyList(),
                        receiverIds = receiverIds ?: emptyList(),
                        deliveredAt = deliveredAt,
                    ),
                ).requireData()
                .toDomain()

        override suspend fun updateTimeLetter(
            timeLetterId: Long,
            title: String?,
            content: String?,
            sendAt: String?,
            status: TimeLetterStatus?,
            mediaList: List<Pair<TimeLetterMediaType, String>>?,
        ): TimeLetter =
            timeLetterApiService
                .updateTimeLetter(
                    timeLetterId = timeLetterId,
                    request =
                        TimeLetterUpdateRequest(
                            title = title,
                            content = content,
                            sendAt = sendAt,
                            status = status?.toDto(),
                            mediaList =
                                mediaList?.map { (type, url) ->
                                    TimeLetterMediaRequest(
                                        mediaType = type.toDto(),
                                        mediaUrl = url,
                                    )
                                } ?: emptyList(),
                        ),
                ).requireData()
                .toDomain()

        override suspend fun deleteTimeLetters(timeLetterIds: List<Long>) {
            timeLetterApiService
                .deleteTimeLetters(TimeLetterDeleteRequest(timeLetterIds = timeLetterIds))
                .requireStatus()
        }

        override suspend fun deleteAllTemporary() {
            timeLetterApiService
                .deleteAllTemporary()
                .requireStatus()
        }
    }
