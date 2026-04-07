package com.afternote.feature.afternote.presentation.author.editor.model

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.afternote.core.ui.form.LastWishOption
import com.afternote.core.ui.scaffold.bottombar.BottomNavTab
import com.afternote.feature.afternote.presentation.author.editor.CATEGORY_MEMORIAL_GUIDELINE
import com.afternote.feature.afternote.presentation.author.editor.message.EditorMessage
import com.afternote.feature.afternote.presentation.author.editor.processing.model.AccountProcessingMethod
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodCallbacks
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodItem
import com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodManager
import com.afternote.feature.afternote.presentation.author.editor.selection.SelectionDropdownState
import com.afternote.feature.afternote.presentation.shared.DataProviderLocals
import com.afternote.feature.afternote.presentation.shared.detail.song.AlbumCover
import com.afternote.feature.afternote.presentation.shared.util.AfternoteServiceCatalog

private const val TAG = "AfternoteEditorState"
private const val CATEGORY_GALLERY_AND_FILE = "갤러리 및 파일"
private const val CUSTOM_ADD_OPTION = "직접 추가하기"
private const val LAST_WISH_DEFAULT_CALM = "차분하고 조용하게 보내주세요."
private const val LAST_WISH_DEFAULT_BRIGHT = "슬퍼 하지 말고 밝고 따뜻하게 보내주세요."

/**
 * 다이얼로그 타입
 */
enum class DialogType {
    ADD_AFTERNOTE_EDIT_RECEIVER,
    CUSTOM_SERVICE,
}

/**
 * AfternoteEditorScreen의 상태를 관리하는 State Holder
 *
 * Note: State Holder 패턴으로 인해 많은 함수가 필요합니다.
 * [com.afternote.feature.afternote.presentation.author.editor.processing.model.ProcessingMethodManager]로 처리 방법 목록 책임을 분리하여 함수 수를 20 이하로 유지합니다.
 * 추가 확장 시 AfternoteEditorReceiverManager, CategoryManager 등 분리 고려.
 */
@Stable
class AfternoteEditorState(
    // TextFieldState는 Composable에서 생성하여 전달
    val idState: TextFieldState,
    val passwordState: TextFieldState,
    val afternoteEditReceiverNameState: TextFieldState,
    val phoneNumberState: TextFieldState,
    val customServiceNameState: TextFieldState,
    initialAfternoteEditorReceivers: List<com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver>,
    albumCovers: List<AlbumCover>,
) {
    // 남기실 말씀 (multiple messages)
    val editorMessages: SnapshotStateList<EditorMessage> =
        mutableStateListOf(EditorMessage())

    fun addEditorMessage() {
        editorMessages.add(EditorMessage())
    }

    fun removeEditorMessage(message: EditorMessage) {
        if (editorMessages.size > 1) {
            editorMessages.removeAll { it.id == message.id }
        }
    }

    // Navigation
    var selectedBottomNavItem by mutableStateOf(BottomNavTab.NOTE)
        private set

    // Category & Service
    var selectedCategory by mutableStateOf("소셜네트워크")
        private set
    var selectedService by mutableStateOf(AfternoteServiceCatalog.defaultSocialService)
        private set

    // Processing Methods
    var selectedProcessingMethod by mutableStateOf(
        AccountProcessingMethod.MEMORIAL_ACCOUNT,
    )
        private set
    var selectedInformationProcessingMethod by mutableStateOf(
        com.afternote.feature.afternote.presentation.author.editor.model.InformationProcessingMethod.TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER,
    )
        private set

    // AfternoteEditorReceivers
    var afternoteEditReceivers by mutableStateOf(initialAfternoteEditorReceivers)
        private set

    // Dialog States
    var relationshipSelectedValue by mutableStateOf("친구")
        private set
    var activeDialog by mutableStateOf<DialogType?>(null)
        private set

    // Which item we last loaded (so we don't overwrite when returning from sub-route)
    var loadedItemId: String? by mutableStateOf(null)
        private set

    // Processing Method Lists (delegated to manager to keep function count under threshold)
    private val processingMethodManager = ProcessingMethodManager()
    val processingMethods: List<ProcessingMethodItem>
        get() = processingMethodManager.processingMethods
    val galleryProcessingMethods: List<ProcessingMethodItem>
        get() = processingMethodManager.galleryProcessingMethods

    // Memorial Guideline
    var selectedLastWish by mutableStateOf<String?>(null)
        private set
    var pickedMemorialPhotoUri by mutableStateOf<String?>(null)
        private set
    var funeralVideoUrl by mutableStateOf<String?>(null)
        private set

    /** Memorial video thumbnail URL when available (e.g. from API on edit or future upload). */
    var funeralThumbnailUrl by mutableStateOf<String?>(null)
        private set

    /** Memorial(PLAYLIST) only: 영정 사진 URL from API (when editing). Sent as playlist.memorialPhotoUrl on save. */
    var memorialPhotoUrl by mutableStateOf<String?>(null)
        private set
    var playlistSongCount by mutableIntStateOf(16)
        private set

    var customLastWishText by mutableStateOf("")
        private set

    // Memorial Playlist State Holder (옵셔널 - 공유 상태)
    var playlistStateHolder: MemorialPlaylistStateHolder? = null
        private set

    /**
     * 플레이리스트 상태 홀더 설정
     */
    fun setPlaylistStateHolder(stateHolder: MemorialPlaylistStateHolder) {
        playlistStateHolder = stateHolder
        // 상태 홀더가 설정되면 실제 노래 개수로 업데이트
        updatePlaylistSongCount()
    }

    /**
     * 플레이리스트 노래 개수 업데이트
     */
    fun updatePlaylistSongCount() {
        playlistSongCount = playlistStateHolder?.songs?.size ?: 16
    }

    // Dropdown States
    var categoryDropdownState by mutableStateOf(
        SelectionDropdownState(),
    )
        private set
    var serviceDropdownState by mutableStateOf(
        SelectionDropdownState(),
    )
        private set

    @Suppress("UNUSED")
    var relationshipDropdownState by mutableStateOf(
        SelectionDropdownState(),
    )
        private set

    // Constants (service names from single source: AfternoteServiceCatalog)
    val categories = listOf("소셜네트워크", CATEGORY_GALLERY_AND_FILE, "추모 가이드라인")
    val services: List<String>
        get() = AfternoteServiceCatalog.socialServices
    val galleryServices: List<String>
        get() = AfternoteServiceCatalog.galleryServices
    val relationshipOptions = listOf("친구", "가족", "연인")
    val lastWishOptions =
        listOf(
            LastWishOption(
                text = "차분하고 조용하게 보내주세요.",
                value = "calm",
            ),
            LastWishOption(
                text = "슬퍼 하지 말고 밝고 따뜻하게 보내주세요.",
                value = "bright",
            ),
            LastWishOption(
                text = "기타(직접 입력)",
                value = "other",
            ),
        )
    val playlistAlbumCovers = albumCovers

    /** 플레이리스트 앨범 커버 (라이브 상태 우선, 없으면 초기 로드값) */
    val displayAlbumCovers: List<AlbumCover>
        get() =
            playlistStateHolder?.songs?.map { s ->
                AlbumCover(id = s.id, imageUrl = s.albumCoverUrl, title = s.title)
            } ?: playlistAlbumCovers

    /** 플레이리스트 곡 수 (라이브 상태 우선) */
    val livePlaylistSongCount: Int
        get() = playlistStateHolder?.songs?.size ?: playlistSongCount

    /** 영정 사진 표시용 URI (사용자 선택 우선, 없으면 API 값) */
    val displayMemorialPhotoUri: String?
        get() = pickedMemorialPhotoUri ?: memorialPhotoUrl

    // Computed Properties (Line 295 해결: 삼항 연산자 제거)
    val currentServiceOptions: List<String>
        get() =
            if (selectedCategory == CATEGORY_GALLERY_AND_FILE) {
                galleryServices
            } else {
                services + CUSTOM_ADD_OPTION
            }

    /**
     * 선택된 서비스가 "직접 추가하기"인지 확인
     */
    fun isCustomAddOption(service: String): Boolean = service == CUSTOM_ADD_OPTION

    // Callbacks (Composable 내부 람다 제거로 인지 복잡도 최소화)
    val galleryAfternoteEditorReceiverCallbacks:
        com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiverCallbacks by lazy {
            com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiverCallbacks(
                onAddClick = ::showAddAfternoteEditorReceiverDialog,
                onItemDeleteClick = ::onAfternoteEditorReceiverDelete,
                onItemAdded = ::onAfternoteEditorReceiverItemAdded,
                onTextFieldVisibilityChanged = { _ ->
                    // 텍스트 필드 표시 상태 변경 처리
                },
            )
        }

    val galleryProcessingCallbacks: ProcessingMethodCallbacks by lazy {
        ProcessingMethodCallbacks(
            onItemDeleteClick = processingMethodManager::deleteGalleryProcessingMethod,
            onItemAdded = processingMethodManager::addGalleryProcessingMethod,
            onTextFieldVisibilityChanged = { _ ->
                // 텍스트 필드 표시 상태 변경 처리
            },
            onItemEdited = processingMethodManager::editGalleryProcessingMethod,
        )
    }

    val socialProcessingCallbacks: ProcessingMethodCallbacks by lazy {
        ProcessingMethodCallbacks(
            onItemDeleteClick = processingMethodManager::deleteProcessingMethod,
            onItemAdded = processingMethodManager::addProcessingMethod,
            onTextFieldVisibilityChanged = { _ ->
                // 텍스트 필드 표시 상태 변경 처리
            },
            onItemEdited = processingMethodManager::editProcessingMethod,
        )
    }

    // Actions (Line 279 해결: 람다 내부 중첩 조건문 제거)
    fun onCategorySelected(category: String) {
        selectedCategory = category
        // 카테고리 변경 시 서비스를 해당 카테고리의 기본값으로 초기화
        selectedService =
            if (category == CATEGORY_GALLERY_AND_FILE) {
                AfternoteServiceCatalog.defaultGalleryService
            } else {
                AfternoteServiceCatalog.defaultSocialService
            }
    }

    fun onServiceSelected(service: String) {
        if (isCustomAddOption(service)) {
            showCustomServiceDialog()
        } else {
            selectedService = service
        }
    }

    fun onProcessingMethodSelected(method: AccountProcessingMethod) {
        selectedProcessingMethod = method
    }

    fun onInformationProcessingMethodSelected(
        method: com.afternote.feature.afternote.presentation.author.editor.model.InformationProcessingMethod,
    ) {
        selectedInformationProcessingMethod = method
    }

    fun onLastWishSelected(wish: String?) {
        selectedLastWish = wish
    }

    fun onCustomLastWishChanged(text: String) {
        customLastWishText = text
    }

    /**
     * Resolves the "남기고 싶은 당부" value to send as playlist.atmosphere (Memorial only).
     */
    fun getAtmosphereForSave(): String =
        when (selectedLastWish) {
            "calm" -> LAST_WISH_DEFAULT_CALM
            "bright" -> LAST_WISH_DEFAULT_BRIGHT
            "other" -> customLastWishText.trim()
            else -> ""
        }

    /**
     * 영정사진 선택 시 호출 (갤러리 등에서 선택한 URI 저장).
     */
    fun onMemorialPhotoSelected(uri: Uri?) {
        pickedMemorialPhotoUri = uri?.toString()
    }

    /**
     * 추모 영상 선택 시 호출 (갤러리 등에서 선택한 URI 저장).
     * 새 영상 선택 시 썸네일 URL은 초기화 (FuneralVideoUpload가 생성 후 콜백으로 설정).
     */
    fun onFuneralVideoSelected(uri: Uri?) {
        funeralVideoUrl = uri?.toString()
        funeralThumbnailUrl = null
    }

    /**
     * 장례식 영상 썸네일이 준비되면 호출 (앱이 생성한 프레임을 data URL로 인코딩한 값).
     */
    fun onFuneralThumbnailDataUrlReady(dataUrl: String?) {
        funeralThumbnailUrl = dataUrl
    }

    fun showAddAfternoteEditorReceiverDialog() {
        activeDialog = DialogType.ADD_AFTERNOTE_EDIT_RECEIVER
    }

    fun showCustomServiceDialog() {
        activeDialog = DialogType.CUSTOM_SERVICE
    }

    fun dismissDialog() {
        activeDialog = null
        afternoteEditReceiverNameState.edit { replace(0, length, "") }
        phoneNumberState.edit { replace(0, length, "") }
        customServiceNameState.edit { replace(0, length, "") }
        relationshipSelectedValue = "친구"
    }

    fun onAddCustomService() {
        val serviceName = customServiceNameState.text.toString().trim()
        if (serviceName.isEmpty()) return

        selectedService = serviceName
        dismissDialog()
    }

    // Line 350 해결: Guard Clause로 중첩 줄이기
    fun onAddAfternoteEditorReceiver() {
        val name = afternoteEditReceiverNameState.text.toString().trim()
        if (name.isEmpty()) return

        val newAfternoteEditorReceiver =
            com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver(
                id = (afternoteEditReceivers.size + 1).toString(),
                name = name,
                label = relationshipSelectedValue,
            )
        afternoteEditReceivers = afternoteEditReceivers + newAfternoteEditorReceiver
        dismissDialog()
    }

    fun onRelationshipSelected(relationship: String) {
        relationshipSelectedValue = relationship
    }

    fun onAfternoteEditorReceiverDelete(afternoteEditReceiverId: String) {
        afternoteEditReceivers = afternoteEditReceivers.filter { it.id != afternoteEditReceiverId }
    }

    /**
     * Adds a receiver from the receiver selection screen (e.g. Time Letter 수신자 목록).
     * Call when returning from navigation with a selected receiver.
     */
    fun addReceiverFromSelection(
        receiverId: Long,
        name: String,
        relation: String,
    ) {
        if (afternoteEditReceivers.any { it.id == receiverId.toString() }) return
        val newReceiver =
            com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver(
                id = receiverId.toString(),
                name = name,
                label = relation,
            )
        afternoteEditReceivers = afternoteEditReceivers + newReceiver
    }

    fun onAfternoteEditorReceiverItemAdded(text: String) {
        val newAfternoteEditorReceiver =
            com.afternote.feature.afternote.presentation.author.editor.model.AfternoteEditorReceiver(
                id = (afternoteEditReceivers.size + 1).toString(),
                name = text,
                label = "친구",
            )
        afternoteEditReceivers = afternoteEditReceivers + newAfternoteEditorReceiver
    }

    fun onBottomNavItemSelected(item: BottomNavTab) {
        selectedBottomNavItem = item
    }

    /**
     * Pre-fill state from an existing afternote item (when editing).
     * Sets category, service, text fields, and processing method lists.
     * `params.itemId` is stored so we do not overwrite the form when returning from a sub-route.
     */
    fun loadFromExisting(params: LoadFromExistingParams) {
        Log.d(
            TAG,
            "loadFromExisting: itemId=${params.itemId}, serviceName=${params.serviceName}, " +
                "category=${params.categoryDisplayString}, " +
                "accountPM=${params.processing.accountMethodName}, infoPM=${params.processing.informationMethodName}, " +
                "processingMethods=${params.processing.methods.size}, " +
                "galleryProcessingMethods=${params.processing.galleryMethods.size}",
        )
        loadedItemId = params.itemId
        selectedService = params.serviceName
        selectedCategory = params.categoryDisplayString

        idState.edit { replace(0, length, params.account.id) }
        passwordState.edit { replace(0, length, params.account.password) }
        // 기존 메시지를 첫 번째 EditorMessage에 로드
        val firstMessage = editorMessages.firstOrNull() ?: EditorMessage().also { editorMessages.add(it) }
        firstMessage.contentState.edit { replace(0, length, params.processing.message) }

        if (params.processing.accountMethodName.isNotEmpty()) {
            selectedProcessingMethod =
                runCatching {
                    AccountProcessingMethod.valueOf(
                        params.processing.accountMethodName,
                    )
                }.getOrDefault(
                    AccountProcessingMethod.MEMORIAL_ACCOUNT,
                )
        }

        if (params.processing.informationMethodName.isNotEmpty()) {
            val infoMethodName =
                when (params.processing.informationMethodName) {
                    "TRANSFER_TO_ADDITIONAL_AFTERNOTE_EDIT_RECEIVER",
                    "ADDITIONAL",
                    -> "TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER"

                    else -> params.processing.informationMethodName
                }
            selectedInformationProcessingMethod =
                runCatching {
                    InformationProcessingMethod.valueOf(infoMethodName)
                }.getOrDefault(InformationProcessingMethod.TRANSFER_TO_AFTERNOTE_EDIT_RECEIVER)
        }

        processingMethodManager.replaceProcessingMethods(params.processing.methods)
        processingMethodManager.replaceGalleryProcessingMethods(params.processing.galleryMethods)

        // Memorial only: when atmosphere does not match a default option, select "기타(직접 입력)" and show saved text.
        params.atmosphere?.let { atmosphereValue ->
            val trimmed = atmosphereValue.trim()
            when {
                trimmed.isEmpty() -> {
                    selectedLastWish = null
                    customLastWishText = ""
                }

                trimmed == LAST_WISH_DEFAULT_CALM -> {
                    selectedLastWish = "calm"
                    customLastWishText = ""
                }

                trimmed == LAST_WISH_DEFAULT_BRIGHT -> {
                    selectedLastWish = "bright"
                    customLastWishText = ""
                }

                else -> {
                    selectedLastWish = "other"
                    customLastWishText = trimmed
                }
            }
        }
        // Memorial only: 장례식에 남길 영상 URL and thumbnail from API.
        funeralVideoUrl = params.memorialVideoUrl
        funeralThumbnailUrl = params.memorialThumbnailUrl
        memorialPhotoUrl = params.memorialPhotoUrl
    }

    /**
     * 등록 버튼 클릭 시 [RegisterAfternotePayload]를 생성한다.
     */
    fun createRegisterPayload(): RegisterAfternotePayload {
        val date =
            java.time.LocalDate
                .now()
                .format(
                    java.time.format.DateTimeFormatter
                        .ofPattern("yyyy.MM.dd"),
                )
        val socialMethods =
            processingMethods.map {
                com.afternote.feature.afternote.domain.model
                    .ProcessingMethod(it.id, it.text)
            }
        val galleryMethods =
            galleryProcessingMethods.map {
                com.afternote.feature.afternote.domain.model
                    .ProcessingMethod(it.id, it.text)
            }
        val fullMessage =
            editorMessages
                .map { msg ->
                    val t = msg.titleState.text.toString()
                    val c = msg.contentState.text.toString()
                    if (t.isNotEmpty()) "$t\n$c" else c
                }.filter { it.isNotBlank() }
                .joinToString("\n---\n")

        return RegisterAfternotePayload(
            serviceName =
                if (selectedCategory == CATEGORY_MEMORIAL_GUIDELINE) {
                    CATEGORY_MEMORIAL_GUIDELINE
                } else {
                    selectedService
                },
            date = date,
            accountId = idState.text.toString(),
            password = passwordState.text.toString(),
            message = fullMessage,
            accountProcessingMethod = selectedProcessingMethod.name,
            informationProcessingMethod = selectedInformationProcessingMethod.name,
            processingMethods = socialMethods,
            galleryProcessingMethods = galleryMethods,
            atmosphere = getAtmosphereForSave(),
        )
    }
}

/**
 * Parameters for pre-filling edit state from an existing afternote item.
 *
 * @param categoryDisplayString Edit-screen category dropdown value (e.g. "갤러리 및 파일").
 *        Must come from API [detail.category], not inferred from title, so Gallery processMethod loads.
 * @param atmosphere Memorial(PLAYLIST) only: playlist.atmosphere for "남기고 싶은 당부". When non-null and
 *        not matching a default option, edit screen selects "기타(직접 입력)" and shows this text.
 * @param memorialVideoUrl Memorial(PLAYLIST) only: playlist memorial video URL from API (장례식에 남길 영상).
 * @param memorialThumbnailUrl Memorial(PLAYLIST) only: playlist memorial thumbnail URL from API.
 * @param memorialPhotoUrl Memorial(PLAYLIST) only: playlist 영정 사진 URL from API.
 */
data class LoadFromExistingParams(
    val itemId: String,
    val serviceName: String,
    val categoryDisplayString: String,
    val account: LoadFromExistingAccountParams = LoadFromExistingAccountParams(),
    val processing: LoadFromExistingProcessingParams = LoadFromExistingProcessingParams(),
    val atmosphere: String? = null,
    val memorialVideoUrl: String? = null,
    val memorialThumbnailUrl: String? = null,
    val memorialPhotoUrl: String? = null,
)

data class LoadFromExistingAccountParams(
    val id: String = "",
    val password: String = "",
)

data class LoadFromExistingProcessingParams(
    val message: String = "",
    val accountMethodName: String = "",
    val informationMethodName: String = "",
    val methods: List<ProcessingMethodItem> = emptyList(),
    val galleryMethods: List<ProcessingMethodItem> = emptyList(),
)

@Composable
fun rememberAfternoteEditorState(): AfternoteEditorState {
    val afternoteProvider = DataProviderLocals.LocalAfternoteEditorDataProvider.current
    val idState = rememberTextFieldState()
    val passwordState = rememberTextFieldState()
    val afternoteEditReceiverNameState = rememberTextFieldState()
    val phoneNumberState = rememberTextFieldState()
    val customServiceNameState = rememberTextFieldState()

    return remember(afternoteProvider) {
        AfternoteEditorState(
            idState = idState,
            passwordState = passwordState,
            afternoteEditReceiverNameState = afternoteEditReceiverNameState,
            phoneNumberState = phoneNumberState,
            customServiceNameState = customServiceNameState,
            initialAfternoteEditorReceivers = afternoteProvider.getAfternoteEditorReceivers(),
            albumCovers = afternoteProvider.getAlbumCovers(),
        )
    }
}
