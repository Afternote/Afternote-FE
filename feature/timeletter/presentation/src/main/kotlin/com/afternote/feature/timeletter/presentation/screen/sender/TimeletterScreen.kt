package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.BottomBar
import com.afternote.core.ui.TopBar
import com.afternote.feature.timeletter.domain.TimeLetters
import com.afternote.feature.timeletter.presentation.component.EmptyTimeletterContent

@Composable
fun TimeletterScreen(
    letters: TimeLetters = TimeLetters(emptyList()),
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopBar() },
        bottomBar = { BottomBar() },
    ) { paddingValues ->
        TimeletterContent(
            letters = letters,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@Composable
private fun TimeletterContent(
    letters: TimeLetters,
    modifier: Modifier = Modifier,
) {
    if (letters.isEmpty()) {
        EmptyTimeletterContent(modifier = modifier)
        return
    }
}

@Preview
@Composable
private fun TimeletterScreenEmptyPreview() {
    TimeletterScreen(letters = TimeLetters(emptyList()))
}
