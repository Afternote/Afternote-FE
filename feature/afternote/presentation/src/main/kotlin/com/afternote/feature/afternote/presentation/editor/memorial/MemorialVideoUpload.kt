package com.afternote.feature.afternote.presentation.editor.memorial

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.afternote.core.ui.button.PlusBadgeButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.feature.afternote.presentation.editor.isLocalContentUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

internal const val MEMORIAL_VIDEO_ADD_TEST_TAG = "memorialVideoAdd"

/**
 * 장례식에 남길 영상 추가 컴포넌트
 *
 * 피그마 디자인 기반:
 * - 라벨: 12sp, Regular, AfternoteDesign.colors.gray9
 * - 라벨과 버튼 간 간격: 6dp
 * - 큰 원형 버튼: AfternoteDesign.colors.gray9, 120dp
 * - 플러스 아이콘: 중앙에 위치, 24dp
 *
 * @param thumbnailUrl When set (e.g. from API when loading for edit), shown as thumbnail instead of extracting from video.
 * @param onThumbnailExtractionFailed 로컬 영상에서 썸네일 프레임을 뽑지 못했을 때 호출된다.
 * @param thumbnailRetryToken 값이 바뀌면 같은 영상에서 프레임 추출을 다시 시도한다.
 *   화면에는 썸네일 자리가 비는 것 외에 아무 표시도 하지 않으므로, 호출처가 개발자 텔레메트리로 남긴다.
 */
@Composable
fun MemorialVideoUpload(
    modifier: Modifier = Modifier,
    label: String? = null,
    videoUrl: String? = null,
    thumbnailUrl: String? = null,
    onAddVideoClick: () -> Unit,
    onThumbnailBytesReady: (ByteArray?) -> Unit,
    onThumbnailExtractionFailed: (Throwable) -> Unit,
    thumbnailRetryToken: Int,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val hasVideo = !videoUrl.isNullOrBlank()
    val labelText = label ?: stringResource(R.string.afternote_editor_funeral_video_label)
    val addContentDescription =
        if (hasVideo) {
            stringResource(R.string.afternote_editor_funeral_video_cd_change)
        } else {
            stringResource(R.string.afternote_editor_funeral_video_cd_add)
        }
    val context = LocalContext.current
    var thumbnailBitmap by remember(videoUrl, thumbnailRetryToken) { mutableStateOf<ImageBitmap?>(null) }

    // 같은 영상이라도 [thumbnailRetryToken] 이 바뀌면 추출을 다시 돈다 — 추출 실패의 유일한 복구
    // 경로가 «영상을 처음부터 다시 고르기» 였던 것을 푼다 (#1550).
    LaunchedEffect(videoUrl, thumbnailRetryToken) {
        if (videoUrl.isNullOrBlank()) {
            thumbnailBitmap = null
            onThumbnailBytesReady(null)
            return@LaunchedEffect
        }
        if (!videoUrl.isLocalContentUri()) {
            thumbnailBitmap = null
            onThumbnailBytesReady(null)
            return@LaunchedEffect
        }
        // 프레임을 못 뽑은 사유를 호출처에 넘기기 위해 Result 로 감싼다. 예외 없이 null 프레임만
        // 돌아오는 경로(코덱 미지원 등)도 있어, 그 경우는 여기서 실패로 승격시켜 사유를 만든다.
        val extraction =
            withContext(ioDispatcher) {
                runCatching {
                    val frame =
                        // run 은 블록을 식으로 만든다. try/finally 는 그 자체로 식이지만 val retriever
                        // 선언은 문이라 식 자리에 못 온다 — 밖에 선언하면 release 된 retriever 가 아래
                        // 압축 구간까지 살아남으므로, 수명을 프레임 추출 구간에 가둔다.
                        run {
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(context, videoUrl.toUri())
                                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            } finally {
                                retriever.release()
                            }
                        } ?: error("no video frame at position 0")
                    val bytes =
                        ByteArrayOutputStream().use { out ->
                            frame.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            out.toByteArray()
                        }
                    frame to bytes
                }
            }
        thumbnailBitmap = extraction.getOrNull()?.first?.asImageBitmap()
        extraction.exceptionOrNull()?.let { onThumbnailExtractionFailed(it) }
        onThumbnailBytesReady(extraction.getOrNull()?.second)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        Text(
            text = labelText,
            style =
                AfternoteDesign.typography.textField.copy(
                    fontWeight = FontWeight.Medium,
                    color = AfternoteDesign.colors.gray9,
                ),
        )

        if (!hasVideo) {
            // 업로드 전 상태: 흰색 카드 + 플러스 아이콘
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(color = AfternoteDesign.colors.white, shape = RoundedCornerShape(size = 16.dp))
                        .testTag(MEMORIAL_VIDEO_ADD_TEST_TAG)
                        .semantics { contentDescription = addContentDescription }
                        .clickable(onClick = onAddVideoClick),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    // 카드 전체가 단일 클릭 영역을 소유한다. 자식 clickable 은 중앙 탭을 가로챈다.
                    PlusBadgeButton(
                        contentDescription = null,
                        onClick = null,
                        paddingValues = PaddingValues(12.dp),
                        size = 24.dp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        } else {
            // 업로드 후 상태: 썸네일(URL 또는 로컬 프레임) 또는 그라데이션 배경 + 재생 아이콘
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(size = 16.dp))
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            AfternoteDesign.colors.gray6.copy(alpha = 153f / 255f),
                                            AfternoteDesign.colors.gray9.copy(alpha = 153f / 255f),
                                        ),
                                ),
                        ).clickable(onClick = onAddVideoClick),
            ) {
                when {
                    !thumbnailUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(R.drawable.afternote_img_placeholder_1),
                        )
                    }

                    thumbnailBitmap != null -> {
                        Image(
                            bitmap = thumbnailBitmap!!,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Image(
                    painter = painterResource(R.drawable.afternote_ic_playback),
                    contentDescription = stringResource(R.string.afternote_content_description_video_play),
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(32.dp),
                )
            }
        }
    }
}
