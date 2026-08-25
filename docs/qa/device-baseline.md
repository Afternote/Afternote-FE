# QA 기기 기준

실기 QA 를 **어떤 화면에서** 돌리는지, 그리고 그것을 증거에 **어떻게 남기는지** 정한다.

## 왜 필요했나

2026-08-25 기준 에뮬레이터 QA 증거(`.codex/qa-evidence/emulator/<sha>.json`) 24건은 전부 같은
기기였다 — android-35 / 1080×2400 @420dpi, 전부 에뮬레이터, 실기기 0건.

문제는 그 기기가 표준도 아니었다는 점이다. `Pixel_7_Claude_QA` 에 `wm size 1080x2340` +
`wm density 387` override 가 남아 있어 실효 **446×967dp** 로 돌고 있었다. 표준 Pixel 7(411×914dp)
보다 넓고, 국내 보급형(360×800dp) 대비 폭 24%·높이 21% 여유다. 잘림과 오버플로가 QA 에서
구조적으로 잡히지 않는 화면이었다.

증거에는 `"avd": "Pixel_7_Claude_QA"` 라고만 적혀 있었다. `wm` override 는 재부팅으로 풀리지
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
보유 AVD 중 `KUIT_7th_Device` 가 360×800dp @160dpi 라 이 이유로 부적합하다.

### override 는 세션 도중 풀린다

2026-08-25 QA 에서 `compact` 로 전환해 검사하던 중 override 가 두 번 조용히 사라져 화면이
411×914dp 로 되돌아갔다. 앱 재시작·다른 세션의 조작 등으로 풀릴 수 있고, **풀려도 아무 신호가
없다.** 화면이 바뀐 채로 이어서 검사하면 그 결과는 좁은 화면 근거가 아니게 된다.

그래서 좁은 화면 QA 는 **검사 단위마다** `status` 로 확인하고, 화면 캡처와 함께 그때의 실효 dp 를
남긴다. `uiautomator dump` 의 노드 bounds 범위(720×1600 인지 1080×2400 인지)로도 사후에 판별할
수 있으니, 덤프를 남겨 두면 판정 근거가 된다.

## 증거에 남기는 것 (schema 2)

**실측을 시작하기 전에** `status` 로 override 부터 확인하고, `--json` 출력을 증거의 `device`
블록에 그대로 넣는다.

```bash
./scripts/qa-device-profile.sh status --json
```

```json
"device": {
  "serial": "emulator-5554",
  "avd": "Pixel_7_Claude_QA",
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

schema 1 로 기록된 기존 24건은 당시 화면 값을 복원할 수 없으므로 소급하지 않는다. schema 1 증거는
**화면 스펙 미상**으로 읽는다.

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

현재 실기기 검증은 0건이다(`docs/qa/assumptions.md` 에도 "실기기 종단 확인을 하지 못했다"고 남아
있다). 에뮬레이터와 결과가 갈리는 영역만 종단으로 본다.

- [ ] 카카오 로그인 — 카카오톡 앱이 설치된 기기에서의 앱 간 전환
- [ ] 생체인증 — 실제 지문/얼굴 등록 상태에서 `USE_BIOMETRIC`
- [ ] FCM 푸시 수신 — 백그라운드·종료 상태 각각
- [ ] 사진·동영상 업로드 — 실제 카메라롤 파일 크기

### 최저 API 스모크

`minSdk = 26` 을 지원한다고 선언하고 있으므로, 그 경계에서 한 번은 확인한다. `SDK_INT` 분기가
없으니 매 PR 검증은 필요 없다.

- [ ] API 26 에뮬레이터에 설치·기동
- [ ] 로그인 → 홈 진입까지 종단 1회
