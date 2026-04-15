package com.afternote.feature.afternote.data.repositoryimpl

import com.afternote.core.network.model.ApiException
import com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationException
import com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

private val apiErrorJson =
    Json {
        ignoreUnknownKeys = true
    }

@Serializable
private data class ApiErrorBody(
    val code: Int? = null,
)

/**
 * 저장 API 실패 Throwable을 도메인 검증 예외로 치환할 수 있으면 치환한다.
 */
internal fun mapAuthoringFailure(throwable: Throwable): Throwable {
    if (throwable is AfternoteAuthoringValidationException) return throwable

    if (throwable is ApiException && throwable.code == RECEIVERS_REQUIRED_SERVER_CODE) {
        return AfternoteAuthoringValidationException(AfternoteAuthoringValidationKind.RECEIVERS_REQUIRED)
    }

    if (throwable is HttpException && throwable.code() == HTTP_BAD_REQUEST) {
        val body = throwable.response()?.errorBody()?.string() ?: return throwable
        val parsed =
            runCatching {
                apiErrorJson.decodeFromString<ApiErrorBody>(body)
            }.getOrNull()
        if (parsed?.code == RECEIVERS_REQUIRED_SERVER_CODE) {
            return AfternoteAuthoringValidationException(AfternoteAuthoringValidationKind.RECEIVERS_REQUIRED)
        }
    }

    return throwable
}

private const val HTTP_BAD_REQUEST = 400
private const val RECEIVERS_REQUIRED_SERVER_CODE = 475
