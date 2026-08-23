# Afternote-FE

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

# 📦 비개발자 APK 배포 (Firebase App Distribution)

디자이너·PM·QA·외부 베타테스터에게 release APK 를 자동 배포하는 흐름. Firebase 프로젝트 `afternote-android` + 테스터 그룹 `afternote` 사용.

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

모든 배포의 릴리스 노트에는 `포함 이슈`와 `QA 포인트`가 필요하다. 둘 중 하나라도 비어 있거나 포함 이슈에 `#123` 형식의 번호가 없으면 Firebase 업로드 전에 실패한다.

### 배포 판단 기준

배포 시점은 일 단위 주기가 아니라 `develop`에 머지된 변경 묶음의 크기와 위험도로 정한다.

- 인증·온보딩·데이터 손실·API 계약·빌드/서명처럼 영향이 큰 변경은 다른 변경을 기다리지 않고 단독 배포한다.
- 작은 변경은 하나의 QA 세션에서 회귀 원인을 구분할 수 있는 범위까지만 묶는다. 서로 다른 사용자 흐름을 한꺼번에 확인해야 하거나 함께 롤백하기 어려워지는 시점이 배포 경계다.
- 수정 결과를 테스터가 확인해야 하는 결함이 머지되면, 묶음 크기와 관계없이 확인 가능한 빌드를 배포한다.
- 현재 묶음의 모든 QA 포인트가 통과한 뒤 `develop`을 `main`으로 승격한다.

### 머지별 자동 판단

`develop` 대상 PR이 머지되면 [`deployment-decision.yml`](.github/workflows/deployment-decision.yml)이 마지막 성공 QA 배포 이후의 누적 PR·연결 이슈·실제 diff를 읽는다. 고위험 경로, 버그·기능 PR, 누적 이슈 수, 구조화 QA 원천 수, 영향 스코프 수를 판정한 뒤 의미 감사를 거쳐 `QA 배포 권장`·`QA 배포 보류`·`QA 사람 검토 필요`와 실행 가능한 시나리오를 머지된 PR에 코멘트한다. 판단만 자동화하며 APK 업로드는 실행하지 않는다.

의미 감사는 PR 메타데이터를 정본으로 사용한다. Copilot은 원천 ID의 병합과 P0-P3 우선순위만 결정하며 사전조건·행동·기대 결과·위험·근거·제외 사유는 다시 쓰지 못한다. 응답이 원천을 누락하거나 허용되지 않은 필드를 만들면 결과를 폐기한다. 구조화 입력 누락, 개인 Copilot token 누락, 호출 실패, 근거 부족은 generic 문구로 대체하지 않고 `human_review_required`로 남긴다.

개인 Copilot 좌석을 쓰려면 개인 계정 소유의 fine-grained PAT에 Account permission `Copilot Requests`를 부여하고 Actions repository secret `COPILOT_PERSONAL_TOKEN`으로 등록한다. classic PAT는 지원하지 않는다. workflow에는 조직 과금용 `copilot-requests: write` 권한을 두지 않으며, 개인 좌석의 AI credits를 사용한다. 한 감사 실행은 최대 1 AI credit로 제한한다. PAT는 모델 호출 step에만 주입되고 Copilot의 shell·read·write·URL·memory 도구와 built-in MCP는 모두 거부된다. Copilot에는 미배포 PR의 구조화 QA 메타데이터, 연결 이슈 제목·본문, 변경 파일 경로만 전송하며 secret 값은 입력·응답·로그에 넣지 않는다. 자세한 인증·과금 방식은 [GitHub Copilot CLI Actions 문서](https://docs.github.com/en/copilot/how-tos/copilot-cli/automate-copilot-cli/automate-with-actions)를 따른다. 모델을 고정해야 하면 repository variable `QA_SEMANTIC_AUDIT_MODEL`을 설정하고, 비어 있으면 개인 좌석의 기본 모델을 사용한다.

Actions의 **Evaluate QA Distribution Candidate**에서 이미 머지된 PR 번호를 입력해 같은 규칙으로 수동 재검증할 수도 있다.

### PR별 구조화 QA 원천

모든 PR은 `QA Metadata` 섹션의 JSON 객체를 채운다. `app-runtime`·`release-only`는 `precondition`·`action`·`expected`·`risk`·`evidence`가 필요하다. `ci-only`·`covered-by-ci`는 빈 QA 문구 대신 `exclusionReason`과 동일 입력·경계·관찰 결과를 적은 `ci` 또는 `test` evidence가 필요하다. 누락과 `#123 관련 동작을 재현...` 형태의 generic 문구는 Unit Test workflow에서 실패한다. 게이트 도입(`QA_METADATA_GATE_CUTOFF`) 전에 생성된 PR은 섹션이 없으면 검증을 건너뛰므로, 리베이스로 이 workflow를 받아도 소급 차단되지 않는다. 섹션을 채우면 생성 시각과 무관하게 검증한다.

```json
{
  "scope": "app-runtime",
  "precondition": "삭제할 애프터노트가 목록에 있는 로그인 상태",
  "action": "삭제 확인에서 확인을 눌러 DELETE 요청을 보낸다",
  "expected": "성공 시 목록에서 제거되고 실패 시 기존 항목과 오류 안내가 유지된다",
  "risk": "실패한 삭제가 성공처럼 보이거나 기존 항목이 유실될 수 있다",
  "evidence": [
    {
      "kind": "issue",
      "ref": "#550",
      "assertion": "삭제 성공·실패의 관찰 결과를 정의한다"
    }
  ]
}
```

앱 QA 제외 원천은 다음처럼 같은 경계를 검증하는 CI 근거를 구조화한다.

```json
{
  "scope": "ci-only",
  "exclusionReason": "GitHub Actions 제어 변경으로 APK 사용자 흐름이 존재하지 않는다",
  "evidence": [
    {
      "kind": "ci",
      "ref": "Unit Test / Run deployment script tests",
      "assertion": "같은 스크립트 입력과 종료 상태를 CI에서 검증한다",
      "input": "배포 판단 context fixture",
      "boundary": "구조화 메타데이터 파싱부터 최종 JSON 검증까지",
      "observation": "node test가 제외·병합·generic 0건을 단언한다"
    }
  ]
}
```

### QA 배포 — `develop` → Firebase App Distribution (수동, 기본 경로)

GitHub Actions의 **Release Distribution**에서 `Run workflow`를 누르고 ref를 `develop`으로 선택한 뒤 다음 값을 입력한다.

- `issue_numbers`: 포함된 이슈 번호. 예: `#716, #723`
- `qa_points`: 확인할 동작과 기대 결과. 여러 건은 세미콜론(`;`)으로 구분. generic fallback 문구는 거부된다.

워크플로가 위 입력을 릴리스 노트로 만들어 APK를 빌드하고 Firebase App Distribution의 `afternote` 그룹에 배포한다.

### 릴리스 후보 배포 — `main` → Firebase App Distribution (자동)

여기서 릴리스 후보 배포는 검증할 `main` 빌드를 Firebase 테스터에게 전달하는 단계이며, Play Store 프로덕션 릴리스를 뜻하지 않는다.

`develop` → `main` 릴리스 PR 본문에 다음 섹션을 채운다.

```markdown
## 포함 이슈
- #716
- #723

## QA 포인트
- 오프라인에서 오류 안내와 재시도 수단이 표시되는지 확인
- 주차를 변경한 뒤 최신 리포트가 표시되는지 확인
```

PR이 `main`에 머지되면 워크플로가 두 섹션을 릴리스 노트로 사용한다. 연결된 PR이나 필수 섹션을 찾지 못하면 배포하지 않는다.

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

### 로컬 — 1hyok 머신 (fallback / 긴급 시)

```bash
EVENT_NAME=workflow_dispatch \
ISSUE_NUMBERS="#716, #723" \
QA_POINTS="오프라인 오류 안내 확인;주차 변경 후 재시도 확인" \
SOURCE_REF=develop \
SOURCE_SHA="$(git rev-parse HEAD)" \
bash .github/scripts/render-distribution-release-notes.sh /tmp/afternote-release-notes.txt

./gradlew assembleRelease appDistributionUploadRelease \
  --releaseNotesFile=/tmp/afternote-release-notes.txt
```

→ 동일하게 APK 빌드 + Firebase 업로드. CI 장애 시에만 사용한다.

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
