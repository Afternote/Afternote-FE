package com.afternote.feature.setting.presentation.receiver.social

import android.content.Context
import android.util.Log
import com.kakao.sdk.common.util.KakaoCustomTabsClient
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.share.WebSharerClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 카카오디벨로퍼스 «애프터노트 수신자 초대링크» 사용자 정의 템플릿 (2026-09-08 생성).
 *
 * 비밀값이 아니라 코드 상수다. 사용자 인자는 `inviteToken`·`senderName` 둘이고, 버튼의 Android 실행
 * 파라미터가 `inviteToken=${inviteToken}` 으로 `kakao{앱키}://kakaolink` 를 연다.
 */
private const val KAKAO_RECEIVER_INVITATION_TEMPLATE_ID: Long = 136940L

private const val LOG_TAG = "KakaoInviteShare"
private const val TEMPLATE_ARG_INVITE_TOKEN = "inviteToken"
private const val TEMPLATE_ARG_SENDER_NAME = "senderName"

/**
 * 수신자 초대를 카카오톡으로 보낸다 (#944).
 *
 * 카카오톡이 있으면 앱 공유 Intent 를 띄우고, 없으면 웹 공유 URL 을 브라우저(CustomTab)로 연다.
 * 성공은 «공유 화면을 띄웠다» 까지다 — 사용자가 카카오톡 안에서 실제로 보냈는지는 SDK 가 알려주지 않는다.
 * 실패는 `Result.failure` 로만 돌려주고 던지지 않는다 — 공유 취소·브라우저 부재로 앱이 죽지 않게 한다.
 *
 * [KakaoLoginHelper][requestKakaoAccessToken] 과 같은 결로 콜백 SDK 를 코루틴으로 감싼다.
 * 토큰은 템플릿 인자로만 실리고 로그에 남기지 않는다.
 */
internal suspend fun shareReceiverInvitationViaKakao(
    context: Context,
    token: String,
    senderName: String,
): Result<Unit> {
    val templateArgs =
        mapOf(
            TEMPLATE_ARG_INVITE_TOKEN to token,
            TEMPLATE_ARG_SENDER_NAME to senderName,
        )
    val shareClient = ShareClient.instance
    if (shareClient.isKakaoTalkSharingAvailable(context)) {
        return suspendCancellableCoroutine { continuation ->
            shareClient.shareCustom(context, KAKAO_RECEIVER_INVITATION_TEMPLATE_ID, templateArgs) { result, error ->
                if (!continuation.isActive) return@shareCustom
                val outcome =
                    when {
                        error != null -> {
                            Log.w(LOG_TAG, "카카오톡 공유 실패", error)
                            Result.failure(error)
                        }

                        result != null -> {
                            runCatching { context.startActivity(result.intent) }
                        }

                        else -> {
                            Result.failure(IllegalStateException("카카오톡 공유 실패: 결과값 없음"))
                        }
                    }
                continuation.resume(outcome)
            }
        }
    }
    return runCatching {
        val url = WebSharerClient.instance.makeCustomUrl(KAKAO_RECEIVER_INVITATION_TEMPLATE_ID, templateArgs)
        KakaoCustomTabsClient.openWithDefault(context, url)
    }
}
