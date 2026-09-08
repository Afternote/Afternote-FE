package com.afternote.feature.afternote.presentation.editor

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.afternote.core.common.reporting.ErrorReporter
import com.afternote.core.common.result.runCatchingCancellable
import com.afternote.core.domain.repository.UserRepository
import com.afternote.core.ui.mvi.MviViewModel
import com.afternote.feature.afternote.domain.AfternoteType
import com.afternote.feature.afternote.domain.error.AfternoteFailure
import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.SaveAfternoteCommand
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import com.afternote.feature.afternote.domain.repository.author.MediaInput
import com.afternote.feature.afternote.domain.repository.author.MemorialThumbnailUploadRepository
import com.afternote.feature.afternote.domain.usecase.editor.ResolveMemorialMediaForSaveUseCase
import com.afternote.feature.afternote.presentation.editor.mapper.toAfternoteEditorReceivers
import com.afternote.feature.afternote.presentation.editor.memorial.Song
import com.afternote.feature.afternote.presentation.editor.model.EditorFormPrefill
import com.afternote.feature.afternote.presentation.editor.model.RegisterAfternotePayload
import com.afternote.feature.afternote.presentation.editor.processing.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.editor.receiver.AfternoteEditorReceiver
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorError
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorErrorEvent
import com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorUiState
import com.afternote.feature.afternote.presentation.editor.state.AfternoteTypeForm
import com.afternote.feature.afternote.presentation.editor.state.EditableMemorialVideo
import com.afternote.feature.afternote.presentation.editor.state.EditorFormState
import com.afternote.feature.afternote.presentation.editor.state.withMemorialAudio
import com.afternote.feature.afternote.presentation.editor.state.withMemorialAudioRemoved
import com.afternote.feature.afternote.presentation.editor.state.withMemorialPhoto
import com.afternote.feature.afternote.presentation.editor.state.withMemorialPhotoRemoved
import com.afternote.feature.afternote.presentation.editor.state.withMemorialPlaylistSongs
import com.afternote.feature.afternote.presentation.editor.state.withMemorialThumbnail
import com.afternote.feature.afternote.presentation.editor.state.withMemorialVideo
import com.afternote.feature.afternote.presentation.editor.state.withMemorialVideoRemoved
import com.afternote.feature.afternote.presentation.editor.state.withPrefillApplied
import com.afternote.feature.afternote.presentation.editor.state.withProcessingMethodAdded
import com.afternote.feature.afternote.presentation.editor.state.withProcessingMethodDeleted
import com.afternote.feature.afternote.presentation.editor.state.withProcessingMethodEdited
import com.afternote.feature.afternote.presentation.editor.state.withProcessingMethodsInitialized
import com.afternote.feature.afternote.presentation.editor.state.withReceiverAddedIfAbsent
import com.afternote.feature.afternote.presentation.editor.state.withReceiverDeleted
import com.afternote.feature.afternote.presentation.editor.state.withReceiversReplaced
import com.afternote.feature.afternote.presentation.editor.state.withReceiversReplacedIfEmpty
import com.afternote.feature.afternote.presentation.editor.state.withService
import com.afternote.feature.afternote.presentation.editor.state.withType
import com.afternote.feature.afternote.presentation.navigation.model.AfternoteRoute
import com.afternote.feature.afternote.presentation.navigation.model.SELECTED_RECEIVER_IDS_KEY
import com.afternote.feature.afternote.presentation.reporting.AfternoteFailureStage
import com.afternote.feature.afternote.presentation.reporting.recordAfternoteFailure
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val EDITOR_FORM_SNAPSHOT_KEY = "editor_form_snapshot_v4"
private const val INITIALIZED_ACTION_TEMPLATE_TYPE_KEY = "initialized_action_template_type"
private const val PREFILL_SEEDED_ITEM_ID_KEY = "editor_prefill_seeded_item_id"

private const val TAG = "AfternoteEditorViewModel"

@Serializable
private data class ReceiverSnap(
    val id: Long,
    val name: String,
    val label: String,
)

@Serializable
private data class ProcessingMethodSnap(
    val localId: Int,
    val text: String,
)

/**
 * [SavedStateHandle]에 JSON으로 넣는 폼 스냅샷. 번들 전체 크기는 대략 500KB~1MB를 넘기지 않도록 설계해야 하며,
 * 그렇지 않으면 [android.os.TransactionTooLargeException]이 날 수 있다. 큰 Base64/data URL은 폼에 넣지 말고 URL·URI 문자열만 저장한다.
 */
@Serializable
private data class EditorFormSnapshot(
    val type: AfternoteType = AfternoteType.SOCIAL_NETWORK,
    val selectedService: String = "",
    val receivers: List<ReceiverSnap> = emptyList(),
    val processingMethods: List<ProcessingMethodSnap> = emptyList(),
    val pickedMemorialPhotoUri: String? = null,
    val memorialVideo: EditableMemorialVideo? = null,
    val memorialPhotoUrl: String? = null,
    val memorialAudioUrl: String? = null,
    val memorialPlaylistSongs: List<Song> = emptyList(),
) {
    fun toEditorFormState(): EditorFormState =
        EditorFormState(
            afternoteEditReceivers =
                receivers.map { AfternoteEditorReceiver(id = it.id, name = it.name, label = it.label) },
            typeForm = toTypeForm(type),
        )

    /** 다른 카테고리 칸에 값이 남아 있어도 복원 단계에서 버려진다. */
    private fun toTypeForm(type: AfternoteType): AfternoteTypeForm {
        // 스냅샷의 빈 문자열은 미선택(null)로 복원 — process death 후에도 임의 기본값을 확정하지 않는다 (이슈 #468).
        val service = selectedService.ifBlank { null }
        val methodItems = processingMethods.map { ProcessingMethodItem(it.localId, it.text) }
        return when (type) {
            AfternoteType.SOCIAL_NETWORK -> {
                AfternoteTypeForm.Social(service, methodItems)
            }

            AfternoteType.BUSINESS -> {
                AfternoteTypeForm.Business(service, methodItems)
            }

            AfternoteType.GALLERY_AND_FILES -> {
                AfternoteTypeForm.Gallery(service, methodItems)
            }

            AfternoteType.MEMORIAL -> {
                AfternoteTypeForm.Memorial(
                    pickedPhotoUri = pickedMemorialPhotoUri,
                    video = memorialVideo ?: EditableMemorialVideo.empty(),
                    photoUrl = memorialPhotoUrl,
                    audioUrl = memorialAudioUrl,
                    playlistSongs = memorialPlaylistSongs,
                )
            }

            AfternoteType.ESTATE -> {
                AfternoteTypeForm.Estate
            }
        }
    }

    companion object {
        fun from(form: EditorFormState): EditorFormSnapshot =
            EditorFormSnapshot(
                type = form.selectedType,
                selectedService = form.selectedService.orEmpty(),
                receivers =
                    form.afternoteEditReceivers.map {
                        ReceiverSnap(id = it.id, name = it.name, label = it.label)
                    },
                processingMethods = form.processingMethods.map { ProcessingMethodSnap(it.localId, it.text) },
                pickedMemorialPhotoUri = form.pickedMemorialPhotoUri,
                memorialVideo = form.memorialVideo,
                memorialPhotoUrl = form.memorialPhotoUrl,
                memorialAudioUrl = form.memorialAudioUrl,
                memorialPlaylistSongs = form.memorialPlaylistSongs,
            )
    }
}

/**
 * 애프터노트 생성/수정 ViewModel.
 *
 * **SSOT:** 일반 폼은 [uiState]의 [EditorFormState], Compose 텍스트 입력은
 * [com.afternote.feature.afternote.presentation.editor.state.AfternoteEditorState]가 소유한다.
 * 추억 플레이리스트 화면과 곡 추가 화면은 같은 flow-scoped ViewModel의 폼을 사용한다.
 *
 * **경계:** Compose UI 객체(`TextFieldState`·`SnapshotStateList`·파사드)를 들지 않고 Retrofit 타입도 알지 않는다 —
 * 저장 API 의 HTTP·에러 바디 해석은 [AfternoteRepository] 구현이 도메인 예외로 변환한다.
 */
@HiltViewModel(assistedFactory = AfternoteEditorViewModel.Factory::class)
internal class AfternoteEditorViewModel
    @AssistedInject
    constructor(
        @Assisted private val route: AfternoteRoute.EditorFlowRoute,
        private val savedStateHandle: SavedStateHandle,
        private val userRepository: UserRepository,
        private val afternoteRepository: AfternoteRepository,
        private val memorialThumbnailUploadRepository: MemorialThumbnailUploadRepository,
        private val resolveMemorialMediaForSave: ResolveMemorialMediaForSaveUseCase,
        private val errorReporter: ErrorReporter,
    ) : MviViewModel<AfternoteEditorIntent, AfternoteEditorUiState, AfternoteEditorReducerEvent>(AfternoteEditorUiState()) {
        /** 진행 중인 prefill 조회 — 재시도가 이전 조회를 자르기 위한 핸들. */
        private var prefillJob: Job? = null

        /** 새 선택이 이전 수신자 조회와 폼 반영을 취소한다. */
        private var receiverSelectionJob: Job? = null

        private val formSnapshotJson =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        /**
         * 스냅샷 복원 결과. **「키가 있는가」가 아니라 「실제로 복원됐는가」를 들고 있다.**
         *
         * [readFormSnapshotOrDefault] 는 디코딩 실패를 삼켜 빈 기본 폼으로 떨어진다. 키 존재만 보면
         * 「표식 있음 + 문자열 있음 + 디코딩 실패」 조합에서 가드가 참이 되어 프리필이 막히고,
         * 빈 폼이 새 기준 스냅샷과 짝지어져 아래 KDoc 이 피하겠다고 적은 「전부 지움」 저장이 된다.
         */
        private val restoredForm: RestoredForm = readFormSnapshotOrDefault()

        /**
         * 이 ViewModel 이 «상세 프리필이 이미 실렸던» 폼 스냅샷에서 되살아났는가 (#1732).
         *
         * 참이면 복원된 폼은 서버 값 + 사용자가 그 뒤에 고친 것을 함께 들고 있다 — 프로세스 사망
         * 복원은 [EDITOR_FORM_SNAPSHOT_KEY] 의 폼뿐 아니라 화면이 가진 계정 정보·남기실 말씀
         * 입력까지 (`rememberTextFieldState`·`rememberSaveable`) 같은 번들로 되살리기 때문이다.
         * 그 위에 [loadExistingAfternoteForEdit] 의 재조회 프리필을 다시 실으면 남는 건 서버 값뿐이라,
         * 사용자가 쓴 편집이 아무 안내 없이 사라진다.
         *
         * 두 조건을 함께 본다. 표식만으로는 부족하다 — [persistFormSnapshot] 은 번들 용량 초과 같은
         * 실패를 삼키므로, 표식은 남았는데 폼 스냅샷이 없는 조합이 가능하다. 그때 프리필까지 막으면
         * 빈 폼이 기준 스냅샷과 짝지어져 「전부 지움」 저장이 된다 (#705·#1617 이 막은 그 경로다).
         *
         * **그래서 「키가 있는가」가 아니라 「복원됐는가」([RestoredForm.fromSnapshot])를 본다.**
         * 문자열이 남아 있어도 디코딩이 실패하면 폼은 빈 기본값이므로, 키 존재로 판정하면 위 조합을
         * 그대로 통과시킨다 — 스키마가 바뀌는 순간(키 접미사를 올리지 않은 채) 열리는 잠복 경로다.
         *
         * 폼과 프리필의 값 비교로 대신하지 않는다. 계정 정보·남기실 말씀은 화면이 소유해 이 폼에
         * 없으므로, 비밀번호만 고친 복원은 «폼이 같다» 로 읽혀 그 편집이 그대로 덮인다.
         */

        private val restoredFromSeededSnapshot: Boolean =
            route.itemId != null &&
                restoredForm.fromSnapshot &&
                savedStateHandle.get<Long>(PREFILL_SEEDED_ITEM_ID_KEY) == route.itemId

        val isEditing: Boolean get() = route.itemId != null

        override fun onIntent(intent: AfternoteEditorIntent) {
            when (intent) {
                is AfternoteEditorIntent.SetType -> {
                    if (currentState.form.selectedType != intent.type) savedStateHandle.remove<String>(INITIALIZED_ACTION_TEMPLATE_TYPE_KEY)
                    dispatchForm(AfternoteEditorReducerEvent.TypeChanged(intent.type))
                }

                is AfternoteEditorIntent.SetService -> {
                    dispatchForm(AfternoteEditorReducerEvent.ServiceChanged(intent.service))
                }

                is AfternoteEditorIntent.SetMemorialPhoto -> {
                    dispatchForm(AfternoteEditorReducerEvent.MemorialPhotoChanged(intent.uri))
                }

                AfternoteEditorIntent.RemoveMemorialPhoto -> {
                    dispatchForm(AfternoteEditorReducerEvent.MemorialPhotoRemoved)
                }

                is AfternoteEditorIntent.SetMemorialVideo -> {
                    pendingThumbnailBytes = null
                    dispatchForm(AfternoteEditorReducerEvent.MemorialVideoChanged(intent.url))
                }

                AfternoteEditorIntent.RemoveMemorialVideo -> {
                    pendingThumbnailBytes = null
                    dispatchForm(AfternoteEditorReducerEvent.MemorialVideoRemoved)
                }

                is AfternoteEditorIntent.SetMemorialThumbnail -> {
                    dispatchForm(AfternoteEditorReducerEvent.MemorialThumbnailChanged(intent.dataUrl))
                }

                is AfternoteEditorIntent.SetMemorialAudio -> {
                    dispatchForm(AfternoteEditorReducerEvent.MemorialAudioChanged(intent.url))
                }

                AfternoteEditorIntent.RemoveMemorialAudio -> {
                    dispatchForm(AfternoteEditorReducerEvent.MemorialAudioRemoved)
                }

                is AfternoteEditorIntent.AddMemorialPlaylistSongs -> {
                    if (intent.songs.isEmpty()) return
                    dispatchForm(AfternoteEditorReducerEvent.PlaylistSongsAdded(intent.songs))
                }

                is AfternoteEditorIntent.RemoveMemorialPlaylistSongs -> {
                    if (intent.selectionKeys.isEmpty()) return
                    dispatchForm(AfternoteEditorReducerEvent.PlaylistSongsRemoved(intent.selectionKeys))
                }

                AfternoteEditorIntent.ClearMemorialPlaylistSongs -> {
                    dispatchForm(AfternoteEditorReducerEvent.PlaylistSongsCleared)
                }

                is AfternoteEditorIntent.DeleteReceiver -> {
                    dispatchForm(AfternoteEditorReducerEvent.ReceiverDeleted(intent.receiverId))
                }

                is AfternoteEditorIntent.AddReceiverIfAbsent -> {
                    dispatchForm(AfternoteEditorReducerEvent.ReceiverAdded(intent.receiverId, intent.name, intent.label))
                }

                is AfternoteEditorIntent.ReplaceReceiversIfEmpty -> {
                    dispatchForm(AfternoteEditorReducerEvent.EmptyReceiversReplaced(intent.receivers))
                }

                is AfternoteEditorIntent.ApplyPrefill -> {
                    readEditItemId()?.let { savedStateHandle[PREFILL_SEEDED_ITEM_ID_KEY] = it }
                    dispatchForm(AfternoteEditorReducerEvent.PrefillApplied(intent.prefill))
                }

                is AfternoteEditorIntent.InitializeProcessingMethodDefaults -> {
                    if (isEditing || currentState.form.selectedType != intent.type) return
                    if (savedStateHandle.get<String>(INITIALIZED_ACTION_TEMPLATE_TYPE_KEY) == intent.type.name) return
                    savedStateHandle[INITIALIZED_ACTION_TEMPLATE_TYPE_KEY] = intent.type.name
                    if (intent.methods.isEmpty() || currentState.form.processingMethods.isNotEmpty()) return
                    dispatchForm(AfternoteEditorReducerEvent.ProcessingMethodsInitialized(intent.type, intent.methods))
                }

                is AfternoteEditorIntent.AddProcessingMethod -> {
                    dispatchForm(AfternoteEditorReducerEvent.ProcessingMethodAdded(intent.text))
                }

                is AfternoteEditorIntent.DeleteProcessingMethod -> {
                    dispatchForm(AfternoteEditorReducerEvent.ProcessingMethodDeleted(intent.localId))
                }

                is AfternoteEditorIntent.EditProcessingMethod -> {
                    dispatchForm(AfternoteEditorReducerEvent.ProcessingMethodEdited(intent.localId, intent.newText))
                }

                AfternoteEditorIntent.RefreshAuthorReceivers -> {
                    refreshAuthorReceivers()
                }

                is AfternoteEditorIntent.UploadMemorialThumbnail -> {
                    uploadMemorialThumbnail(intent.jpegBytes)
                }

                is AfternoteEditorIntent.MemorialThumbnailExtractionFailed -> {
                    onMemorialThumbnailExtractionFailed(intent.throwable)
                }

                AfternoteEditorIntent.RetryMemorialThumbnail -> {
                    retryMemorialThumbnail()
                }

                is AfternoteEditorIntent.MemorialCaptureLaunchFailed -> {
                    onMemorialCaptureLaunchFailed(intent.throwable)
                }

                is AfternoteEditorIntent.Save -> {
                    saveAfternote(intent.payload, intent.selectedReceiverIds, intent.memorialMedia)
                }

                AfternoteEditorIntent.RetryPrefill -> {
                    retryPrefill()
                }

                AfternoteEditorIntent.ConsumePrefill -> {
                    dispatch(AfternoteEditorReducerEvent.PrefillConsumed)
                }

                AfternoteEditorIntent.ConsumeSaveSuccess -> {
                    dispatch(AfternoteEditorReducerEvent.SaveSuccessConsumed)
                }

                AfternoteEditorIntent.ConsumeThumbnailUploaded -> {
                    dispatch(AfternoteEditorReducerEvent.ThumbnailUploadConsumed)
                }

                is AfternoteEditorIntent.ConsumeError -> {
                    dispatch(AfternoteEditorReducerEvent.ErrorConsumed(intent.consumed))
                }

                is AfternoteEditorIntent.ReceiversSelected -> {
                    receiverSelectionJob?.cancel()
                    savedStateHandle[SELECTED_RECEIVER_IDS_KEY] = intent.receiverIds.toLongArray()
                }

                AfternoteEditorIntent.ApplyPendingReceiverSelection -> {
                    val selectedIds = savedStateHandle.remove<LongArray>(SELECTED_RECEIVER_IDS_KEY)?.toList() ?: return
                    receiverSelectionJob?.cancel()
                    receiverSelectionJob = viewModelScope.launch { applySelectedReceivers(selectedIds) }
                }
            }
        }

        override fun reduce(
            state: AfternoteEditorUiState,
            event: AfternoteEditorReducerEvent,
        ): AfternoteEditorUiState =
            when (event) {
                is AfternoteEditorReducerEvent.TypeChanged -> {
                    state.copy(form = state.form.withType(event.type))
                }

                is AfternoteEditorReducerEvent.ServiceChanged -> {
                    state.copy(form = state.form.withService(event.service))
                }

                is AfternoteEditorReducerEvent.MemorialPhotoChanged -> {
                    state.copy(form = state.form.withMemorialPhoto(event.uri))
                }

                AfternoteEditorReducerEvent.MemorialPhotoRemoved -> {
                    state.copy(form = state.form.withMemorialPhotoRemoved())
                }

                is AfternoteEditorReducerEvent.MemorialVideoChanged -> {
                    state.copy(form = state.form.withMemorialVideo(event.url))
                }

                AfternoteEditorReducerEvent.MemorialVideoRemoved -> {
                    state.copy(form = state.form.withMemorialVideoRemoved())
                }

                is AfternoteEditorReducerEvent.MemorialThumbnailChanged -> {
                    state.copy(form = state.form.withMemorialThumbnail(event.dataUrl))
                }

                is AfternoteEditorReducerEvent.MemorialAudioChanged -> {
                    state.copy(form = state.form.withMemorialAudio(event.url))
                }

                AfternoteEditorReducerEvent.MemorialAudioRemoved -> {
                    state.copy(form = state.form.withMemorialAudioRemoved())
                }

                is AfternoteEditorReducerEvent.PlaylistSongsAdded -> {
                    state.copy(
                        form =
                            state.form.withMemorialPlaylistSongs(state.form.memorialPlaylistSongs + event.songs),
                    )
                }

                is AfternoteEditorReducerEvent.PlaylistSongsRemoved -> {
                    state.copy(
                        form =
                            state.form.withMemorialPlaylistSongs(
                                state.form.memorialPlaylistSongs.filterNot {
                                    it.selectionKey in
                                        event.selectionKeys
                                },
                            ),
                    )
                }

                AfternoteEditorReducerEvent.PlaylistSongsCleared -> {
                    state.copy(form = state.form.withMemorialPlaylistSongs(emptyList()))
                }

                is AfternoteEditorReducerEvent.ReceiverDeleted -> {
                    state.copy(form = state.form.withReceiverDeleted(event.receiverId))
                }

                is AfternoteEditorReducerEvent.ReceiverAdded -> {
                    state.copy(form = state.form.withReceiverAddedIfAbsent(event.receiverId, event.name, event.label))
                }

                is AfternoteEditorReducerEvent.EmptyReceiversReplaced -> {
                    state.copy(form = state.form.withReceiversReplacedIfEmpty(event.receivers))
                }

                is AfternoteEditorReducerEvent.PrefillApplied -> {
                    state.copy(form = state.form.withPrefillApplied(event.prefill))
                }

                is AfternoteEditorReducerEvent.ProcessingMethodsInitialized -> {
                    state.copy(form = state.form.withProcessingMethodsInitialized(event.methods))
                }

                is AfternoteEditorReducerEvent.ProcessingMethodAdded -> {
                    state.copy(form = state.form.withProcessingMethodAdded(event.text))
                }

                is AfternoteEditorReducerEvent.ProcessingMethodDeleted -> {
                    state.copy(form = state.form.withProcessingMethodDeleted(event.localId))
                }

                is AfternoteEditorReducerEvent.ProcessingMethodEdited -> {
                    state.copy(form = state.form.withProcessingMethodEdited(event.localId, event.newText))
                }

                is AfternoteEditorReducerEvent.Initialized -> {
                    state.copy(form = event.form, originalType = event.originalType, isPrefillLoading = event.isPrefillLoading)
                }

                is AfternoteEditorReducerEvent.AuthorReceiversLoaded -> {
                    state.copy(authorReceivers = event.receivers)
                }

                is AfternoteEditorReducerEvent.ThumbnailUploaded -> {
                    state.copy(pendingThumbnailUrl = event.url)
                }

                AfternoteEditorReducerEvent.ThumbnailExtractionRetried -> {
                    state.copy(memorialThumbnailRetryToken = state.memorialThumbnailRetryToken + 1)
                }

                is AfternoteEditorReducerEvent.ErrorRaised -> {
                    state.withError(event.error)
                }

                AfternoteEditorReducerEvent.SaveStarted -> {
                    state.copy(isSaving = true, errorEvent = null)
                }

                is AfternoteEditorReducerEvent.SaveSucceeded -> {
                    state.copy(isSaving = false, savedId = event.id, pendingSaveSuccessId = event.id)
                }

                is AfternoteEditorReducerEvent.SaveFailed -> {
                    state.copy(isSaving = false).withError(event.error)
                }

                AfternoteEditorReducerEvent.PrefillStarted -> {
                    state.copy(isPrefillLoading = true, isPrefillFailed = false)
                }

                is AfternoteEditorReducerEvent.PrefillLoaded -> {
                    state.copy(
                        originalType = event.prefill.type,
                        pendingPrefill = if (event.preserveRestoredForm) null else event.prefill,
                        isPrefillLoading =
                            state.isPrefillLoading && !event.preserveRestoredForm,
                        updateBaseline = event.baseline,
                    )
                }

                AfternoteEditorReducerEvent.PrefillFailed -> {
                    state.copy(isPrefillLoading = false, isPrefillFailed = true)
                }

                is AfternoteEditorReducerEvent.SelectedReceiversApplied -> {
                    state.copy(form = state.form.withReceiversReplaced(event.receivers))
                }

                AfternoteEditorReducerEvent.PrefillConsumed -> {
                    state.copy(isPrefillLoading = false, pendingPrefill = null)
                }

                AfternoteEditorReducerEvent.SaveSuccessConsumed -> {
                    state.copy(pendingSaveSuccessId = null)
                }

                AfternoteEditorReducerEvent.ThumbnailUploadConsumed -> {
                    state.copy(pendingThumbnailUrl = null)
                }

                is AfternoteEditorReducerEvent.ErrorConsumed -> {
                    if (state.errorEvent == event.consumed) state.copy(errorEvent = null) else state
                }
            }

        /** 폼 전이가 끝난 뒤 저장한다. 재실행될 수 있는 reducer에는 IO를 두지 않는다. */
        private fun dispatchForm(event: AfternoteEditorReducerEvent) {
            dispatch(event)
            persistFormSnapshot(currentState.form)
        }

        private fun currentForm(): EditorFormState = currentState.form

        init {
            dispatch(
                AfternoteEditorReducerEvent.Initialized(
                    restoredForm.form,
                    route.initialType.takeIf { route.itemId != null },
                    readEditItemId() != null,
                ),
            )
            readEditItemId()?.let(::loadExistingAfternoteForEdit)
        }

        private fun readEditItemId(): Long? = route.itemId

        /**
         * 저장된 폼 스냅샷을 읽는다.
         *
         * **복원 성공 여부를 함께 돌려준다.** 실패를 기본 폼으로 삼키기만 하면 호출부가 「빈 폼으로
         * 떨어졌다」와 「원래 빈 폼이었다」를 못 가른다 — [restoredFromSeededSnapshot] 이 그 차이로
         * 갈리므로 여기서 알려 줘야 한다.
         */
        private fun readFormSnapshotOrDefault(): RestoredForm {
            val defaultForm = EditorFormState().withType(route.initialType)
            val raw =
                savedStateHandle.get<String>(EDITOR_FORM_SNAPSHOT_KEY)
                    ?: return RestoredForm(form = defaultForm, fromSnapshot = false)
            return runCatching {
                RestoredForm(
                    form =
                        formSnapshotJson
                            .decodeFromString(EditorFormSnapshot.serializer(), raw)
                            .toEditorFormState(),
                    fromSnapshot = true,
                )
            }.getOrElse { RestoredForm(form = defaultForm, fromSnapshot = false) }
        }

        /** [readFormSnapshotOrDefault] 의 결과 — 폼과 «그 폼이 스냅샷에서 왔는가». */
        private data class RestoredForm(
            val form: EditorFormState,
            val fromSnapshot: Boolean,
        )

        /** [EditorFormSnapshot] 직렬화. 실패 시 무시한다(용량 초과 등은 [EditorFormSnapshot] KDoc 참고). */
        private fun persistFormSnapshot(form: EditorFormState) {
            runCatching {
                savedStateHandle[EDITOR_FORM_SNAPSHOT_KEY] =
                    formSnapshotJson.encodeToString(EditorFormSnapshot.serializer(), EditorFormSnapshot.from(form))
            }
        }

        /**
         * 작성자가 등록한 수신자 전체를 받아 [AfternoteEditorUiState.authorReceivers] 에 채운다.
         *
         * 신규 작성 진입 시 1회 호출된다. 폼이 비어 있으면 화면이 이 목록으로 수신자를 채우고
         * (`AfternoteNavGraphEditor` 의 `replaceReceiversIfEmpty`), 사용자는 불필요한 수신자를 지운다.
         * 수정 진입은 상세 응답 prefill 이 지정 수신자를 채우므로 이 목록을 쓰지 않는다.
         */
        private fun refreshAuthorReceivers() {
            viewModelScope.launch { loadAuthorReceivers() }
        }

        private suspend fun loadAuthorReceivers() {
            runCatchingCancellable { userRepository.getReceivers() }
                .onSuccess { receivers ->
                    currentCoroutineContext().ensureActive()
                    dispatch(AfternoteEditorReducerEvent.AuthorReceiversLoaded(receivers.toAfternoteEditorReceivers()))
                }.onFailure { e ->
                    errorReporter.recordAfternoteFailure(AfternoteFailureStage.AUTHOR_RECEIVER_LOAD, e)
                }
        }

        /**
         * 업로드에 실패한 프레임 바이트. 스낵바의 «다시 시도» 와 저장 직전 재업로드가 이것을 다시 올린다.
         *
         * 폼·UI 상태에 싣지 않는다 — 프로세스 재생성 번들에 수백 KB 를 얹지 않기 위해서다. 복원 뒤에는
         * 화면이 같은 영상에서 프레임을 다시 뽑아 [uploadMemorialThumbnail] 로 들어온다.
         */
        private var pendingThumbnailBytes: ByteArray? = null

        private fun uploadMemorialThumbnail(jpegBytes: ByteArray?) {
            if (jpegBytes == null) return
            pendingThumbnailBytes = jpegBytes
            viewModelScope.launch {
                memorialThumbnailUploadRepository
                    .uploadThumbnail(jpegBytes)
                    .onSuccess { url ->
                        Log.d(TAG, "uploadMemorialThumbnail: success, url=$url")
                        pendingThumbnailBytes = null
                        dispatch(AfternoteEditorReducerEvent.ThumbnailUploaded(url))
                    }.onFailure { e ->
                        errorReporter.recordAfternoteFailure(AfternoteFailureStage.MEMORIAL_THUMBNAIL_UPLOAD, e)
                        dispatch(
                            AfternoteEditorReducerEvent.ErrorRaised(
                                AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.THUMBNAIL),
                            ),
                        )
                    }
            }
        }

        /**
         * 선택한 영상에서 썸네일 프레임을 뽑지 못한 실패.
         *
         * 로컬 디코딩이라 [uploadMemorialThumbnail] 경로를 타지 않는다. 종전에는 기록만 하고 화면에는
         * 아무 신호도 없어, 사용자는 썸네일 자리가 빈 이유도 되돌릴 방법도 알 수 없었다.
         */
        private fun onMemorialThumbnailExtractionFailed(throwable: Throwable) {
            errorReporter.recordAfternoteFailure(AfternoteFailureStage.MEMORIAL_THUMBNAIL_EXTRACT, throwable)
            // 뽑지 못했으니 재업로드할 바이트가 없다 — 재시도는 추출부터 다시 돌아야 한다.
            pendingThumbnailBytes = null
            dispatch(
                AfternoteEditorReducerEvent.ErrorRaised(AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.THUMBNAIL_EXTRACT)),
            )
        }

        /**
         * 영상 재선택 없이 썸네일만 다시 만든다 (#1550).
         *
         * 손에 바이트가 있으면(업로드 실패) 그대로 다시 올리고, 없으면(추출 실패) 토큰을 올려 화면이
         * 프레임 추출부터 다시 돌게 한다. 어느 쪽이든 사용자가 고른 영상은 그대로 둔다.
         */
        private fun retryMemorialThumbnail() {
            val bytes = pendingThumbnailBytes
            if (bytes != null) {
                uploadMemorialThumbnail(bytes)
                return
            }
            dispatch(AfternoteEditorReducerEvent.ThumbnailExtractionRetried)
        }

        /**
         * 저장 직전, 업로드에 실패해 남아 있는 썸네일 바이트를 한 번 더 업로드 시도한다.
         *
         * 영상 자체는 저장 시점에 업로드하는데(`resolveMemorialMediaForSave`) 썸네일만 선택 시점
         * 업로드라 비대칭이었다. 실패해도 저장은 막지 않는다 — 썸네일 때문에 장례식 영상 저장을
         * 버리게 하는 편이 더 나쁘다.
         */
        private suspend fun recoverPendingThumbnailOrNull(): String? {
            val bytes = pendingThumbnailBytes ?: return null
            return memorialThumbnailUploadRepository
                .uploadThumbnail(bytes)
                .onSuccess { pendingThumbnailBytes = null }
                .onFailure { e ->
                    errorReporter.recordAfternoteFailure(AfternoteFailureStage.MEMORIAL_THUMBNAIL_UPLOAD, e)
                }.getOrNull()
        }

        /**
         * 즉석 촬영 인텐트를 띄우지 못한 실패를 기록한다.
         *
         * 화면에는 "카메라를 사용할 수 없습니다" 한 줄만 나가고 사유가 지워지므로, UI 가 예외를
         * 넘겨주지 않으면 저장공간 문제인지 카메라 앱 부재인지 콘솔에서 가를 수 없다.
         */
        private fun onMemorialCaptureLaunchFailed(throwable: Throwable) {
            errorReporter.recordAfternoteFailure(AfternoteFailureStage.MEMORIAL_CAPTURE_LAUNCH, throwable)
        }

        private fun saveAfternote(
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            memorialMedia: SaveAfternoteMemorialMedia,
        ) {
            val editorState = currentState
            if (editorState.isSaving) return
            // prefill 을 못 읽은 채로 저장하면 서버가 빈 폼 값으로 기존 기록을 덮는다 (#705).
            // 화면이 이미 저장 액션을 막지만, 저장 진입점은 여기 하나뿐이라 규칙도 여기서 지킨다.
            //
            // 실패(isPrefillFailed)뿐 아니라 «아직 읽는 중»(isPrefillLoading)도 막는다 — skeleton 이
            // 떠 있는 동안에도 등록 버튼은 눌리고, 그때 폼은 아직 기본 빈 값이라 느린 상세 GET 을
            // 앞질러 저장하면 같은 덮어쓰기가 난다. isPrefillLoading 은 편집 진입(itemId != null)에서만
            // true 라 신규 작성은 영향받지 않는다.
            //
            // 두 상태는 저장을 막는 이유가 같아도 사용자에게 할 말이 다르다. 실패는 「불러오지
            // 못했다」 이고 진행 중은 「곧 도착한다」 다 — 한 갈래로 뭉치면 아직 읽는 중인
            // 사용자에게 실패했다고 말하게 된다.
            if (editorState.isPrefillFailed) {
                dispatch(AfternoteEditorReducerEvent.ErrorRaised(AfternoteEditorError.PrefillUnavailable))
                return
            }
            if (editorState.isPrefillLoading) {
                dispatch(AfternoteEditorReducerEvent.ErrorRaised(AfternoteEditorError.PrefillNotReady))
                return
            }

            val form = editorState.form
            val editingId = readEditItemId()
            val type = form.selectedType
            val playlistSongs = form.memorialPlaylistSongs

            val validationError =
                AfternoteEditorValidator.validate(
                    form = form,
                    payload = payload,
                )
            if (validationError != null) {
                dispatch(AfternoteEditorReducerEvent.ErrorRaised(AfternoteEditorError.Validation(validationError)))
                return
            }

            val typeForSave =
                if (editingId != null) (editorState.originalType ?: type) else type

            // 수정인데 기준 스냅샷이 없다 = 상세를 못 받았다. 이 상태로 보내면 「안 건드림」과
            // 「전부 지움」을 가릴 수 없어 빈 폼이 그대로 삭제 지시가 된다 (#1617).
            val updateBaseline = editorState.updateBaseline
            if (editingId != null && updateBaseline == null) {
                dispatch(AfternoteEditorReducerEvent.ErrorRaised(AfternoteEditorError.PrefillUnavailable))
                return
            }

            viewModelScope.launch {
                dispatch(AfternoteEditorReducerEvent.SaveStarted)
                // 썸네일 URL 이 비어 있고 업로드에 실패한 바이트가 남아 있으면, payload 를 만들기 전에 그
                // 바이트를 한 번 더 업로드해 본다. 여기서 놓치면 썸네일 없는 영상이 그대로 확정되고,
                // 재편집으로 들어와도 원격 URL 이라 프레임을 다시 뽑지 않는다 (#1550). 실패해도 저장은
                // 그대로 진행한다.
                val memorialMediaForSave =
                    if (memorialMedia.memorialVideo.displayed?.thumbnailUrl != null) {
                        memorialMedia
                    } else {
                        recoverPendingThumbnailOrNull()
                            ?.let { url -> memorialMedia.copy(memorialVideo = memorialMedia.memorialVideo.withSelectionThumbnail(url)) }
                            ?: memorialMedia
                    }
                buildSaveCommand(
                    editingId = editingId,
                    typeForSave = typeForSave,
                    payload = payload,
                    selectedReceiverIds = selectedReceiverIds,
                    playlistSongs = playlistSongs,
                    memorialMedia = memorialMediaForSave,
                    updateBaseline = updateBaseline,
                ).fold(
                    onSuccess = { command ->
                        executeSaveCommand(command).fold(
                            onSuccess = { id ->
                                dispatch(AfternoteEditorReducerEvent.SaveSucceeded(id))
                            },
                            onFailure = { e -> handleSaveFailure(e) },
                        )
                    },
                    onFailure = { e -> handleSaveFailure(e) },
                )
            }
        }

        /**
         * [SaveAfternoteCommand] 분기에 따라 [AfternoteRepository] 의 적합한 메서드를 직접 호출한다.
         *
         * 과거에는 별도 `SaveAfternoteUseCase` 로 분리돼 있었으나, 단일 Repository 내 메서드 라우팅
         * 외에 비즈니스 로직이 없어 *약한 UseCase* (`#246`) 로 판단해 ViewModel 로 흡수.
         */
        private suspend fun executeSaveCommand(command: SaveAfternoteCommand): Result<Long> =
            when (command) {
                is SaveAfternoteCommand.Create -> {
                    when (val input = command.input) {
                        is CreateAfternoteInput.Social -> afternoteRepository.createSocial(input.payload)
                        is CreateAfternoteInput.Business -> afternoteRepository.createBusiness(input.payload)
                        is CreateAfternoteInput.Gallery -> afternoteRepository.createGallery(input.payload)
                        is CreateAfternoteInput.Memorial -> afternoteRepository.createMemorial(input.payload)
                    }
                }

                is SaveAfternoteCommand.Update -> {
                    afternoteRepository.update(command.id, command.payload)
                }
            }

        // 음성: 로컬 pick(content://) 인지 원격 prefill URL 인지를 진입 경계에서 한 번 확정해
        // MediaInput 으로 넘긴다. 영상은 #1406 이후 [EditableMemorialVideo] 가 출처를 들고 있어
        // 이 추론이 필요 없다 — 음성만 아직 한 필드에 로컬·원격이 섞인다 (#1118).
        private fun singleFieldMediaInput(url: String?): MediaInput {
            if (url.isNullOrBlank()) return MediaInput.None
            return if (url.isLocalContentUri()) MediaInput.Local(url) else MediaInput.Remote(url)
        }

        // 영정 사진: 새로 고른 로컬 픽 우선 → 없으면 기존 원격 → 둘 다 없으면 없음.
        private fun photoMediaInput(
            picked: String?,
            existing: String?,
        ): MediaInput =
            when {
                !picked.isNullOrBlank() -> MediaInput.Local(picked)
                !existing.isNullOrBlank() -> MediaInput.Remote(existing)
                else -> MediaInput.None
            }

        private suspend fun buildSaveCommand(
            editingId: Long?,
            typeForSave: AfternoteType,
            payload: RegisterAfternotePayload,
            selectedReceiverIds: List<Long>,
            playlistSongs: List<Song>,
            memorialMedia: SaveAfternoteMemorialMedia,
            updateBaseline: AfternoteEditorSnapshot?,
        ): Result<SaveAfternoteCommand> {
            val resolved =
                resolveMemorialMediaForSave(
                    video = memorialMedia.memorialVideo.toMediaInput(),
                    photo =
                        photoMediaInput(
                            picked = memorialMedia.pickedMemorialPhotoUri,
                            existing = memorialMedia.memorialPhotoUrl,
                        ),
                    audio = singleFieldMediaInput(memorialMedia.memorialAudioUrl),
                ).getOrElse { return Result.failure(it) }

            val command =
                if (editingId != null) {
                    val updatePayload =
                        AfternoteEditorFormMapper.buildUpdatePayload(
                            type = typeForSave,
                            payload = payload,
                            selectedReceiverIds = selectedReceiverIds,
                            playlistSongs = playlistSongs,
                            memorialMedia =
                                MemorialMediaUrls(
                                    memorialVideoUrl = resolved.resolvedVideoUrl,
                                    memorialThumbnailUrl = memorialMedia.memorialVideo.displayed?.thumbnailUrl,
                                    memorialPhotoUrl = resolved.resolvedMemorialPhotoUrl,
                                    memorialAudioUrl = resolved.resolvedMemorialAudioUrl,
                                ),
                            // saveAfternote 가 기준 없는 수정을 이미 막았다 — 여기 도달하면 반드시 있다.
                            baseline =
                                checkNotNull(updateBaseline) {
                                    "수정 저장에 기준 스냅샷이 없다 — saveAfternote 의 가드가 빠졌다"
                                },
                        )
                    SaveAfternoteCommand.Update(id = editingId, payload = updatePayload)
                } else {
                    val createInput =
                        AfternoteEditorFormMapper.buildCreateInput(
                            type = typeForSave,
                            payload = payload,
                            selectedReceiverIds = selectedReceiverIds,
                            playlistSongs = playlistSongs,
                            memorialVideoUrl = resolved.resolvedVideoUrl,
                            memorialThumbnailUrl = memorialMedia.memorialVideo.displayed?.thumbnailUrl,
                            memorialPhotoUrl = resolved.resolvedMemorialPhotoUrl,
                            memorialAudioUrl = resolved.resolvedMemorialAudioUrl,
                        )
                    SaveAfternoteCommand.Create(input = createInput)
                }
            return Result.success(command)
        }

        /**
         * 수정 진입 prefill 조회 실패 화면의 «다시 시도» (#705).
         *
         * 실패 상태를 걷고 skeleton 을 다시 세운 뒤 같은 조회를 새로 건다. 이미 성공해 폼이 채워진
         * 뒤라면 부를 일이 없고(화면이 오류 상태에서만 버튼을 그린다), 신규 작성 진입은 [readEditItemId]
         * 가 null 이라 아무 일도 하지 않는다.
         */
        private fun retryPrefill() {
            val afternoteId = readEditItemId() ?: return
            loadExistingAfternoteForEdit(afternoteId)
        }

        /**
         * 수정 진입 시 기존 애프터노트를 읽어 폼에 실을 prefill 을 만든다.
         *
         * 실패를 «빈 폼» 으로 흘려보내지 않는다 (#705) — 서버 수정(PATCH)은 보낸 값으로 기존 기록을
         * 덮으므로, 못 읽은 상태의 빈 폼이 저장되면 기록이 소실된다. 그래서 실패는 [AfternoteEditorUiState.isPrefillFailed]
         * 로 남겨 화면이 오류·재시도를 그리고 [saveAfternote] 가 저장을 막게 한다.
         */
        private fun loadExistingAfternoteForEdit(afternoteId: Long) {
            // 재시도가 진행 중인 조회를 자르고 들어온다 — 자르지 않으면 두 응답이 같은 폼을 두고 경합한다.
            prefillJob?.cancel()
            prefillJob =
                viewModelScope.launch {
                    dispatch(AfternoteEditorReducerEvent.PrefillStarted)
                    afternoteRepository
                        .getDetail(id = afternoteId)
                        .onSuccess { detail ->
                            val prefill = AfternoteEditorFormMapper.buildEditorFormPrefill(detail)
                            // UI 레이어 파사드가 TextFieldState·SnapshotStateList 등 UI 상태를 갱신하도록 위임.
                            // skeleton 종료는 UI 가 prefill 적용을 마친 뒤 [AfternoteEditorIntent.ConsumePrefill] 로 통보한다
                            // (uiState 갱신 시점에 prefill 도착했어도 UI 가 form·TextFieldState 에 반영하기 전이라
                            //  여기서 끄면 skeleton 사라짐 → 빈 폼 → prefill 깜빡임 발생).
                            //
                            // 복원된 편집이 있으면 프리필을 싣지 않는다 (#1732). 조회 자체는 그대로 돈다 —
                            // 기준 스냅샷([AfternoteEditorUiState.updateBaseline])이 없으면 저장이 막히고, 카테고리도
                            // 서버가 아는 값이어야 한다. 막는 것은 «폼에 덮어쓰기» 하나다.
                            dispatch(
                                AfternoteEditorReducerEvent.PrefillLoaded(
                                    prefill = prefill,
                                    baseline = AfternoteEditorFormMapper.buildUpdateBaseline(detail),
                                    preserveRestoredForm = restoredFromSeededSnapshot,
                                ),
                            )
                        }.onFailure { e ->
                            errorReporter.recordAfternoteFailure(AfternoteFailureStage.PREFILL_LOAD, e)
                            // skeleton 은 걷되 빈 폼으로 넘기지 않는다 — 오류·재시도 상태로 남긴다.
                            dispatch(AfternoteEditorReducerEvent.PrefillFailed)
                        }
                }
        }

        /**
         * UI 가 [AfternoteEditorUiState.pendingPrefill] 신호를 받아 폼·TextFieldState 에 모두 반영한 직후 호출.
         * skeleton 종료([AfternoteEditorUiState.isPrefillLoading] = false) + 신호 reset (pendingPrefill = null) 동시 처리.
         */
        private fun handleSaveFailure(e: Throwable) {
            val editorError = e.toAfternoteEditorError()
            // 필수 필드 검증에 걸린 실패(수신자 미선택 등)는 사용자가 채우면 풀리는 정상 경로라 기록하지 않는다 —
            // 보관 한도(최근 8건) 를 사용자 오류가 차지하면 실제 등록 장애가 밀려난다.
            // 여기 걸리는 건 검증 외 실패 전부다 — 그중 5xx 본문엔 내부 SQL 이 섞여 오므로 예외 타입만 남긴다.
            if (editorError !is AfternoteEditorError.Validation) {
                errorReporter.recordAfternoteFailure(AfternoteFailureStage.SAVE, e)
            }
            dispatch(AfternoteEditorReducerEvent.SaveFailed(editorError))
        }

        /**
         * 수신자 선택 화면이 돌려준 id 를 폼에 넣을 수 있는 값으로 해석한다.
         *
         * [refreshAuthorReceivers] 로 받아 둔 목록에 없으면 — 그 로드가 실패했다는 뜻이므로 — 한 번 더 받아 보고,
         * 그래도 못 찾으면 [AfternoteEditorError.ReceiverSelectionUnavailable] 을 세워 화면이 알리게 한다.
         * 이 신호가 없으면 사용자가 고른 수신자가 아무 표시 없이 사라진다 (#1405).
         */
        private suspend fun resolveSelectedReceiver(id: Long): AfternoteEditorReceiver? {
            findReceiverById(id)?.let { return it }
            loadAuthorReceivers()
            return findReceiverById(id) ?: run {
                dispatch(AfternoteEditorReducerEvent.ErrorRaised(AfternoteEditorError.ReceiverSelectionUnavailable))
                null
            }
        }

        /**
         * 수신자 선택 화면이 확정한 [receiverIds] 전체를 폼에 반영한다 (#1426).
         *
         * 화면은 폼의 현재 수신자를 선택 상태로 열고 확정된 전체를 돌려준다 — 그래서 반영은
         * «추가» 가 아니라 «교체» 다. 화면에서 푼 수신자는 폼에서도 빠진다.
         *
         * 이미 폼에 있는 id 는 표시에 필요한 이름·관계를 폼이 이미 들고 있으므로 재조회하지 않는다.
         * 새로 들어온 id 만 [resolveSelectedReceiver] 로 해석하고, 해석 실패는 그쪽이 오류 이벤트로
         * 알린다 — 그 id 만 빠지고 나머지 선택은 반영된다 (#1405).
         */
        private suspend fun applySelectedReceivers(receiverIds: List<Long>) {
            val alreadyInForm = currentForm().afternoteEditReceivers.associateBy { it.id }
            val next = receiverIds.mapNotNull { id -> alreadyInForm[id] ?: resolveSelectedReceiver(id) }
            currentCoroutineContext().ensureActive()
            dispatchForm(AfternoteEditorReducerEvent.SelectedReceiversApplied(next))
        }

        private fun findReceiverById(id: Long): AfternoteEditorReceiver? = currentState.authorReceivers.find { it.id == id }

        private fun AfternoteEditorUiState.withError(error: AfternoteEditorError): AfternoteEditorUiState {
            val nextOccurrence = errorOccurrence + 1L
            return copy(errorEvent = AfternoteEditorErrorEvent(error, nextOccurrence), errorOccurrence = nextOccurrence)
        }

        @AssistedFactory
        interface Factory {
            fun create(route: AfternoteRoute.EditorFlowRoute): AfternoteEditorViewModel
        }
    }

/**
 * 저장 실패를 화면이 소비할 단일 오류 상태로 좁힌다.
 *
 * [AfternoteFailure] 는 루트로 받아 `when` 을 exhaustive 하게 만든다 — 실패 유형이 늘면 여기가
 * 컴파일 에러로 잡힌다. `else` 로 뭉개 두면 새 유형이 조용히 서버 오류로 흘러간다.
 */
private fun Throwable.toAfternoteEditorError(): AfternoteEditorError =
    when (this) {
        is AfternoteFailure -> {
            when (this) {
                // 미디어 해석 실패는 사용자가 입력을 고쳐 푸는 검증 실패가 아니라 업로드 장애다.
                is AfternoteFailure.MediaSave -> {
                    AfternoteEditorError.Upload(AfternoteEditorError.Upload.Target.SAVE_MEDIA)
                }

                is AfternoteFailure.NetworkUnavailable -> {
                    AfternoteEditorError.Network
                }
            }
        }

        else -> {
            AfternoteEditorError.Server
        }
    }
