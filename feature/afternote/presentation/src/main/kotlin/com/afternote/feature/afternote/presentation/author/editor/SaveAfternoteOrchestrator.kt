package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.domain.repository.AfternoteRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.author.editor.memorial.MemorialPlaylistStateHolder
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import javax.inject.Inject

/**
 * 검증 이후 미디어 업로드·생성/수정 API 호출까지 담당 (매퍼·리포지토리 조합).
 *
 * 에디터 화면 전용 타입([com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory], [MemorialPlaylistStateHolder] 등)을 다루므로
 * 도메인 모듈로 옮길 수 있는 재사용 유즈케이스가 아니라, 프레젠테이션 레이어의 저장 흐름 지휘자입니다.
 */
class SaveAfternoteOrchestrator
    @Inject
    constructor(
        private val afternoteRepository: AfternoteRepository,
        private val resolveMemorialMediaForSave: ResolveMemorialMediaForSaveUseCase,
    ) {
        suspend operator fun invoke(
            editingId: Long?,
            categoryForApi: EditorCategory,
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            playlistStateHolder: MemorialPlaylistStateHolder?,
            memorialMedia: SaveAfternoteMemorialMedia,
        ): Result<Long> {
            val resolved =
                resolveMemorialMediaForSave(
                    funeralVideoUrl = memorialMedia.funeralVideoUrl,
                    memorialPhotoUrl = memorialMedia.memorialPhotoUrl,
                    pickedMemorialPhotoUri = memorialMedia.pickedMemorialPhotoUri,
                    funeralThumbnailUrl = memorialMedia.funeralThumbnailUrl,
                    isUpdate = editingId != null,
                ).getOrElse { return Result.failure(it) }

            return if (editingId != null) {
                val updatePayload =
                    AfternoteEditorFormMapper.buildUpdatePayload(
                        category = categoryForApi,
                        payload = payload,
                        selectedReceiverIds = selectedReceiverIds,
                        playlistStateHolder = playlistStateHolder,
                        memorialMedia =
                            MemorialMediaUrls(
                                funeralVideoUrl = resolved.videoUrlForUpdate,
                                funeralThumbnailUrl = resolved.funeralThumbnailUrlForUpdate,
                                memorialPhotoUrl = resolved.resolvedMemorialPhotoUrl,
                            ),
                    )
                afternoteRepository.update(id = editingId, payload = updatePayload)
            } else {
                performCreate(
                    category = categoryForApi,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                    playlistStateHolder = playlistStateHolder,
                    funeralVideoUrl = resolved.resolvedVideoUrl,
                    funeralThumbnailUrl = memorialMedia.funeralThumbnailUrl,
                    memorialPhotoUrl = resolved.resolvedMemorialPhotoUrl,
                )
            }
        }

        private suspend fun performCreate(
            category: EditorCategory,
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            playlistStateHolder: MemorialPlaylistStateHolder?,
            funeralVideoUrl: String?,
            funeralThumbnailUrl: String?,
            memorialPhotoUrl: String?,
        ): Result<Long> {
            val createInput =
                AfternoteEditorFormMapper.buildCreateInput(
                    category = category,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                    playlistStateHolder = playlistStateHolder,
                    funeralVideoUrl = funeralVideoUrl,
                    funeralThumbnailUrl = funeralThumbnailUrl,
                    memorialPhotoUrl = memorialPhotoUrl,
                )
            return when (createInput) {
                is CreateInput.Social -> afternoteRepository.createSocial(createInput.payload)
                is CreateInput.Gallery -> afternoteRepository.createGallery(createInput.payload)
                is CreateInput.Playlist -> afternoteRepository.createPlaylist(createInput.payload)
            }
        }
    }
