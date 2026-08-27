# API 명세 정합성 — Notion / OpenAPI / FE

이 문서는 이슈 #423의 7개 HTTP 계약과 현재 FE 소비 상태를 한곳에 정리한다. 결론부터 말하면
활동 기록·수신자별 전달 조건·아이디 찾기는 앱에서 사용 중이고, 비밀번호 찾기와 앱 버전 확인은
네트워크 경계까지만 준비되어 있다. 후자의 화면·도메인 배선은 이 이슈 범위가 아니다.

## 현재 구현 상태

| 엔드포인트 | 인증 | FE 네트워크 | 현재 소비처 |
| --- | --- | --- | --- |
| `POST /api/v1/users/me/activity` | Bearer 필수 | `UserApiService.logActivity` | 로그인 확정 후 앱 실행당 1회 |
| `GET /api/v1/users/me/receivers/{receiverId}/delivery-conditions` | Bearer 필수 | `UserApiService.getReceiverDeliveryConditions` | 설정의 수신자별 전달 조건 화면 |
| `PUT /api/v1/users/me/receivers/{receiverId}/delivery-conditions` | Bearer 필수 | `UserApiService.updateReceiverDeliveryConditions` | 설정의 수신자별 전달 조건 화면 |
| `POST /api/v1/auth/find/send/code` | 불필요 | `AccountApiService.sendFindCode` | 아이디 찾기. `expiresAt`은 아직 UI에서 미소비 |
| `POST /api/v1/auth/email/find` | 불필요 | `AccountApiService.findEmail` | 아이디 찾기 이메일 인증·조회 |
| `POST /api/v1/auth/password/find` | 불필요 | `AccountApiService.findPassword` | 없음. #457 제품 흐름 범위 |
| `GET /api/v1/app/version` | 불필요 | `AppVersionApiService.checkVersion` | 없음. 스플래시 차단 UI는 후속 범위 |

## 검증 기준과 제한

- FE 기준: `origin/develop` `6d1f8f481d3d7f99b5920c05998622beea0b79ae` (2026-08-28).
- 서버 계약 기준: Afternote-BE `release`
  `8bfe43d1637c756d66956fb13a6e60caef448988`. 해당 HEAD의 controller, DTO,
  service, error code와 controller test를 대조했다.
- 공개 OpenAPI `https://afternote.kro.kr/v3/api-docs`는 2026-08-28 05:03 KST에 접속을
  시도했으나 연결 시간이 초과됐다. dev 서버가 03:00~12:00 KST 닫힌다는
  [`docs/qa/status.md`](qa/status.md)의 운영 제약과 일치한다. 따라서 이 버전은 라이브 OpenAPI를
  새로 내려받은 결과가 아니라 위 BE release HEAD를 기준으로 한다.
- 저장소에 남은 Notion 근거는 닫힌 PR #425가 기록한 2026-07-08 `API 기본 명세서` 스냅샷뿐이며
  원문 URL·버전이 없다. 아래 Notion 비교는 그 스냅샷의 재검증이지 현재 Notion 원문을 조회했다는
  뜻이 아니다.

## 공통 응답과 오류 봉투

성공과 실패 모두 다음 봉투를 사용한다.

```json
{
  "status": 200,
  "code": 200,
  "message": "성공",
  "data": {}
}
```

- payload가 없는 성공은 `data: null`이다.
- 실패도 같은 네 필드를 사용하며 HTTP 상태와 `status`가 오류 상태로 바뀐다.
- FE의 `ApiErrorInterceptor`가 비정상 HTTP 응답을 `ApiException`으로 변환한다. 따라서 Retrofit
  메서드의 `BaseResponse<T>` 반환은 성공 응답의 wire 타입을 나타낸다.

## 엔드포인트 계약

### 사용자 활동 기록

```http
POST /api/v1/users/me/activity
Authorization: Bearer <access-token>
```

요청 바디는 없고 성공 `data`는 null이다. 서버는 토큰의 사용자 `lastActiveAt`을 갱신한다.

### 수신자별 전달 조건

조회와 변경은 같은 응답을 사용한다.

```json
{
  "receiverId": 77,
  "conditions": [
    {
      "contentType": "AFTERNOTE",
      "conditionType": "INACTIVITY",
      "inactivityPeriod": "ONE_YEAR",
      "state": "ACTIVE",
      "fulfilled": false,
      "gracePeriodStartedAt": null,
      "fulfilledAt": null
    }
  ]
}
```

PUT 요청은 서버 판정 필드를 제외한 목록만 보낸다.

```json
{
  "conditions": [
    {
      "contentType": "AFTERNOTE",
      "conditionType": "INACTIVITY",
      "inactivityPeriod": "ONE_YEAR"
    }
  ]
}
```

- `contentType`: `TIME_LETTER`, `AFTERNOTE`, `DAILY_QUESTION`, `DIARY`, `DEEP_THOUGHT`
- `conditionType`: `INACTIVITY`, `RECEIVER_REQUEST`
- `inactivityPeriod`: `THREE_MONTHS`, `SIX_MONTHS`, `ONE_YEAR`
- `state`: `ACTIVE`, `PENDING_CONFIRMATION`, `WAITING_VERIFICATION`, `FULFILLED`
- `INACTIVITY`에는 기간이 필수이고, `RECEIVER_REQUEST`의 기간은 null이다.
- `gracePeriodStartedAt`과 `fulfilledAt`은 서버의 timezone 없는 ISO local date-time 문자열이다.

### 계정 찾기 인증번호 발송

```json
// POST /api/v1/auth/find/send/code
// request
{
  "email": "local@example.com"
}

// response.data
{
  "expiresAt": "2026-08-28T03:05:00Z"
}
```

`expiresAt`은 UTC 만료 절대시각이다. 2026-08-02 서버 변경 전의 `data: null` 계약을 사용하면
새 필드를 조용히 버리므로 FE DTO에서는 기본값 없는 필수 필드로 둔다. 현재 repository는 성공 여부만
소비한다. 서버 TTL 기반 UI가 필요해지면 repository/domain 반환 타입부터 별도로 확장해야 한다.

### 아이디(이메일) 찾기

```json
// POST /api/v1/auth/email/find
// request
{
  "email": "local@example.com",
  "certificateCode": "123456"
}

// response.data
{
  "name": "테스터",
  "email": "local@example.com"
}
```

인증번호는 숫자 6자리다. 이 앱의 로그인 아이디가 이메일이므로 서버가 가입 이메일을 그대로 반환한다.

### 비밀번호 찾기·재설정

```json
// POST /api/v1/auth/password/find
{
  "email": "local@example.com",
  "certificateCode": "123456",
  "newPassword": "NewPass1!",
  "confirmPassword": "NewPass1!"
}
```

인증번호 검증과 비밀번호 변경을 한 요청에서 수행하며 성공 `data`는 null이다. 서버 비밀번호 규칙은
8~15자의 영문·숫자·`@$!%*#?&` 특수문자 조합이다.

### 앱 버전 확인

```http
GET /api/v1/app/version?platform=ANDROID&versionCode=10001
```

```json
{
  "updateRequired": false,
  "latestVersionCode": 10001,
  "storeUrl": null
}
```

현재 서버의 platform 값은 `ANDROID` 하나이고 `versionCode`는 1 이상이다. 업데이트가 필요하면
`updateRequired: true`와 Play Store URL을 반환한다. 판정 필드 누락이 업데이트 불필요로 흡수되지
않도록 `updateRequired`, `latestVersionCode`, `storeUrl` 모두 응답 키가 필수이고 `storeUrl` 값만
nullable이다.

## 확인된 오류

아래는 BE release의 실제 service 분기와 `ErrorCode`에 존재하는 오류다. OpenAPI가 모든 도메인 오류를
응답별로 열거하지 않으므로 코드에 없는 오류를 추정해 추가하지 않았다.

| 범위 | HTTP / code | 의미 |
| --- | --- | --- |
| Bearer 필수 3개 API | `401 / 1000` | 인증되지 않은 요청 |
| 전달 조건 | `404 / 1608` | 수신자를 찾을 수 없음 |
| 전달 조건 | `403 / 1002` | 다른 사용자의 수신자 |
| 전달 조건 | `400 / 400` 또는 `400 / 1400` | 빈 조건 목록, 필수 값·INACTIVITY 기간 오류 |
| 계정 찾기 3개 API | `400 / 1219` | 가입되지 않았거나 비활성인 이메일 |
| 계정 찾기 3개 API | `400 / 1702` | 비밀번호가 없는 소셜 로그인 계정 |
| 인증번호 발송 | `429 / 1216`, `429 / 1217` | 재전송 쿨다운 또는 시간당 한도 초과 |
| 이메일/비밀번호 찾기 | `400 / 1207` | 인증번호가 유효하지 않음 |
| 비밀번호 찾기 | `400 / 1218` | 새 비밀번호 확인 불일치 |
| 비밀번호 찾기 | `400 / 1206` | 현재 비밀번호와 새 비밀번호가 같음 |
| 앱 버전 | `400 / 1400` | platform·versionCode 누락/형식/범위 오류 |
| 앱 버전 | `500 / 2501` | 릴리스 정책이 없거나 업데이트용 스토어 URL이 비어 있음 |

## Notion 스냅샷과 현재 계약

2026-07-08 스냅샷은 활동 기록, 수신자별 GET/PUT, 위 네 enum을 사후 관리 설계와 일치한다고
기록했다. 현재 BE release와 FE DTO를 다시 대조해도 경로와 enum 값은 동일하다. 달라진 핵심은
계정 찾기 인증번호 발송 응답에 `expiresAt`이 추가된 점이다. 현재 Notion 원문 확인 전에는 이 변경이
Notion에도 반영됐다고 단정하지 않는다.

## 테스트와 후속 범위

- `ApiWireContractSmokeTest`가 7개 API의 method, 상대 경로, 인증 헤더 경계, 엄격한 요청 JSON,
  query와 응답 역직렬화를 실제 HTTP 소켓을 통해 검증한다. 운영 서버나 계정은 사용하지 않는다.
- DTO 단위 테스트가 `expiresAt`과 앱 버전 필수 판정 필드가 누락됐을 때 디코드 실패하는지 고정한다.
- Docker가 없는 일반 unit test에서는 wire smoke가 skip되고, 전용 `api-contract-smoke` workflow가
  `RUN_API_CONTRACT_SMOKE=true`로 실행한다.
- 비밀번호 찾기 domain/repository/UI는 #457, 앱 버전 조회 실패·강제 업데이트 시 스플래시 차단 UI는
  별도 후속 작업이다. 이 문서와 #423은 두 화면 동작을 구현했다고 간주하지 않는다.
- 제품 결정 #943이 이메일 가입·계정 찾기 갈래를 제거하는 쪽으로 확정되면, 계정 찾기 소비처와 #457
  범위를 다시 판단해야 한다. 네트워크 계약 존재와 제품 노출 여부는 별개다.
