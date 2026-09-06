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

2026-08-05 기준, 현재 팀에서 확인 가능한 Google 계정은 Play Console 접속 시 `/console/signup`의 개발자 계정 생성 화면으로 이동한다. 따라서 아직 앱 등록과 Play App Signing 등록이 없으며, Automatic integrity protection 제공 대상인지도 판정할 수 없다.

개발자 계정 생성·팀 초대와 앱 등록을 마친 뒤, Play Console의 **Test and release > App integrity**에서 다음 두 항목을 다시 확인한다.

- Play App Signing 등록 상태
- Automatic integrity protection 메뉴와 opt-in 제공 여부

## 내부 테스트 트랙 자동 배포

[`release-play-internal.yml`](../.github/workflows/release-play-internal.yml)이 `main`에서 수동 실행 + `play-internal` environment 승인을 받아 signed AAB를 Play **내부 테스트 트랙에만** 올린다. production·open·closed 트랙으로 승격하는 단계는 워크플로에도 업로드 스크립트에도 없다. 승격은 Play Console에서 사람이 한다.

Firebase App Distribution 경로([`release-distribution.yml`](../.github/workflows/release-distribution.yml))와 별개다. 그쪽은 `main` push마다 자동으로 APK를 QA 그룹에 뿌리고, 이쪽은 사람이 눌러야 움직이는 AAB 경로다. 둘은 함께 돌지 않으며 서로의 자격도 공유하지 않는다.

### versionCode 정책

| 빌드 | versionCode | 산출 주체 |
|---|---|---|
| 로컬·CI 검증·Firebase App Distribution | `1` (고정) | `build-logic/src/main/kotlin/VersionCode.kt`의 기본값 |
| Play 내부 테스트 트랙 | `run_number * 100 + run_attempt` | [`resolve-play-version-code.mjs`](../.github/scripts/resolve-play-version-code.mjs) |

- Play로 나가는 경로가 이 워크플로 하나뿐이라 `run_number`만으로 단조 증가가 보장된다. 재실행은 `run_attempt`가 올라가 같은 run에서도 값이 겹치지 않는다.
- 업로드 **전에** Play가 알고 있는 최대 versionCode(업로드된 bundle + 모든 트랙의 release)를 조회해, 산출값이 그보다 크지 않으면 빌드 전에 멈춘다.
- 같은 versionCode 재업로드는 bundle 목록 대조로 업로드 전에 막고, Play가 돌려준 versionCode가 빌드한 값과 다르면 트랙을 건드리지 않는다.
- `app/build.gradle.kts`의 versionCode를 손으로 올리지 않는다. 로컬에서 Play용 값이 필요하면 `AFTERNOTE_VERSION_CODE` 환경변수로 주입한다.

### 자동화 사전 준비

워크플로가 참조하는 설정은 전부 사람이 웹 UI에서 만들어야 한다. 하나라도 비어 있으면 첫 스텝이 누락된 이름을 출력하고 빌드 전에 실패한다.

**1. Play Console — 앱 등록** (`play.google.com/console`)

1. 개발자 계정 생성(1회 등록비). 조직 계정은 D-U-N-S 번호가 필요하다.
2. **모든 앱 → 앱 만들기**: 앱 이름 `Afternote`, 기본 언어 한국어, 유형 `앱`, 무료.
3. **정책 및 프로그램 → 앱 콘텐츠**의 필수 선언(개인정보처리방침 URL, 광고, 콘텐츠 등급, 타겟층, 데이터 보안)을 모두 채운다. 내부 테스트도 이게 비면 릴리스를 만들 수 없다.
4. **테스트 및 출시 → 테스트 → 내부 테스트 → 테스터**에서 테스터 이메일 목록을 만든다.

**2. Play Console — 첫 AAB 수동 업로드**

Android Publisher API는 **Console에서 최소 한 번 수동 업로드된 앱**에만 업로드를 허용한다. 첫 AAB는 `./scripts/verify-play-release-bundle.sh`로 만든 산출물을 **테스트 및 출시 → 내부 테스트 → 새 버전 만들기**에서 직접 올린다.

- 이때 `AFTERNOTE_VERSION_CODE` 없이 빌드해 versionCode `1`을 쓴다. 워크플로가 만드는 첫 값은 `101`이라 단조 증가 조건을 자동으로 만족한다.
- 이 업로드에서 Play App Signing 방식이 확정된다. 아래 「Play App Signing 키 결정」을 먼저 읽고 되돌릴 수 없는 선택을 한다.

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

**Settings → Environments → New environment**로 `play-internal`을 만든다.

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

**Actions → Release Play Internal Track → Run workflow**에서 브랜치 `main`을 선택해 실행한다. environment 승인자가 승인해야 job이 시작된다.

워크플로가 하는 일:

1. `main`이 아니면 거부하고, 필요한 secret·variable이 비면 이름을 출력하고 멈춘다.
2. WIF로 조회용 단기 토큰을 받아 Play의 현재 최대 versionCode를 읽는다(edit는 commit 없이 되돌린다).
3. 단조 증가 versionCode를 확정한다. 중복이면 여기서 끝난다 — AAB를 만들지 않는다.
4. 확정된 versionCode로 signed AAB를 빌드하고, 서명·필수 항목·R8 mapping을 검증한다.
5. AAB에 SLSA provenance를 붙이고 이 워크플로·이 commit으로 검증한다.
6. 업로드 직전에 토큰을 새로 받고 digest를 다시 확인한 뒤, edit 생성 → bundle 업로드 → internal 트랙 갱신 → commit 순으로 게시한다.
7. run summary에 track·versionCode·source SHA·AAB digest·attestation·Play edit id를 남긴다.

### 실패 모드

| 상황 | 어디서 멈추는가 |
|---|---|
| Play Console·서비스 계정 준비 전 | 첫 스텝. 누락된 secret·variable 이름을 출력한다 |
| `main` 이외의 ref | keystore를 풀기 전 |
| versionCode 중복·역행 | 빌드 전 |
| 잘못 서명된 AAB | `scripts/verify-play-release-bundle.sh` |
| API 조회·업로드·트랙 갱신·commit 오류 | 원래 오류를 보고하고, edit id를 받은 경우 삭제를 시도한다. 삭제 실패는 별도 경고다. |

edit 정리는 조회의 `finally`와 게시 실패의 `catch`에서 수행하는 삭제 시도다. API의 삭제 실패, SIGTERM·실행 취소·timeout·러너 강제 종료에서는 정리를 보장하지 않는다. 취소나 commit 응답 유실 뒤에는 Play Console의 실제 트랙과 versionCode를 확인한 뒤 재실행한다.

AAB·mapping·keystore·빌드 설정은 `if: always()` 정리 단계에서 러너로부터 삭제를 시도하며, Actions artifact로 게시하는 단계는 없다. 러너가 강제 종료되면 이 정리 단계도 실행을 보장하지 않는다.

### 롤백

Play는 이미 게시된 versionCode를 되돌리지 않는다.

1. Play Console **내부 테스트 → 출시 관리**에서 문제 릴리스를 중단(halt)한다.
2. 수정본은 **더 큰 versionCode**로 다시 배포한다. 이 워크플로를 다시 실행하면 값이 자동으로 올라간다.
3. Firebase App Distribution은 별개 채널이라 영향을 받지 않는다. 테스터에게 급히 검증본을 줘야 하면 그쪽 경로를 쓴다.

## AAB 빌드와 로컬 검증

루트에서 다음 명령을 실행한다.

~~~bash
./scripts/verify-play-release-bundle.sh
~~~

스크립트는 다음을 수행한다.

1. :app:bundleRelease를 실행하되 로컬 검증 중 Crashlytics mapping 업로드는 제외한다.
2. app-release.aab의 필수 bundle 항목과 JAR 서명을 `jarsigner -verify -strict`로 확인한다. 자가서명 또는 인증서 체인 미검증 경고(exit 4)만 허용한다. 같은 exit 4를 공유하는 서명 인증서 만료·유효 시작 전, TSA 만료, JDK에서 비활성화된 알고리즘은 진단 문구로 거부하고, 알 수 없는 새 exit 4 원인도 fail-closed한다. 서명 뒤 unsigned entry가 추가되면 exit 20으로 실패한다.
3. R8 mapping 파일 존재 여부를 확인한다.
4. AAB와 서명 인증서의 SHA-256을 출력한다.

이미 bundleRelease를 실행한 뒤 산출물만 다시 확인하려면 다음을 사용한다.

~~~bash
./scripts/verify-play-release-bundle.sh --skip-build
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
3. Play Console에서 내부 테스트 트랙을 만들고 Play App Signing 방식을 확정한다.
4. AAB를 업로드한다.
5. Play Console의 app signing certificate를 다음 제공자에 등록한다.
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
