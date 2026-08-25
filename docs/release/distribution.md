# 비개발자 APK 배포 (Firebase App Distribution)

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

### 릴리스 PR 범위 자동 산출

배포 시점은 위 기준에 따라 사람이 정한다 — `develop` → `main` 릴리스 PR을 여는 것이 곧 배포 결정이다.

릴리스 PR이 열리거나 head가 갱신되면 [`release-scope.yml`](../../.github/workflows/release-scope.yml)이 마지막 성공 배포 이후 `develop`에 머지된 PR과 그 연결 이슈를 모아 PR 본문의 `## 포함 이슈`를 채운다. head가 움직일 때마다 다시 채우므로 머지 직전에 목록을 손으로 대조할 필요가 없다.

`## QA 포인트`는 비어 있을 때만 구성 PR 본문에서 모은 초안으로 채우고, 사람이 쓴 문장이 있으면 건드리지 않는다. 두 섹션은 main push 시 그대로 릴리스 노트가 되므로 배포 전에 테스터가 실행할 문장으로 다듬는다.

별도 API나 유료 AI를 호출하지 않으며 기존 GitHub Actions 실행량만 사용한다. Actions의 **Collect Release Scope**에서 릴리스 PR 번호를 입력해 다시 산출할 수도 있다.

### 배포 — `main` → Firebase App Distribution (자동, 유일한 경로)

여기서 배포는 검증할 `main` 빌드를 Firebase 테스터에게 전달하는 단계이며, Play Store 프로덕션 릴리스를 뜻하지 않는다.

`develop` 수동 배포(`workflow_dispatch`)도 있었으나 #1029에서 제거했다. 도착지와 산출물 버전이 main 경로와 같아 실익이 PR 생성 한 단계뿐이었던 반면, release keystore와 service account를 임의 ref에 노출하는 표면이었다.

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
| `KAKAO_NATIVE_APP_KEY` · `GOOGLE_WEB_CLIENT_ID` · `GOOGLE_SERVICES_JSON_B64` | 배포 APK용 실서비스 앱 설정 (`release-distribution.yml` 전용) |

> base64 인코딩: `base64 -i ~/afternote-release.jks | pbcopy` (macOS)

PR 검증용 lint·unit-test·screenshot은 repository secret 대신
`.github/actions/setup-ci-config`가 만드는 결정적 CI 전용 placeholder를 사용한다. 이 fixture는
배포에 사용할 수 없으며, `release-distribution.yml`은 계속 승인된 환경의 위 secret만 사용한다.

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

