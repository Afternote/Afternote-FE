package com.afternote.feature.mindrecord.domain.usecase.deepthought

import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategory
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategoryUpdatePayload
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import javax.inject.Inject

class UpdateDeepThoughtCategoryUseCase
    @Inject
    constructor(
        private val repository: DeepThoughtRepository,
    ) {
        suspend operator fun invoke(
            categoryId: Long,
            newName: String,
        ): Result<DeepThoughtCategory> {
            val trimmed = newName.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("카테고리 이름이 비어 있습니다."))
            }
            return repository.updateCategory(
                categoryId = categoryId,
                payload = DeepThoughtCategoryUpdatePayload(category = trimmed),
            )
        }
    }
