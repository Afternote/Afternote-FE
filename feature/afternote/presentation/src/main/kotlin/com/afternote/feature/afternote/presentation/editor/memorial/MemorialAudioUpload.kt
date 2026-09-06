package com.afternote.feature.afternote.presentation.editor.memorial

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.PlusBadgeButton
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.afternote.presentation.R
import com.afternote.core.ui.R as CoreUiR

internal const val MEMORIAL_AUDIO_ADD_TEST_TAG = "memorialAudioAdd"

/**
 * 추모 음성 첨부 슬롯 (#1118).
 *
 * **시안 없음 — FE 확정.** 착수 시점(2026-08-31)에 Figma 정본 페이지의 «NEW 추억 노트» 구역
 * (`4327:72816`) 을 다시 훑었지만 `추억노트_사진추가`·`추억노트_영상 추가` 만 있고 음성 프레임은 없다.
 * 그래서 같은 화면의 영상 슬롯([MemorialVideoUpload]) 규격을 그대로 따른다 —
 * 라벨 + 흰 카드 + 중앙 플러스, 첨부 후에는 카드 안이 상태 표시로 바뀐다.
 * 아이콘은 타임레터 «음성 추가하기» 시트가 쓰는 것과 같은 `core_ui_ic_mic` 를 재사용한다.
 *
 * 붙인 음성을 **앱 안에서 재생하는 수단은 이 범위에 없다.** 영상 슬롯도 같다 — 첨부 여부만
 * 보여 주고 다시 누르면 교체·삭제 시트가 뜬다. 재생 UI 는 수신자 열람 화면과 함께 후속으로 다룬다.
 *
 * @param audioUrl 첨부된 음성. 로컬 `content://` 픽이든 수정 진입 prefill 의 원격 URL 이든 같은 표시다.
 */
@Composable
fun MemorialAudioUpload(
    audioUrl: String?,
    onAddAudioClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val hasAudio = !audioUrl.isNullOrBlank()
    val labelText = label ?: stringResource(R.string.afternote_editor_memorial_audio_label)
    val addContentDescription =
        if (hasAudio) {
            stringResource(R.string.afternote_editor_memorial_audio_cd_change)
        } else {
            stringResource(R.string.afternote_editor_memorial_audio_cd_add)
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

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(color = AfternoteDesign.colors.white, shape = RoundedCornerShape(size = 16.dp))
                    .testTag(MEMORIAL_AUDIO_ADD_TEST_TAG)
                    .semantics { contentDescription = addContentDescription }
                    .clickable(onClick = onAddAudioClick),
            contentAlignment = Alignment.Center,
        ) {
            if (hasAudio) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(CoreUiR.drawable.core_ui_ic_mic),
                        contentDescription = null,
                        tint = AfternoteDesign.colors.iconBk,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.afternote_editor_memorial_audio_attached),
                        style = AfternoteDesign.typography.bodyBase,
                        color = AfternoteDesign.colors.gray9,
                    )
                }
            } else {
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
        }
    }
}
