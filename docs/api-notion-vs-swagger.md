# API 명세 정합성 정리 — Notion ↔ Swagger ↔ 코드

- **기준(소스 오브 트루스): Swagger** `https://afternote.kro.kr/v3/api-docs` (AfterNote API 명세서 1.0.0, 64 endpoints)
- **Notion**: `API 기본 명세서` 허브 문서 (S3 가이드 / 공통 오류 응답 / 명세서 템플릿 / ERD / 사후 관리)
- 작성일: 2026-07-08

---

## 1. Notion ↔ Swagger 불일치 (Notion 문서가 낡음)

### 1-1. S3 Presigned URL 가이드
| 항목 | Notion 가이드 | Swagger (실제) | 판정 |
|---|---|---|---|
| 엔드포인트 | `POST /images/presigned-url` | `POST /api/v1/files/presigned-url` | ❌ 경로 다름 (이미지 전용 → 파일 범용으로 일반화) |
| 응답 이미지 필드 | `imageUrl` | `fileUrl` (+ `fileKey` 추가) | ❌ 필드명 다름 |
| directory 허용값 | profiles, timeletters, afternotes, mindrecords | + `documents` 추가 | ⚠️ Swagger가 더 많음 |
| extension 허용값 | jpg, jpeg, png, gif, webp | + heic, mp4, mov, mp3, m4a, wav, pdf | ⚠️ Swagger가 더 많음(영상/음성/문서) |
| 요청 바디 | `{directory, extension}` | `{directory, extension}` | ✅ 일치 |

### 1-2. 마음의 기록 작성
| 항목 | Notion 가이드 | Swagger (실제) |
|---|---|---|
| 엔드포인트 | 통합 `POST /mind-records` | ❌ 없음. 타입별 분리: `POST /diary`, `POST /deep-thought`, `POST /daily-questions` |
| 요청 필드 | `{type, title, content, date, isDraft, imageList:[{imageUrl}]}` (최대 10장) | 타입별 상이. **이미지 첨부 필드가 아예 없음** (diary/deep-thought/daily-question 요청에 image 없음) |

> Notion의 통합 `/mind-records` + `imageList` 예시는 실제 스웨거와 맞지 않음. 마음의 기록 작성은 이미지 미지원(스웨거 기준).

### 1-3. 에러 응답 봉투
| 항목 | Notion | Swagger / 코드 | 판정 |
|---|---|---|---|
| 「공통 오류 응답」 봉투 | `{status, code, message, data}` | `{status, code, message, data}` | ✅ 일치 (코드 `BaseResponse<T>`도 동일) |
| 「명세서 템플릿」 봉투 | `{isSuccess, code, message, timestamp, result}` (code 1000/4000) | 미사용 | ❌ 옛 양식 (템플릿 문서만의 잔재) |
| 도메인별 에러코드(1000~2000번대) | 상세 표 존재 | Swagger엔 개별 코드 미문서화 | ⚠️ 런타임 확인 필요 |
| S3 에러코드 493/494/495 | 존재 | Swagger 미문서화 | ⚠️ |

---

## 2. Notion 「사후 관리」 ↔ Swagger — **거의 일치** (이름차이만)

Notion 사후관리 설계 문서는 스웨거에 정확히 반영돼 있음.

| Notion | Swagger | 판정 |
|---|---|---|
| 신규 GET `/users/me/receivers/{receiverId}/delivery-conditions` | ✅ 존재 | 일치 |
| 신규 PUT 동일 경로 | ✅ 존재 | 일치 |
| 신규 POST `/users/me/activity` (활동 ping) | ✅ 존재 | 일치 |
| 변경 POST/PATCH `/time-letters` + `deliveryMode`(DATE/POST_DEATH) | ✅ 존재 | 일치 |
| "응답엔 deliveryMode 미포함" | ✅ TimeLetterResponse에 deliveryMode 없음 | 일치 |
| 제거 GET/PATCH `/users/delivery-condition` | ✅ 스웨거에 없음(제거됨) | 일치 |
| enum: DeliveryContentType / DeliveryConditionType / InactivityPeriod / **ConditionState** | contentType / conditionType / inactivityPeriod / **state** | ⚠️ 이름차이(값은 동일) |

- contentType: `TIME_LETTER, AFTERNOTE, DAILY_QUESTION, DIARY, DEEP_THOUGHT` ✅
- conditionType: `INACTIVITY, RECEIVER_REQUEST` ✅
- inactivityPeriod: `THREE_MONTHS, SIX_MONTHS, ONE_YEAR` ✅
- state: `ACTIVE, PENDING_CONFIRMATION, WAITING_VERIFICATION, FULFILLED` ✅

---
