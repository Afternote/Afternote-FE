# Compose Preview Screenshot Testing (docker baseline)

`Compose Preview Screenshot Testing`의 anti-aliasing·font hinting·scale 등 host 환경 의존 렌더링 차이로 CI rendered PNG를 baseline으로 교체하는 ping-pong이 발생해 왔다 (PR [#302](https://github.com/Afternote/Afternote-FE/pull/302) / [#322](https://github.com/Afternote/Afternote-FE/pull/322)). 본 저장소는 `Dockerfile.screenshot`을 공통 환경으로 사용하고 역할을 세 workflow로 나눈다.

- [Compose Preview Screenshot Test](../../.github/workflows/screenshot.yml): PR과 `develop` 합성 결과의 기존 baseline 검증
- [Generate Screenshot Baselines](../../.github/workflows/screenshot-baseline-generate.yml): 라벨이 붙은 PR의 정확한 head에서 baseline 생성·재검증 후 artifact 발행
- [Apply Screenshot Baselines](../../.github/workflows/screenshot-baseline-apply.yml): artifact와 head를 다시 검증한 뒤 허용된 PNG만 PR 브랜치에 적용

## 로컬 fallback 사전 준비

- Docker 호환 runtime 설치 (macOS 의 Colima/Docker Desktop 또는 Linux Docker)

## Actions 에서 baseline 갱신 (기본 경로)

1. 갱신할 PR에 `screenshot-baseline` 라벨을 붙인다.
2. 읽기 전용 [Generate Screenshot Baselines](../../.github/workflows/screenshot-baseline-generate.yml)가 PR의 정확한 head SHA를 CI 표준 Docker 이미지에서 렌더하고 재검증한다.
3. 생성 workflow는 라벨을 붙인 `pull_request` 권한 경계에서 실행되므로 PR 코드가 default branch cache를 오염시키지 않는다.
4. 별도 [Apply Screenshot Baselines](../../.github/workflows/screenshot-baseline-apply.yml)가 artifact에 PNG baseline 이외 변경이 없는지, 생성 대상과 현재 PR head가 같은지 checkout 없이 재검증한다.
5. 검증된 PNG만 PR 브랜치에 커밋하고 필수 검사를 다시 요청한다. 성공하면 라벨도 제거된다.

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
            :feature:setting:presentation:updateScreenshotTest \
            :feature:timeletter:presentation:updateScreenshotTest \
            --rerun
```

→ 변경된 PNG 가 각 모듈 `src/screenshotTestDebug/reference/...` 에 갱신. `git add` 후 commit.

> 실패한 모듈만 갱신하려면 그 모듈 태스크만 지정한다 — 예: `./gradlew :feature:home:presentation:updateScreenshotTest`
>
> 대상 모듈 목록은 [검증](../../.github/workflows/screenshot.yml)·[생성](../../.github/workflows/screenshot-baseline-generate.yml) workflow와 [적용 workflow의 허용 경로](../../.github/workflows/screenshot-baseline-apply.yml)에 함께 들어 있다. 모듈을 추가·이전했다면 세 workflow와 `Dockerfile.screenshot`, 이 문서를 함께 갱신한다.

## 로컬 baseline 검증 (CI 실패 재현)

```bash
docker run --rm --platform linux/amd64 -v "$PWD":/workspace -w /workspace afternote-screenshot:latest \
  ./gradlew :core:ui:validateScreenshotTest \
            :feature:home:presentation:validateScreenshotTest \
            :feature:receiver:presentation:validateScreenshotTest \
            :feature:onboarding:presentation:validateScreenshotTest \
            :feature:afternote:presentation:validateScreenshotTest \
            :feature:mindrecord:presentation:validateScreenshotTest \
            :feature:setting:presentation:validateScreenshotTest \
            :feature:timeletter:presentation:validateScreenshotTest
```

→ baseline 과 docker 환경에서 새로 그린 PNG 비교. 실패 시 `build/outputs/screenshotTest-results/preview/debug/diffs/` 에서 diff PNG 확인.

## 호스트 직접 실행은 사용하지 않음

`./gradlew :<module>:updateScreenshotTest` 를 host 에서 직접 실행하면 macOS / Linux / JDK 마이너 버전 / 폰트 캐시 차이로 CI 와 baseline 이 어긋난다. docker 환경 통일이 root fix.
