package com.afternote.feature.timeletter.presentation.screen.sender

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.afternote.core.ui.TopBar
import com.afternote.feature.timeletter.domain.TimeLetters
import com.afternote.feature.timeletter.presentation.component.EmptyTimeletterContent
import com.afternote.feature.timeletter.presentation.component.TimeletterListContent

@Composable
fun TimeletterScreen(
    letters: TimeLetters,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopBar() },
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
    TimeletterListContent(
        letters = letters,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun TimeletterScreenEmptyPreview() {
    TimeletterScreen(letters = TimeLetters(emptyList()))
}


