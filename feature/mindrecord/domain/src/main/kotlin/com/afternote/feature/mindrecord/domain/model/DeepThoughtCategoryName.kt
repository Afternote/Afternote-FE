package com.afternote.feature.mindrecord.domain.model

@JvmInline
value class DeepThoughtCategoryName private constructor(
    val value: String,
) {
    companion object {
        fun from(raw: String): Result<DeepThoughtCategoryName> {
            val trimmed = raw.trim()
            return if (trimmed.isEmpty()) {
                Result.failure(MindRecordError.EmptyCategoryName)
            } else {
                Result.success(DeepThoughtCategoryName(trimmed))
            }
        }
    }
}
