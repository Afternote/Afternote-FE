package com.afternote.feature.mindrecord.domain.model

@JvmInline
value class DeepThoughtCategoryName private constructor(
    val value: String,
) {
    companion object {
        fun from(raw: String): Result<DeepThoughtCategoryName> {
            val trimmed = raw.trim()
            return if (trimmed.isEmpty()) {
                Result.failure(IllegalArgumentException("카테고리 이름이 비어 있습니다."))
            } else {
                Result.success(DeepThoughtCategoryName(trimmed))
            }
        }
    }
}
