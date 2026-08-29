package com.afternote.core.model.user

data class User(
    val name: String,
    val email: String,
    val phone: String?,
    val profileImageUrl: String?,
)

data class Receiver(
    val receiverId: Long,
    val name: String,
    val relation: String,
    val authCode: String,
)

data class ReceiverDetail(
    val receiverId: Long,
    val name: String,
    val relation: String,
    val phone: String?,
    val email: String?,
    val dailyQuestionCount: Int,
    val timeLetterCount: Int,
    val afterNoteCount: Int,
    val message: String?,
    val authCode: String,
)

data class ReceiverCreated(
    val receiverId: Long,
    val authCode: String,
)

data class UserPushSetting(
    val timeLetter: Boolean,
    val mindRecord: Boolean,
    val afterNote: Boolean,
)

data class UserMarketingConsent(
    val sms: Boolean,
    val email: Boolean,
    val push: Boolean,
)

data class UserConnectedAccount(
    val local: Boolean,
    val google: Boolean,
    val naver: Boolean,
    val kakao: Boolean,
    val apple: Boolean,
    val localEmail: String?,
    val googleEmail: String?,
    val naverEmail: String?,
    val kakaoEmail: String?,
    val appleEmail: String?,
)
