package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.core.ui.UiText
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategory
import com.afternote.feature.mindrecord.domain.model.DeepThoughtList
import com.afternote.feature.mindrecord.domain.model.RandomDeepThought
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import com.afternote.feature.mindrecord.presentation.R
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import com.afternote.feature.mindrecord.presentation.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeepThoughtListViewModel
    @Inject
    constructor(
        private val repository: DeepThoughtRepository,
    ) : ViewModel() {
        private val internalState = MutableStateFlow(InternalState())

        val uiState: StateFlow<DeepThoughtListUiState> =
            internalState
                .map { it.toUiState() }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = DeepThoughtListUiState.Loading,
                )

        init {
            load()
        }

        fun onCategorySelected(category: String?) {
            internalState.update { it.copy(selectedCategory = category) }
            load()
        }

        fun onTagSelected(tag: Tag?) {
            internalState.update { it.copy(selectedTag = tag) }
            load()
        }

        fun refresh() = load()

        private fun load() {
            val state = internalState.value
            viewModelScope.launch {
                internalState.update { it.copy(loadPhase = LoadPhase.Loading) }

                val random = repository.getRandom().getOrNull()
                val categories = repository.getCategories().getOrNull().orEmpty()
                repository
                    .getList(
                        date = null,
                        tag = state.selectedTag?.name,
                        category = state.selectedCategory,
                    ).onSuccess { list ->
                        internalState.update {
                            it.copy(loadPhase = LoadPhase.Loaded(random, list, categories))
                        }
                    }.onFailure { e ->
                        internalState.update {
                            it.copy(
                                loadPhase =
                                    LoadPhase.Failed(
                                        UiText.DynamicOrResource(
                                            value = e.message,
                                            fallbackResId = R.string.mindrecord_error_deep_thought_list_failed,
                                        ),
                                    ),
                            )
                        }
                    }
            }
        }

        private data class InternalState(
            val loadPhase: LoadPhase = LoadPhase.Loading,
            val selectedTag: Tag? = null,
            val selectedCategory: String? = null,
        )

        private sealed interface LoadPhase {
            data object Loading : LoadPhase

            data class Loaded(
                val random: RandomDeepThought?,
                val list: DeepThoughtList,
                val categories: List<DeepThoughtCategory>,
            ) : LoadPhase

            data class Failed(
                val message: UiText,
            ) : LoadPhase
        }

        private fun InternalState.toUiState(): DeepThoughtListUiState =
            when (val phase = loadPhase) {
                LoadPhase.Loading -> {
                    DeepThoughtListUiState.Loading
                }

                is LoadPhase.Loaded -> {
                    val items = phase.list.items.map { it.toUi() }
                    val tags = phase.list.tagCounts.map { Tag(name = it.tag, count = it.count) }
                    DeepThoughtListUiState.Success(
                        randomThoughtTitle = phase.random?.title,
                        randomThoughtCreatedAt = phase.random?.createdAt,
                        tags = tags,
                        selectedTag = selectedTag,
                        categories = phase.categories.map { it.toUi() },
                        selectedCategory = selectedCategory,
                        items = items,
                    )
                }

                is LoadPhase.Failed -> {
                    DeepThoughtListUiState.Error(phase.message)
                }
            }
    }
