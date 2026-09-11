package com.afternote.feature.afternote.domain.usecase.editor

import com.afternote.feature.afternote.domain.error.AfternoteFailure
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MediaKind
import com.afternote.feature.afternote.domain.repository.author.MemorialMediaUploadRepository
import javax.inject.Inject

/**
 * 장례식에 남길 영상·영정사진·추모 음성의 *서버 저장 직전 정리* 도메인 로직.
 *
 * 호출부가 로컬/원격을 확정한 [MediaInput] 을 Repository 가 저장 페이로드에 실을 URL 로 해석해 주고,
 * 본 UseCase 는 그 실패를 [AfternoteFailure.MediaSave] 로 wrap 한다. 도메인 본문이 `"content://"` 같은 인프라
 * 형식 디테일 문자열을 직접 비교하지 않는다.
 */
class ResolveMemorialMediaForSaveUseCase
    @Inject
    constructor(
        private val memorialMediaUploadRepository: MemorialMediaUploadRepository,
    ) {
        /**
         * @param video 영상 입력 — 호출부가 로컬/원격/없음을 확정한 [MediaInput].
         * @param photo 영정 사진 입력 — 픽 우선순위까지 반영해 호출부가 확정한 [MediaInput].
         * @param audio 추모 음성 입력 (#1118) — 위와 같다.
         */
        suspend operator fun invoke(
            video: MediaInput,
            photo: MediaInput,
            audio: MediaInput,
        ): Result<ResolvedMemorialMediaForSave> {
            // resolve 중 하나라도 실패 → 도메인 예외로 wrap 후 invoke 자체를 즉시 종료
            // (getOrElse 람다 안의 `return` 은 non-local return — 함수 전체에서 빠져나감)
            val resolvedVideoUrl =
                memorialMediaUploadRepository
                    .resolve(video, MediaKind.VIDEO)
                    .getOrElse { return Result.failure(AfternoteFailure.MediaSave(MediaKind.VIDEO, it)) }

            val resolvedPhotoUrl =
                memorialMediaUploadRepository
                    .resolve(photo, MediaKind.PHOTO)
                    .getOrElse { return Result.failure(AfternoteFailure.MediaSave(MediaKind.PHOTO, it)) }

            val resolvedAudioUrl =
                memorialMediaUploadRepository
                    .resolve(audio, MediaKind.AUDIO)
                    .getOrElse { return Result.failure(AfternoteFailure.MediaSave(MediaKind.AUDIO, it)) }

            return Result.success(
                ResolvedMemorialMediaForSave(
                    resolvedVideoUrl = resolvedVideoUrl,
                    resolvedMemorialPhotoUrl = resolvedPhotoUrl,
                    resolvedMemorialAudioUrl = resolvedAudioUrl,
                ),
            )
        }
    }
