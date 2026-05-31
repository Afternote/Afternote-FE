package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteDesign
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.TextStyleState
import com.afternote.feature.mindrecord.presentation.model.TextStyleType

@Composable
fun BottomToolbar(
    onTextStyleClick: () -> Unit,
    onAlignChange: (TextAlign) -> Unit,
    modifier: Modifier = Modifier,
    onLinkClick: () -> Unit = {},
    onSaveDraftClick: () -> Unit = {},
    onDraftCountClick: () -> Unit = {},
    draftCount: Int = 0,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(AfternoteDesign.colors.white)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onLinkClick) {
            Icon(
                painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_ic_link),
                contentDescription = stringResource(R.string.mindrecord_toolbar_link_cd),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onTextStyleClick) {
            Text(
                text = "T",
                style = AfternoteDesign.typography.bodyLargeB,
                color = AfternoteDesign.colors.gray9,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = { onAlignChange(TextAlign.Start) }) {
            Icon(
                painter = painterResource(R.drawable.mindrecord_align_left),
                contentDescription = stringResource(R.string.mindrecord_toolbar_align_left_cd),
            )
        }
        IconButton(onClick = { onAlignChange(TextAlign.Center) }) {
            Icon(
                painter = painterResource(R.drawable.mindrecord_align_center),
                contentDescription = stringResource(R.string.mindrecord_toolbar_align_center_cd),
            )
        }
        IconButton(onClick = { onAlignChange(TextAlign.End) }) {
            Icon(
                painter = painterResource(R.drawable.mindrecord_align_right),
                contentDescription = stringResource(R.string.mindrecord_toolbar_align_right_cd),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.mindrecord_toolbar_draft_label),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray6,
                modifier = Modifier.clickable(onClick = onSaveDraftClick),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = draftCount.toString(),
                style = AfternoteDesign.typography.captionLargeR,
                color = AfternoteDesign.colors.gray4,
                modifier = Modifier.clickable(onClick = onDraftCountClick),
            )
        }
    }
}

/**
 * 키보드 위에 떠 있는 텍스트 스타일 설정 패널.
 *
 * Figma node 552:19970 — `nav_daily_write` 의 `텍스트 설정` 영역과 1:1 매칭된다.
 */
@Composable
fun TextStyleToolbar(
    onClose: () -> Unit,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onUnderlineClick: () -> Unit,
    onStrikethroughClick: () -> Unit,
    onAlignChange: (TextAlign) -> Unit,
    onTextStyleChange: (TextStyleType) -> Unit,
    styleState: TextStyleState,
    modifier: Modifier = Modifier,
    onLinkClick: () -> Unit = {},
    onTypeClick: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(AfternoteDesign.colors.gray1),
    ) {
        HorizontalDivider(thickness = 1.dp, color = AfternoteDesign.colors.gray2)

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.mindrecord_toolbar_text_settings),
                style = AfternoteDesign.typography.bodyBase,
                color = AfternoteDesign.colors.gray6,
            )
            Spacer(modifier = Modifier.weight(1f))
            CloseButton(onClick = onClose)
        }

        HorizontalDivider(thickness = 1.dp, color = AfternoteDesign.colors.gray2)

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconActionButton(onClick = onLinkClick) {
                    Icon(
                        painter = painterResource(com.afternote.core.ui.R.drawable.core_ui_ic_link),
                        contentDescription = stringResource(R.string.mindrecord_toolbar_link_cd),
                        tint = AfternoteDesign.colors.gray9,
                    )
                }

                IconActionButton(onClick = onTypeClick) {
                    Text(
                        text = "T",
                        style = AfternoteDesign.typography.bodyLargeB,
                        color = AfternoteDesign.colors.gray9,
                    )
                }

                AlignPill(
                    selected = styleState.textAlign,
                    onAlignChange = onAlignChange,
                )

                StylePill(
                    styleState = styleState,
                    onBoldClick = onBoldClick,
                    onItalicClick = onItalicClick,
                    onUnderlineClick = onUnderlineClick,
                    onStrikethroughClick = onStrikethroughClick,
                )
            }

            HeaderTypeRow(
                selected = styleState.textStyle,
                onTextStyleChange = onTextStyleChange,
            )
        }
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(AfternoteDesign.colors.gray2)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.mindrecord_close),
            contentDescription = stringResource(R.string.mindrecord_toolbar_close_cd),
            tint = AfternoteDesign.colors.gray9,
            modifier = Modifier.size(10.dp),
        )
    }
}

@Composable
private fun IconActionButton(
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun AlignPill(
    selected: TextAlign,
    onAlignChange: (TextAlign) -> Unit,
) {
    val items =
        listOf(
            TextAlign.Start to R.drawable.mindrecord_align_left,
            TextAlign.Center to R.drawable.mindrecord_align_center,
            TextAlign.End to R.drawable.mindrecord_align_right,
        )
    Row(
        modifier =
            Modifier
                .height(44.dp)
                .clip(CircleShape)
                .background(AfternoteDesign.colors.gray2)
                .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (align, iconRes) ->
            PillIconSlot(
                selected = selected == align,
                onClick = { onAlignChange(align) },
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = AfternoteDesign.colors.gray9,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun StylePill(
    styleState: TextStyleState,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onUnderlineClick: () -> Unit,
    onStrikethroughClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(44.dp)
                .clip(CircleShape)
                .background(AfternoteDesign.colors.gray2)
                .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillIconSlot(selected = styleState.isBold, onClick = onBoldClick) {
            Text(
                text = "B",
                fontWeight = FontWeight.Bold,
                color = AfternoteDesign.colors.gray9,
                style = AfternoteDesign.typography.bodyLargeB,
            )
        }
        PillIconSlot(selected = styleState.isItalic, onClick = onItalicClick) {
            Text(
                text = "I",
                fontStyle = FontStyle.Italic,
                color = AfternoteDesign.colors.gray9,
                style = AfternoteDesign.typography.bodyLargeR,
            )
        }
        PillIconSlot(selected = styleState.isUnderline, onClick = onUnderlineClick) {
            Text(
                text = "U",
                textDecoration = TextDecoration.Underline,
                color = AfternoteDesign.colors.gray9,
                style = AfternoteDesign.typography.bodyLargeR,
            )
        }
        PillIconSlot(selected = styleState.isStrikethrough, onClick = onStrikethroughClick) {
            Text(
                text = "S",
                textDecoration = TextDecoration.LineThrough,
                color = AfternoteDesign.colors.gray9,
                style = AfternoteDesign.typography.bodyLargeR,
            )
        }
    }
}

@Composable
private fun PillIconSlot(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val size = if (selected) 33.6.dp else 28.dp
    val baseModifier =
        Modifier
            .size(size)
            .let {
                if (selected) {
                    it
                        .shadow(elevation = 1.dp, shape = CircleShape, clip = false)
                        .background(AfternoteDesign.colors.gray1, CircleShape)
                } else {
                    it
                }
            }.clip(CircleShape)
            .clickable(onClick = onClick)
    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun HeaderTypeRow(
    selected: TextStyleType,
    onTextStyleChange: (TextStyleType) -> Unit,
) {
    val items =
        listOf(
            HeaderTypeItem(TextStyleType.TITLE, stringResource(R.string.mindrecord_toolbar_style_title), AfternoteDesign.typography.h3),
            HeaderTypeItem(
                TextStyleType.HEADER,
                stringResource(R.string.mindrecord_toolbar_style_header),
                AfternoteDesign.typography.bodyLargeB,
            ),
            HeaderTypeItem(
                TextStyleType.SUBHEADER,
                stringResource(R.string.mindrecord_toolbar_style_subheader),
                AfternoteDesign.typography.bodySmallB,
            ),
            HeaderTypeItem(
                TextStyleType.BODY,
                stringResource(R.string.mindrecord_toolbar_style_body),
                AfternoteDesign.typography.captionLargeR,
            ),
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            HeaderTypeSegment(
                modifier = Modifier.weight(1f),
                label = item.label,
                style = item.style,
                selected = selected == item.type,
                onClick = { onTextStyleChange(item.type) },
            )
            if (index < items.lastIndex) {
                Box(
                    modifier =
                        Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(AfternoteDesign.colors.gray4),
                )
            }
        }
    }
}

@Composable
private fun HeaderTypeSegment(
    label: String,
    style: TextStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(AfternoteDesign.colors.gray9)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = style,
                    color = AfternoteDesign.colors.gray1,
                    maxLines = 1,
                )
            }
        } else {
            Text(
                text = label,
                style = style,
                color = AfternoteDesign.colors.gray6,
                maxLines = 1,
            )
        }
    }
}

private data class HeaderTypeItem(
    val type: TextStyleType,
    val label: String,
    val style: TextStyle,
)
