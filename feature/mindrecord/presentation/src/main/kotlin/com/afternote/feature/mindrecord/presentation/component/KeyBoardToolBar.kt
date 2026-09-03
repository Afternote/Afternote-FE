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
import com.afternote.core.ui.R as CoreUiR

@Composable
fun BottomToolbar(
    onTextStyleClick: () -> Unit,
    onAlignChange: (TextAlign) -> Unit,
    modifier: Modifier = Modifier,
    onLinkClick: () -> Unit,
    onSaveDraftClick: () -> Unit,
    onDraftCountClick: () -> Unit,
    /** 임시저장 개수. `null` 은 아직 모름(조회 중·실패) — 0 으로 단정하지 않는다. */
    draftCount: Int? = null,
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

        // 정렬 아이콘 3종은 `core:ui` 승격본을 쓴다 (#1404). 종전에는 이 모듈만 Material 기성
        // 채움 패스를 써서 같은 아이콘이 타임레터 작성 화면과 다르게 렌더됐다 — 시안은
        // 스트로크 2.4 아웃라인 판이다(4327:76948).
        IconButton(onClick = { onAlignChange(TextAlign.Start) }) {
            Icon(
                painter = painterResource(CoreUiR.drawable.core_ui_ic_align_left),
                contentDescription = stringResource(R.string.mindrecord_toolbar_align_left_cd),
            )
        }
        IconButton(onClick = { onAlignChange(TextAlign.Center) }) {
            Icon(
                painter = painterResource(CoreUiR.drawable.core_ui_ic_align_center),
                contentDescription = stringResource(R.string.mindrecord_toolbar_align_center_cd),
            )
        }
        IconButton(onClick = { onAlignChange(TextAlign.End) }) {
            Icon(
                painter = painterResource(CoreUiR.drawable.core_ui_ic_align_right),
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
                // 조회 전·실패에 0 을 보이면 "임시저장이 없다" 는 틀린 사실을 말하게 된다.
                // 진입점(탭 영역)은 유지해야 해서 숨기는 대신 임시저장 목록의 미지정 표기와 같은 '–' 를 쓴다.
                text = draftCount?.toString() ?: UNKNOWN_DRAFT_COUNT,
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
    onLinkClick: () -> Unit,
    onTypeClick: () -> Unit,
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
            contentDescription = stringResource(CoreUiR.string.core_ui_content_description_close),
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
            TextAlign.Start to CoreUiR.drawable.core_ui_ic_align_left,
            TextAlign.Center to CoreUiR.drawable.core_ui_ic_align_center,
            TextAlign.End to CoreUiR.drawable.core_ui_ic_align_right,
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

/** 임시저장 개수를 아직 모를 때 자리 표시 — `DraftDateFormatter` 의 미지정 표기와 같은 문자다. */
private const val UNKNOWN_DRAFT_COUNT = "\u2013"
