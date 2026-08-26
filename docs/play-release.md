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

1. versionCode가 Play에 올린 모든 이전 산출물보다 큰지 확인한다.
2. AAB 검증 스크립트의 경로·AAB SHA-256·서명 인증서 SHA-256을 릴리스 기록에 남긴다.
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
