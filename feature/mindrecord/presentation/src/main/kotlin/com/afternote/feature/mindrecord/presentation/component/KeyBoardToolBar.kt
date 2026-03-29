package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.Gray2
import com.afternote.core.ui.theme.Gray6
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.model.TextStyleState
import com.afternote.feature.mindrecord.presentation.model.TextStyleType

@Composable
fun BottomToolbar(
    modifier: Modifier = Modifier,
    onTextStyleClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 링크
        Icon(painter = painterResource(R.drawable.mindrecord_link), contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))

        // T 버튼 - 텍스트 설정 열기
        IconButton(onClick = onTextStyleClick) {
            Text("T", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.width(16.dp))

        // 정렬
        Icon(painter = painterResource(R.drawable.mindrecord_align_left), contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(painter = painterResource(R.drawable.mindrecord_align_center), contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(painter = painterResource(R.drawable.mindrecord_align_right), contentDescription = null)

        Spacer(modifier = Modifier.weight(1f))

        // 임시저장
        Text("임시저장 1", style = MaterialTheme.typography.labelSmall, color = Gray6)
    }
}

// 텍스트 설정 툴바
@Composable
fun TextStyleToolbar(
    modifier: Modifier = Modifier,
    styleState: TextStyleState,
    onClose: () -> Unit,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onUnderlineClick: () -> Unit,
    onStrikethroughClick: () -> Unit,
    onAlignChange: (TextAlign) -> Unit,
    onTextStyleChange: (TextStyleType) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("텍스트 설정", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(painter = painterResource(R.drawable.mindrecord_close), contentDescription = null)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 링크, T
            Icon(painter = painterResource(R.drawable.mindrecord_link), contentDescription = null)
            Text("T")

            Spacer(modifier = Modifier.width(4.dp))

            // 정렬
            listOf(TextAlign.Start, TextAlign.Center, TextAlign.End).forEach { align ->
                ToolbarButton(selected = styleState.textAlign == align, onClick = { onAlignChange(align) }) {
                    // 정렬 아이콘
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // B I U S
            ToolbarButton(selected = styleState.isBold, onClick = onBoldClick) {
                Text("B", fontWeight = FontWeight.Bold)
            }
            ToolbarButton(selected = styleState.isItalic, onClick = onItalicClick) {
                Text("I", fontStyle = FontStyle.Italic)
            }
            ToolbarButton(selected = styleState.isUnderline, onClick = onUnderlineClick) {
                Text("U", textDecoration = TextDecoration.Underline)
            }
            ToolbarButton(selected = styleState.isStrikethrough, onClick = onStrikethroughClick) {
                Text("S", textDecoration = TextDecoration.LineThrough)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 제목 / 머릿말 / 부머릿말 / 본문
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                TextStyleType.TITLE to "제목",
                TextStyleType.HEADER to "머릿말",
                TextStyleType.SUBHEADER to "부머릿말",
                TextStyleType.BODY to "본문",
            ).forEach { (type, label) ->
                ToolbarButton(
                    selected = styleState.textStyle == type,
                    onClick = { onTextStyleChange(type) },
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}



@Composable
fun ToolbarButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) Gray2 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}