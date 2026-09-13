package com.afternote.feature.setting.presentation.component

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.afternote.core.ui.sheet.MediaSelectBottomSheet
import com.afternote.core.ui.sheet.MediaSheetItem
import com.afternote.feature.setting.presentation.R
import kotlinx.coroutines.launch
import java.io.IOException
import com.afternote.core.ui.R as CoreUiR

/**
 * 프로필 사진을 「어디서 가져올지」 상태 + 갤러리·촬영 인텐트 발사구 (#1438).
 *
 * 추억 노트의 같은 자리(`MemorialMediaSourceState`)와 구조가 같다. 다른 점은 슬롯이 하나뿐이라
 * 열림 여부가 boolean 이고, 삭제 갈래가 없다는 것뿐이다 — 사진 지우기는 이 이슈 범위가 아니다.
 */
@Stable
internal class ProfilePhotoSourceState(
    private val isSheetOpen: MutableState<Boolean>,
    private val onPickFromGallery: () -> Unit,
    private val onCapture: () -> Unit,
) {
    val isOpen: Boolean get() = isSheetOpen.value

    fun open() {
        isSheetOpen.value = true
    }

    fun dismiss() {
        isSheetOpen.value = false
    }

    fun pickFromGallery() {
        consume(onPickFromGallery)
    }

    fun capture() {
        consume(onCapture)
    }

    /** 인텐트를 쏘기 전에 시트를 닫는다 — 결과를 들고 돌아왔을 때 시트가 남아 있으면 화면을 가린다. */
    private inline fun consume(launch: () -> Unit) {
        if (!isOpen) return
        dismiss()
        launch()
    }
}

/**
 * [ProfilePhotoSourceState] 와 그것이 쏘는 두 런처(갤러리 선택·사진 촬영)를 만든다.
 *
 * 촬영 인텐트(`ACTION_IMAGE_CAPTURE`)는 결과를 우리가 지정한 URI 에 써 넣고 성공 여부만 boolean 으로
 * 돌려준다. 그래서 「어느 파일에 쓰라고 했는지」를 결과가 올 때까지 들고 있어야 하는데, 촬영 중
 * 프로세스가 죽어도 복원되도록 [rememberSaveable] 로 둔다 — 카메라 앱이 전면에 있는 동안은 우리
 * 프로세스가 회수되기 쉬운 구간이다. 같은 이유로 이 함수는 화면의 `when` 분기 **밖**에서 불러야 한다.
 *
 * 카메라 권한을 요청하지 않는 것은 의도다. 매니페스트에 `android.permission.CAMERA` 를 선언하지 않은
 * 앱이 이 인텐트를 쏠 때 Android 는 런타임 권한을 요구하지 않는다 — 촬영은 카메라 앱의 프로세스에서
 * 일어나고 우리는 결과 파일만 받는다. 반대로 선언해 두면 그 순간부터 런타임 권한이 *필수* 가 된다.
 *
 * @param onPhotoSelected 확정된 사진의 URI. 갤러리 취소·촬영 취소·촬영 실행 실패에는 **불리지 않는다** —
 *   그 갈래에서는 화면에 있던 사진이 그대로 남는다.
 * @param isProfileReady 프로필 조회가 끝났고 저장 중이 아니어서 선택을 받을 수 있는 상태.
 */
@Composable
internal fun rememberProfilePhotoSourceState(
    snackbarHostState: SnackbarHostState,
    isProfileReady: Boolean,
    onPhotoSelected: (String) -> Unit,
): ProfilePhotoSourceState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val captureUnavailableMessage = stringResource(R.string.setting_profile_capture_unavailable)

    val isSheetOpen = rememberSaveable { mutableStateOf(false) }
    val pendingCapture = rememberSaveable { mutableStateOf<String?>(null) }
    // 복원된 결과가 새 ViewModel의 프로필 조회보다 먼저 와도, 선택을 받을 때까지 보관한다.
    val queuedPhoto = rememberSaveable { mutableStateOf<String?>(null) }
    val currentOnPhotoSelected = rememberUpdatedState(onPhotoSelected)

    LaunchedEffect(queuedPhoto.value, isProfileReady) {
        val uri = queuedPhoto.value
        if (uri != null && isProfileReady) {
            queuedPhoto.value = null
            currentOnPhotoSelected.value(uri)
        }
    }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            // 취소 결과(null)는 선택 변경이 아니다 — 기존 사진을 그대로 둔다. 온보딩이 #1113·#1115 로
            // 같은 함정을 밟고 잡아 둔 처리를 그대로 따른다.
            uri?.let { queuedPhoto.value = it.toString() }
        }
    val captureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured: Boolean ->
            val uri = pendingCapture.value?.toUri()
            pendingCapture.value = null
            when {
                // 대기 중인 결과가 없으면 우리가 띄운 촬영이 아니다.
                uri == null -> Unit

                captured -> queuedPhoto.value = uri.toString()

                // 취소는 우리가 만든 빈 파일만 치운다. 사진은 건드리지 않는다.
                else -> discardProfileCapture(context, uri)
            }
        }

    return remember(isSheetOpen) {
        ProfilePhotoSourceState(
            isSheetOpen = isSheetOpen,
            onPickFromGallery = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onCapture = {
                launchProfileCapture(context, pendingCapture, captureLauncher::launch) {
                    scope.launch { snackbarHostState.showSnackbar(captureUnavailableMessage) }
                }
            },
        )
    }
}

/**
 * 결과 파일을 만들고 촬영 인텐트를 쏜다. 실패하면 만들다 만 파일을 되돌리고 [onUnavailable] 로 알린다.
 *
 * 실패 갈래는 둘뿐이다 — 캐시에 파일을 못 만들거나(저장공간), 촬영을 받아 줄 앱이 없거나
 * ([ActivityNotFoundException] — 카메라 없는 기기·에뮬레이터). 둘 다 「지금은 촬영할 수 없다」로 같은
 * 안내를 준다. 조용히 삼키면 눌러도 아무 일이 없는 항목이 된다.
 */
private inline fun launchProfileCapture(
    context: Context,
    pending: MutableState<String?>,
    launch: (Uri) -> Unit,
    onUnavailable: () -> Unit,
) {
    val uri =
        try {
            createProfileCaptureUri(context)
        } catch (_: IOException) {
            onUnavailable()
            return
        }
    pending.value = uri.toString()
    try {
        launch(uri)
    } catch (_: ActivityNotFoundException) {
        pending.value = null
        discardProfileCapture(context, uri)
        onUnavailable()
    }
}

/**
 * [ProfilePhotoSourceState.isOpen] 일 때만 뜨는 소스 선택 시트 — 「갤러리에서 선택」 / 「사진 촬영」.
 *
 * 시각 규격은 공용 [MediaSelectBottomSheet] 가 정한다(시안 정본 4327:72281). 여기 남는 건 이 화면만의
 * 것뿐이다 — 두 갈래의 아이콘과 문구. 머리글은 공용 기본값("미디어 추가하기")을 그대로 쓴다.
 *
 * 공용 래퍼가 기본값으로 들고 있는 `SheetState` 가 아직 실험 API 라 호출부도 같이 표시한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfilePhotoSourceSheet(state: ProfilePhotoSourceState) {
    if (!state.isOpen) return
    MediaSelectBottomSheet(
        onDismiss = state::dismiss,
        items =
            listOf(
                MediaSheetItem(
                    iconRes = CoreUiR.drawable.core_ui_ic_image,
                    label = stringResource(R.string.setting_profile_photo_gallery),
                    onClick = state::pickFromGallery,
                ),
                MediaSheetItem(
                    iconRes = R.drawable.setting_ic_camera,
                    label = stringResource(R.string.setting_profile_photo_take),
                    onClick = state::capture,
                ),
            ),
    )
}
