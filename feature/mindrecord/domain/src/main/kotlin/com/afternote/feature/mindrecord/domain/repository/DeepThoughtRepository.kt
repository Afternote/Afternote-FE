package com.afternote.feature.mindrecord.domain.repository

import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategory
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategoryCreatePayload
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCategoryUpdatePayload
import com.afternote.feature.mindrecord.domain.model.DeepThoughtCreatePayload
import com.afternote.feature.mindrecord.domain.model.DeepThoughtList
import com.afternote.feature.mindrecord.domain.model.DeepThoughtUpdatePayload
import com.afternote.feature.mindrecord.domain.model.RandomDeepThought

interface DeepThoughtRepository {
    suspend fun getList(
        date: String? = null,
        tag: String? = null,
        category: String? = null,
    ): Result<DeepThoughtList>

    suspend fun getRandom(): Result<RandomDeepThought>

    suspend fun create(payload: DeepThoughtCreatePayload): Result<Unit>

    suspend fun update(
        id: Long,
        payload: DeepThoughtUpdatePayload,
    ): Result<Unit>

    suspend fun delete(id: Long): Result<Unit>

    suspend fun getCategories(): Result<List<DeepThoughtCategory>>

    suspend fun createCategory(payload: DeepThoughtCategoryCreatePayload): Result<DeepThoughtCategory>

    suspend fun updateCategory(
        categoryId: Long,
        payload: DeepThoughtCategoryUpdatePayload,
    ): Result<DeepThoughtCategory>

    suspend fun deleteCategory(categoryId: Long): Result<Unit>
}
