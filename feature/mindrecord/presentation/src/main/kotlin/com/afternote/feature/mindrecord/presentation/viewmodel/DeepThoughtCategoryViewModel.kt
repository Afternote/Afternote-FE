package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.mindrecord.domain.usecase.deepthought.CreateDeepThoughtCategoryUseCase
import com.afternote.feature.mindrecord.domain.usecase.deepthought.DeleteDeepThoughtCategoryUseCase
import com.afternote.feature.mindrecord.domain.usecase.deepthought.GetDeepThoughtCategoriesUseCase
import com.afternote.feature.mindrecord.domain.usecase.deepthought.UpdateDeepThoughtCategoryUseCase
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeepThoughtCategoryViewModel
    @Inject
    constructor(
        private val getCategoriesUseCase: GetDeepThoughtCategoriesUseCase,
        private val createCategoryUseCase: CreateDeepThoughtCategoryUseCase,
        private val updateCategoryUseCase: UpdateDeepThoughtCategoryUseCase,
        private val deleteCategoryUseCase: DeleteDeepThoughtCategoryUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DeepThoughtCategoryUiState())
        val uiState: StateFlow<DeepThoughtCategoryUiState> = _uiState.asStateFlow()

        private val _events = Channel<DeepThoughtCategoryEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        init {
            load()
        }

        fun refresh() = load()

        fun addCategory(
            name: String,
            deepThoughtId: Long? = null,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isMutating = true, errorMessage = null) }
                createCategoryUseCase(name = name, deepThoughtId = deepThoughtId)
                    .onSuccess { created ->
                        val ui = created.toUi()
                        _uiState.update {
                            it.copy(
                                isMutating = false,
                                categories = it.categories + ui,
                            )
                        }
                        _events.send(DeepThoughtCategoryEvent.Created(ui))
                    }.onFailure { e ->
                        val msg = e.message ?: "카테고리 추가에 실패했습니다."
                        _uiState.update { it.copy(isMutating = false, errorMessage = msg) }
                        _events.send(DeepThoughtCategoryEvent.Error(msg))
                    }
            }
        }

        fun renameCategory(
            categoryId: Long,
            newName: String,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isMutating = true, errorMessage = null) }
                updateCategoryUseCase(categoryId = categoryId, newName = newName)
                    .onSuccess { updated ->
                        val ui = updated.toUi()
                        _uiState.update { state ->
                            state.copy(
                                isMutating = false,
                                categories =
                                    state.categories.map {
                                        if (it.id == ui.id) ui else it
                                    },
                            )
                        }
                        _events.send(DeepThoughtCategoryEvent.Updated(ui))
                    }.onFailure { e ->
                        val msg = e.message ?: "카테고리 수정에 실패했습니다."
                        _uiState.update { it.copy(isMutating = false, errorMessage = msg) }
                        _events.send(DeepThoughtCategoryEvent.Error(msg))
                    }
            }
        }

        fun deleteCategory(categoryId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(isMutating = true, errorMessage = null) }
                deleteCategoryUseCase(categoryId)
                    .onSuccess {
                        _uiState.update { state ->
                            state.copy(
                                isMutating = false,
                                categories = state.categories.filterNot { it.id == categoryId.toString() },
                            )
                        }
                        _events.send(DeepThoughtCategoryEvent.Deleted(categoryId))
                    }.onFailure { e ->
                        val msg = e.message ?: "카테고리 삭제에 실패했습니다."
                        _uiState.update { it.copy(isMutating = false, errorMessage = msg) }
                        _events.send(DeepThoughtCategoryEvent.Error(msg))
                    }
            }
        }

        fun consumeError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        private fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                getCategoriesUseCase()
                    .onSuccess { list ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                categories = list.map { c -> c.toUi() },
                            )
                        }
                    }.onFailure { e ->
                        val msg = e.message ?: "카테고리 목록을 불러오지 못했습니다."
                        _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                        _events.send(DeepThoughtCategoryEvent.Error(msg))
                    }
            }
        }
    }
