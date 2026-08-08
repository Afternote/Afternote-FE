package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.timeletter.domain.model.TimeLetter
import com.afternote.feature.timeletter.domain.model.TimeLetterStatus
import com.afternote.feature.timeletter.presentation.R

@Composable
fun DraftLetterItem(
    draft: TimeLetter,
    modifier: Modifier = Modifier,
    receiverNameMap: Map<Long, String> = emptyMap(),
    isEditMode: Boolean = false,
    isSelected: Boolean = false,
    onToggle: () -> Unit = {},
) {
    val receiverNames =
        draft.receiverIds
            .mapNotNull(receiverNameMap::get)
            .joinToString(", ")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (isEditMode) {
                            Modifier.clickable(role = Role.Checkbox, onClick = onToggle)
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEditMode) {
                AfternoteCircularCheckbox(
                    state = if (isSelected) CheckboxState.Default else CheckboxState.None,
                    onClick = onToggle,
                    size = 20.dp,
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text =
                            if (receiverNames.isEmpty()) {
                                stringResource(R.string.timeletter_draft_recipient_unspecified)
                            } else {
                                stringResource(R.string.timeletter_draft_recipient_names, receiverNames)
                            },
                        style = AfternoteDesign.typography.footnoteCaption,
                        color = AfternoteDesign.colors.gray6,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text =
                            stringResource(
                                R.string.timeletter_draft_scheduled_date,
                                formatDraftSendAt(draft.sendAt),
                            ),
                        style = AfternoteDesign.typography.footnoteCaption,
                        color = AfternoteDesign.colors.gray6,
                    )
                }
                Text(
                    text =
                        draft.title?.takeIf(String::isNotBlank)
                            ?: stringResource(R.string.timeletter_draft_untitled),
                    style = AfternoteDesign.typography.bodyBase,
                    color = AfternoteDesign.colors.gray9,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp),
            color = AfternoteDesign.colors.gray3,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DraftLetterItemPreview() {
    DraftLetterItem(
        draft =
            TimeLetter(
                id = 1L,
                title = "미래의 나에게",
                sendAt = "2026-12-31T00:00:00",
                deliveredAt = null,
                status = TimeLetterStatus.DRAFT,
                blocks = emptyList(),
                receiverIds = listOf(1L),
            ),
        receiverNameMap = mapOf(1L to "김지은"),
    )
}
