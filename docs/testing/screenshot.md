# Compose Preview Screenshot Testing (docker baseline)

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

