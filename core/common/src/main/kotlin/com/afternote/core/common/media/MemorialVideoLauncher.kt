package com.afternote.core.common.media

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri

/**
 * 서버가 준 추모 영상 URL을 외부 재생 앱으로 연다.
 *
 * 이 함수는 **보안 가드**다. 저장된 값은 발신자가 넣은 비관리 URL 이고 서버가 원문 그대로
 * 돌려주므로, `http`/`https` 가 아닌 스킴은 실행하지 않는다 (#1394 — 발신자발 위험 스킴 차단).
 * 허용 스킴·host 검증 규칙이 여러 곳에 흩어져 있으면 한쪽만 고칠 때 다른 쪽이 뚫리므로,
 * 발신자·수신자 화면이 이 한 벌을 공유한다 (#1436).
 *
 * Android 11+ 패키지 가시성에서는 외부 앱 사전 조회가 실제 처리 가능한 앱이 있어도 실패할 수
 * 있다. 따라서 사전 조회 대신 실행을 직접 시도하고, OS가 명시적으로 거부한 경우에만 폴백한다.
 *
 * 콜백을 둘로 나눈 이유는 **원인이 다르면 안내도 달라야 하기 때문**이다. 하나로 합치면 URL 이
 * 막힌 경우에도 «재생할 앱이 없습니다» 가 나가는데, 그건 앱 유무와 무관한 거짓이다.
 *
 * @param onRejected URL 이 검증을 통과하지 못해 실행 자체를 시도하지 않은 경우
 * @param onUnavailable 실행을 시도했으나 OS 가 거부한 경우
 */
fun launchMemorialVideo(
    videoUrl: String,
    startActivity: (Intent) -> Unit,
    onRejected: () -> Unit,
    onUnavailable: () -> Unit,
) {
    val uri =
        try {
            videoUrl
                .takeUnless { it.isBlank() || it.any(Char::isWhitespace) }
                ?.toUri()
                ?.takeIf {
                    val scheme = it.scheme
                    (
                        scheme.equals("http", ignoreCase = true) ||
                            scheme.equals("https", ignoreCase = true)
                    ) &&
                        !it.host.isNullOrBlank()
                }
        } catch (_: IllegalArgumentException) {
            null
        }

    if (uri == null) {
        onRejected()
        return
    }

    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        onUnavailable()
    } catch (_: SecurityException) {
        onUnavailable()
    } catch (_: IllegalArgumentException) {
        onUnavailable()
    }
}
