package com.afternote.feature.afternote.presentation.author.editor.state

import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.presentation.author.editor.memorial.playlist.Song
import com.afternote.feature.afternote.presentation.author.editor.model.EditorContentPrefill
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem

/**
 * 카테고리 전용 입력 묶음. [EditorFormState] 가 합성으로 소유한다.
 *
 * 상속(`sealed class EditorFormState`)이 아니라 합성인 이유는 추상 타입에 `copy` 가 없어서다 —
 * 이탈 가드 지문의 공용 필드 중립화를 하위 타입마다 손으로 재작성하게 되고, 그 순간 "필드 추가 자동 포함" 성질을 잃는다.
 */
sealed interface AfternoteTypeForm {
    val type: AfternoteType

    /** 이탈 가드 지문에 실을 조각. 수정 진입 기준선을 포함해 비교할 카테고리 상태가 없으면 `null`. */
    fun enteredContentOrNull(): String?

    sealed interface WithServiceAndProcessingMethods : AfternoteTypeForm {
        val selectedService: String?
        val processingMethods: List<ProcessingMethodItem>

        fun withService(service: String?): WithServiceAndProcessingMethods

        fun withProcessingMethods(methods: List<ProcessingMethodItem>): WithServiceAndProcessingMethods
    }

    data class Social(
        override val selectedService: String? = null,
        override val processingMethods: List<ProcessingMethodItem> = emptyList(),
    ) : WithServiceAndProcessingMethods {
        override val type = AfternoteType.SOCIAL_NETWORK

        override fun withService(service: String?) = copy(selectedService = service)

        override fun withProcessingMethods(methods: List<ProcessingMethodItem>) = copy(processingMethods = methods)

        override fun enteredContentOrNull(): String? = takeUnless { it == PRISTINE }?.toString()

        private companion object {
            val PRISTINE = Social()
        }
    }

    data class Business(
        override val selectedService: String? = null,
        override val processingMethods: List<ProcessingMethodItem> = emptyList(),
    ) : WithServiceAndProcessingMethods {
        override val type = AfternoteType.BUSINESS

        override fun withService(service: String?) = copy(selectedService = service)

        override fun withProcessingMethods(methods: List<ProcessingMethodItem>) = copy(processingMethods = methods)

        override fun enteredContentOrNull(): String? = takeUnless { it == PRISTINE }?.toString()

        private companion object {
            val PRISTINE = Business()
        }
    }

    data class Gallery(
        override val selectedService: String? = null,
        override val processingMethods: List<ProcessingMethodItem> = emptyList(),
    ) : WithServiceAndProcessingMethods {
        override val type = AfternoteType.GALLERY_AND_FILES

        override fun withService(service: String?) = copy(selectedService = service)

        override fun withProcessingMethods(methods: List<ProcessingMethodItem>) = copy(processingMethods = methods)

        override fun enteredContentOrNull(): String? = takeUnless { it == PRISTINE }?.toString()

        private companion object {
            val PRISTINE = Gallery()
        }
    }

    /**
     * 미디어 두 축을 같은 모양으로 든다 — `picked`(이 폼에서 고른 것) + 서버에 저장된 것.
     *
     * 어느 칸에 들었는지가 곧 출처다. 삭제는 현재 표시된 층을 비워, `picked` 였으면 서버 값으로
     * 돌아가고 서버 값이었으면 PATCH `null` 로 이어진다. 종전 영상 축은 한 칸이 둘을 겸해 출처를 URL
     * 스킴으로 추론했고, 로컬로 덮이는 순간 서버 값을 잃어 거짓 삭제가 났다(#1406, #1597).
     */
    data class Memorial(
        val pickedPhotoUri: String? = null,
        val pickedVideo: MemorialVideoAttachment? = null,
        val serverVideo: MemorialVideoAttachment? = null,
        val photoUrl: String? = null,
        val playlistSongs: List<Song> = emptyList(),
    ) : AfternoteTypeForm {
        override val type = AfternoteType.MEMORIAL

        fun displayPhotoUri(): String? = pickedPhotoUri ?: photoUrl

        /** 이 폼에서 고른 영상이 있으면 그것, 없으면 서버에 저장된 것. */
        fun displayVideo(): MemorialVideoAttachment? = pickedVideo ?: serverVideo

        /**
         * 미디어 URL은 수정 진입 기준선과 비교해야 서버 원본 삭제도 미저장 변경으로 잡힌다. 썸네일만
         * 영상에서 자동 파생되는 값이라 [MemorialVideoAttachment.userEnteredPart] 로 양쪽에서 걷어낸다.
         */
        override fun enteredContentOrNull(): String? =
            copy(
                pickedVideo = pickedVideo?.userEnteredPart(),
                serverVideo = serverVideo?.userEnteredPart(),
            ).takeUnless { it == PRISTINE }
                ?.toString()

        private companion object {
            val PRISTINE = Memorial()
        }
    }

    /** 전용 입력이 0개인 "준비 중" 카테고리 (이슈 #195). 지문 중립 원소로도 쓰인다. */
    data object Estate : AfternoteTypeForm {
        override val type = AfternoteType.ESTATE

        override fun enteredContentOrNull(): String? = null
    }

    companion object {
        /** 카테고리 전환의 유일한 생성 경로 — 항상 입력이 비어 있는 하위 타입을 만든다. */
        fun pristineFor(type: AfternoteType): AfternoteTypeForm =
            when (type) {
                AfternoteType.SOCIAL_NETWORK -> Social()
                AfternoteType.BUSINESS -> Business()
                AfternoteType.GALLERY_AND_FILES -> Gallery()
                AfternoteType.MEMORIAL -> Memorial()
                AfternoteType.ESTATE -> Estate
            }

        fun fromPrefill(content: EditorContentPrefill): AfternoteTypeForm =
            when (content) {
                is EditorContentPrefill.SocialNetwork -> {
                    Social(selectedService = content.serviceName, processingMethods = content.processingMethods)
                }

                is EditorContentPrefill.Business -> {
                    Business(selectedService = content.serviceName, processingMethods = content.processingMethods)
                }

                is EditorContentPrefill.Gallery -> {
                    Gallery(selectedService = content.serviceName, processingMethods = content.processingMethods)
                }

                is EditorContentPrefill.Memorial -> {
                    Memorial(
                        serverVideo =
                            MemorialVideoAttachment.ofOrNull(
                                url = content.videoUrl,
                                thumbnailUrl = content.thumbnailUrl,
                            ),
                        photoUrl = content.photoUrl,
                        playlistSongs = content.playlistSongs,
                    )
                }

                EditorContentPrefill.Estate -> {
                    Estate
                }
            }
    }
}
