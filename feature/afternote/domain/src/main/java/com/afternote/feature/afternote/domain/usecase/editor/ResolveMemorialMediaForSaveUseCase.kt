package com.afternote.feature.afternote.domain.usecase.editor

import com.afternote.feature.afternote.domain.repository.author.MemorialPhotoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.MemorialVideoUploadRepository
import com.afternote.feature.afternote.domain.repository.author.PhotoUploadOutcome
import com.afternote.feature.afternote.domain.repository.author.VideoUploadOutcome
import javax.inject.Inject

class MemorialVideoSaveException(
    cause: Throwable,
) : Exception("영상 업로드에 실패했습니다.", cause)

class MemorialPhotoSaveException(
    cause: Throwable,
) : Exception("영정 사진 업로드에 실패했습니다.", cause)

/**
 * 추모 영상·영정 사진의 *서버 저장 직전 정리* 도메인 로직.
 *
 * Repository 가 입력 String 의 형식을 판별해 sealed [VideoUploadOutcome]/[PhotoUploadOutcome] 로
 * 결과를 주고, 본 UseCase 는 그 sealed 분기를 저장 페이로드 규칙에 매핑한다.
 * 도메인 본문이 `"content://"`, `"X-Amz-"` 같은 인프라 형식 디테일 문자열을 직접 비교하지 않는다.
 *
 * **POST/PATCH 동일 규칙** — 백엔드가 `S3Service.resolvePublicUrl(key)` 로 영구 public URL 을 발급하므로
 * 클라이언트가 GET 응답에서 받은 URL 을 그대로 PATCH 페이로드에 다시 보낼 수 있다. 따라서 PATCH 전용 분기
 * (`isUpdate`, presigned 마커 검사, `urlForUpdate`) 불필요 — #258 BE 확정 결과 반영.
 *
 * 각 Outcome 의 처리:
 * - [VideoUploadOutcome.Empty] / [PhotoUploadOutcome.Empty] → null (미첨부)
 * - [VideoUploadOutcome.Existing] / [PhotoUploadOutcome.Existing] → URL 그대로 전송 (영구 public URL)
 * - [VideoUploadOutcome.FreshlyUploaded] / [PhotoUploadOutcome.FreshlyUploaded] → 업로드 결과 URL 전송
 */
class ResolveMemorialMediaForSaveUseCase
    @Inject
    constructor(
        private val memorialVideoUploadRepository: MemorialVideoUploadRepository,
        private val memorialPhotoUploadRepository: MemorialPhotoUploadRepository,
    ) {
        /**
         * @param funeralVideoUrl 현재 ViewModel 의 영상 URL — 로컬/원격 중 하나, 또는 null.
         * @param memorialPhotoUrl 현재 영정 사진의 *영구 원격* URL (없으면 null).
         * @param pickedMemorialPhotoUri 사용자가 *방금 새로 고른* 영정 사진의 로컬 URI (안 골랐으면 null).
         */
        suspend operator fun invoke(
            funeralVideoUrl: String?,
            memorialPhotoUrl: String?,
            pickedMemorialPhotoUri: String?,
        ): Result<ResolvedMemorialMediaForSave> {
            // 두 Repository 중 하나라도 실패 → 도메인 예외로 wrap 후 invoke 자체를 즉시 종료
            // (getOrElse 람다 안의 `return` 은 non-local return — 함수 전체에서 빠져나감)
            val videoOutcome =
                memorialVideoUploadRepository
                    .resolveVideo(funeralVideoUrl)
                    .getOrElse { return Result.failure(MemorialVideoSaveException(it)) }

            val photoOutcome =
                memorialPhotoUploadRepository
                    .resolvePhoto(existingUrl = memorialPhotoUrl, pickedUri = pickedMemorialPhotoUri)
                    .getOrElse { return Result.failure(MemorialPhotoSaveException(it)) }

            return Result.success(
                ResolvedMemorialMediaForSave(
                    resolvedVideoUrl = videoOutcome.urlOrNull(),
                    resolvedMemorialPhotoUrl = photoOutcome.urlOrNull(),
                ),
            )
        }

        private fun VideoUploadOutcome.urlOrNull(): String? =
            when (this) {
                VideoUploadOutcome.Empty -> null
                is VideoUploadOutcome.Existing -> url
                is VideoUploadOutcome.FreshlyUploaded -> url
            }

        private fun PhotoUploadOutcome.urlOrNull(): String? =
            when (this) {
                PhotoUploadOutcome.Empty -> null
                is PhotoUploadOutcome.Existing -> url
                is PhotoUploadOutcome.FreshlyUploaded -> url
            }
    }
