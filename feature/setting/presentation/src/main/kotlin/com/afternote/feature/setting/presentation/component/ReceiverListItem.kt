package com.afternote.feature.setting.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.model.setting.ReceiverListItem
import com.afternote.core.ui.button.AfternoteCircularCheckbox
import com.afternote.core.ui.button.CheckboxState
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.setting.presentation.R

@Composable
fun ReceiverListItem(
    receiver: ReceiverListItem,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(R.drawable.ic_default_profile),
            contentDescription = "프로필 이미지",
            modifier = Modifier.size(50.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = receiver.name,
                style = AfternoteDesign.typography.captionLargeB,
            )
            Spacer(modifier = Modifier.padding(top = 5.dp))
            Text(
                text = receiver.relation,
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray8,
            )
        }
        AfternoteCircularCheckbox(
            state = if (selected) CheckboxState.Default else CheckboxState.None,
            onClick = { onSelectedChange(!selected) },
            size = 20.dp,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReceiverListItemPrev() {
    ReceiverListItem(
        receiver = ReceiverListItem(receiverId = 1L, name = "박경민", relation = "친구"),
        selected = false,
        onSelectedChange = {},
    )
}
