# Google Play release runbook

이 문서는 Firebase App Distribution과 Google Play의 목적을 분리하고, 첫 Play 출시에서 되돌릴 수 없는 서명 결정을 내리기 전에 확인할 기준을 정한다.

## 현재 배포 채널

| 채널 | 목적 | 산출물 | 서명·보호 상태 |
|---|---|---|---|
| Firebase App Distribution | 개발·기획·QA 내부 배포 | release APK | 팀 보관 release key로 직접 서명 |
| Google Play | 내부 테스트를 거쳐 production 배포 | release AAB | Play App Signing 등록 전 |

- Firebase APK 배포는 Google Play 출시 뒤에도 내부 QA 용도로만 사용한다.
- AAB는 기기에 직접 설치하는 파일이 아니다. Play 내부 테스트 트랙 또는 bundletool로 생성한 APK를 통해 검증한다.
- Play Console 등록·키 업로드·production 승격은 이 문서의 로컬 검증과 별개의 외부 상태다.

### Play Console 확인 상태

2026-09-06 기준, 개발자 계정이 개설됐고 FE 계정도 초대를 받아 콘솔에 들어간다. 다만 계정 설정이 아직 끝나지 않아 앱을 만들 수 없다. 아래는 그날 콘솔을 직접 열어 확인한 것이다.

| 확인한 것 | 상태 | 확인 경로 |
|---|---|---|
| 개발자 계정 | 개인 계정으로 존재. 계정 ID `7986315520990588977` | `play.google.com/console` 접속 시 개발자 계정 선택 화면 |
| 신원 확인 | Google 심사 중. 문서는 업로드됨, 완료 시 계정 소유자에게 메일 | 홈의 "개발자 계정 설정 완료" 카드 |
| 연락처 전화번호 인증 | 미완료 | 홈 카드 → `account/phone-verification-issue-details` |
| 앱 등록 | 불가. "앱 만들기" 버튼이 잠겨 있다 | 홈의 "첫 번째 앱 만들기" |
| 패키지 이름 등록 | 불가 | 좌측 "Android 개발자 인증" |
| 초대받은 FE 계정 권한 | 제한됨 | "사용자 및 권한" 이 "권한이 필요함" 으로 막힘 |

잠긴 사유는 하나로 모인다. 앱 만들기 버튼의 안내가 "새 앱을 만들려면 계정 확인을 완료하세요" 이고, 패키지 이름 등록도 "먼저 홈페이지에서 처리되지 않은 인증을 완료해야 합니다" 라고 같은 곳을 가리킨다.

그래서 아래 "자동화 사전 준비" 는 1번(앱 등록)부터 대기 상태이고, 2~5번은 전부 1번에 매달려 있다.

### 지금 막혀 있는 것과 푸는 사람

| 막힌 것 | 푸는 주체 | 비고 |
|---|---|---|
| 신원 확인 | Google 심사 | 기다리는 것 말고 할 수 있는 일이 없다. 며칠 소요 안내 |
| 연락처 전화번호 인증 | 개발자 계정 소유자 | 초대받은 계정에는 버튼이 비활성이다. 안내 문구가 "계정 소유자만 연락처 전화번호를 인증할 수 있습니다" |
| 앱 등록 | 위 둘이 끝난 뒤 계정 소유자 또는 앱 생성 권한을 받은 사람 | 앱 이름·기본 언어·유형은 아래 사전 준비 1번 참고 |
| 서비스 계정에 Play 권한 부여 | 계정 소유자 | 초대받은 FE 계정은 "사용자 및 권한" 과 API 액세스 경로가 열리지 않는다 |

저장소 쪽은 자격이 필요 없는 부분까지 끝내 두었다. 아래 "자동화 사전 준비" 5번의 environment 와 보호 규칙, 변수는 서 있고 남은 것은 값이 있어야 넣는 secret 여섯 개뿐이다.

Play에 처음 올릴 AAB에는 담당자가 보관한 upload key가 필요하다(`local.properties`의 `RELEASE_*` 네 키). 이 값이 없는 일반 로컬 빌드는 `checkReleaseSigningForRelease`에서 멈춘다. 다만 아래 [Release AAB Preflight](#release-aab-preflight)는 CI 전용 설정과 일회용 서명으로 release AAB를 검증하므로, Play 계정 설정이나 팀 keystore 인계를 기다리지 않고 실행할 수 있다. 이 검증용 AAB는 Play에 업로드하지 않는다.

### 앱이 생긴 뒤 다시 볼 것

Play Console의 **Test and release > App integrity** 에서 두 항목을 확인하고 이 절을 갱신한다. 그 페이지는 앱 단위라 앱 등록 전에는 열리지 않으므로, 지금은 판정 자체가 불가능하다.

- Play App Signing 등록 상태
- Automatic integrity protection 메뉴와 opt-in 제공 여부

## Release AAB Preflight

[`release-aab-preflight.yml`](../.github/workflows/release-aab-preflight.yml)의 **Build and verify release AAB** job은 R8 minify·resource shrinking을 적용한 release 빌드가 패키징되고 기동하는지 확인한다. Play·Firebase 자격이나 보호 environment를 사용하지 않으며 배포 단계도 없다.

### 실행 조건과 확인 범위

| 진입점 | 검사 대상 |
|---|---|
| PR | 같은 저장소의 `develop` → `main` PR. 다른 head 브랜치와 fork PR은 이 job을 건너뛴다. checkout은 PR merge ref다. |
| 예약 | 매주 화·금 03:37 KST (`37 18 * * 1,4` UTC). 기본 브랜치(현재 `develop`)의 최신 commit을 검사한다. |
| 수동 | **Actions → Release AAB Preflight → Run workflow**에서 선택한 ref |

1. [`setup-ci-config`](../.github/actions/setup-ci-config/action.yml)가 실서비스 값이 없는 CI placeholder를 만들고, [`setup-ci-release-signing`](../.github/actions/setup-ci-release-signing/action.yml)이 한 번의 run에만 쓸 keystore와 CI 전용 서명 설정을 만든다. 팀 release keystore가 없어도 이 경로로 검증할 수 있다.
2. [`run-release-aab-preflight.sh`](../.github/scripts/run-release-aab-preflight.sh)가 release AAB를 빌드하고, 필수 bundle 항목·JAR 서명·비어 있지 않은 R8 mapping과 manifest의 versionCode를 검증한다. Crashlytics mapping 업로드는 제외한다.
3. 버전과 SHA-256이 고정된 bundletool로 universal APK를 생성하고, 다운로드 예상 크기와 APK 파일 크기를 계산한다.
4. [`run-release-startup-smoke.sh`](../.github/scripts/run-release-startup-smoke.sh)가 그 APK를 다시 빌드하지 않고 Android 34 에뮬레이터에 `adb`로 설치·실행한다. 실행 뒤 20초 시점에 logcat의 `FATAL EXCEPTION`과 앱 프로세스 생존 여부를 검사한다.
5. [`test-release-aab-negative-fixtures.sh`](../.github/scripts/test-release-aab-negative-fixtures.sh)가 잘못된 R8 규칙·빈 AAB·누락된 R8 mapping·누락된 bundle 항목·서명 변조의 음성 fixture를 거부하는지 확인한다.

CI placeholder와 일회용 서명을 쓰므로, 이 검사는 실서비스 로그인·Play에서 설치한 앱의 업데이트·Play 보호 기능 검증을 대신하지 않는다. 실제 키를 쓰는 [로컬 AAB 검증](#aab-빌드와-로컬-검증)과 [첫 내부 테스트 릴리스](#첫-내부-테스트-릴리스)는 별도다.

### 보고서와 실패 판단

run summary의 **Release AAB preflight**에는 source SHA, AAB·서명 인증서·R8 mapping의 SHA-256과 크기가 나온다. **Release startup smoke: PASS/FAIL** 및 job의 최종 결과도 함께 확인한다. 크기 보고서는 기동 검사 전에 생성되므로 보고서가 있다는 것만으로 전체 통과는 아니다.

성공한 run의 `release-aab-preflight-report` artifact에는 `release-aab-preflight.json`과 `release-aab-preflight.md`만 90일 보관한다. AAB·APK set·universal APK·R8 mapping·keystore는 artifact에 포함하지 않는다.

| 보고서 항목 | 읽는 법 |
|---|---|
| `AAB archive` | AAB ZIP 파일 크기 |
| `Estimated download (min/max)` | bundletool이 APK set에서 계산한 최소·최대 다운로드 예상 크기. 기기별 Play 실측값은 아니다. |
| `Installable universal APK` | 생성한 universal APK 파일 크기. 설치 후 저장공간 사용량은 아니다. |
| `Baseline`·`Change from baseline` | 직전 성공 preflight run의 공개 보고서와 비교한다. source SHA를 대조하며, 같은 브랜치나 특정 배포본만을 기준으로 고르는 것은 아니다. |
| `⚠️`·`policy.meaningfulIncrease` | 어느 지표든 **5% 이상 또는 1 MiB(1,048,576 bytes) 이상 증가**하면 경고한다. 두 조건을 모두 충족할 필요는 없고, 이 경고 자체로 job이 실패하지 않는다. |
| `baseline unavailable` | 이전 성공 run이나 보고서가 없어 비교를 수행하지 않았다. 크기 회귀가 없다는 뜻이 아니다. 현재 값으로 첫 기준을 남긴다. |

크기 경고가 있으면 baseline/source SHA와 증가한 지표를 확인하고, 추가된 리소스·의존성·R8 설정으로 설명되는지 릴리스 PR에서 검토한다. baseline API 조회나 다운로드 자체가 실패하면 **Restore the previous public size report**에서 멈추므로, `baseline unavailable`과 구분한다.

job이 실패하면 해당 step 로그를 먼저 확인한다. 서명·필수 항목·mapping 오류는 빌드/검증 단계, bundletool checksum·APK 생성 오류는 bundletool 단계, KVM·AVD 부팅·앱 크래시는 기동 단계에서 구분한다. 기동 실패는 summary에 진단이 있는 경우 함께 보고, 음성 fixture 실패는 검증기가 잘못된 산출물을 통과시킨 것인지 로그의 예상 실패 원인과 대조한다. 수정 후 preflight 전체를 다시 실행한다.

## 내부 테스트 트랙 자동 배포

[`release-play-internal.yml`](../.github/workflows/release-play-internal.yml)이 `main`에서 수동 실행 + `play-internal` environment 승인을 받아 signed AAB를 Play **내부 테스트 트랙에만** 올린다. production·open·closed 트랙으로 승격하는 단계는 워크플로에도 업로드 스크립트에도 없다. 승격은 Play Console에서 사람이 한다.

Firebase App Distribution 경로([`release-distribution.yml`](../.github/workflows/release-distribution.yml))와 별개다. 그쪽은 `main` push마다 자동으로 APK를 QA 그룹에 뿌리고, 이쪽은 사람이 눌러야 움직이는 AAB 경로다. 둘은 함께 돌지 않으며 서로의 자격도 공유하지 않는다.

### versionCode 정책

| 빌드 | versionCode | 산출 주체 |
|---|---|---|
| 로컬·CI 검증·Firebase App Distribution | `1` (고정) | `build-logic/src/main/kotlin/VersionCode.kt`의 기본값 |
| Play 내부 테스트 트랙 | `run_number * 100 + run_attempt` | [`resolve-play-version-code.mjs`](../.github/scripts/resolve-play-version-code.mjs) |

- 워크플로는 `run_number`와 `run_attempt`로 후보 versionCode를 만든다. 같은 run의 재실행은 `run_attempt`가 올라가지만, 오래된 run을 재실행하거나 Console에서 더 큰 값을 올렸다면 Play 최댓값 비교에서 거부될 수 있다.
- 업로드 **전에** Play가 알고 있는 최대 versionCode(업로드된 bundle + 모든 트랙의 release)를 조회해, 산출값이 그보다 크지 않으면 빌드 전에 멈춘다.
- 빌드가 끝난 뒤에도 업로드 직전의 새 edit에서 bundle과 모든 트랙의 최댓값을 다시 조회한다. 후보가 그보다 크지 않으면 업로드하지 않는다. Play가 업로드 응답으로 돌려준 versionCode가 기대값과 달라도 트랙을 건드리지 않는다.
- AAB 검증 단계는 pinned bundletool로 manifest의 실제 versionCode를 읽어 `AFTERNOTE_VERSION_CODE`와 대조한다. 워크플로는 resolver의 값을 빌드와 검증 모두에 전달하므로, 환경변수 주입이 빠진 AAB도 게시 전에 거부한다.
- `app/build.gradle.kts`의 versionCode를 손으로 올리지 않는다. 로컬에서 Play용 값이 필요하면 `AFTERNOTE_VERSION_CODE` 환경변수로 주입한다.

### 자동화 사전 준비

워크플로가 참조하는 설정은 사람이 웹 UI에서 준비한다. 누락 검사는 아래 두 단계로 나뉘며, 모두 빌드 전에 값 대신 누락된 이름만 출력하고 실패한다. 첫 검사의 통과만으로 서명·앱 설정이 준비됐다는 뜻은 아니다.

| 검사 단계 | 누락을 확인하는 값 |
|---|---|
| `Verify Play publishing configuration is set` | `GCP_WORKLOAD_IDENTITY_PROVIDER`, `GCP_PLAY_SERVICE_ACCOUNT`, `PLAY_PACKAGE_NAME` |
| `Set up release build configuration` → [`setup-release-config`](../.github/actions/setup-release-config/action.yml)의 `Verify required secrets are set` | `RELEASE_STORE_FILE_B64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`와 앱 설정 `KAKAO_NATIVE_APP_KEY`, `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_SERVICES_JSON_B64` |

두 번째 검사는 JDK·Gradle 설정 뒤에 실행된다. 값이 비어 있지 않은지 보는 검사이므로, 실제 Play 접근 권한이나 서명 파일의 유효성은 뒤의 인증·API 조회·빌드·AAB 검증에서 확인한다.

**1. Play Console — 앱 등록** (`play.google.com/console`)

1. 개발자 계정 생성(1회 등록비). 조직 계정은 D-U-N-S 번호가 필요하다.
2. **모든 앱 → 앱 만들기**: 앱 이름 `Afternote`, 기본 언어 한국어, 유형 `앱`, 무료.
3. **정책 및 프로그램 → 앱 콘텐츠**의 필수 선언(개인정보처리방침 URL, 광고, 콘텐츠 등급, 타겟층, 데이터 보안)을 모두 채운다. 내부 테스트도 이게 비면 릴리스를 만들 수 없다.
4. **테스트 및 출시 → 테스트 → 내부 테스트 → 테스터**에서 테스터 이메일 목록을 만든다.

**2. Play Console — 첫 AAB 수동 업로드**

Android Publisher API는 **Console에서 최소 한 번 수동 업로드된 앱**에만 업로드를 허용한다. 첫 AAB는 `./scripts/verify-play-release-bundle.sh`로 만든 산출물을 **테스트 및 출시 → 내부 테스트 → 새 버전 만들기**에서 직접 올린다.

- 이때 `AFTERNOTE_VERSION_CODE` 없이 빌드해 versionCode `1`을 쓴다. 워크플로가 만드는 첫 값은 `101`이라 단조 증가 조건을 자동으로 만족한다.
- 이 업로드에서 Play App Signing 방식이 확정된다. 방식은 아래 「Play App Signing 키 결정」에서 기본안으로 확정해 두었으니, 업로드 전에 그 절을 읽고 화면에서 같은 쪽을 고른다.

**3. Google Cloud — API와 서비스 계정** (`console.cloud.google.com`)

1. **API 및 서비스 → 라이브러리**에서 `Google Play Android Developer API`를 사용 설정한다.
2. **IAM 및 관리자 → 서비스 계정 → 서비스 계정 만들기**: 이름 예 `play-internal-publisher`. **프로젝트 IAM 역할은 주지 않는다** — Play 권한은 Play Console에서 별도로 준다.
3. Firebase 배포용 서비스 계정을 재사용하지 않는다. 두 채널의 자격을 분리해 두는 것이 이 이슈의 요구사항이다.
4. **IAM 및 관리자 → Workload Identity 제휴**에서 기존 provider를 재사용하되, 새 서비스 계정에 `roles/iam.workloadIdentityUser`를 부여하면서 principal을 이 저장소로 한정한다.

**4. Play Console — 서비스 계정에 최소 권한 부여**

**설정 → API 액세스**에서 위 Google Cloud 프로젝트를 연결한 뒤, **사용자 및 권한 → 사용자 초대**로 서비스 계정 이메일을 추가한다.

- 앱 범위: `Afternote` 하나만 선택한다(계정 전체 권한을 주지 않는다).
- 체크할 권한: **앱 정보 보기**, **테스트 트랙에 출시**.
- 해제할 권한: **프로덕션 트랙에 출시**, 재무·주문 관리, 사용자 관리.

**5. GitHub — environment와 자격**

environment 와 보호 규칙, 변수는 2026-09-06 에 만들어 두었다. 남은 것은 secret 여섯 개이고, GitHub API 가 기존 값을 돌려주지 않으므로 `release-distribution` 에 같은 이름이 있어도 복사할 수 없다. 값을 쥔 사람이 직접 넣어야 한다.

| 항목 | 상태 |
|---|---|
| environment `play-internal` | 생성됨 |
| Required reviewers | `1hyok` |
| Deployment branches | `main` 하나 |
| `PLAY_PACKAGE_NAME` | `com.afternote.afternote_fe` 로 설정됨 |
| secret 6종 | 미등록 |

승인 규칙에 한 가지 주의가 있다. `prevent self review` 는 꺼져 있어서 워크플로를 실행한 사람이 자기 배포를 스스로 승인할 수 있다(`release-distribution` 도 같다). 승인을 남이 눌러 주는 관문으로 기대하지 말 것. 필요하면 승인자를 한 명 더 지정하거나 그 설정을 켠다.

아래는 그 설정을 손으로 다시 만들거나 확인할 때의 원본이다. **Settings → Environments** 에서 본다.

| 설정 | 위치 | 값 |
|---|---|---|
| Required reviewers | play-internal → Deployment protection rules | 배포 담당자(최소 1명) |
| Deployment branches | play-internal → Deployment branches and tags | `Selected branches` → `main` |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | play-internal → Environment secrets | `projects/<번호>/locations/global/workloadIdentityPools/<pool>/providers/<provider>` |
| `GCP_PLAY_SERVICE_ACCOUNT` | play-internal → Environment secrets | `play-internal-publisher@<프로젝트>.iam.gserviceaccount.com` |
| `RELEASE_STORE_FILE_B64` | play-internal → Environment secrets | upload key keystore의 base64 |
| `RELEASE_STORE_PASSWORD` | play-internal → Environment secrets | keystore 비밀번호 |
| `RELEASE_KEY_ALIAS` | play-internal → Environment secrets | key alias |
| `RELEASE_KEY_PASSWORD` | play-internal → Environment secrets | key 비밀번호 |
| `PLAY_PACKAGE_NAME` | play-internal → Environment variables | `com.afternote.afternote_fe` |

`KAKAO_NATIVE_APP_KEY`·`GOOGLE_WEB_CLIENT_ID`·`GOOGLE_SERVICES_JSON_B64`는 이미 저장소 secret으로 있어 그대로 쓴다. keystore·비밀번호·서비스 계정 값은 담당자가 직접 입력하며 저장소나 문서에 넣지 않는다.

### 실행

**Actions → Release Play Internal Track → Run workflow**에서 브랜치 `main`을 선택해 실행한다. environment 승인자가 승인해야 job이 시작된다. 지금 승인자는 한 명이고 self review 가 막혀 있지 않으므로, 그 승인은 남의 확인이 아니라 실행자의 두 번째 확인이다.

워크플로가 하는 일:

1. `main`이 아니면 거부한다. Play 연결값 3개를 먼저 검사하고, JDK·Gradle 설정 뒤 `setup-release-config`에서 서명값 4개와 앱 설정 3개를 검사·복원한다.
2. WIF로 조회용 단기 토큰을 받아 Play의 현재 최대 versionCode를 읽는다. 조회 edit는 commit하지 않고 삭제를 시도한다.
3. 단조 증가 versionCode를 확정한다. 현재 최댓값보다 크지 않으면 AAB를 만들기 전에 멈춘다.
4. source commit에 연결된 PR 중 `main`에 병합된 가장 최근 PR의 본문을 읽어 Play 릴리스 노트를 만든다. 생성한 문구는 run summary에도 남긴다.
5. 확정된 versionCode로 signed AAB를 빌드하고, 서명·필수 항목·R8 mapping·manifest의 실제 versionCode를 검증한다.
6. AAB에 SLSA provenance를 붙이고 이 워크플로·이 commit으로 검증한다.
7. 업로드 직전에 토큰을 새로 받고 digest를 다시 확인한다. 이어 새 edit에서 Play의 최신 versionCode 최댓값을 재검사한다. 통과하면 bundle 업로드 → internal 트랙 갱신 → commit 순으로 게시하며, 생성한 릴리스 노트를 함께 전달한다.
8. run summary에 track·versionCode·source SHA·AAB digest·attestation·Play edit id를 남긴다.

### Play 릴리스 노트

[`play-internal-track.mjs`](../.github/scripts/play-internal-track.mjs)는 선택한 main PR **본문 전체**에서 `#N` 형식의 양의 이슈 번호를 추출한다. `## 포함 이슈` 섹션이나 `Closes` 문구로 범위를 제한하지 않으며, 본문 등장 순서를 유지하면서 각 번호의 첫 등장만 남겨 중복을 제거한다.

Play의 **What's new**에는 `ko-KR` 한 언어로 내부 테스트 versionCode, source ref·짧은 SHA, 추출한 이슈 목록이 들어간다. 스크립트는 JavaScript 문자열 길이 기준 **최대 500 UTF-16 code units**로 제한하고, 초과하면 499 code units 뒤에 `…`를 붙인다. 이는 화면에서 보이는 글자 수와 다를 수 있다.

연결된 main PR이 없거나 본문에 이슈 번호가 없으면 versionCode·source 정보만 남긴다. Play 경로는 `QA 포인트`를 읽거나 필수로 요구하지 않는다. Firebase 배포에서 요구하는 두 섹션은 [Firebase 릴리스 노트 규칙](release/distribution.md#배포-매-회)을 따른다.

### 실패 모드

| 상황 | 어디서 멈추는가 |
|---|---|
| Play 연결값 3개 누락 | `Verify Play publishing configuration is set` |
| 서명값 4개·앱 설정 3개 중 누락 | `Set up release build configuration`의 필수 secret 검사 |
| `main` 이외의 ref | keystore를 풀기 전 |
| versionCode 중복·역행 | 빌드 전, 그리고 업로드 직전 새 edit에서 다시 검사 |
| AAB manifest의 versionCode가 기대값과 다름 | AAB 검증 단계. Play 업로드 전 |
| 잘못 서명된 AAB | `scripts/verify-play-release-bundle.sh` |
| 권한 부족·API commit 실패 | 업로드 스텝. 미완료 edit를 삭제한 뒤 원인을 그대로 올린다 |

어느 단계에서 실패하든 열린 edit는 정리되고, AAB·mapping·keystore는 러너에서 삭제된다. Actions artifact로는 게시되지 않는다.

### 롤백

Play는 이미 게시된 versionCode를 되돌리지 않는다.

1. Play Console **내부 테스트 → 출시 관리**에서 문제 릴리스를 중단(halt)한다.
2. 수정본은 **더 큰 versionCode**로 다시 배포한다. 새 run의 후보값도 Play 최댓값보다 커야 하며, 오래된 run 재실행은 거부될 수 있다.
3. Firebase App Distribution은 별개 채널이라 영향을 받지 않는다. 테스터에게 급히 검증본을 줘야 하면 그쪽 경로를 쓴다.

## AAB 빌드와 로컬 검증

루트에서 담당자의 서명·앱 설정을 준비한 뒤 다음 명령을 실행한다. 팀 keystore가 없으면 위 [Release AAB Preflight](#release-aab-preflight)를 사용한다.

~~~bash
./scripts/verify-play-release-bundle.sh
~~~

스크립트는 다음을 수행한다.

1. :app:bundleRelease를 실행하되 로컬 검증 중 Crashlytics mapping 업로드는 제외한다.
2. app-release.aab의 필수 bundle 항목과 비어 있지 않은 R8 mapping 파일이 있는지 확인한다.
3. JAR 서명을 `jarsigner -verify -strict`로 확인한다. 자가서명 또는 인증서 체인 미검증 경고(exit 4)만 허용한다. 같은 exit 4를 공유하는 서명 인증서 만료·유효 시작 전, TSA 만료, JDK에서 비활성화된 알고리즘은 진단 문구로 거부하고, 알 수 없는 새 exit 4 원인도 fail-closed한다. 서명 뒤 unsigned entry가 추가되면 exit 20으로 실패한다.
4. pinned bundletool `1.18.3`으로 AAB manifest의 실제 versionCode를 읽고 기대값과 대조한다. 기대값은 `AFTERNOTE_VERSION_CODE`이며, 미설정이면 `build-logic/src/main/kotlin/VersionCode.kt`의 기본값(현재 `1`)을 읽는다. 명시한 값이 비어 있거나 유효한 범위의 정수가 아니면 실패한다.
5. AAB와 서명 인증서의 SHA-256을 출력한다.

`BUNDLETOOL_JAR`가 지정돼 있으면 그 파일의 SHA-256이 고정된 값과 일치하는지 다시 확인해 사용한다. 지정되지 않았으면 임시 디렉터리에 같은 버전을 다운로드·검증하고 종료 시 정리한다. Java와 SHA-256 계산 도구가 필요하며, 자동 다운로드에는 `curl`과 네트워크도 필요하다.

이미 bundleRelease를 실행한 뒤 산출물만 다시 확인하려면 다음을 사용한다.

~~~bash
./scripts/verify-play-release-bundle.sh --skip-build
~~~

Play용 versionCode를 주입해 만든 AAB는 `--skip-build`에서도 같은 기대값을 전달해야 한다. 예를 들어 `101`로 빌드했다면 다음과 같이 검사한다.

~~~bash
AFTERNOTE_VERSION_CODE=101 ./scripts/verify-play-release-bundle.sh --skip-build
~~~

산출물:

- AAB: app/build/outputs/bundle/release/app-release.aab
- R8 mapping: app/build/outputs/mapping/release/mapping.txt

AAB와 R8 mapping은 GitHub Actions artifact, 이슈, PR에 첨부하지 않는다. AAB는 Play Console의 비공개 릴리스에 직접 올리고, mapping은 Play·Crashlytics의 비공개 난독화 해제 경로에서만 사용한다.

공식 근거: [명령줄에서 App Bundle 빌드](https://developer.android.com/build/building-cmdline#build_bundle)

## Play App Signing 키 결정

Play App Signing은 설치되는 APK에 사용하는 app signing key와 Play에 제출하는 AAB에 사용하는 upload key를 분리한다.

| 키 | 보관 주체 | 용도 | 분실·노출 시 처리 |
|---|---|---|---|
| app signing key | Google Play | 사용자 기기에 배포할 APK 서명 | Play의 key upgrade 절차 사용 |
| upload key | FE 배포 담당·CI | Play Console에 올릴 AAB 서명 | Play Console에서 reset 요청 가능 |

첫 Play 등록 전 아래 두 방식 중 하나를 확정한다. 등록 화면에서 선택한 뒤에는 app signing key 사본을 다시 내려받을 수 없으므로 추측으로 진행하지 않는다.

### 결정: 기본안으로 간다 (2026-09-06)

첫 업로드에서 이 선택이 확정되므로 업로드하는 사람이 그 자리에서 고르지 않도록 미리 박아 둔다.

근거는 대안의 조건이 성립하지 않는다는 것이다. 대안은 같은 applicationId 의 Firebase APK 와 Play APK 를 서로 업데이트해야 한다는 요구가 확정된 경우에만 고르는데, 그 요구는 확정된 적이 없다. 오히려 두 채널을 목적으로 갈라 두는 쪽이 이미 문서에 두 번 적혀 있다.

- 이 문서 위의 채널 표: Firebase APK 배포는 Google Play 출시 뒤에도 내부 QA 용도로만 사용한다.
- [비개발자 APK 배포](release/distribution.md): Firebase App Distribution 은 디자이너·PM·QA·외부 베타테스터 채널이다.

대가는 하나다. Firebase APK 를 쓰던 사람이 Play 내부 테스트 트랙으로 옮길 때 한 번은 기존 앱을 지우고 다시 깔아야 한다. 일회성이고, QA 채널을 계속 쓸 사람에게는 영향이 없다. 그 대신 production app signing key 가 Google 인프라 밖에 존재한 적이 없어서, 팀 keystore 가 새더라도 Play 에서 upload key 를 reset 하면 앱을 잃지 않는다.

이 결정을 뒤집으려면 첫 업로드 전이어야 한다. 업로드 뒤에는 Play 의 key upgrade 절차 말고는 방법이 없다.

### 기본안: Play와 Firebase를 별도 설치 채널로 유지

1. Google Play가 app signing key를 생성한다.
2. 현재 release key로 첫 AAB를 서명하고, 이 키를 upload key로 사용한다.
3. Play가 발급한 app signing certificate의 SHA-1·SHA-256과 카카오 key hash를 API 제공자 콘솔에 등록한다.

이 방식은 production app signing key를 Google 인프라에만 보관한다. 대신 Firebase APK와 Play APK의 설치 인증서가 달라 서로 위에 업데이트할 수 없다. Firebase 테스터가 Play 빌드로 이동할 때는 기존 앱 삭제와 재설치가 필요하다.

### 대안: Firebase와 Play 사이의 인플레이스 업데이트 유지

기존 release key를 Play에 app signing key로 제공하고, 별도 upload key를 생성·등록한다. 같은 applicationId의 Firebase APK와 Play APK를 서로 업데이트해야 한다는 요구가 확정된 경우에만 선택한다.

공식 근거:

- [Play App Signing과 두 키의 역할](https://developer.android.com/studio/publish/app-signing#app-signing-google-play)
- [여러 배포 채널에서 같은 서명 키 사용](https://developer.android.com/studio/publish/app-signing#considerations)

## 첫 내부 테스트 릴리스

첫 1회는 Console 수동 업로드다(API가 요구한다). 그 뒤부터는 위 「내부 테스트 트랙 자동 배포」가 이 절차를 대신한다.

1. versionCode가 Play에 올린 모든 이전 산출물보다 큰지 확인한다. 자동 배포에서는 워크플로가 Play를 조회해 빌드 전에 판정한다.
2. AAB 검증 스크립트의 경로·AAB SHA-256·서명 인증서 SHA-256을 릴리스 기록에 남긴다. 자동 배포에서는 run summary가 이 기록이다.
3. Play Console에서 내부 테스트 트랙을 만들고 Play App Signing 방식을 적용한다(기본안, 위 「Play App Signing 키 결정」).
4. AAB를 업로드한다.
5. Play Console의 app signing certificate를 다음 제공자에 **추가** 등록한다. 기존 값을 지우지 않는다 — 기본안에서는 Play 인증서와 Firebase APK 를 서명한 팀 release 인증서가 서로 다르므로 둘 다 등록돼 있어야 한다. 기존 등록을 새 값으로 바꾸면 Firebase QA 채널의 카카오·구글 로그인이 그날로 깨진다.
   - Kakao Developers Android key hash
   - Firebase Android 앱 SHA 인증서 지문
   - Google API/OAuth 설정 중 package name과 인증서 지문을 검증하는 항목
6. Play 링크로 신규 설치와 업데이트를 확인한다.
7. 카카오·구글 로그인과 앱의 핵심 진입 흐름을 확인한 뒤 다음 트랙으로 승격한다.

서명 key·keystore·비밀번호·서비스 계정 JSON은 저장소나 문서에 넣지 않는다. 비밀값 입력과 Play Console의 되돌릴 수 없는 확인 동작은 담당자가 직접 수행한다.

## Automatic integrity protection

[Automatic integrity protection](https://developer.android.com/security/fraud-prevention/environment#automatic-integrity-protection)은 Play가 앱 코드에 변조·비공식 재배포 검사를 추가하는 기능이다.

- Play App Signing이 선행 조건이다.
- 현재는 일부 Play Partner에게만 제공되므로 Console에 메뉴가 없으면 코드로 활성화할 수 없다.
- 앱 코드나 백엔드 연동 없이 동작하지만, production 승격 전에 보호된 내부 테스트 릴리스를 검증해야 한다.

내부 테스트 확인 항목:

| 축 | 기대 결과 |
|---|---|
| Play에서 신규 설치 | 정상 실행 |
| Play에서 이전 버전 업데이트 | 정상 업데이트·실행 |
| AAB 또는 생성 APK 변조·재서명 | 실행 차단 |
| 비공식 경로로 재배포 | Google Play 설치 유도 |
| 보호 적용 뒤 일반 사용 | 신규 crash·로그인 실패 증가 없음 |

## Play Integrity API 경계

Automatic integrity protection과 Play Integrity API는 별개다. 현재는 Play Integrity 클라이언트 의존성이나 토큰 요청 코드를 추가하지 않는다.

추후 서버 계약이 준비되면 다음 순서로 별도 이슈에서 연동한다.

1. 보호할 서버 요청과 requestHash 입력을 정의한다.
2. FE가 Standard Integrity token을 요청해 해당 서버 요청과 함께 전달한다.
3. 서버가 verdict를 검증하고 replay·proxying을 막는다.
4. 서버가 허용·제한·추가 인증·거절처럼 단계화된 결과를 반환한다.
5. enforcement 전에 실제 사용자 verdict를 관측한다.

클라이언트가 verdict를 자체 판정하거나 결과를 캐시해 권한을 열어 주는 구현은 하지 않는다.

공식 근거: [Play Integrity API 개요와 서버 판정](https://developer.android.com/google/play/integrity/overview)

## 릴리스 중단과 복구

- 내부 테스트에서 보호·로그인·업데이트 문제가 발생하면 production 승격을 중단한다.
- AAB, R8 mapping, 인증서 지문, versionCode를 같은 릴리스 기록으로 묶는다.
- 수정본은 더 큰 versionCode로 다시 빌드하고 동일 검증을 반복한다.
- Firebase QA와 Play production의 설치 호환성은 선택한 app signing key 방식에 따라 릴리스 기록에 명시한다.
