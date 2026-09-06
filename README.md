# Afternote-FE

애프터노트 안드로이드 앱 — Kotlin · Jetpack Compose · Hilt 멀티모듈.

```
app/          진입점 · 네비게이션 · androidTest
core/         common · data · datastore · domain · model · network · ui
feature/      afternote · mindrecord · onboarding · receiver · setting · timeletter
              (각 data · domain · presentation) + home/presentation · timeletter/res
konsist/      아키텍처 규칙 테스트
```

## 문서

| 문서 | 내용 |
|---|---|
| [docs/security/actions-supply-chain.md](docs/security/actions-supply-chain.md) | Actions SHA 고정 · 허용 목록 정책 (조직 수준 감사) |
| [docs/release/distribution.md](docs/release/distribution.md) | 비개발자 APK 배포 (Firebase App Distribution) |
| [docs/play-release.md](docs/play-release.md) | Google Play 내부 테스트 트랙 배포 · versionCode 정책 |
| [docs/testing/screenshot.md](docs/testing/screenshot.md) | Compose Preview 스크린샷 baseline (Docker) |
| [docs/qa/status.md](docs/qa/status.md) | QA 현황 · 회차 기록 · 커버리지 |
| [docs/qa/assumptions.md](docs/qa/assumptions.md) | 시안 · 명세에 없어 판단으로 정한 것 |
| [docs/qa/evidence/README.md](docs/qa/evidence/README.md) | 커밋별 런타임 QA 증거 · 기록 규약 |
| [docs/qa/device-baseline.md](docs/qa/device-baseline.md) | 에뮬레이터 화면 프로파일 · 기기 기준 |

---

# 🚀 신규 팀원 빌드 셋업

`local.properties` 는 `.gitignore` 에 등록되어 있어 **git 으로 받아지지 않는다**. 로컬 개발에서는 clone 직후 다음 두 키를 루트 `local.properties` 에 채우는 방식을 권장한다. 빌드는 같은 이름의 환경변수도 지원하며, 둘 다 있으면 `local.properties` 값이 우선한다. 추가로 아래 **공유 debug keystore** 섹션까지 마치면 카카오 키 해시를 본인 머신용으로 따로 등록할 필요가 없다.

## 필요 키

| 키 | 용도 | 발급 위치 |
|---|---|---|
| `KAKAO_NATIVE_APP_KEY` | 카카오 SDK 초기화 (`KakaoSdk.init`) + 카카오 로그인 콜백 intent-filter 의 `kakao{NATIVE_APP_KEY}` scheme | [Kakao Developers](https://developers.kakao.com) → 내 애플리케이션 → 앱 키 → **네이티브 앱 키** |
| `GOOGLE_WEB_CLIENT_ID` | Google 로그인 시 `CredentialManager.requestGoogleIdToken(serverClientId = ...)` 의 server client id (백엔드가 ID Token 의 `aud` 를 검증할 수 있도록 *Web* client ID 사용) | [Google Cloud Console](https://console.cloud.google.com) → APIs & Services → Credentials → OAuth 2.0 Client IDs → **Web application** 타입 |

## `google-services.json`

`app/google-services.json` 도 `.gitignore` 대상이라 clone 으로 받아지지 않는다. Firebase Console → 프로젝트 설정 → 일반 → Android 앱 `com.afternote.afternote_fe` 카드에서 직접 내려받아 `app/google-services.json` 에 둔다. 콘솔 접근 권한이 없으면 Firebase 프로젝트 관리자에게 멤버 초대를 요청한다.

## `local.properties` 양식

프로젝트 루트의 `local.properties` 끝에 다음 라인 추가:

```properties
KAKAO_NATIVE_APP_KEY=<카카오 네이티브 앱 키>
GOOGLE_WEB_CLIENT_ID=<구글 OAuth web client id>.apps.googleusercontent.com
```

파일을 만들지 않는 환경에서는 같은 이름의 `KAKAO_NATIVE_APP_KEY`·`GOOGLE_WEB_CLIENT_ID` 환경변수를 사용해도 된다.

## 키 수령 채널

신규 팀원은 **`Afternote Debug Build Config` 1Password 항목 관리자에게 요청**해 공유 링크를 받는다. 요청·전달은 Slack·카톡 등 편한 채널로 하면 된다 — 링크에 **수신자 이메일 제한과 만료**가 걸려 있어 지정 주소 밖으로 새도 열리지 않는다. 링크 하나에 아래 셋업에 필요한 값이 모두 들어 있다.

| `local.properties` 키 | 공유 링크의 필드 |
|---|---|
| `KAKAO_NATIVE_APP_KEY` | `kakao_native_app_key` |
| `GOOGLE_WEB_CLIENT_ID` | `google_web_client_id` |
| `DEBUG_STORE_PASSWORD` | `debug_store_password` |
| `DEBUG_KEY_ALIAS` | `debug_key_alias` |
| `DEBUG_KEY_PASSWORD` | `debug_key_password` |
| (공유 debug keystore 파일) | `debug_store_file_b64` |

(직접 발급 권한이 있으면 위 콘솔에서 두 키를 직접 조회해도 된다.)

<details>
<summary>배포자용 — 링크 발급 방법</summary>

```bash
op item share "Afternote Debug Build Config" --emails <요청자 메일> --expires-in 3d
```

`--emails` 를 빼면 **링크를 아는 누구나** 열 수 있으니 반드시 붙인다. 1회만 열리게 하려면 `--view-once` 를 추가. 항목 값을 고친 뒤에는 링크를 **다시 발급해야** 반영된다.

아이템 공유는 **필드 단위 선택이 안 되고 항목 전체가 나간다.** 배포에는 반드시 위 전용 항목을 쓰고, 다른 자격이 섞인 항목은 공유하지 않는다.
</details>

## 누락 시 증상

**release 빌드는 두 키 중 하나라도 비어 있으면 실패한다** — 가드 태스크 `checkKakaoNativeAppKeyForRelease` / `checkGoogleWebClientIdForRelease` 가 `preReleaseBuild` 앞에서 차단한다 (빈 키로 배포된 APK 의 소셜 로그인 전면 불능 재발 방지, #535). release variant 를 조립하는 라이프사이클 태스크(`build`·`assemble`·`bundleRelease`·`lintRelease`, `build-leaf.sh` 의 `:모듈:build` 포함)도 동일하게 실패한다. `check` 는 `preReleaseBuild` 를 타지 않아 영향받지 않는다.

debug 빌드는 빈 값으로도 통과하지만(로컬 개발 편의) 다음이 깨진다:

- `KakaoSdk.init("")` → SDK 초기화 실패 (앱 내 안내: `KAKAO_NATIVE_APP_KEY를 확인해주세요.`)
- `AndroidManifest.xml` 의 `android:scheme="kakao${KAKAO_NATIVE_APP_KEY}"` 가 `kakao` 로 등록 → 정상 `kakao{KEY}` 콜백과 불일치
- `requestGoogleIdToken(serverClientId = "")` → Credential Manager 가 invalid request 로 실패

## 공유 debug keystore

debug 빌드는 기본적으로 머신마다 다른 `~/.android/debug.keystore` 로 서명되어, 카카오 로그인 키 해시를 팀원 머신별로 콘솔에 등록해야 한다. 팀 공유 debug keystore 를 배치하면 전 머신이 동일 키 해시로 서명되어 콘솔 등록이 keystore 1개로 끝난다. (미배치 시에도 빌드는 정상 — 기본 debug keystore 폴백 — 대신 본인 머신 키 해시를 직접 등록해야 카카오 로그인이 동작한다.)

1. **keystore 수령·배치** — 위 1Password 공유 링크의 `debug_store_file_b64` 값을 복사한 뒤 홈 디렉토리에 복원한다. (공유 링크에는 파일 첨부가 실리지 않아 keystore 를 base64 텍스트로 전달한다.)

    ```bash
    pbpaste | base64 -d > ~/afternote-debug-shared.jks
    ```

2. **`local.properties` 끝에 4개 키 추가** (경로는 `~` 없이 **절대경로** — Gradle `file()` 은 `~` 를 확장하지 않는다)

    ```properties
    DEBUG_STORE_FILE=/Users/<you>/afternote-debug-shared.jks
    DEBUG_STORE_PASSWORD=<공유 링크의 debug_store_password>
    DEBUG_KEY_ALIAS=afternote-debug-shared
    DEBUG_KEY_PASSWORD=<공유 링크의 debug_key_password>
    ```

3. **적용 확인** — `./gradlew :app:signingReport` 출력의 `Variant: debug` 에서 `Store:` 가 공유 keystore 경로를 가리키는지 확인

공유 keystore 의 카카오 키 해시 추출 명령 (Kakao Developers → 앱 → 플랫폼 → Android → 키 해시 등록·재확인용):

```bash
keytool -exportcert -alias afternote-debug-shared -keystore ~/afternote-debug-shared.jks | openssl sha1 -binary | openssl base64
```

---

# 💻 코딩 및 패키지 컨벤션

## Kotlin·Compose

- 포맷과 정적 분석은 PR의 Ktlint·Android Lint 필수 검사와 현재 설정을 따른다.
- `feature/*/presentation`은 **기능(화면) 폴더**를 기본 단위로 한다. `screen/`·`viewmodel/`·`component/`로 먼저 쪼개지 않는다.
- 깊이 제한, `shared/` 판정 기준, `*UiState` 위치와 이관 절차는 [presentation 패키지 구조 규칙](docs/convention/presentation-package-structure.md)을 따른다.
- 공용 Composable과 UI 헬퍼는 새로 만들기 전에 [`core/ui` 카탈로그](core/ui/README.md)를 확인한다.

## Android 리소스

- 리소스를 가진 `core/*`·`feature/*` 모듈은 `lower_snake_case`와 모듈별 prefix를 사용한다. `core/*`는 `core_<모듈>_`, `feature/*`는 `<기능>_` 형식이다.
- 적용된 모듈은 `android.resourcePrefix`와 Android Lint가 위반을 막는다. 모듈별 prefix와 예외는 [리소스 네이밍 규칙](docs/convention/resource-naming.md)이 정본이다.

# 🦥 Git·Issue·PR 흐름

## 브랜치와 머지

- 기본·통합 브랜치는 `develop`, Firebase 일반 배포 기준 브랜치는 `main`이다. 일반 변경은 `develop`을 향하고, 릴리스 PR만 `develop`에서 `main`으로 올린다.
- 작업 브랜치는 변경 성격을 나타내는 lowercase prefix와 `/`를 사용한다. 예: `feat/123`, `fix/123-login`, `docs/readme-refresh`. 언더스코어 사용 규칙은 없다.
- 서로 의존하는 변경은 부모 작업 브랜치를 base로 둔 스택 PR을 사용할 수 있다. 부모가 머지되면 base를 `develop`로 이관하고 [머지 순서 가드](.github/workflows/merge-order-guard.yml)를 통과시킨다.
- 스택 PR의 base 갱신은 `PUT /repos/{owner}/{repo}/pulls/{number}/update-branch`로 하지 못한다. 네이티브 스택 멤버는 base가 `develop`인 밑단까지 포함해 이 엔드포인트가 `403 Updating a stacked PR's branch via this endpoint is not supported`로 거절한다. base를 로컬에서 머지하고 평범하게 push하면 통하며, 이 경로는 기존 커밋 SHA를 유지해 리뷰어의 파일 조회 상태와 이미 받은 CI 결과를 살린다. `rebase` 뒤 강제 push는 SHA를 전부 갈아치우므로 둘 다 잃는다.
- `develop`·`main`의 삭제와 강제 push는 금지된다. 머지 조건의 최종 정본은 GitHub의 [활성 ruleset](https://github.com/Afternote/Afternote-FE/rules)과 PR의 Required 상태다.

## 커밋

- 저장소 이력과 이슈 자동화의 type에 맞춘 lowercase Conventional Commit 형식을 사용한다: `<type>(<선택 scope>): <설명>`.
- 주요 type은 `feat`, `fix`, `chore`, `refactor`, `test`, `ci`, `build`, `docs`다.
- 예: `fix(auth): 로그인 실패 안내를 복구한다`, `docs(readme): 기여 절차를 갱신한다`.
- 대문자 `[FEAT]`나 이모지는 요구하지 않는다.

## Issue와 PR

- Issue는 [현재 Issue form](.github/ISSUE_TEMPLATE/issue.yml)을 사용하고, 같은 작업의 기존 Issue가 있으면 새로 만들지 않고 재사용한다.
- PR은 [현재 PR 템플릿](.github/PULL_REQUEST_TEMPLATE.md)을 그대로 채운다. 본문에 같은 저장소의 실제 Issue를 `Refs #N`으로 연결해야 Repository Quality 검사를 통과한다.
- `src/main`에 새로 넣은 Kotlin 함수는 main 어딘가에서 참조되어야 한다. 테스트만 부르거나 아무도 부르지 않으면 Repository Quality가 실패한다. 후속 PR이 곧 소비하는 공개 계약이면 PR 라벨 `test-only-production-exempt`로 경고로 낮추고 본문에 소비처를 적는다.
- 여러 PR이 같은 Issue를 공유할 수 있다. 그 Issue의 작업을 최종 완료하는 PR에서만 `Closes #N`·`Fixes #N`·`Resolves #N`을 사용한다.
- `CI Test Plan`에는 Android 계측 테스트를 `none`·`selected`·`full`로 선언하고 선택 이유를 변경 경계 기준으로 남긴다.
- 필수 검사는 머지 순서 가드, Ktlint, Android Lint, Unit Test, Screenshot, Repository Quality, CodeQL(Java/Kotlin·Actions)다. JavaScript/TypeScript CodeQL도 저장소 자동화 스크립트를 분석한다. 이름이나 구성이 바뀌면 README 목록보다 활성 ruleset과 [PR 검증 진입점](.github/workflows/pr-validation.yml)을 우선한다.

PR Validation은 rename의 이전·현재 경로를 포함한 전체 변경 파일을 기준으로 Ktlint는 직접 변경 모듈, Android Lint·단위 테스트·Kover는 역의존 모듈, Compose screenshot은 영향받는 baseline 모듈만 실행한다. Gradle 전역 설정·build-logic·영향도 계산기 자체가 바뀌거나 분류가 실패하면 전체 검증으로 닫히며, `develop`·`main` push도 전체 검증을 유지한다.

Kover는 임의의 절대 커버리지 목표를 강제하지 않는다. 정확한 `develop` 기준선과 변경 모듈의 line·branch 비율을 비교해 후퇴를 먼저 warning으로 수집하며, 정책 파일의 mode를 별도 검토로 `enforce`로 바꿀 수 있다.

`develop` 병합은 merge queue를 통과한다. 큐가 현재 base 위에 merge group을 만들어 required check를 다시 실행하므로, 낡은 base에서 green을 받은 PR이 그대로 들어갈 수 없다. merge group에는 pull request가 없어 변경 범위를 좁히지 못하므로 검증은 전량으로 돈다 — PR 단계에서 건너뛴 lane도 큐에서는 실행된다. base 최신성을 별도 status로 감시하고 `Require branches to be up to date before merging`으로 강제하던 방식은 큐가 대체했다.

`CI Test Plan`의 `none`은 두 필수 Managed Device check를 에뮬레이터 없이 성공 처리한다. `selected`는 선언한 `path`, fully-qualified `Class#method`, `api30` 또는 `api34`만 실행하고 JUnit XML의 실제 성공 결과까지 확인한다. `full`은 테스트 하네스·Gradle·릴리스 경계 변경에서 전체 API 30 회귀와 API 34 접근성 smoke를 실행한다.

정기·기본 브랜치 수동 검증은 PR critical path 밖에서 minSdk API 26과 targetSdk API 36 경계 smoke도 실행한다. 문서 링크는 PR에서 로컬 경로·heading anchor를 검사하고 외부 URL은 주간 실행으로 분리하며, release AAB/R8 preflight는 `develop`에서 주 2회 조기 검증한다.

## 코드 리뷰

- Draft가 아닌 내부 팀원 PR이 열리거나 리뷰 가능 상태가 되면 [자동 요청 workflow](.github/workflows/review-request-all.yml)가 작성자를 제외한 리뷰 담당 팀원(`TEAM`)에게 리뷰를 요청한다. 요청 인원 수와 필수 승인 수는 같은 뜻이 아니다.
- 현재 `develop`·`main` 머지에는 **승인 1건**이 필요하다. `main`은 모든 리뷰 스레드 해결도 필요하다.
- 리뷰 결과는 GitHub의 `APPROVED`·`CHANGES_REQUESTED`·일반 코멘트로 표현한다. 별도 RCA prefix나 12시간 SLA는 두지 않는다.
- [리뷰 적체 가드](.github/workflows/review-debt-guard.yml)는 응답을 기다리는 다른 PR이 남았거나 자기 PR의 최신 변경요청 뒤 아무 조치도 하지 않은 팀원의 새 PR을 닫을 수 있다. 아래 면제 목록에 있는 작성자는 대상이 아니다. 각 PR은 팀원 한 명이 먼저 유효한 판정을 내리면 최초 미응답 목록에서 빠지고, 작성자가 실질 커밋이나 응답을 남기면 작성자 대기 목록에서 빠진다.
- `awaiting-author` 라벨은 리뷰 게이트 적용 대상 중 변경요청 뒤 작성자 무조치 상태인 PR을 보여 준다. 면제 작성자에게는 붙이지 않으며, 이미 붙은 라벨도 리컨사일러가 제거한다. 새 PR 가드는 라벨 갱신 시점에 의존하지 않고 같은 판정을 현재 열린 PR에 다시 적용한다.
- 쓰기 권한이 있는 리뷰어별 최신 `APPROVED`·`CHANGES_REQUESTED` 가운데 PR 전체에서 가장 늦은 판정을 최종 판정으로 사용한다. 가장 늦은 판정이 승인이면 더 오래된 변경 요청은 자동 해제되고, 승인 뒤에 새 변경 요청이 오면 다시 차단된다.
- 적체 가드에는 라벨 우회가 없다. 가드와 `awaiting-author` 라벨이 함께 쓰는 면제 목록은 [review-debt-guard.yml](.github/workflows/review-debt-guard.yml)의 `REVIEW_GATE_EXEMPT_AUTHORS`이며, 지금은 `koongmai`가 거기에 있다. 면제받은 사람은 리뷰 지적 반영 여부와 무관하게 새 PR을 열 수 있고 남의 PR을 리뷰할 의무가 없다. 그래서 자동 요청 대상에서도 빠지고, 그가 낸 변경요청은 다른 팀원의 빚으로 세지 않는다. 머지 게이트는 면제와 무관하다. `develop` 승인 1건과 필수 체크는 그대로 요구된다.
