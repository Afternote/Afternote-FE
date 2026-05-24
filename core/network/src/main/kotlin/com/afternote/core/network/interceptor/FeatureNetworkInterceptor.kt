package com.afternote.core.network.interceptor

import javax.inject.Qualifier

/**
 * 피처 모듈이 제공하는 프로덕션 OkHttp [okhttp3.Interceptor] 묶음.
 * MainClient 체인에 합류하므로 인증 헤더처럼 호출 단위로 동적인 헤더를
 * URL 패턴 기준으로 자동 부착하는 용도에 사용.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class FeatureNetworkInterceptor
