package com.afternote.feature.afternote.presentation.author.editor.memorial

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import kotlinx.coroutines.launch
import java.io.IOException

/** 사진 촬영 결과 파일 확장자. `image/jpeg` 로 역산되어 presigned 발급 확장자가 된다. */
private const val PHOTO_CAPTURE_EXTENSION = "jpg"

/** 영상 촬영 결과 파일 확장자. `video/mp4` 로 역산된다. */
private const val VIDEO_CAPTURE_EXTENSION = "mp4"

/**
 * 추억 노트 미디어 슬롯의 "어디서 가져올지" 상태 + 갤러리·촬영 인텐트 발사구 (#369).
 *
 * 시트 표시 여부는 [target] 하나로 표현한다 — non-null 이면 그 슬롯의 시트가 떠 있고, null 이면 닫혀 있다.
 * 별도 boolean 을 두지 않아 "떠 있는데 어느 슬롯인지 모르는" 상태가 애초에 만들어지지 않는다.
 */
@Stable
internal class MemorialMediaSourceState(
    private val openTarget: MutableState<MemorialMediaTarget?>,
    private val onPickFromGallery: (MemorialMediaTarget) -> Unit,
    private val onCapture: (MemorialMediaTarget) -> Unit,
) {
    /** 시트가 열려 있는 슬롯. null 이면 시트가 닫혀 있다. */
    val target: MemorialMediaTarget? get() = openTarget.value

    fun open(target: MemorialMediaTarget) {
        openTarget.value = target
    }

    fun dismiss() {
        openTarget.value = null
    }

    fun pickFromGallery() {
        consumeTarget()?.let(onPickFromGallery)
    }

    fun capture() {
        consumeTarget()?.let(onCapture)
    }

    /** 인텐트를 쏘기 전에 시트를 닫는다 — 결과를 들고 돌아왔을 때 시트가 남아 있으면 화면을 가린다. */
    private fun consumeTarget(): MemorialMediaTarget? = target?.also { dismiss() }
}

/**
 * [MemorialMediaSourceState] 와 그것이 쏘는 4종 런처(갤러리 사진·갤러리 영상·사진 촬영·영상 촬영)를 만든다.
 *
 * 촬영 인텐트(`ACTION_IMAGE_CAPTURE`·`ACTION_VIDEO_CAPTURE`)는 결과를 우리가 지정한 URI 에 써 넣고
 * 성공 여부만 boolean 으로 돌려준다. 그래서 "어느 파일에 쓰라고 했는지" 를 결과가 올 때까지 들고 있어야
 * 하는데, 촬영 중 프로세스가 죽어도 복원되도록 [rememberSaveable] 로 둔다 — 카메라 앱이 전면에 있는 동안은
 * 우리 프로세스가 회수되기 쉬운 구간이다.
 *
 * 카메라 권한을 요청하지 않는 것은 의도다. 매니페스트에 `android.permission.CAMERA` 를 선언하지 않은 앱이
 * 이 인텐트를 쏠 때 Android 는 런타임 권한을 요구하지 않는다 — 촬영은 카메라 앱의 프로세스에서 일어나고
 * 우리는 결과 파일만 받는다. 반대로 선언해 두면 그 순간부터 런타임 권한이 *필수* 가 된다.
 *
 * @param onPhotoSelected 영정사진 확정 URI. 취소·실패에는 호출되지 않는다 — 슬롯의 기존 값이 그대로 남는다.
 * @param onVideoSelected 장례식에 남길 영상 확정 URI. 위와 같다.
 */
@Composable
internal fun rememberMemorialMediaSourceState(
    snackbarHostState: SnackbarHostState,
    onPhotoSelected: (String) -> Unit,
    onVideoSelected: (String) -> Unit,
): MemorialMediaSourceState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val captureUnavailableMessage = stringResource(R.string.afternote_editor_media_capture_unavailable)

    val openTarget = rememberSaveable { mutableStateOf<MemorialMediaTarget?>(null) }
    val pendingPhotoCapture = rememberSaveable { mutableStateOf<String?>(null) }
    val pendingVideoCapture = rememberSaveable { mutableStateOf<String?>(null) }

    val photoGalleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { onPhotoSelected(it.toString()) }
        }
    val videoGalleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { onVideoSelected(it.toString()) }
        }
    val photoCaptureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured: Boolean ->
            val uri = pendingPhotoCapture.value?.toUri()
            pendingPhotoCapture.value = null
            if (captured && uri != null) onPhotoSelected(uri.toString()) else discardMemorialCapture(context, uri)
        }
    val videoCaptureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { captured: Boolean ->
            val uri = pendingVideoCapture.value?.toUri()
            pendingVideoCapture.value = null
            if (captured && uri != null) onVideoSelected(uri.toString()) else discardMemorialCapture(context, uri)
        }

    return remember(openTarget) {
        MemorialMediaSourceState(
            openTarget = openTarget,
            onPickFromGallery = { target ->
                when (target) {
                    MemorialMediaTarget.PHOTO -> {
                        photoGalleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }

                    MemorialMediaTarget.VIDEO -> {
                        videoGalleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                        )
                    }
                }
            },
            onCapture = { target ->
                val pending =
                    when (target) {
                        MemorialMediaTarget.PHOTO -> pendingPhotoCapture
                        MemorialMediaTarget.VIDEO -> pendingVideoCapture
                    }
                val launch: (Uri) -> Unit =
                    when (target) {
                        MemorialMediaTarget.PHOTO -> photoCaptureLauncher::launch
                        MemorialMediaTarget.VIDEO -> videoCaptureLauncher::launch
                    }
                val extension =
                    when (target) {
                        MemorialMediaTarget.PHOTO -> PHOTO_CAPTURE_EXTENSION
                        MemorialMediaTarget.VIDEO -> VIDEO_CAPTURE_EXTENSION
                    }
                launchCapture(context, pending, extension, launch) {
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
 * ([ActivityNotFoundException] — 카메라 없는 기기·에뮬레이터). 둘 다 "지금은 촬영할 수 없다" 로 같은 안내를
 * 준다. 조용히 삼키면 눌러도 아무 일이 없는 버튼이 된다.
 */
private inline fun launchCapture(
    context: Context,
    pending: MutableState<String?>,
    extension: String,
    launch: (Uri) -> Unit,
    onUnavailable: () -> Unit,
) {
    val uri =
        try {
            createMemorialCaptureUri(context, extension)
        } catch (_: IOException) {
            onUnavailable()
            return
        }
    pending.value = uri.toString()
    try {
        launch(uri)
    } catch (_: ActivityNotFoundException) {
        pending.value = null
        discardMemorialCapture(context, uri)
        onUnavailable()
    }
}

/**
 * [MemorialMediaSourceState.target] 이 정해져 있을 때만 뜨는 소스 선택 시트.
 *
 * `ModalBottomSheet` 자체를 여기서 감싸 두어, 호출 화면은 이 컴포저블 한 줄만 두면 된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemorialMediaSourceSheet(state: MemorialMediaSourceState) {
    val target = state.target ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = state::dismiss,
        sheetState = sheetState,
        containerColor = AfternoteDesign.colors.gray1,
        dragHandle = null,
    ) {
        MemorialMediaSourceBottomSheet(
            target = target,
            onPickFromGallery = state::pickFromGallery,
            onCapture = state::capture,
        )
    }
}
