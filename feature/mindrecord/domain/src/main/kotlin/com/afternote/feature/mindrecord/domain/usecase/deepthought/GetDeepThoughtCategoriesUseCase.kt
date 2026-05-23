package com.afternote.feature.mindrecord.domain.usecase.deepthought

import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategory
import com.afternote.feature.mindrecord.domain.repository.DeepThoughtRepository
import javax.inject.Inject

class GetDeepThoughtCategoriesUseCase
    @Inject
    constructor(
        private val repository: DeepThoughtRepository,
    ) {
        suspend operator fun invoke(): Result<List<DeepThoughtCategory>> = repository.getCategories()
    }
