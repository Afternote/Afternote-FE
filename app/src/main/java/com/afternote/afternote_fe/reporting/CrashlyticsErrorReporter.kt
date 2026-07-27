package com.afternote.afternote_fe.reporting

import com.afternote.core.common.reporting.ErrorReporter
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.recordException
import javax.inject.Inject

/**
 * [ErrorReporter] 의 Crashlytics 구현.
 *
 * Firebase 의존이 app 모듈 밖으로 새지 않도록 구현체를 여기에만 둔다.
 * SDK 싱글톤([Firebase.crashlytics])은 이 어댑터가 직접 잡는다 — 대체가 필요한 층은
 * SDK 핸들이 아니라 [ErrorReporter] 자체이고, 그걸 갈아끼우는 건 Hilt 바인딩에서 한다.
 *
 * [attributes] 의 각 쌍은 이 예외 리포트 하나에 붙는 커스텀 키가 되어, 콘솔에서 이슈를 열고
 * 개별 이벤트 리포트까지 들어가면 보인다 — 스택트레이스만으로는 구분 안 되는 "어느 단계·어느
 * 수단에서 깨졌는지"를 남기려는 것이다. 같은 키를 다시 넣으면 값이 덮어써지고,
 * 최대 64쌍·쌍당 1kB 제한이 있다.
 * non-fatal 은 즉시 전송이 아니라 다음 fatal 리포트에 실려 가거나 앱 재시작 시 올라간다.
 * `setCustomKey` 로 전역에 심지 않는 이유는 그렇게 하면 이후에 발생하는 무관한 리포트까지
 * 그 값을 달고 올라가기 때문이다.
 * 참고: https://firebase.google.com/docs/crashlytics/android/customize-crash-reports
 *
 * `recordException` 의 KDoc 블록 형태는 KTX **최상위 확장 함수**라
 * `com.google.firebase.crashlytics.recordException` 를 명시적으로 import 해야 잡힌다.
 * 빠뜨리면 `CustomKeysAndValues` 를 받는 자바 오버로드로 해석돼 인자 타입 불일치로 깨진다.
 */
class CrashlyticsErrorReporter
    @Inject
    constructor() : ErrorReporter {
        override fun recordFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            // 뒤에 붙는 람다는 리시버가 KeyValueBuilder 라 key(..) 를 수식어 없이 부를 수 있다.
            // key(..) 는 Unit 을 반환하지만 값이 소실되지 않는다 — SDK 가 빌더 인스턴스를 하나
            // 만들어 이 람다에 넘기고, key(..) 가 거기에 값을 쌓은 뒤 람다가 끝나면 그 빌더를
            // build 해서 CustomKeysAndValues 로 넘긴다. 반환이 아니라 공유 인스턴스가 전달 경로다.
            // (name, value) 는 파라미터 둘이 아니라 Map.Entry 하나를 구조 분해한 것 —
            // 표준 라이브러리가 Map.Entry 에 component1/component2 를 확장으로 제공한다.
            // https://kotlinlang.org/docs/destructuring-declarations.html
            Firebase.crashlytics.recordException(throwable) {
                attributes.forEach { (name, value) -> key(name, value) }
            }
        }
    }
