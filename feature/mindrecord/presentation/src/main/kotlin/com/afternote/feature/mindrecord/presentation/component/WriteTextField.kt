package com.afternote.feature.mindrecord.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afternote.core.ui.theme.AfternoteTheme
import com.afternote.core.ui.theme.Gray4
import com.afternote.feature.mindrecord.presentation.model.TextStyleState
import com.afternote.feature.mindrecord.presentation.model.TextStyleType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WriteTextField(modifier: Modifier = Modifier) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    var styleState by remember { mutableStateOf(TextStyleState()) }
    var showTextStyleToolbar by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible

    // 키보드 닫히면 텍스트 설정도 닫기
    LaunchedEffect(imeVisible) {
        if (!imeVisible) showTextStyleToolbar = false
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 텍스트 입력 영역
        BasicTextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (textFieldValue.text.isEmpty()) {
                        Text("당신의 오늘을 기록해보세요", color = Gray4)
                    }
                    innerTextField()
                    Text(
                        text = "${textFieldValue.text.length} 글자",
                        color = Gray4,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            }
        )

        // 텍스트 설정 툴바 (T 눌렀을 때)
        if (showTextStyleToolbar && imeVisible) {
            TextStyleToolbar(
                styleState = styleState,
                onClose = { showTextStyleToolbar = false },
                onBoldClick = { /* toggleBold */ },
                onItalicClick = { /* toggleItalic */ },
                onUnderlineClick = { /* toggleUnderline */ },
                onStrikethroughClick = { /* toggleStrikethrough */ },
                onAlignChange = { styleState = styleState.copy(textAlign = it) },
                onTextStyleChange = { styleState = styleState.copy(textStyle = it) },
            )
        }

        // 기본 하단 툴바 (항상 표시)
        BottomToolbar(
            modifier = Modifier.imePadding(),
            onTextStyleClick = { showTextStyleToolbar = !showTextStyleToolbar },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WriteTextFieldPreview() {
    AfternoteTheme {
        WriteTextField()
    }
}