package com.afternote.feature.mindrecord.domain.usecase.deepthought

import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import javax.inject.Inject

class DeleteDeepThoughtCategoryUseCase
    @Inject
    constructor(
        private val repository: DeepThoughtRepository,
    ) {
        suspend operator fun invoke(categoryId: Long): Result<Unit> = repository.deleteCategory(categoryId)
    }
