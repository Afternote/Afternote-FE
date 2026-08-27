# Afternote-FE

## QA 문서

| 문서 | 내용 |
|---|---|
| [docs/qa/status.md](docs/qa/status.md) | QA 현황 · 회차 기록 · 커버리지 |
| [docs/qa/evidence/README.md](docs/qa/evidence/README.md) | 커밋별 런타임 QA 증거 · 기록 규약 |
| [docs/qa/device-baseline.md](docs/qa/device-baseline.md) | 에뮬레이터 화면 프로파일 · 기기 기준 |
| [docs/qa/assumptions.md](docs/qa/assumptions.md) | 시안 · 명세에 없어 판단으로 정한 것 |

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

# 📦 비개발자 APK 배포 (Firebase App Distribution)

디자이너·PM·QA·외부 베타테스터에게 release APK 를 자동 배포하는 흐름. Firebase 프로젝트 `afternote-android` + 테스터 그룹 `afternote` 사용.

## 운영 자격 인계 (로컬 fallback 담당자만)

일반 배포는 GitHub Actions가 담당하므로 모든 개발자가 이 설정을 가질 필요는 없다. CI 장애 때 로컬 fallback을 수행할 배포 권한 보유자만 다음을 준비한다.

1. **기존 release keystore와 자격 값 인계** — 승인된 보안 저장소에서 기존 서명 파일과 네 값을 받는다. 새 keystore를 임의로 생성하거나 교체하지 않는다. 기존 앱과 다른 키로 서명하면 업데이트할 수 없으므로, 자격을 찾지 못하면 배포를 중단하고 복구·공식 키 교체 절차를 먼저 진행한다.

2. **`local.properties` 끝에 4개 키 추가** (signing config 가 읽음)

    ```properties
    RELEASE_STORE_FILE=/Users/<you>/afternote-release.jks
    RELEASE_STORE_PASSWORD=<keystore 비밀번호>
    RELEASE_KEY_ALIAS=afternote-release
    RELEASE_KEY_PASSWORD=<key 비밀번호>
    ```

3. **`google-services.json` 배치** — Firebase Console → 프로젝트 설정 → 일반 → Android 앱 `com.afternote.afternote_fe` 카드에서 다운로드 → `app/google-services.json`

4. **Firebase CLI 설치 + 인증** (로컬 fallback 업로드용)

    ```bash
    npm install -g firebase-tools
    firebase login
    ```

5. **기존 서명 ID 확인** — `./gradlew :app:signingReport`의 release SHA-1과 `keytool`로 구한 카카오 키 해시가 각 콘솔에 이미 등록된 값과 같은지 확인한다. 다른 값이면 새로 등록해 우회하지 말고 인계받은 keystore가 맞는지부터 확인한다.

## 배포 (매 회)

모든 배포의 릴리스 노트에는 `포함 이슈`와 `QA 포인트`가 필요하다. 둘 중 하나라도 비어 있거나 포함 이슈에 `#123` 형식의 번호가 없으면 Firebase 업로드 전에 실패한다.

### 배포 판단 기준

배포 시점은 일 단위 주기가 아니라 `develop`에 머지된 변경 묶음의 크기와 위험도로 정한다.

- 인증·온보딩·데이터 손실·API 계약·빌드/서명처럼 영향이 큰 변경은 다른 변경을 기다리지 않고 단독 배포한다.
- 작은 변경은 하나의 QA 세션에서 회귀 원인을 구분할 수 있는 범위까지만 묶는다. 서로 다른 사용자 흐름을 한꺼번에 확인해야 하거나 함께 롤백하기 어려워지는 시점이 배포 경계다.
- 수정 결과를 테스터가 확인해야 하는 결함이 머지되면, 묶음 크기와 관계없이 확인 가능한 빌드를 배포한다.
- 현재 묶음의 모든 QA 포인트가 통과한 뒤 `develop`을 `main`으로 승격한다.

### 릴리스 PR 범위 자동 산출

배포 시점은 위 기준에 따라 사람이 정한다 — `develop` → `main` 릴리스 PR을 여는 것이 곧 배포 결정이다.

릴리스 PR이 열리거나 head가 갱신되면 [`release-scope.yml`](.github/workflows/release-scope.yml)이 마지막 성공 배포 이후 `develop`에 머지된 PR과 `Closes`·`Fixes`·`Resolves`로 완료 처리할 이슈를 모아 PR 본문의 `## 포함 이슈`를 채운다. head가 움직일 때마다 다시 채우므로 머지 직전에 목록을 손으로 대조할 필요가 없다.

`## QA 포인트`는 비어 있을 때만 구성 PR 본문에서 모은 초안으로 채우고, 사람이 쓴 문장이 있으면 건드리지 않는다. 두 섹션은 main push 시 그대로 릴리스 노트가 되므로 배포 전에 테스터가 실행할 문장으로 다듬는다.

별도 API나 유료 AI를 호출하지 않으며 기존 GitHub Actions 실행량만 사용한다. Actions의 **Collect Release Scope**에서 릴리스 PR 번호를 입력해 다시 산출할 수도 있다.

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

### 일반 배포 — `main` → Firebase App Distribution (자동)

여기서 배포는 검증할 `main` 빌드를 Firebase 테스터에게 전달하는 단계이며, Play Store 프로덕션 릴리스를 뜻하지 않는다. 일반 배포의 자동 진입점은 `main` push 하나다.

기존 `release-distribution.yml`의 `develop` 수동 배포(`workflow_dispatch`)는 #1029에서 제거했다. 도착지와 산출물 버전이 main 경로와 같아 실익이 PR 생성 한 단계뿐이었던 반면, release keystore와 service account를 임의 ref에 노출하는 표면이었다.

예외적으로 [`firebase-wif-canary.yml`](.github/workflows/firebase-wif-canary.yml)은 `develop` 또는 `main`에서 보호 Environment 승인과 명시적 확인을 받은 뒤 실제 APK를 올리는 **수동 인증 호환성 검증 경로**다. 일반 릴리스나 임의 브랜치 배포 수단으로 사용하지 않으며, production JSON 자격을 WIF로 교체하기 전 판정 절차는 [WIF canary runbook](docs/release/firebase-wif-canary.md)을 따른다.

`develop` → `main` 릴리스 PR 본문에 다음 섹션을 채운다.

```markdown
## 포함 이슈
- #716
- #723

## QA 포인트
- 오프라인에서 오류 안내와 재시도 수단이 표시되는지 확인
- 주차를 변경한 뒤 최신 리포트가 표시되는지 확인
```

PR이 `main`에 머지되면 워크플로가 두 섹션을 릴리스 노트로 사용한다. 연결된 PR이나 필수 섹션을 찾지 못하면 배포하지 않는다. `## QA 포인트`에 사전조건·행동·기대 결과가 없는 generic fallback 문구가 있으면 릴리스 노트 렌더 단계에서 실패한다.

CI 가 사용하는 GitHub Secrets (Settings → Secrets and variables → Actions):

| 키 | 용도 |
|---|---|
| `RELEASE_STORE_FILE_B64` | release keystore 파일 (`~/afternote-release.jks`) 의 base64 인코딩 |
| `RELEASE_STORE_PASSWORD` | keystore 비밀번호 |
| `RELEASE_KEY_ALIAS` | key alias (`afternote-release`) |
| `RELEASE_KEY_PASSWORD` | key 비밀번호 |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | App Distribution Admin 권한 부여된 service account JSON 원문 |
| `KAKAO_NATIVE_APP_KEY` · `GOOGLE_WEB_CLIENT_ID` · `GOOGLE_SERVICES_JSON_B64` | release distribution·WIF canary·dependency audit가 쓰는 실서비스 앱 설정 |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` · `GCP_FIREBASE_SERVICE_ACCOUNT` | 수동 WIF canary 전용 OIDC 설정 ([runbook](docs/release/firebase-wif-canary.md)) |

> base64 인코딩: `base64 -i ~/afternote-release.jks | pbcopy` (macOS)

PR 검증용 lint·unit-test·screenshot은 repository secret 대신
`.github/actions/setup-ci-config`가 만드는 결정적 CI 전용 placeholder를 사용한다. 이 fixture는
배포에 사용할 수 없으며, production `release-distribution.yml`은 계속 승인된 환경의 JSON 자격을 사용한다. WIF canary 성공만으로 이를 제거하지 않는다.

### 배포 provenance — 이 APK 가 어느 commit·run 에서 나왔는지 (#851)

배포 워크플로는 signing 이 끝난 그 APK 하나를 subject 로 [GitHub artifact attestation](https://docs.github.com/en/actions/how-tos/secure-your-work/use-artifact-attestations/use-artifact-attestations)을 발급하고, 업로드 전에 스스로 검증한다. 서명·저장소·signer workflow·source commit·GitHub-hosted 러너 중 하나라도 어긋나거나 attestation subject digest 가 빌드 직후 digest 와 다르면 Firebase 업로드까지 가지 않는다.

성공한 run 의 summary 에 남는 값은 넷이다 — source commit SHA, `sha256:` artifact digest, attestation URL, run URL. APK·AAB 와 R8 mapping 자체는 여기서도 public Actions artifact 로 게시하지 않는다.

받은 APK 가 정말 그 배포 경로에서 나왔는지는 손에 든 파일로 직접 확인할 수 있다.

```bash
gh attestation verify ~/Downloads/afternote-release.apk --repo Afternote/Afternote-FE --signer-workflow Afternote/Afternote-FE/.github/workflows/release-distribution.yml --source-ref refs/heads/main --deny-self-hosted-runners
```

특정 릴리스 commit 으로 좁히려면 `--source-digest <commit SHA>` 를 더한다. 파일이 1비트라도 다르면 digest 가 달라져 검증이 실패한다.

> 아래 로컬 fallback 으로 올린 빌드에는 attestation 이 없다. 그 경로로 배포한 APK 는 위 명령이 실패하는 게 정상이며, 그래서 CI 장애 때만 쓴다.

### 로컬 — 배포 자격 보유자 (fallback / 긴급 시)

`origin/main`의 배포할 commit을 정확히 checkout하고 위 운영 자격을 준비한 뒤 실행한다.

```bash
EVENT_NAME=workflow_dispatch \
ISSUE_NUMBERS="#716, #723" \
QA_POINTS="오프라인 오류 안내 확인;주차 변경 후 재시도 확인" \
SOURCE_REF=main \
SOURCE_SHA="$(git rev-parse HEAD)" \
bash .github/scripts/render-distribution-release-notes.sh /tmp/afternote-release-notes.txt

./gradlew assembleRelease appDistributionUploadRelease \
  --releaseNotesFile=/tmp/afternote-release-notes.txt
```

→ 동일하게 APK 빌드 + Firebase 업로드. CI 장애 시에만 사용한다.

> 일반 로컬·Firebase 빌드는 기본 `versionCode` 1을 유지한다. Play용 다른 값을 빌드할 때는 `AFTERNOTE_VERSION_CODE` 환경변수로 주입할 수 있지만 현재 값을 자동 산출·주입하거나 Play에 배포하는 workflow는 없다. `app/build.gradle.kts`를 직접 수정하지 말고, 업로드 전 이전 Play 산출물보다 큰 값을 확인하는 절차는 [Play release runbook](docs/play-release.md)을 따른다.

## 테스터 관리

- 추가/제거: Firebase Console → App Distribution → 테스터 및 그룹 → `afternote` 그룹 편집
- 신규 테스터는 첫 초대 이메일에서 **App Tester** 앱 설치 안내를 받음 → 이후 빌드는 자동 알림

# 📸 Compose Preview Screenshot Testing (docker baseline)

`Compose Preview Screenshot Testing` 의 anti-aliasing / font hinting / scale 등 host 환경 의존 렌더링 차이로 CI rendered PNG 를 baseline 으로 교체하는 ping-pong 이 발생해 왔다 (PR [#302](https://github.com/Afternote/Afternote-FE/pull/302) / [#322](https://github.com/Afternote/Afternote-FE/pull/322)). 본 리포의 `Dockerfile.screenshot`을 공통 환경으로 사용해 [PR 검증](.github/workflows/screenshot.yml)은 baseline을 검증하고, [baseline 생성 workflow](.github/workflows/screenshot-baseline-generate.yml)는 같은 환경에서 생성 후 재검증한다.

## 로컬 fallback 사전 준비

- Docker 호환 runtime 설치 (macOS 의 Colima/Docker Desktop 또는 Linux Docker)

## Actions 에서 baseline 갱신 (기본 경로)

1. 갱신할 PR 에 `screenshot-baseline` 라벨을 붙인다.
2. 읽기 전용 **Generate Screenshot Baselines** job 이 PR 의 정확한 head SHA 를 CI 표준 Docker 이미지에서 렌더하고 검증한다.
3. 생성 job 은 라벨을 붙인 `pull_request` 권한 경계에서 실행되므로 PR 코드가 default branch cache 를 오염시키지 않는다.
4. 별도 **Apply Screenshot Baselines** job 이 결과가 PNG baseline 경로만 바꾸는지와 PR head 가
   그대로인지 checkout 없이 재검증한 뒤 PR 브랜치에 커밋하고 필수 검사를 다시 요청한다. 성공하면 라벨도 제거된다.

무엇을 캡처할지는 Action 이 화면을 탐색해서 추측하지 않는다. 각 모듈의
`src/screenshotTest/kotlin/**/*ScreenshotTest.kt` 가 Preview 함수, 상태와 device spec 을 선언하며,
Action 은 그 테스트 전체를 실행한다. 새 화면·새 상태를 추가하려면 먼저 screenshot test 를 추가한다.
생성된 이미지는 PR 의 PNG diff 에서 눈으로 최종 확인한다.

## 로컬 baseline 갱신 (Actions 장애 시 fallback)

```bash
docker build --platform linux/amd64 -t afternote-screenshot:latest -f Dockerfile.screenshot .
docker run --rm --platform linux/amd64 -v "$PWD":/workspace -w /workspace afternote-screenshot:latest \
  ./gradlew :core:ui:updateScreenshotTest \
            :feature:home:presentation:updateScreenshotTest \
            :feature:receiver:presentation:updateScreenshotTest \
            :feature:onboarding:presentation:updateScreenshotTest \
            :feature:afternote:presentation:updateScreenshotTest \
            :feature:mindrecord:presentation:updateScreenshotTest \
            --rerun
```

→ 변경된 PNG 가 각 모듈 `src/screenshotTestDebug/reference/...` 에 갱신. `git add` 후 commit.

> 실패한 모듈만 갱신하려면 그 모듈 태스크만 지정한다 — 예: `./gradlew :feature:home:presentation:updateScreenshotTest`
>
> 대상 모듈 목록은 [검증](.github/workflows/screenshot.yml)·[생성](.github/workflows/screenshot-baseline-generate.yml) workflow와 [적용 workflow의 허용 경로](.github/workflows/screenshot-baseline-apply.yml)에 함께 들어 있다. 모듈을 추가·이전했다면 세 workflow와 `Dockerfile.screenshot`의 예시, 이 문서를 함께 갱신한다.

## 로컬 baseline 검증 (CI 실패 재현)

```bash
docker run --rm -v "$PWD":/workspace -w /workspace afternote-screenshot:latest \
  ./gradlew :core:ui:validateScreenshotTest \
            :feature:home:presentation:validateScreenshotTest \
            :feature:receiver:presentation:validateScreenshotTest \
            :feature:onboarding:presentation:validateScreenshotTest \
            :feature:afternote:presentation:validateScreenshotTest \
            :feature:mindrecord:presentation:validateScreenshotTest
```

→ baseline 과 docker 환경에서 새로 그린 PNG 비교. 실패 시 `build/outputs/screenshotTest-results/preview/debug/diffs/` 에서 diff PNG 확인.

## 호스트 직접 실행은 사용하지 않음

`./gradlew :<module>:updateScreenshotTest` 를 host 에서 직접 실행하면 macOS / Linux / JDK 마이너 버전 / 폰트 캐시 차이로 CI 와 baseline 이 어긋난다. docker 환경 통일이 root fix.

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
- `develop`·`main`의 삭제와 강제 push는 금지된다. 머지 조건의 최종 정본은 GitHub의 [활성 ruleset](https://github.com/Afternote/Afternote-FE/rules)과 PR의 Required 상태다.

## 커밋

- 저장소 이력과 이슈 자동화의 type에 맞춘 lowercase Conventional Commit 형식을 사용한다: `<type>(<선택 scope>): <설명>`.
- 주요 type은 `feat`, `fix`, `chore`, `refactor`, `test`, `ci`, `build`, `docs`다.
- 예: `fix(auth): 로그인 실패 안내를 복구한다`, `docs(readme): 기여 절차를 갱신한다`.
- 대문자 `[FEAT]`나 이모지는 요구하지 않는다.

## Issue와 PR

- Issue는 [현재 Issue 템플릿](.github/ISSUE_TEMPLATE/custom.md)을 사용하고, 같은 작업의 기존 Issue가 있으면 새로 만들지 않고 재사용한다.
- PR은 [현재 PR 템플릿](.github/PULL_REQUEST_TEMPLATE.md)을 그대로 채운다. 본문에 같은 저장소의 실제 Issue를 `Refs #N`으로 연결해야 Repository Quality 검사를 통과한다.
- 여러 PR이 같은 Issue를 공유할 수 있다. 그 Issue의 작업을 최종 완료하는 PR에서만 `Closes #N`·`Fixes #N`·`Resolves #N`을 사용한다.
- `QA Metadata`는 변경 경계에 맞게 `app-runtime`·`release-only` 또는 `ci-only`·`covered-by-ci`로 작성하고, 같은 입력과 결과를 확인하는 evidence를 남긴다.
- 필수 검사는 머지 순서 가드, Ktlint, Android Lint, Unit Test, Screenshot, Repository Quality, CodeQL(Java/Kotlin·Actions)다. 이름이나 구성이 바뀌면 README 목록보다 활성 ruleset과 [PR 검증 진입점](.github/workflows/pr-validation.yml)을 우선한다.

## 코드 리뷰

- Draft가 아닌 내부 팀원 PR이 열리거나 리뷰 가능 상태가 되면 [자동 요청 workflow](.github/workflows/review-request-all.yml)가 작성자를 제외한 팀원에게 리뷰를 요청한다. 요청 인원 수와 필수 승인 수는 같은 뜻이 아니다.
- 현재 `develop`·`main` 머지에는 **승인 1건**이 필요하다. `main`은 모든 리뷰 스레드 해결도 필요하다.
- 리뷰 결과는 GitHub의 `APPROVED`·`CHANGES_REQUESTED`·일반 코멘트로 표현한다. 별도 RCA prefix나 12시간 SLA는 두지 않는다.
- [리뷰 적체 가드](.github/workflows/review-debt-guard.yml)는 응답을 기다리는 다른 PR이 남은 팀원의 새 PR을 닫을 수 있다. 각 PR은 팀원 한 명이 먼저 유효한 판정을 내리면 최초 미응답 목록에서 빠진다.
- 변경 요청에 대응한 뒤에는 실질 커밋, `/review-response` 코멘트 또는 해당 리뷰 스레드 답변으로 근거를 남기고 재검토를 받는다. 긴급 PR은 근거가 있을 때만 `review-debt-exempt` 라벨로 적체 가드를 우회한다.
