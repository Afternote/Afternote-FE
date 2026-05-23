package com.afternote.feature.mindrecord.domain.usecase.deepthought

import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategory
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategoryCreatePayload
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import javax.inject.Inject

class CreateDeepThoughtCategoryUseCase
    @Inject
    constructor(
        private val repository: DeepThoughtRepository,
    ) {
        suspend operator fun invoke(
            name: String,
            deepThoughtId: Long? = null,
        ): Result<DeepThoughtCategory> {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("카테고리 이름이 비어 있습니다."))
            }
            return repository.createCategory(
                DeepThoughtCategoryCreatePayload(
                    category = trimmed,
                    deepThoughtId = deepThoughtId,
                ),
            )
        }
    }
