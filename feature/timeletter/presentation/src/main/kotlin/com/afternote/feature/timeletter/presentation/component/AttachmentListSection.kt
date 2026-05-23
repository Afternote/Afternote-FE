package com.afternote.feature.timeletter.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.timeletter.presentation.R
import com.afternote.feature.timeletter.presentation.viewmodel.LetterAttachment

@Composable
fun AttachmentListSection(
    attachments: List<LetterAttachment>,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        attachments.forEachIndexed { index, attachment ->
            AttachmentChip(
                attachment = attachment,
                onRemove = { onRemove(index) },
            )
            if (index < attachments.lastIndex) {
                Spacer(modifier = Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun AttachmentChip(
    attachment: LetterAttachment,
    onRemove: () -> Unit,
) {
    val (iconRes, label) =
        when (attachment) {
            is LetterAttachment.ImageAttachment -> R.drawable.ic_image to attachment.name
            is LetterAttachment.AudioAttachment -> R.drawable.ic_mic to attachment.name
            is LetterAttachment.FileAttachment -> R.drawable.ic_file to attachment.name
            is LetterAttachment.LinkAttachment -> R.drawable.ic_link to attachment.url
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(AfternoteDesign.colors.gray2, RoundedCornerShape(8.dp))
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = AfternoteDesign.colors.gray7,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = AfternoteDesign.typography.bodySmallR,
            color = AfternoteDesign.colors.gray7,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "삭제",
                tint = AfternoteDesign.colors.gray5,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
