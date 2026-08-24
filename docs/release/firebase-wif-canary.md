# Firebase App Distribution WIF canary

이 문서는 기존 `FIREBASE_SERVICE_ACCOUNT_JSON` 인증을 즉시 교체하지 않고, 별도 수동
canary에서 GitHub OIDC와 Google Cloud Workload Identity Federation 호환성을 먼저 확인하는
절차다. 저장소 workflow만 준비하며 Google Cloud·GitHub Settings 변경과 실제 업로드 실행은
별도 승인 범위다.

## 현재 안전 경계

- production `release-distribution.yml`은 기존 JSON 인증을 그대로 사용한다.
- canary는 `develop` 또는 `main`의 수동 실행과 `release-distribution` Environment 승인을 모두
  요구한다.
- signed APK는 인증 전에 빌드하고 같은 runner에서 바로 업로드한다. public Actions artifact로
  게시하지 않는다.
- WIF 인증은 업로드 직전에만 수행한다. provider·service account 값, token, 생성된 ADC 상세는
  workflow summary나 별도 artifact에 기록하지 않는다.
- canary 실패 시 production workflow나 기존 JSON secret을 변경·삭제하지 않는다.

## 별도 승인이 필요한 외부 준비

다음 항목은 이 PR에서 만들거나 수정하지 않는다.

1. GitHub OIDC용 workload identity pool/provider와 Firebase App Distribution 전용 최소 권한
   service account를 준비한다.
2. provider attribute mapping에 `repository`, `repository_owner`, `ref`, `workflow_ref`,
   `environment` claim을 포함한다.
3. attribute condition은 최소한 다음을 동시에 제한한다.
   - repository: `Afternote/Afternote-FE`
   - owner: `Afternote`
   - ref: `refs/heads/develop` 또는 `refs/heads/main`
   - workflow: `.github/workflows/firebase-wif-canary.yml`
   - environment: `release-distribution`
4. 보호된 Environment에 `GCP_WORKLOAD_IDENTITY_PROVIDER`와
   `GCP_FIREBASE_SERVICE_ACCOUNT`를 masked secret으로 등록한다.
5. 허용 branch canary 성공뿐 아니라 다른 branch·fork·workflow의 credential 발급 거부도
   Google Cloud audit evidence로 확인한다.

공식 근거:

- [Google Cloud deployment pipeline WIF](https://docs.cloud.google.com/iam/docs/workload-identity-federation-with-deployment-pipelines)
- [GitHub Actions OIDC for Google Cloud](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-google-cloud-platform)
- [Firebase App Distribution CI/CD와 WIF credential configuration](https://firebase.google.com/docs/app-distribution/best-practices-distributing-android-apps-to-qa-testers-with-ci-cd)
- [google-github-actions/auth](https://github.com/google-github-actions/auth)

## 전환 판정

JSON 경로 제거는 아래가 모두 PASS인 별도 후속 변경에서만 한다.

- 승인된 `develop` 또는 `main` canary APK가 WIF ADC로 1회 업로드된다.
- Firebase release의 source SHA와 실행 URL이 해당 canary run과 일치한다.
- 허용하지 않은 branch·fork·workflow의 credential 발급이 거부된다.
- 대상 service account 권한이 Firebase App Distribution 업로드 최소 범위로 검토된다.
- 실패 canary 뒤 기존 production JSON 경로가 정상 동작한다.

하나라도 확인되지 않으면 상태는 `UNVERIFIED`이며 기존 JSON secret을 유지한다.
