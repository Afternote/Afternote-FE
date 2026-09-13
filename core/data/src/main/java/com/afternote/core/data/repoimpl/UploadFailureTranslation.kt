package com.afternote.core.data.repoimpl

import com.afternote.core.domain.error.FileUploadFailure
import com.afternote.core.network.model.ApiException

private const val CODE_FILE_SIZE_EXCEEDED = 1803

/**
 * 파일 업로드 API 실패를 도메인 예외로 옮긴다 — presentation 이 `core:network` 를 모른 채 타입만으로
 * 분기하게 하는 것이 목적이다(#1868).
 *
 * 가르는 신호는 서버 봉투의 `code` 뿐이고 `message` 는 옮기지 않는다(BE#92). 표시 문구는 각 화면이
 * 자기 리소스로 갖는다. **사유를 확인하지 못한 실패는 치환하지 않고 원본 그대로 흘려보낸다** —
 * 전송 실패나 S3 PUT 실패는 재시도가 실제로 통하는 실패라 기존 안내 경로에 그대로 남아야 한다.
 *
 * 취소는 다시 보지 않는다 — 호출부가 전부 `runCatchingCancellable`(#661)이라
 * `CancellationException` 이 [Result] 로 도달하지 않는다.
 *
 * 사진·영상 두 구현이 같은 실패를 같은 규칙으로 옮기므로 어느 한쪽 impl 안에 두지 않는다.
 */
internal fun <T> Result<T>.mapUploadFailure(): Result<T> =
    when (val exception = exceptionOrNull()) {
        is ApiException -> {
            when (exception.code) {
                CODE_FILE_SIZE_EXCEEDED -> {
                    Result.failure(FileUploadFailure.FileSizeExceeded(exception))
                }

                else -> {
                    this
                }
            }
        }

        else -> {
            this
        }
    }
