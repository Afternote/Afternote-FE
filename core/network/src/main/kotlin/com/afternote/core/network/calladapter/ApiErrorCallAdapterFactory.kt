package com.afternote.core.network.calladapter

import com.afternote.core.network.model.ApiException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTP 응답을 받은 뒤 400..599 실패 본문을 [ApiException]으로 옮긴다.
 *
 * OkHttp 파이프라인 밖인 Retrofit [Call] 경계에서 변환하므로 [ApiException]이 전송 실패를 뜻하는
 * [IOException]을 상속할 필요가 없다. 3xx는 원래 응답으로 돌려 Retrofit이 [retrofit2.HttpException]으로
 * 처리하게 하고, OkHttp가 [Callback.onFailure]로 전달한 전송 예외는 손대지 않는다. 변환 뒤에는
 * Retrofit의 다음 CallAdapter로 넘겨 callback executor 등 기본 호출 의미를 그대로 보존한다.
 */
@Singleton
class ApiErrorCallAdapterFactory
    @Inject
    constructor(
        private val json: Json,
    ) : CallAdapter.Factory() {
        override fun get(
            returnType: Type,
            annotations: Array<Annotation>,
            retrofit: Retrofit,
        ): CallAdapter<*, *>? {
            @Suppress("UNCHECKED_CAST")
            val delegate =
                retrofit.nextCallAdapter(this, returnType, annotations) as CallAdapter<Any, Any>

            return ApiErrorCallAdapter<Any>(
                delegate = delegate,
                json = json,
            )
        }
    }

private class ApiErrorCallAdapter<T>(
    private val delegate: CallAdapter<T, Any>,
    private val json: Json,
) : CallAdapter<T, Any> {
    override fun responseType(): Type = delegate.responseType()

    override fun adapt(call: Call<T>): Any = delegate.adapt(ApiErrorCall(delegate = call, json = json))
}

private class ApiErrorCall<T>(
    private val delegate: Call<T>,
    private val json: Json,
) : Call<T> {
    override fun execute(): Response<T> {
        val response = delegate.execute()
        response.toApiExceptionOrNull(json)?.let { throw it }
        return response
    }

    override fun enqueue(callback: Callback<T>) {
        delegate.enqueue(
            object : Callback<T> {
                override fun onResponse(
                    call: Call<T>,
                    response: Response<T>,
                ) {
                    val apiException =
                        try {
                            response.toApiExceptionOrNull(json)
                        } catch (exception: IOException) {
                            callback.onFailure(this@ApiErrorCall, exception)
                            return
                        }

                    if (apiException == null) {
                        callback.onResponse(this@ApiErrorCall, response)
                    } else {
                        callback.onFailure(this@ApiErrorCall, apiException)
                    }
                }

                override fun onFailure(
                    call: Call<T>,
                    throwable: Throwable,
                ) {
                    callback.onFailure(this@ApiErrorCall, throwable)
                }
            },
        )
    }

    override fun isExecuted(): Boolean = delegate.isExecuted

    override fun cancel() = delegate.cancel()

    override fun isCanceled(): Boolean = delegate.isCanceled

    override fun clone(): Call<T> = ApiErrorCall(delegate = delegate.clone(), json = json)

    override fun request() = delegate.request()

    override fun timeout(): Timeout = delegate.timeout()
}

private fun Response<*>.toApiExceptionOrNull(json: Json): ApiException? {
    if (code() !in 400..599) return null

    val rawBody = errorBody()?.string().orEmpty()
    val parsed = runCatching { json.parseToJsonElement(rawBody).jsonObject }.getOrNull()
    val code = runCatching { parsed?.get("code")?.jsonPrimitive?.intOrNull }.getOrNull() ?: code()
    val serverMessage =
        runCatching { parsed?.get("message")?.jsonPrimitive?.contentOrNull }
            .getOrNull()
            ?.takeUnless { it.isBlank() }
    val message =
        serverMessage
            ?: raw().message.takeUnless { it.isBlank() }
            ?: "요청에 실패했습니다."

    return ApiException(
        status = code(),
        code = code,
        serverMessage = serverMessage,
        message = message,
    )
}
