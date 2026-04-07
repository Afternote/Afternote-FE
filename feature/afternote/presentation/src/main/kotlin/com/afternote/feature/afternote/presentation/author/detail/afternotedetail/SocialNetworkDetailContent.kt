package com.afternote.feature.afternote.presentation.author.detail.afternotedetail

import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver

/**
 * Display data for [SocialNetworkDetailScreen].
 *
 * Used for 소셜 네트워크 category afternote detail.
 */
data class SocialNetworkDetailContent(
    val serviceName: String = "",
    val userName: String = "",
    val accountId: String = "",
    val password: String = "",
    val accountProcessingMethod: String = "",
    val processingMethods: List<String> = emptyList(),
    val message: String = "",
    val finalWriteDate: String = "",
    val afternoteEditReceivers: List<AfternoteEditorReceiver> = emptyList(),
    val iconResId: Int = R.drawable.feature_afternote_img_insta_pattern,
    val badgeTextResId: Int = R.string.feature_afternote_detail_receiver_assigned,
)

internal val PREVIEW_CONTENT =
    SocialNetworkDetailContent(
        serviceName = "인스타그램",
        userName = "서영",
        accountId = "qwerty123",
        password = "qwerty123",
        accountProcessingMethod = "MEMORIAL",
        processingMethods = listOf("게시물 내리기", "추모 게시물 올리기", "추모 계정으로 전환하기"),
        message = "이 계정에는 우리 가족 여행 사진이 많아.\n계정 삭제하지 말고 꼭 추모 계정으로 남겨줘!",
        finalWriteDate = "2025.11.26",
        afternoteEditReceivers =
            listOf(
                AfternoteEditorReceiver(
                    id = "1",
                    name = "황규운",
                    label = "친구",
                ),
            ),
    )

internal val NAVER_MAIL_PREVIEW_CONTENT =
    SocialNetworkDetailContent(
        serviceName = "네이버 메일",
        userName = "서영",
        accountId = "qwerty123",
        password = "qwerty123",
        accountProcessingMethod = "TRANSFER",
        processingMethods = listOf("자동 응답 설정 (부재 알림)", "메일함 데이터 백업", "중요 메일 전달"),
        message = "",
        finalWriteDate = "2025.11.26",
        iconResId = R.drawable.feature_afternote_img_naver_mail_pattern,
        badgeTextResId = R.string.feature_afternote_detail_receiver_info_transfer_badge,
        afternoteEditReceivers =
            listOf(
                AfternoteEditorReceiver(
                    id = "1",
                    name = "황규운",
                    label = "친구",
                ),
            ),
    )
