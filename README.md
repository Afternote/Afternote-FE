# Afternote-FE

# 🚀 신규 팀원 빌드 셋업

`local.properties` 는 `.gitignore` 에 등록되어 있어 **git 으로 받아지지 않는다**. clone 직후 다음 두 키를 루트 `local.properties` 에 직접 채워야 카카오·구글 로그인이 정상 동작한다.

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

# 📦 비개발자 APK 배포 (Firebase App Distribution)

디자이너·PM·QA·외부 베타테스터에게 release APK 를 자동 배포하는 흐름. Firebase 프로젝트 `afternote-b4d3c` + 테스터 그룹 `afternote` 사용.

## 셋업 (1hyok 만 1회 — 신규 인계자도 동일)

1. **Release keystore 생성** (분실 시 앱 업데이트 영구 불가 → 1Password / iCloud 등 2곳 이상 백업 필수)

    ```bash
    keytool -genkeypair -v \
      -keystore ~/afternote-release.jks \
      -keyalg RSA -keysize 4096 -validity 10000 \
      -alias afternote-release
    ```

2. **`local.properties` 끝에 4개 키 추가** (signing config 가 읽음)

    ```properties
    RELEASE_STORE_FILE=/Users/<you>/afternote-release.jks
    RELEASE_STORE_PASSWORD=<keystore 비밀번호>
    RELEASE_KEY_ALIAS=afternote-release
    RELEASE_KEY_PASSWORD=<key 비밀번호>
    ```

3. **`google-services.json` 배치** — Firebase Console → 프로젝트 설정 → 일반 → Android 앱 `com.afternote.afternote_fe` 카드에서 다운로드 → `app/google-services.json`

4. **Firebase CLI 설치 + 인증** (자동 업로드용)

    ```bash
    npm install -g firebase-tools
    firebase login
    ```

5. **콘솔에 신규 keystore SHA 등록** (배포 받은 사람의 카카오/구글 로그인 동작 위해)
   - Release SHA-1 추출: `keytool -list -v -keystore ~/afternote-release.jks -alias afternote-release | grep SHA1`
   - 카카오 키 해시 추출: `keytool -exportcert -alias afternote-release -keystore ~/afternote-release.jks | openssl sha1 -binary | openssl base64`
   - **Kakao Developers** → 앱 → 플랫폼 키 → Android → 키 해시 추가
   - **Firebase Console** → 프로젝트 설정 → Android 앱 → SHA 인증서 지문 추가

## 배포 (매 회)

### 자동 — `main` push 시 (기본 경로)

`develop` → `main` 머지가 push 되면 GitHub Actions 워크플로 [`release-distribution.yml`](.github/workflows/release-distribution.yml) 가 자동 실행 → APK 빌드 → Firebase App Distribution 업로드 → 테스터 그룹 `afternote` 전원에게 자동 이메일 발송. 운영 정책 *"main 머지된 상태만 배포"* 와 일치.

CI 가 사용하는 GitHub Secrets (Settings → Secrets and variables → Actions):

| 키 | 용도 |
|---|---|
| `RELEASE_STORE_FILE_B64` | release keystore 파일 (`~/afternote-release.jks`) 의 base64 인코딩 |
| `RELEASE_STORE_PASSWORD` | keystore 비밀번호 |
| `RELEASE_KEY_ALIAS` | key alias (`afternote-release`) |
| `RELEASE_KEY_PASSWORD` | key 비밀번호 |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | App Distribution Admin 권한 부여된 service account JSON 원문 |
| `KAKAO_NATIVE_APP_KEY` · `GOOGLE_WEB_CLIENT_ID` · `GOOGLE_SERVICES_JSON_B64` | `local.properties` 키 (`lint.yml` 과 공유) |

> base64 인코딩: `base64 -i ~/afternote-release.jks | pbcopy` (macOS)

### 수동 — 1hyok 머신 (fallback / 긴급 시)

```bash
./gradlew assembleRelease appDistributionUploadRelease
```

→ 동일하게 APK 빌드 + Firebase 업로드. CI 장애 시 또는 main 머지 없이 임시 배포 필요할 때 사용.

> 같은 `versionCode` 로 재업로드하면 기존 release 갱신. 새 release 만들려면 `app/build.gradle.kts` 의 `versionCode` 증가.

## 테스터 관리

- 추가/제거: Firebase Console → App Distribution → 테스터 및 그룹 → `afternote` 그룹 편집
- 신규 테스터는 첫 초대 이메일에서 **App Tester** 앱 설치 안내를 받음 → 이후 빌드는 자동 알림

# 📸 Compose Preview Screenshot Testing (docker baseline)

`Compose Preview Screenshot Testing` 의 anti-aliasing / font hinting / scale 등 host 환경 의존 렌더링 차이로 CI rendered PNG 를 baseline 으로 교체하는 ping-pong 이 발생해 왔다 (PR [#302](https://github.com/Afternote/Afternote-FE/pull/302) / [#322](https://github.com/Afternote/Afternote-FE/pull/322)). 본 리포의 `Dockerfile.screenshot` + `.github/workflows/screenshot.yml` 의 container 단계가 baseline 생성·검증을 동일 환경에서 수행해 환경 차이 root fix.

## 사전 준비

- Docker Desktop 설치 (로컬 macOS · Linux 모두 동일)

## 로컬 baseline 갱신 (의도된 시각 변경 시)

```bash
docker build -t afternote-screenshot:latest -f Dockerfile.screenshot .
docker run --rm -v "$PWD":/workspace -w /workspace afternote-screenshot:latest \
  ./gradlew :core:ui:updateScreenshotTest \
            :app:updateScreenshotTest \
            :feature:onboarding:presentation:updateScreenshotTest \
            :feature:afternote:presentation:updateScreenshotTest
```

→ 변경된 PNG 가 각 모듈 `src/screenshotTestDebug/reference/...` 에 갱신. `git add` 후 commit.

## 로컬 baseline 검증 (CI 실패 재현)

```bash
docker run --rm -v "$PWD":/workspace -w /workspace afternote-screenshot:latest \
  ./gradlew :core:ui:validateScreenshotTest \
            :app:validateScreenshotTest \
            :feature:onboarding:presentation:validateScreenshotTest \
            :feature:afternote:presentation:validateScreenshotTest
```

→ baseline 과 docker 환경에서 새로 그린 PNG 비교. 실패 시 `build/outputs/screenshotTest-results/preview/debug/diffs/` 에서 diff PNG 확인.

## 호스트 직접 실행은 더 이상 권장하지 않음

`./gradlew :<module>:updateScreenshotTest` 를 host 에서 직접 실행하면 macOS / Linux / JDK 마이너 버전 / 폰트 캐시 차이로 CI 와 baseline 이 어긋난다. docker 환경 통일이 root fix.

# 🤖 (옵션) Claude Code 워크플로 참고

`docs/claude/` 에 **1hyok** 이 본 repo 에서 [Claude Code](https://claude.com/claude-code) 를 쓰면서 누적한 hook · `CLAUDE.md` 샘플 · 메모리 템플릿이 있다. **강제 아니고 참고용**.

본인 Claude Code 워크플로에 도입하고 싶으면:

```bash
./scripts/install-claude-hooks.sh
```

→ `docs/claude/hooks/*.sh` 를 자기 `.claude/hooks/` 로 symlink (기존 파일 있으면 skip — 덮어쓰기 0). hook 등록·`CLAUDE.md` 일부 가져가기·메모리 도입 등 자세한 가이드는 [`docs/claude/README.md`](docs/claude/README.md) 참고.

본인 `.claude/` 는 `.gitignore` 그대로라 본 폴더와 무관 — 어느 쪽도 다른 쪽을 강제하지 않는다.

# 💻 코딩 컨벤션

> **네이밍 컨벤션**
>
- 네이밍 항목 순서는 android-style-guide를 준수한다.
- 단, Layout을 제외한 네이밍은 CamelCase를 사용한다.
    - 예시) `android:id="@+id/tvPostNovelTitle"`
    - 자세한 정보는 아래 링크를 참고하였다.

[](https://github.com/PRNDcompany/android-style-guide/blob/main/Resource.md)

- Coding Style은 객체지향 생활 체조 원칙을 준수한다.
    - 자세한 정보는 아래 링크를 참고하였다.

[[Java] 객체지향 생활 체조 원칙 9가지 (from 소트웍스 앤솔러지)](https://jamie95.tistory.com/99)

# 🦥 깃 전략 및 컨벤션

> **브랜치 전략**
>
- GitHub Flow를 사용한다.
    - 수시로 코드가 변하는 앱잼의 특성을 고려하였다.
    - 브랜치 이름은 다음과 같이 언더바를 사용한다.
        - 예시) `feat/post_novel`
    - 자세한 정보는 아래 링크를 참고하였다.

[[GIT] 📈 깃 브랜치 전략 정리 - Github Flow / Git Flow](https://inpa.tistory.com/entry/GIT-⚡️-github-flow-git-flow-📈-브랜치-전략)

> **Commit 컨벤션**
>
- 사용할 커밋 타입은 다음과 같다.
    - 🍯 [FEAT] 새로운 기능 추가
    - ♻️ [REFACTOR] 코드 리팩토링
    - 🔨 [FIX] 버그 수정
    - 🚧 [BUILD] 빌드 업무 수정, 패키지 매니저 수정
- 커밋 메시지 예시는 다음과 같다.
    - 예시) `feat: color system 구성`
- 커밋 메시지는 한글로 작성하고, 이슈 번호는 별도로 표기하지 않는다.

> **Issue 컨벤션**
>
- 제목 예시는 다음과 같다.
    - 예시) `feat: library view 구현`

```kotlin
## ⚔️ Kind (Required)    <!-- 이슈 종류를 선택해주세요 -->
`FEATURE` `BUG`

## 📜 Overview (Required)    <!-- 이슈에 대해 간략하게 설명해주세요 -->

> **✔️ To do**    <!-- 진행할 작업에 대해 적어주세요 -->
> - [ ] color system 구성 _(예시)_

## 📍 Note (Optional) <!-- 특이사항을 적어주세요 -->
```

> **PR 컨벤션**
>
- 제목 예시는 다음과 같다.
    - 예시) `feat: bottomNavigation color system 적용`

```kotlin
## 📌𝘐𝘴𝘴𝘶𝘦𝘴
- closed #

## 📎𝘞𝘰𝘳𝘬 𝘋𝘦𝘴𝘤𝘳𝘪𝘱𝘵𝘪𝘰𝘯
- 
- 

## 📷𝘚𝘤𝘳𝘦𝘦𝘯𝘴𝘩𝘰𝘵

## 💬𝘛𝘰 𝘙𝘦𝘷𝘪𝘦𝘸𝘦𝘳𝘴
```

> **Code Review 컨벤션 및 추가정보**
>
- Merge는 리뷰 인원 2명의 승인을 받는다.
- 리뷰 인원으로 할당받은 사람은 12시간 이내에 코드리뷰를 완료한다.
- RCA룰을 통해 Prefix를 적고, 코드 리뷰 반영의 우선순위를 표시한다.
    - R (Request Changes) : 적극적으로 반영을 고려해주세요.
    - C (Comment) : 웬만하면 반영해주세요.
    - A (Approve) : 반영해도 좋고, 넘어가도 좋습니다. 사소한 의견입니다.
        - 예시) `R: @Data 어노테이션 사용은 지양해야 할 것 같습니다. 참고자료 별첨합니다.`