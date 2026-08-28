package com.afternote.feature.afternote.data.repositoryimpl.author

import com.afternote.core.network.model.ApiException
import com.afternote.feature.afternote.domain.error.AfternoteAuthoringValidationKind
import com.afternote.feature.afternote.domain.error.AfternoteFailure
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

private val apiErrorJson =
    Json {
        ignoreUnknownKeys = true
    }

@Serializable
private data class ApiErrorBodyDto(
    val code: Int? = null,
)

/**
 * 저장 API 실패를 presentation이 네트워크·서버·검증 오류로 타입 분기할 수 있는 도메인 예외로 치환한다.
 */
internal fun mapAuthoringFailure(throwable: Throwable): Throwable {
    if (throwable is AfternoteFailure.AuthoringValidation) return throwable

    if (throwable is ApiException) {
        return if (throwable.code == RECEIVERS_REQUIRED_SERVER_CODE) {
            AfternoteFailure.AuthoringValidation(AfternoteAuthoringValidationKind.RECEIVERS_REQUIRED)
        } else {
            throwable
        }
    }

    if (throwable is HttpException && throwable.code() == HTTP_BAD_REQUEST) {
        val body = throwable.response()?.errorBody()?.string() ?: return throwable
        val parsed =
            runCatching {
                apiErrorJson.decodeFromString<ApiErrorBodyDto>(body)
            }.getOrNull()
        if (parsed?.code == RECEIVERS_REQUIRED_SERVER_CODE) {
            return AfternoteFailure.AuthoringValidation(AfternoteAuthoringValidationKind.RECEIVERS_REQUIRED)
        }
    }

    if (throwable is IOException) {
        return AfternoteFailure.NetworkUnavailable(throwable)
    }

    return throwable
}

private const val HTTP_BAD_REQUEST = 400
private const val RECEIVERS_REQUIRED_SERVER_CODE = 475
