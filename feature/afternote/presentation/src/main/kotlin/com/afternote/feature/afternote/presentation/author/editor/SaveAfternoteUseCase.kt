package com.afternote.feature.afternote.presentation.author.editor

import com.afternote.feature.afternote.domain.repository.AfternoteRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory
import com.afternote.feature.afternote.presentation.author.editor.model.RegisterAfternotePayload
import javax.inject.Inject

/**
 * 검증된 페이로드를 받아 미디어 해석 → 매퍼 → 생성/수정 Repository 호출까지 조합하는 UseCase
 * (CLAUDE.md UseCase 도입 조건 1번: *"여러 Repository를 조합하는 비즈니스 로직"*).
 *
 * 에디터 화면 전용 타입([com.afternote.feature.afternote.presentation.author.editor.model.EditorCategory] 등)을 다루므로
 * 도메인 모듈로 격상하지 않고 presentation 레이어에 둔다. UseCase 네이밍 규칙(`동사 + 명사 + UseCase`)을 따른다.
 *
 */
class SaveAfternoteUseCase
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
            playlistSongs: List<Song>,
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
                        playlistSongs = playlistSongs,
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
                    playlistSongs = playlistSongs,
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
            playlistSongs: List<Song>,
            funeralVideoUrl: String?,
            funeralThumbnailUrl: String?,
            memorialPhotoUrl: String?,
        ): Result<Long> {
            val createInput =
                AfternoteEditorFormMapper.buildCreateInput(
                    category = category,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                    playlistSongs = playlistSongs,
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
