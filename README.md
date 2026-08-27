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
| [docs/release/distribution.md](docs/release/distribution.md) | 비개발자 APK 배포 (Firebase App Distribution) |
| [docs/testing/screenshot.md](docs/testing/screenshot.md) | Compose Preview 스크린샷 baseline (docker) |
| [docs/qa/status.md](docs/qa/status.md) | QA 현황 · 회차 기록 · 커버리지 |
| [docs/qa/assumptions.md](docs/qa/assumptions.md) | 시안 · 명세에 없어 판단으로 정한 것 |

---

# 🚀 신규 팀원 빌드 셋업

`local.properties` 는 `.gitignore` 에 등록되어 있어 **git 으로 받아지지 않는다**. clone 직후 다음 두 키를 루트 `local.properties` 에 직접 채워야 카카오·구글 로그인이 정상 동작한다. 추가로 아래 **공유 debug keystore** 섹션까지 마치면 카카오 키 해시를 본인 머신용으로 따로 등록할 필요가 없다.

## 필요 키

| 키 | 용도 | 발급 위치 |
|---|---|---|
| `KAKAO_NATIVE_APP_KEY` | 카카오 SDK 초기화 (`KakaoSdk.init`) + 카카오 로그인 콜백 intent-filter 의 `kakao{NATIVE_APP_KEY}` scheme | [Kakao Developers](https://developers.kakao.com) → 내 애플리케이션 → 앱 키 → **네이티브 앱 키** |
| `GOOGLE_WEB_CLIENT_ID` | Google 로그인 시 `CredentialManager.requestGoogleIdToken(serverClientId = ...)` 의 server client id (백엔드가 ID Token 의 `aud` 를 검증할 수 있도록 *Web* client ID 사용) | [Google Cloud Console](https://console.cloud.google.com) → APIs & Services → Credentials → OAuth 2.0 Client IDs → **Web application** 타입 |

## `local.properties` 양식

프로젝트 루트의 `local.properties` 끝에 다음 라인 추가:

```properties
KAKAO_NATIVE_APP_KEY=<카카오 네이티브 앱 키>
GOOGLE_WEB_CLIENT_ID=<구글 OAuth web client id>.apps.googleusercontent.com
```

## 키 수령 채널

신규 팀원은 위 두 키를 **Slack DM 으로 1hyok 에게 요청**. (직접 발급 권한이 있는 경우 위 콘솔에서 직접 조회 가능.)

## 누락 시 증상

**release 빌드는 두 키 중 하나라도 비어 있으면 실패한다** — 가드 태스크 `checkKakaoNativeAppKeyForRelease` / `checkGoogleWebClientIdForRelease` 가 `preReleaseBuild` 앞에서 차단한다 (빈 키로 배포된 APK 의 소셜 로그인 전면 불능 재발 방지, #535). release variant 를 조립하는 라이프사이클 태스크(`build`·`assemble`·`bundleRelease`·`lintRelease`, `build-leaf.sh` 의 `:모듈:build` 포함)도 동일하게 실패한다. `check` 는 `preReleaseBuild` 를 타지 않아 영향받지 않는다.

debug 빌드는 빈 값으로도 통과하지만(로컬 개발 편의) 다음이 깨진다:

- `KakaoSdk.init("")` → SDK 초기화 실패 (앱 내 안내: `KAKAO_NATIVE_APP_KEY를 확인해주세요.`)
- `AndroidManifest.xml` 의 `android:scheme="kakao${KAKAO_NATIVE_APP_KEY}"` 가 빈 scheme 으로 등록 → 카카오 로그인 콜백 intent-filter 매칭 안 됨
- `requestGoogleIdToken(serverClientId = "")` → Credential Manager 가 invalid request 로 실패

## 공유 debug keystore

debug 빌드는 기본적으로 머신마다 다른 `~/.android/debug.keystore` 로 서명되어, 카카오 로그인 키 해시를 팀원 머신별로 콘솔에 등록해야 한다. 팀 공유 debug keystore 를 배치하면 전 머신이 동일 키 해시로 서명되어 콘솔 등록이 keystore 1개로 끝난다. (미배치 시에도 빌드는 정상 — 기본 debug keystore 폴백 — 대신 본인 머신 키 해시를 직접 등록해야 카카오 로그인이 동작한다.)

1. **keystore 수령·배치** — `afternote-debug-shared.jks` 를 **Slack DM 으로 1hyok 에게 요청** 후 홈 디렉토리에 배치 (예: `~/afternote-debug-shared.jks`)

2. **`local.properties` 끝에 4개 키 추가** (경로는 `~` 없이 **절대경로** — Gradle `file()` 은 `~` 를 확장하지 않는다)

    ```properties
    DEBUG_STORE_FILE=/Users/<you>/afternote-debug-shared.jks
    DEBUG_STORE_PASSWORD=<keystore 비밀번호 — keystore 와 함께 전달>
    DEBUG_KEY_ALIAS=afternote-debug-shared
    DEBUG_KEY_PASSWORD=<key 비밀번호 — keystore 와 함께 전달>
    ```

3. **적용 확인** — `./gradlew :app:signingReport` 출력의 `Variant: debug` 에서 `Store:` 가 공유 keystore 경로를 가리키는지 확인

공유 keystore 의 카카오 키 해시 추출 명령 (Kakao Developers → 앱 → 플랫폼 → Android → 키 해시 등록·재확인용):

```bash
keytool -exportcert -alias afternote-debug-shared -keystore ~/afternote-debug-shared.jks | openssl sha1 -binary | openssl base64
```

---

# 🧭 개발 규칙

## 브랜치

`<type>/<이슈번호>` 형식을 쓴다. 예: `fix/910`, `ci/1028`, `perf/996`

`feat` · `fix` · `refactor` · `test` · `chore` · `docs` · `ci` · `build` · `perf` · `security` · `release`

## 커밋

Conventional Commits — `type(scope): 한글 설명`

```
fix(mindrecord): 주차 조회 실패 뒤에도 화면 안에서 복구할 수 있게 한다
refactor(core): core:di 모듈 삭제 — 바인딩을 구현 옆으로 옮긴다
```

## 코드 스타일

ktlint 가 강제한다. 커밋 전 `./gradlew ktlintFormat` 을 돌린다.

`feature/*/presentation`은 기능(화면) 폴더를 기본 단위로 한다. 깊이 제한과 `shared/` 판정 기준, 이관 절차는 [presentation 패키지 구조 규칙](docs/convention/presentation-package-structure.md)을 따른다.

## PR · 머지

이슈 · PR 템플릿은 [`.github/`](.github/) 에 있다.

`develop` 머지 요건은 **승인 1건 + 필수 체크 8종**이다.

| 필수 체크 |
|---|
| `guard` · `Run Unit Tests` · `Check Code Quality (Ktlint)` · `Check Project Issues (Android Lint)` |
| `Validate Compose Preview Screenshots` · `Repository Quality` · `Analyze (java-kotlin)` · `Analyze (actions)` |
