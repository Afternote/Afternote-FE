package com.afternote.feature.afternote.presentation.editor.memorial

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
import com.afternote.feature.afternote.domain.repository.author.MemorialAudioFormats
import com.afternote.feature.afternote.presentation.R
import kotlinx.coroutines.launch
import java.io.IOException

/** 사진 촬영 결과 파일 확장자. `image/jpeg` 로 역산되어 presigned 발급 확장자가 된다. */
private const val PHOTO_CAPTURE_EXTENSION = "jpg"

/** 영상 촬영 결과 파일 확장자. `video/mp4` 로 역산된다. */
private const val VIDEO_CAPTURE_EXTENSION = "mp4"

/**
 * 추억 노트 미디어 슬롯의 "어디서 가져올지" 상태 + 갤러리·촬영·녹음 인텐트 발사구 (#369, 음성은 #1118).
 *
 * 시트 표시 여부는 [target] 하나로 표현한다 — non-null 이면 그 슬롯의 시트가 떠 있고, null 이면 닫혀 있다.
 * 별도 boolean 을 두지 않아 "떠 있는데 어느 슬롯인지 모르는" 상태가 애초에 만들어지지 않는다.
 */
@Stable
internal class MemorialMediaSourceState(
    private val openTarget: MutableState<MemorialMediaTarget?>,
    private val onPickFromGallery: (MemorialMediaTarget) -> Unit,
    private val onCapture: (MemorialMediaTarget) -> Unit,
    private val onRemove: (MemorialMediaTarget) -> Unit,
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

    /** 슬롯에 현재 표시된 로컬 또는 서버 첨부 삭제 (#1114, #1597). 시트를 닫고 넘긴다. */
    fun remove() {
        consumeTarget()?.let(onRemove)
    }

    /** 인텐트를 쏘기 전에 시트를 닫는다 — 결과를 들고 돌아왔을 때 시트가 남아 있으면 화면을 가린다. */
    private fun consumeTarget(): MemorialMediaTarget? = target?.also { dismiss() }
}

/**
 * [MemorialMediaSourceState] 와 그것이 쏘는 6종 런처(갤러리 사진·갤러리 영상·사진 촬영·영상 촬영·
 * 음성 파일 선택·음성 녹음)를 만든다.
 *
 * 촬영 인텐트(`ACTION_IMAGE_CAPTURE`·`ACTION_VIDEO_CAPTURE`)는 결과를 우리가 지정한 URI 에 써 넣고
 * 성공 여부만 boolean 으로 돌려준다. 그래서 "어느 파일에 쓰라고 했는지" 를 결과가 올 때까지 들고 있어야
 * 하는데, 촬영 중 프로세스가 죽어도 복원되도록 [rememberSaveable] 로 둔다 — 카메라 앱이 전면에 있는 동안은
 * 우리 프로세스가 회수되기 쉬운 구간이다. **녹음(`RECORD_SOUND_ACTION`)은 계약이 반대**라 그 보관이
 * 없다 — 녹음 앱이 만든 URI 를 결과로 돌려준다 ([RecordSoundContract]).
 *
 * 카메라 권한을 요청하지 않는 것은 의도다. 매니페스트에 `android.permission.CAMERA` 를 선언하지 않은 앱이
 * 이 인텐트를 쏠 때 Android 는 런타임 권한을 요구하지 않는다 — 촬영은 카메라 앱의 프로세스에서 일어나고
 * 우리는 결과 파일만 받는다. 반대로 선언해 두면 그 순간부터 런타임 권한이 *필수* 가 된다.
 * `RECORD_AUDIO` 도 같은 이유로 선언하지 않는다 — 녹음은 녹음 앱의 프로세스에서 일어난다.
 *
 * @param onPhotoSelected 영정사진 확정 URI. 취소·실패에는 호출되지 않는다 — 슬롯의 기존 값이 그대로 남는다.
 * @param onVideoSelected 장례식에 남길 영상 확정 URI. 위와 같다.
 * @param onAudioSelected 추모 음성 확정 URI (#1118). 서버가 받는 형식일 때만 호출된다.
 * @param onPhotoRemoved 시트의 삭제 항목으로 현재 표시된 영정사진을 지웠을 때 (#1114, #1597).
 * @param onVideoRemoved 위와 같다 — 장례식에 남길 영상.
 * @param onAudioRemoved 위와 같다 — 추모 음성.
 * @param onCaptureFailed 촬영·녹음 인텐트를 띄우지 못한 사유. 화면 문구는 갈래가 같아 사유가 지워지므로
 *   호출처가 텔레메트리로 남긴다.
 */
@Composable
internal fun rememberMemorialMediaSourceState(
    snackbarHostState: SnackbarHostState,
    onPhotoSelected: (String) -> Unit,
    onVideoSelected: (String) -> Unit,
    onAudioSelected: (String) -> Unit,
    onPhotoRemoved: () -> Unit,
    onVideoRemoved: () -> Unit,
    onAudioRemoved: () -> Unit,
    onCaptureFailed: (Throwable) -> Unit,
): MemorialMediaSourceState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val captureUnavailableMessage = stringResource(R.string.afternote_editor_media_capture_unavailable)
    val recordUnavailableMessage = stringResource(R.string.afternote_editor_media_record_unavailable)
    val audioPickUnavailableMessage = stringResource(R.string.afternote_editor_media_audio_pick_unavailable)
    val audioUnsupportedMessage = stringResource(R.string.afternote_editor_media_audio_unsupported)

    val openTarget = rememberSaveable { mutableStateOf<MemorialMediaTarget?>(null) }
    val pendingPhotoCapture = rememberSaveable { mutableStateOf<String?>(null) }
    val pendingVideoCapture = rememberSaveable { mutableStateOf<String?>(null) }

    // 고른·녹음한 음성이 서버가 받는 형식일 때만 슬롯에 넣는다. 여기서 막지 않으면 업로드까지 가서
    // 저장 단계에 서버가 400(INVALID_FILE_EXTENSION) 을 내므로, 사용자는 "저장 실패" 만 보게 된다.
    val acceptAudio: (Uri?) -> Unit = { uri ->
        when {
            uri == null -> {
                Unit
            }

            MemorialAudioFormats.extensionFor(context.contentResolver.getType(uri)) != null -> {
                onAudioSelected(uri.toString())
            }

            else -> {
                scope.launch { snackbarHostState.showSnackbar(audioUnsupportedMessage) }
            }
        }
    }

    val photoGalleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { onPhotoSelected(it.toString()) }
        }
    val videoGalleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { onVideoSelected(it.toString()) }
        }
    // 음성은 사진 선택기(PickVisualMedia)의 대상이 아니다 — 문서 선택기(SAF)로 고른다.
    val audioPickLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), acceptAudio)
    val audioRecordLauncher = rememberLauncherForActivityResult(RecordSoundContract(), acceptAudio)
    val photoCaptureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured: Boolean ->
            val uri = pendingPhotoCapture.value?.toUri()
            pendingPhotoCapture.value = null
            when {
                uri == null -> Unit
                captured -> onPhotoSelected(uri.toString())
                else -> discardMemorialCapture(context, uri)
            }
        }
    val videoCaptureLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { captured: Boolean ->
            val uri = pendingVideoCapture.value?.toUri()
            pendingVideoCapture.value = null
            when {
                uri == null -> Unit
                captured -> onVideoSelected(uri.toString())
                else -> discardMemorialCapture(context, uri)
            }
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

                    MemorialMediaTarget.AUDIO -> {
                        launchOrReport(
                            launch = { audioPickLauncher.launch(MemorialAudioFormats.supportedMimeTypes.toTypedArray()) },
                        ) { failure ->
                            onCaptureFailed(failure)
                            scope.launch { snackbarHostState.showSnackbar(audioPickUnavailableMessage) }
                        }
                    }
                }
            },
            onCapture = { target ->
                when (target) {
                    // 녹음은 출력 URI 를 미리 만들지 않는다 — 결과 URI 를 인텐트가 돌려준다.
                    MemorialMediaTarget.AUDIO -> {
                        launchOrReport(launch = { audioRecordLauncher.launch(Unit) }) { failure ->
                            onCaptureFailed(failure)
                            scope.launch { snackbarHostState.showSnackbar(recordUnavailableMessage) }
                        }
                    }

                    MemorialMediaTarget.PHOTO, MemorialMediaTarget.VIDEO -> {
                        val pending =
                            when (target) {
                                MemorialMediaTarget.PHOTO -> pendingPhotoCapture
                                else -> pendingVideoCapture
                            }
                        val launch: (Uri) -> Unit =
                            when (target) {
                                MemorialMediaTarget.PHOTO -> photoCaptureLauncher::launch
                                else -> videoCaptureLauncher::launch
                            }
                        val extension =
                            when (target) {
                                MemorialMediaTarget.PHOTO -> PHOTO_CAPTURE_EXTENSION
                                else -> VIDEO_CAPTURE_EXTENSION
                            }
                        launchCapture(context, pending, extension, launch) { failure ->
                            onCaptureFailed(failure)
                            scope.launch { snackbarHostState.showSnackbar(captureUnavailableMessage) }
                        }
                    }
                }
            },
            onRemove = { target ->
                when (target) {
                    MemorialMediaTarget.PHOTO -> onPhotoRemoved()
                    MemorialMediaTarget.VIDEO -> onVideoRemoved()
                    MemorialMediaTarget.AUDIO -> onAudioRemoved()
                }
            },
        )
    }
}

/**
 * 결과 파일을 만들고 촬영 인텐트를 쏜다. 실패하면 만들다 만 파일을 되돌리고 [onUnavailable] 로 알린다.
 *
 * 예외를 통째로 넘기는 이유: 사용자에게 나가는 문구는 두 갈래가 같아도, 제보가 왔을 때 어느 쪽인지
 * 가르려면 사유가 남아 있어야 한다. 문구는 호출부가 만들고 기록은 호출부가 위로 올린다.
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
    onUnavailable: (Throwable) -> Unit,
) {
    val uri =
        try {
            createMemorialCaptureUri(context, extension)
        } catch (e: IOException) {
            onUnavailable(e)
            return
        }
    pending.value = uri.toString()
    try {
        launch(uri)
    } catch (e: ActivityNotFoundException) {
        pending.value = null
        discardMemorialCapture(context, uri)
        onUnavailable(e)
    }
}

/**
 * 출력 파일이 필요 없는 인텐트(녹음·문서 선택)를 쏘고, 받아 줄 앱이 없으면 [onUnavailable] 로 알린다 (#1118).
 *
 * 되돌릴 임시 파일이 없다는 점만 [launchCapture] 와 다르다. 조용히 삼키면 눌러도 아무 일이 없는 항목이 된다.
 */
private inline fun launchOrReport(
    launch: () -> Unit,
    onUnavailable: (Throwable) -> Unit,
) {
    try {
        launch()
    } catch (e: ActivityNotFoundException) {
        onUnavailable(e)
    }
}

/**
 * [MemorialMediaSourceState.target] 이 정해져 있을 때만 뜨는 소스 선택 시트.
 *
 * `ModalBottomSheet` 자체를 여기서 감싸 두어, 호출 화면은 이 컴포저블 한 줄만 두면 된다.
 *
 * @param removableTargets 삭제 항목을 노출할 슬롯 집합 — 호출 화면이 최신 폼으로
 *   [removableMemorialMediaTargets] 를 계산해 넘긴다. 상태 객체는 remember 로 한 번 만들어져
 *   폼 변화를 못 보므로, 폼 파생 값은 매 컴포지션 파라미터로 흐른다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemorialMediaSourceSheet(
    state: MemorialMediaSourceState,
    removableTargets: Set<MemorialMediaTarget>,
) {
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
            onRemove = if (target in removableTargets) state::remove else null,
        )
    }
}
