package com.afternote.feature.setting.presentation.notification.preference

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.modifierextention.bottomBorder
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.setting.presentation.R
import java.time.LocalTime

/**
 * 푸시 알림 화면 아래의 「알림 시간 설정」 — Figma 「설정 - 푸시알림 설정」 (#2321).
 *
 * 구분선, 제목, 시·분 드롭다운 두 칸, 안내 한 줄이다. 시는 0~23 을 그대로, 분은 10분 단위로 고른다.
 * 서버 설정을 불러오지 못했으면 드롭다운 대신 다시 시도를 둔다 — 기본값을 그리면 사용자의 설정처럼 보인다.
 */
@Composable
internal fun NotificationTimeSection(
    uiState: NotificationPreferenceUiState,
    onIntent: (NotificationPreferenceIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HorizontalDivider(color = AfternoteDesign.colors.gray2)
        Text(
            text = stringResource(R.string.setting_notification_time_title),
            style = AfternoteDesign.typography.bodyBase,
            color = AfternoteDesign.colors.gray9,
        )
        val preferences = uiState.preferences
        when {
            preferences != null -> {
                TimePickers(
                    time = preferences.time,
                    enabled = !uiState.isSaving,
                    onTimeChange = { onIntent(NotificationPreferenceIntent.ChangeTime(it)) },
                )
            }

            uiState.loadFailed -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.setting_notification_preference_load_failed),
                        style = AfternoteDesign.typography.captionLargeR,
                        color = AfternoteDesign.colors.gray6,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onIntent(NotificationPreferenceIntent.Retry) }) {
                        Text(
                            text = stringResource(R.string.setting_notification_time_retry),
                            style = AfternoteDesign.typography.captionLargeR,
                            color = AfternoteDesign.colors.gray9,
                        )
                    }
                }
            }
        }
        Row {
            Image(
                painter = painterResource(R.drawable.setting_ic_bullet),
                contentDescription = null,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.setting_notification_time_guide),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray9,
            )
        }
    }
}

@Composable
private fun TimePickers(
    time: LocalTime,
    enabled: Boolean,
    onTimeChange: (LocalTime) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TimeDropdown(
            selected = time.hour,
            options = HOURS,
            label = { it.toString() },
            enabled = enabled,
            onSelect = { onTimeChange(time.withHour(it)) },
            contentDescription = stringResource(R.string.setting_notification_time_hour),
        )
        TimeDropdown(
            selected = time.minute,
            // 서버는 분 단위로 받지만 시안은 칸 하나라 10분 단위로 줄인다. 다른 분에 저장돼 있으면 그 값도 보인다.
            options = (MINUTES + time.minute).distinct().sorted(),
            label = { "%02d".format(it) },
            enabled = enabled,
            onSelect = { onTimeChange(time.withMinute(it)) },
            contentDescription = stringResource(R.string.setting_notification_time_minute),
        )
    }
}

@Composable
private fun TimeDropdown(
    selected: Int,
    options: List<Int>,
    label: (Int) -> String,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    contentDescription: String,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier =
                Modifier
                    .width(160.dp)
                    .clickable(enabled = enabled, role = Role.DropdownList, onClickLabel = contentDescription) { expanded = true }
                    .bottomBorder(color = AfternoteDesign.colors.gray3, width = 0.5.dp)
                    .padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label(selected),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray9,
                modifier = Modifier.weight(1f),
            )
            Image(painter = painterResource(R.drawable.setting_ic_time_dropdown), contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(8.dp),
            containerColor = AfternoteDesign.colors.white,
            modifier = Modifier.width(164.dp).height(DROPDOWN_HEIGHT),
        ) {
            options.forEach { option ->
                Text(
                    text = label(option),
                    style = AfternoteDesign.typography.textField,
                    color = AfternoteDesign.colors.gray9,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                expanded = false
                                onSelect(option)
                            }.padding(16.dp),
                )
            }
        }
    }
}

private val HOURS = (0..23).toList()
private val MINUTES = (0..50 step 10).toList()

/** 시안의 펼친 목록은 세 줄이 보인다. 나머지는 스크롤한다. */
private val DROPDOWN_HEIGHT = 162.dp
