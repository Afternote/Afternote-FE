package com.afternote.feature.mindrecord.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategoryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategoryName
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategoryUpdatePayload
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import com.afternote.feature.mindrecord.presentation.mapper.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeepThoughtCategoryViewModel
    @Inject
    constructor(
        private val repository: DeepThoughtRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DeepThoughtCategoryUiState())
        val uiState: StateFlow<DeepThoughtCategoryUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun refresh() = load()

        fun addCategory(
            name: String,
            deepThoughtId: Long? = null,
        ) {
            DeepThoughtCategoryName
                .from(name)
                .onSuccess { categoryName ->
                    viewModelScope.launch {
                        _uiState.update { it.copy(isMutating = true, errorMessage = null) }
                        repository
                            .createCategory(
                                DeepThoughtCategoryCreatePayload(
                                    category = categoryName.value,
                                    deepThoughtId = deepThoughtId,
                                ),
                            ).onSuccess { created ->
                                _uiState.update {
                                    it.copy(
                                        isMutating = false,
                                        categories = it.categories + created.toUi(),
                                    )
                                }
                            }.onFailure { e -> emitError(e.message ?: "카테고리 추가에 실패했습니다.") }
                    }
                }.onFailure { e -> emitError(e.message ?: "카테고리 이름이 올바르지 않습니다.") }
        }

        fun renameCategory(
            categoryId: Long,
            newName: String,
        ) {
            DeepThoughtCategoryName
                .from(newName)
                .onSuccess { categoryName ->
                    viewModelScope.launch {
                        _uiState.update { it.copy(isMutating = true, errorMessage = null) }
                        repository
                            .updateCategory(
                                categoryId = categoryId,
                                payload = DeepThoughtCategoryUpdatePayload(category = categoryName.value),
                            ).onSuccess { updated ->
                                val ui = updated.toUi()
                                _uiState.update { state ->
                                    state.copy(
                                        isMutating = false,
                                        categories = state.categories.map { if (it.id == ui.id) ui else it },
                                    )
                                }
                            }.onFailure { e -> emitError(e.message ?: "카테고리 수정에 실패했습니다.") }
                    }
                }.onFailure { e -> emitError(e.message ?: "카테고리 이름이 올바르지 않습니다.") }
        }

        fun deleteCategory(categoryId: Long) {
            viewModelScope.launch {
                _uiState.update { it.copy(isMutating = true, errorMessage = null) }
                repository
                    .deleteCategory(categoryId)
                    .onSuccess {
                        _uiState.update { state ->
                            state.copy(
                                isMutating = false,
                                categories = state.categories.filterNot { it.id == categoryId.toString() },
                            )
                        }
                    }.onFailure { e -> emitError(e.message ?: "카테고리 삭제에 실패했습니다.") }
            }
        }

        fun consumeError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        private fun emitError(message: String) {
            _uiState.update { it.copy(isMutating = false, errorMessage = message) }
        }

        private fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                repository
                    .getCategories()
                    .onSuccess { list ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                categories = list.map { c -> c.toUi() },
                            )
                        }
                    }.onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "카테고리 목록을 불러오지 못했습니다.",
                            )
                        }
                    }
            }
        }
    }
