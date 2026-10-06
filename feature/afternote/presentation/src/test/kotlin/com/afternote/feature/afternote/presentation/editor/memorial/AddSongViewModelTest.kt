package com.afternote.feature.afternote.presentation.editor.memorial

import com.afternote.feature.afternote.domain.repository.author.MusicSearchRepository
import com.afternote.feature.afternote.presentation.NoopAuthorErrorReporter
import com.afternote.feature.afternote.presentation.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AddSongViewModelTest {
    @After
    fun resetDispatcher() = Dispatchers.resetMain()

    @Test
    fun `검색 Intent는 debounce한 최신 검색어만 조회하고 실패를 소비한 뒤 다시 표시한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val queries = mutableListOf<String>()
            val repository =
                MusicSearchRepository { query ->
                    queries += query
                    Result.failure(IOException("offline"))
                }
            val viewModel = AddSongViewModel(repository, NoopAuthorErrorReporter)
            viewModel.onIntent(AddSongIntent.Search("old"))
            runCurrent()
            advanceTimeBy(299)
            viewModel.onIntent(AddSongIntent.Search("  latest  "))
            runCurrent()
            advanceTimeBy(300)
            runCurrent()
            assertEquals(listOf("latest"), queries)
            assertEquals("  latest  ", viewModel.uiState.value.searchQuery)
            assertEquals(R.string.afternote_editor_search_failed_generic, viewModel.uiState.value.errorRes)

            viewModel.onIntent(AddSongIntent.ConsumeError)
            assertNull(viewModel.uiState.value.errorRes)
            viewModel.onIntent(AddSongIntent.Search("latest"))
            runCurrent()
            advanceTimeBy(300)
            runCurrent()
            assertEquals(2, queries.size)
            assertEquals(R.string.afternote_editor_search_failed_generic, viewModel.uiState.value.errorRes)

            viewModel.onIntent(AddSongIntent.Search("   "))
            runCurrent()
            advanceTimeBy(300)
            runCurrent()
            assertEquals(2, queries.size)
            assertEquals(false, viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.errorRes)
        }
}
