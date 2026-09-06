# QA 기기 기준

실기 QA 를 **어떤 화면에서** 돌리는지, 그리고 그것을 증거에 **어떻게 남기는지** 정한다.

## 왜 필요했나

2026-08-25에 조사한 에뮬레이터 QA 증거 24건(schema 1 23건 + legacy 1건, 현재
`docs/qa/evidence/<full-head-sha>.json`으로 이관)은 전부 같은 기기였다 — android-35 /
1080×2400 @420dpi, 전부 에뮬레이터, 실기기 0건이었다(2026-09-04 에 실기기 2건이 더해져 36건 — 아래 «실기기 1대» 절). 8/27 이관한 전체 34건의 원본 스키마 분포는
schema 1 32건 + schema 2 1건 + legacy 1건이다.

문제는 그 기기가 표준도 아니었다는 점이다. 당시 QA AVD에 `wm size 1080x2340` +
`wm density 387` override 가 남아 있어 실효 **446×967dp** 로 돌고 있었다. 표준 Pixel 7(411×914dp)
보다 넓고, 국내 보급형(360×800dp) 대비 폭 24%·높이 21% 여유다. 잘림과 오버플로가 QA 에서
구조적으로 잡히지 않는 화면이었다.

비공개 원본 증거에는 AVD 이름만 적혀 있었다. `wm` override 는 재부팅으로 풀리지
않고 세션 사이에 조용히 남기 때문에, **기기 이름만으로는 어떤 화면에서 검증했는지 사후에 알 수 없다.**

## 프로파일 두 가지

| 프로파일 | 물리 | density | 실효 dp | 무엇을 대표하나 |
|---|---|---|---|---|
| `standard` | 기기 물리 해상도 | 물리 density | Pixel 7 기준 411×914dp | 기본. 대부분의 QA 는 여기서 돈다 |
| `compact` | 720×1600 | 320 (xhdpi) | 360×800dp | 국내 보급형. 잘림·오버플로를 드러낸다 |

전환은 스크립트로 한다. 별도 AVD 를 만들지 않는 이유는 로그인 상태(DataStore 토큰)를 그대로
재사용할 수 있어서다.

```bash
./scripts/qa-device-profile.sh status     # 지금 어떤 화면인지 (전환하지 않음)
./scripts/qa-device-profile.sh compact    # 360×800dp 로
./scripts/qa-device-profile.sh standard   # override 해제
```

`compact` 의 dpi 를 320 으로 잡는 것은 실기기의 리소스 버킷(xhdpi)을 맞추기 위해서다. 같은
360×800dp 라도 160dpi(mdpi)로 만들면 다른 `drawable`/`dimen` 이 선택돼 기준으로 쓸 수 없다.
다른 보유 AVD 하나도 360×800dp @160dpi 라 이 이유로 부적합하다.

## 실측 전에 두 가지를 확정한다

### 1. 화면

`status` 로 실효 dp 를 확인한다 — 아래 override 항목을 볼 것.

### 2. 앱이 어느 커밋인가

**증거 대장은 파일 이름이 전체 커밋 sha 다**(`docs/qa/evidence/<full-head-sha>.json`). 증거를 남긴다는
것은 "이 커밋의 앱을 돌렸다"는 선언이므로, 돌린 앱의 출처가 확정돼야 한다.

그런데 지금 앱은 커밋 정보를 들고 있지 않다. `versionCode` 는 Play 워크플로가 주입할 때만 바뀌고,
`versionName` 은 `"1.0"` 하드코딩이다.

```
$ adb shell dumpsys package com.afternote.afternote_fe | grep versionName
    versionName=1.0
```

그래서 **이미 깔려 있는 앱을 그냥 켜서 QA 하면 그 결과를 어느 sha 에도 붙일 수 없다.** 실제로
2026-08-25 좁은 화면 QA 를 그렇게 돌렸다가 증거를 남기지 못했다. 같은 날 다른 세션이 QA 도중
APK 를 재설치한 정황(`lastUpdateTime` 갱신)까지 겹쳤지만, 앱에 커밋이 없어 무엇이 바뀌었는지
대조할 수도 없었다.

증거로 남길 QA 는 **대상 커밋을 직접 빌드해 설치하고 시작한다.** 워크트리는 이 레포 규약대로
`.claude/worktrees/<이름>` 아래에 만든다.

```bash
git worktree add .claude/worktrees/qa-dev --detach origin/develop
cp app/google-services.json .claude/worktrees/qa-dev/app/
./gradlew -p .claude/worktrees/qa-dev :app:assembleDebug
adb install -r .claude/worktrees/qa-dev/app/build/outputs/apk/debug/app-debug.apk
```

`google-services.json` 은 gitignore 라 새 워크트리에 없다 — 복사하지 않으면 빌드가 깨진다.
`-r` 재설치는 DataStore 를 지우지 않으므로 로그인 세션이 그대로 유지된다.

앱이 커밋을 들고 다니게 되면(#1135) 이 단계는 "설치된 앱의 sha 를 읽어 확인"으로 줄어든다.

### 3. 기기를 나 혼자 쓰는가

에이전트 세션 여러 개가 같은 개발 머신에서 돌면 **에뮬레이터 한 대를 공유하게 된다.** 2026-08-25
이 저장소 작업 중에는 세션 17개가 에뮬레이터 하나를 함께 쓰고 있었고, 좁은 화면 QA 도중
다른 세션이 APK 를 재설치하고(`lastUpdateTime` 갱신), 토큰을 비우고(로그아웃), 화면 override 를
풀었다. **아무 신호도 없다** — 검사 결과만 조용히 오염된다.

증거로 남길 QA 는 기기를 따로 띄워서 한다.

```bash
$ANDROID_HOME/emulator/emulator -avd <AVD 이름> -no-snapshot-load &
adb devices          # 새로 붙은 serial 확인
```

그 뒤 모든 명령에 `-s <serial>` 을 붙인다. 붙이지 않으면 기기가 여러 대일 때 어디로 갔는지
알 수 없다. 검사 전후로 `lastUpdateTime` 을 대조하면 그 사이 누가 앱을 갈아끼웠는지 확인할 수 있다.

```bash
adb -s <serial> shell dumpsys package com.afternote.afternote_fe | grep lastUpdateTime
```

### override 는 세션 도중 풀린다

2026-08-25 QA 에서 `compact` 로 전환해 검사하던 중 override 가 두 번 조용히 사라져 화면이
411×914dp 로 되돌아갔다. 원인은 위의 기기 공유였다 — 같은 기기를 쓰던 다른 세션이 화면을
되돌렸다. 앱 재시작으로도 풀릴 수 있고, **풀려도 아무 신호가 없다.** 화면이 바뀐 채로 이어서 검사하면 그 결과는 좁은 화면 근거가 아니게 된다.

그래서 좁은 화면 QA 는 **검사 단위마다** `status` 로 확인하고, 화면 캡처와 함께 그때의 실효 dp 를
남긴다. `uiautomator dump` 의 노드 bounds 범위(720×1600 인지 1080×2400 인지)로도 사후에 판별할
수 있으니, 덤프를 남겨 두면 판정 근거가 된다.

## 증거에 남기는 것 (schema 2)

**실측을 시작하기 전에** `status` 로 override 부터 확인하고, `--json` 출력을 증거의 `device`
블록에 넣는다. 이 JSON은 공개 기록용이라 로컬 serial과 AVD 이름을 포함하지 않는다.

```bash
./scripts/qa-device-profile.sh status --json
```

```json
"device": {
  "is_emulator": true,
  "api_level": 35,
  "screen": {
    "physical": "1080x2400",
    "density": 420,
    "override": null,
    "override_density": null,
    "effective_dp": "411x914"
  }
}
```

8/25 조사 대상 24건은 schema 1 23건과 legacy 1건이었다. 이 기록들은 당시 화면 값을 복원할 수
없으므로 소급하지 않고 **화면 스펙 미상**으로 읽는다. 8/27 이관한 전체 34건 중에서는 schema 2
1건만 구조화된 화면 값을 보존하며, 나머지는 schema 1 32건과 legacy 1건이다.

## 매 PR 에 기기를 늘리지 않는 이유

리스크가 화면 크기 축에 몰려 있고, 그 축은 사람이 아니라 CI 가 지킨다.

- 고정 dp 리터럴 1566개, 그중 `width`/`height`/`size` 고정 지정 508개
- 반응형 API(`BoxWithConstraints` / `LocalConfiguration`) 사용은 2파일
- `verticalScroll` / `Lazy*` / `Pager` 가 없는 `*Screen.kt` 26개

그래서 스크린샷 baseline 에 360×800dp 변형을 넣어 좁은 화면 회귀를 상시 감시한다(#1128).
사람이 도는 `compact` QA 는 **레이아웃을 건드린 PR** 에만 추가한다.

반대로 OS 버전 매트릭스는 지금 단계에서 얻는 게 거의 없다.

- `Build.VERSION.SDK_INT` 분기가 저장소 전체에 0건이다
- 알림 권한은 `DailyNotificationWorker` 가 런타임에 확인한다

제조사 다양화도 마찬가지로 미룬다. 다만 아래 두 가지는 출시 전에 한 번은 해야 한다.

## 출시 전 1회 스모크

### 실기기 1대

실기기 검증은 2건이다 — 2026-09-04 갤럭시 S25(SM-S931N, Android 16)에서 **debug 빌드**
`a23e50fa3040a1025811f79cffd26c2e47b11323`(에뮬레이터로 못 보는 축만 골라 종단)과 **App Distribution
release 배포본** `f5ad611b23a32c6db2de4795bd9cee9fb32aacf0`(minify 산출물 화면 순회). 증거는
`docs/qa/evidence/<sha>.json`. 에뮬레이터와 결과가 갈리는 영역만 종단으로 본다.

- [x] 카카오 로그인 — 카카오톡 앱이 설치된 기기에서의 앱 간 전환. `f5ad611b` release 배포본에서 앱-투-앱
      성공. **debug 로는 판정하지 않는다** — 콘솔 미등록 키 해시라 카카오톡이 조용히 웹 로그인으로
      폴백해 앱 결함처럼 보인다(#1871).
- [x] 생체인증 — 실제 지문 등록 상태에서 `USE_BIOMETRIC`. `a23e50fa` 에서 하드웨어 Keystore(TEE)에
      `afternote_biometric_gate` 키가 생성되고 auth-per-use 연산이 성사됐다. 지문 등록·삭제 뒤
      `KeyPermanentlyInvalidatedException` 복구 경로는 미실측.
- [ ] FCM 푸시 수신 — 백그라운드·종료 상태 각각. 두 기록 모두 `not_covered` — 삼성 절전 정책 아래
      며칠 단위 관찰이 필요해 출시 전 1회 스모크로는 닫히지 않는다.
- [x] 사진·동영상 업로드 — 실제 카메라롤 파일 크기. `a23e50fa` 에서 8160×6120·14 MB 사진으로 서버
      상한 10 MiB 를 실측(#1868). 동영상은 미실측.

### 대화면 배포 여부 (기획 결정 사항)

FE 가 확인할 항목이 아니라 **출시 전에 정해야 하는 결정**이다.

`targetSdk = 36` 이므로 Android 16부터 최소 너비 600dp 이상 기기에서는 앱이 선언한 방향·크기
제한을 시스템이 무시하고 창에 맞춰 펼친다. 세로 고정(#1144)을 선언해도 태블릿과 펼친
폴더블에는 적용되지 않는다. 즉 **"태블릿 시안이 없다"가 "태블릿에 안 뜬다"를 뜻하지 않는다.**

- [ ] Play Console 기기 카탈로그에서 **대화면 기기를 배포 대상에 포함할지 결정**
  - 제외하면 태블릿에 설치되지 않으므로 추가 대응이 필요 없다.
  - 포함하면 시안이 없더라도 "레이아웃이 무너지지 않는지"는 한 번 봐야 한다. 완성도 있는
    태블릿 UI 가 아니라, 펼쳐졌을 때 버튼이 화면 밖으로 나가거나 겹치지 않는 수준.

현재 앱에는 화면 크기 한정자 리소스(`values-sw600dp` 등)가 0건이고 `resizeableActivity` 선언도
없다 — 폰 전용으로 설계돼 있다.

### 최저 API 스모크

`minSdk = 26` 을 지원한다고 선언하고 있으므로, 그 경계에서 한 번은 확인한다. `SDK_INT` 분기가
없으니 매 PR 검증은 필요 없다.

**이미지는 `google_apis` 쪽을 쓴다.** 2026-08-25 구글 저장소 실측 기준, API 26 의 ABI 는 이렇다.

| 이미지 | API 26 의 ABI |
|---|---|
| `system-images;android-26;google_apis` | **arm64-v8a**, x86, x86_64 |
| `system-images;android-26;google_apis_playstore` | x86 만 (arm64 는 API 28부터) |

Apple Silicon 에서 Play Store 이미지를 고르면 x86 뿐이라 Rosetta 에뮬레이션으로 느려진다.
`google_apis` 는 arm64 네이티브로 돌고 Google Play Services 도 들어 있어 FCM 검증까지 된다
(Play Store 앱만 없다 — 카카오톡 설치가 필요한 검증은 어차피 실기기 몫이다).

```bash
sdkmanager --install "system-images;android-26;google_apis;arm64-v8a"
avdmanager create avd -n Afternote_QA_API26 -k "system-images;android-26;google_apis;arm64-v8a"
```

- [ ] API 26 에뮬레이터에 설치·기동
- [ ] 로그인 → 홈 진입까지 종단 1회
