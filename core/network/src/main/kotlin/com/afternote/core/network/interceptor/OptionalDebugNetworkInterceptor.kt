package com.afternote.core.network.interceptor

import javax.inject.Qualifier

/**
 * 디버그 빌드에서만 피처 모듈이 제공하는 OkHttp [okhttp3.Interceptor] 묶음.
 * 릴리즈에서는 [dagger.multibindings.Multibinds]로 빈 Set이 주입됩니다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class OptionalDebugNetworkInterceptor
