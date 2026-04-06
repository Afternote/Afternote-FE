package com.afternote.core.data.mapper.user

import com.afternote.core.model.DailyQuestionAnswerItem
import com.afternote.core.model.ReceiverDailyQuestionsResult
import com.afternote.core.model.ReceiverMindRecordItem
import com.afternote.core.model.ReceiverMindRecordsResult
import com.afternote.core.model.setting.DeliveryCondition
import com.afternote.core.model.setting.DeliveryConditionType
import com.afternote.core.model.setting.PushSettings
import com.afternote.core.model.setting.ReceiverDetail
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.model.user.UserProfileModel
import com.afternote.core.network.dto.DailyQuestionAnswerItemDto
import com.afternote.core.network.dto.DeliveryConditionRequestDto
import com.afternote.core.network.dto.DeliveryConditionResponseDto
import com.afternote.core.network.dto.DeliveryConditionTypeDto
import com.afternote.core.network.dto.ReceiverDetailResponseDto
import com.afternote.core.network.dto.ReceiverItemDto
import com.afternote.core.network.dto.ReceiverMindRecordItemDto
import com.afternote.core.network.dto.UserPushSettingResponse
import com.afternote.core.network.dto.UserResponse

// TODO: 리팩토링

/**
 * User DTO를 Domain 모델로 변환. (스웨거 기준)
 */
object UserMapper {
    fun toUserProfile(dto: UserResponse): UserProfileModel =
        UserProfileModel(
            name = dto.name,
            email = dto.email,
            phone = dto.phone,
            profileImageUrl = dto.profileImageUrl,
        )

    fun toPushSettings(dto: UserPushSettingResponse): PushSettings =
        PushSettings(
            timeLetter = dto.timeLetter,
            mindRecord = dto.mindRecord,
            afterNote = dto.afterNote,
        )

    fun toReceiverListItem(dto: ReceiverItemDto): ReceiverListItem =
        ReceiverListItem(
            receiverId = dto.receiverId,
            name = dto.name,
            relation = dto.relation,
            mindRecordDeliveryEnabled = dto.mindRecordDeliveryEnabled,
        )

    fun toReceiverDetail(dto: ReceiverDetailResponseDto): ReceiverDetail =
        ReceiverDetail(
            receiverId = dto.receiverId,
            name = dto.name,
            relation = dto.relation,
            phone = dto.phone,
            email = dto.email,
            dailyQuestionCount = dto.dailyQuestionCount,
            timeLetterCount = dto.timeLetterCount,
            afterNoteCount = dto.afterNoteCount,
        )

    fun toDailyQuestionAnswerItem(dto: DailyQuestionAnswerItemDto): DailyQuestionAnswerItem =
        DailyQuestionAnswerItem(
            dailyQuestionAnswerId = dto.dailyQuestionAnswerId,
            question = dto.question,
            answer = dto.answer,
            recordDate = dto.recordDate,
        )

    fun toReceiverDailyQuestionsResult(
        items: List<DailyQuestionAnswerItem>,
        hasNext: Boolean,
    ): ReceiverDailyQuestionsResult = ReceiverDailyQuestionsResult(items = items, hasNext = hasNext)

    fun toReceiverMindRecordItem(dto: ReceiverMindRecordItemDto): ReceiverMindRecordItem =
        ReceiverMindRecordItem(
            recordId = dto.recordId,
            type = dto.type,
            titleOrQuestion = dto.titleOrQuestion.orEmpty().ifBlank { "-" },
            contentOrAnswer = dto.contentOrAnswer.orEmpty().ifBlank { "-" },
            recordDate = dto.recordDate,
        )

    fun toReceiverMindRecordsResult(
        items: List<ReceiverMindRecordItem>,
        hasNext: Boolean,
    ): ReceiverMindRecordsResult = ReceiverMindRecordsResult(items = items, hasNext = hasNext)

    /** daily-questions 결과를 mind-records 형식으로 변환 (fallback용). */
    fun dailyQuestionToMindRecord(item: DailyQuestionAnswerItem): ReceiverMindRecordItem =
        ReceiverMindRecordItem(
            recordId = item.dailyQuestionAnswerId,
            type = "DAILY_QUESTION",
            titleOrQuestion = item.question,
            contentOrAnswer = item.answer,
            recordDate = item.recordDate,
        )

    // TODO:코어가 마인드레코드에 의존하는 문제
//    /** GET /mind-records 응답을 ReceiverMindRecordItem으로 변환 (fallback용). */
//    fun mindRecordSummaryToReceiverMindRecordItem(summary: MindRecordSummary): ReceiverMindRecordItem =
//        ReceiverMindRecordItem(
//            recordId = summary.recordId,
//            type = summary.type,
//            titleOrQuestion = summary.title.orEmpty().ifBlank { "-" },
//            contentOrAnswer = summary.content.orEmpty().ifBlank { "-" },
//            recordDate = summary.date
//        )

    fun toDeliveryCondition(dto: DeliveryConditionResponseDto): DeliveryCondition =
        DeliveryCondition(
            conditionType = dto.conditionType.toDomain(),
            inactivityPeriodDays = dto.inactivityPeriodDays,
            specificDate = dto.specificDate,
            leaveMessage = dto.leaveMessage,
            conditionFulfilled = dto.conditionFulfilled,
            conditionMet = dto.conditionMet,
        )

    fun toDeliveryConditionRequestDto(
        conditionType: DeliveryConditionType,
        inactivityPeriodDays: Int?,
        specificDate: String?,
        leaveMessage: String? = null,
    ): DeliveryConditionRequestDto =
        DeliveryConditionRequestDto(
            conditionType = conditionType.toDto(),
            inactivityPeriodDays = inactivityPeriodDays,
            specificDate = specificDate,
            leaveMessage = leaveMessage,
        )

    private fun DeliveryConditionTypeDto.toDomain(): DeliveryConditionType =
        when (this) {
            DeliveryConditionTypeDto.NONE -> DeliveryConditionType.NONE
            DeliveryConditionTypeDto.DEATH_CERTIFICATE -> DeliveryConditionType.DEATH_CERTIFICATE
            DeliveryConditionTypeDto.INACTIVITY -> DeliveryConditionType.INACTIVITY
            DeliveryConditionTypeDto.SPECIFIC_DATE -> DeliveryConditionType.SPECIFIC_DATE
        }

    private fun DeliveryConditionType.toDto(): DeliveryConditionTypeDto =
        when (this) {
            DeliveryConditionType.NONE -> DeliveryConditionTypeDto.NONE
            DeliveryConditionType.DEATH_CERTIFICATE -> DeliveryConditionTypeDto.DEATH_CERTIFICATE
            DeliveryConditionType.INACTIVITY -> DeliveryConditionTypeDto.INACTIVITY
            DeliveryConditionType.SPECIFIC_DATE -> DeliveryConditionTypeDto.SPECIFIC_DATE
        }
}
