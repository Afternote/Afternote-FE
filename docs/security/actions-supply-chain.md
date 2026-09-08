# Actions 공급망 정책 — 조직 수준 감사

`orgs/Afternote/actions/permissions` 의 `sha_pinning_required` 를 켜기 전에 필요한 전수 감사와 그 판정을 남긴다.

**조직 설정은 이 감사 시점에 아직 켜지 않았다.** 켜는 것은 하위 저장소 전부에 즉시 적용되는 되돌리기 어려운 변경이라, 선행 조건이 다 채워진 뒤 사람이 판단해 실행한다. 명령과 조건은 아래 «조직 정책을 켜는 절차» 에 그대로 적어 뒀다.

관련 이슈: [#1538](https://github.com/Afternote/Afternote-FE/issues/1538) (Afternote-FE 저장소 한 곳) · [#1591](https://github.com/Afternote/Afternote-FE/issues/1591) (조직 수준)

## 1. 왜 그냥 켜면 안 되는가

조직의 `sha_pinning_required` 는 하위 저장소 전부에 즉시 걸린다. floating 참조가 남은 저장소의 run 은 **job 이 만들어지기 전에** `startup_failure` 로 죽고, job 이 없으니 로그에 사유도 남지 않는다. admin 만 설정 화면에서 원인을 볼 수 있다. 그래서 켜기 전에 조직 하위 전 저장소를 전수로 감사한다.

## 2. 감사

| 항목 | 값 |
| --- | --- |
| 감사 시각 | 2026-08-30T13:25:28Z |
| 대상 조직 | `Afternote` (저장소 3개 — 아카이브·포크 0건) |
| 스캔 리비전 | 38개 (기본 브랜치 3개 + Afternote-FE 열린 PR 35건의 head) |
| 스캔 대상 파일 | `.github/workflows/*.y[a]ml` + 모든 `action.y[a]ml` (composite action) |
| 총 `uses` | 4,699건 |

기준 리비전 (기본 브랜치):

| 저장소 | 기본 브랜치 | 리비전 | 공개 여부 |
| --- | --- | --- | --- |
| `Afternote/Afternote-FE` | `develop` | `fc8f32cd5f2b41fcf8cae9de2a8da54654a5566e` | public |
| `Afternote/Afternote-BE` | `main` | `0a4b870c8d6e91fa9472cf3058a71fa5d1af3e69` | public |
| `Afternote/demo-repository` | `main` | `57ea595faa5feed92d2ff91e60a04053f5a80451` | **private** |

재현 명령 — 감사 스크립트는 [`.github/scripts/audit-org-action-pinning.mjs`](../../.github/scripts/audit-org-action-pinning.mjs) 다.

```bash
GH_TOKEN=$(gh auth token) node .github/scripts/audit-org-action-pinning.mjs \
  --org Afternote --output /tmp/org-pinning-audit.json
```

floating 참조가 하나라도 남아 있으면 exit code 1 로 끝난다. 열린 PR 의 head 까지 훑는 이유는, 정책이 머지 전 브랜치의 run 에도 걸리기 때문이다 — 열린 PR 에 남은 floating 참조는 정책을 켜는 순간 그 PR 의 CI 를 죽인다.

### 저장소별 집계

| 저장소 | 스캔 리비전 | `uses` 합계 | SHA 고정 | **floating** | 로컬(`./`) |
| --- | ---: | ---: | ---: | ---: | ---: |
| `Afternote-FE` | 36 (`develop` + 열린 PR 35) | 4,665 | 3,739 | **0** | 926 |
| `Afternote-BE` | 1 (`main`, 열린 PR 0건) | 32 | 0 | **32** | 0 |
| `demo-repository` | 1 (`main`, 열린 PR 0건) | 2 | 0 | **2** | 0 |

**Afternote-FE 는 조직 정책과 이미 호환된다** — [#1538](https://github.com/Afternote/Afternote-FE/issues/1538) 에서 저장소 수준으로 켠 뒤 그대로 유지되고 있고, 열린 PR 35건 어디에도 floating 참조가 없다. 조직 정책을 켜도 이 저장소에서 깨지는 곳은 없다.

### floating 참조 전량 (34건)

| 저장소 | 참조 | 건수 | 워크플로 |
| --- | --- | ---: | --- |
| `Afternote-BE` | `actions/checkout@v7` | 12 | ci.yml, codeql.yml, dependency-submission.yml, deploy.yml, quality.yml, tls-expiry.yml |
| `Afternote-BE` | `actions/dependency-review-action@v5` | 1 | ci.yml |
| `Afternote-BE` | `actions/setup-java@v5` | 5 | ci.yml, dependency-submission.yml, deploy.yml |
| `Afternote-BE` | `actions/setup-python@v5` | 1 | deploy.yml |
| `Afternote-BE` | `actions/upload-artifact@v7` | 3 | ci.yml |
| `Afternote-BE` | `appleboy/scp-action@v1.0.0` | 2 | deploy.yml |
| `Afternote-BE` | `appleboy/ssh-action@v1.2.5` | 1 | deploy.yml |
| `Afternote-BE` | `docker/login-action@v4` | 1 | deploy.yml |
| `Afternote-BE` | `github/codeql-action/analyze@v4` | 1 | codeql.yml |
| `Afternote-BE` | `github/codeql-action/init@v4` | 1 | codeql.yml |
| `Afternote-BE` | `gradle/actions/dependency-submission@v6` | 2 | ci.yml, dependency-submission.yml |
| `Afternote-BE` | `gradle/actions/setup-gradle@v6` | 2 | ci.yml |
| `demo-repository` | `anishathalye/proof-html@v1.1.0` | 1 | proof-html.yml |
| `demo-repository` | `pozil/auto-assign-issue@v1` | 1 | auto-assign.yml |

`Afternote-BE` 는 composite action 이 없고 워크플로 6개가 전부다. `demo-repository` 는 워크플로 2개가 전부다.

**`actions/dependency-review-action@v5` 는 태그가 아니라 브랜치다.** `refs/tags/v5` 는 없고 `refs/heads/v5` 만 있다(감사 시점 `a1d282b36b6f3519aa1f3fc636f609c47dddb294`). floating 중에서도 위험도가 가장 높은 형태로, 업스트림이 force-push 하면 그 시점부터 다른 코드가 BE CI 에서 실행된다.

### BE·demo 가 SHA 고정으로 옮길 때 쓸 값 (감사 시점 해석)

이 저장소에서 고칠 수 없는 몫이라 값만 남긴다. `@v7` 같은 이동 태그는 시간이 지나면 다른 커밋을 가리키므로, 실제로 적용할 때 다시 해석한다.

| 참조 | 해석된 커밋 SHA |
| --- | --- |
| `actions/checkout@v7` | `3d3c42e5aac5ba805825da76410c181273ba90b1` |
| `actions/setup-java@v5` | `b6effb05e454b25005698d916606bdc6ffcbf961` |
| `actions/setup-python@v5` | `a26af69be951a213d495a4c3e4e4022e16d87065` |
| `actions/upload-artifact@v7` | `043fb46d1a93c77aae656e7c1c64a875d1fc6a0a` |
| `actions/dependency-review-action@v5` (브랜치) | `a1d282b36b6f3519aa1f3fc636f609c47dddb294` (= 태그 `v5.0.0`) |
| `gradle/actions/dependency-submission@v6` | `9c971963bec38e04b3d30dcc455b5382be2fdbfb` |
| `gradle/actions/setup-gradle@v6` | `9c971963bec38e04b3d30dcc455b5382be2fdbfb` |
| `github/codeql-action/init@v4` | `cdf488f595d80d6e07e03d4674febd5ab45fa938` |
| `github/codeql-action/analyze@v4` | `cdf488f595d80d6e07e03d4674febd5ab45fa938` |
| `docker/login-action@v4` | `dbcb813823bdd20940b903addbd779551569679f` |
| `appleboy/scp-action@v1.0.0` | `ff85246acaad7bdce478db94a363cd2bf7c90345` |
| `appleboy/ssh-action@v1.2.5` | `0ff4204d59e8e51228ff73bce53f80d53301dee2` |
| `anishathalye/proof-html@v1.1.0` | `47a787591515a207d6fc8ef13e016ac42cb877c8` |
| `pozil/auto-assign-issue@v1` | `d11e715efc663fe323c3d8d4d3cbbfdddd539baf` |

## 3. 판정 — `sha_pinning_required`

**켠다. 단 아래 선행 조건이 채워진 뒤에.** 조직 수준에서 켤 값은 이것 하나다.

이 설정은 boolean 이라 «조직 목록은 합집합이 된다» 는 문제가 없다. 어떤 저장소도 지금보다 느슨해지지 않고, 켜지 않은 저장소(BE·demo)만 FE 와 같은 기준으로 올라간다.

GitHub 문서의 정의는 이렇다 — 조직 설정 화면의 **Require actions to be pinned to a full-length commit SHA** 에 대해:

> When you enable **Require actions to be pinned to a full-length commit SHA**, all actions must be pinned to a full-length commit SHA to be used. This includes actions from your organization and actions authored by GitHub. **Reusable workflows can still be referenced by tag.**

재사용 워크플로는 태그 참조가 허용된다는 점이 예외다. Afternote-FE 의 재사용 워크플로 참조는 `develop` 기준 6건(고유 파일 4개) 전부 로컬(`./.github/workflows/*.yml`)이라 어느 쪽이든 영향이 없고, BE·demo 에는 재사용 워크플로 참조가 없다.

## 4. 판정 — `allowed_actions`

**조직 `allowed_actions` 는 `all` 로 둔다. 좁히는 일은 저장소별로 한다.** 근거 셋:

**(1) 조직 목록은 구조적으로 가장 느슨한 목록이 된다.** 저장소별 외부 액션 집합은 이렇게 갈린다.

| 저장소 | 외부 액션 종수 | 목록 |
| --- | ---: | --- |
| `Afternote-FE` | 14 | `actions/attest`, `actions/checkout`, `actions/dependency-review-action`, `actions/download-artifact`, `actions/github-script`, `actions/setup-java`, `actions/upload-artifact`, `docker/build-push-action`, `docker/setup-buildx-action`, `github/codeql-action/analyze`, `github/codeql-action/init`, `google-github-actions/auth`, `gradle/actions/dependency-submission`, `gradle/actions/setup-gradle` |
| `Afternote-BE` | 12 | `actions/checkout`, `actions/dependency-review-action`, `actions/setup-java`, `actions/setup-python`, `actions/upload-artifact`, `appleboy/scp-action`, `appleboy/ssh-action`, `docker/login-action`, `github/codeql-action/analyze`, `github/codeql-action/init`, `gradle/actions/dependency-submission`, `gradle/actions/setup-gradle` |
| `demo-repository` | 2 | `anishathalye/proof-html`, `pozil/auto-assign-issue` |
| **합집합** | **20** | 위 셋의 합 |

`github_owned_allowed` 가 덮는 `actions/*`·`github/*` 를 빼면 조직 목록에 명시해야 할 패턴은 10줄이 된다 — `anishathalye/proof-html`, `appleboy/scp-action`, `appleboy/ssh-action`, `docker/build-push-action`, `docker/login-action`, `docker/setup-buildx-action`, `google-github-actions/auth`, `gradle/actions/dependency-submission`, `gradle/actions/setup-gradle`, `pozil/auto-assign-issue`. Afternote-FE 가 [#1538](https://github.com/Afternote/Afternote-FE/issues/1538) 에서 정한 목록은 5줄이다. 조직 목록은 FE 가 쓰지도 않는 `appleboy/*`·`pozil/*`·`docker/login-action` 을 담게 된다.

**(2) 조직 목록이 저장소 목록을 덮어쓰는지가 문서에 없다.** REST 스펙(`api.github.com.json`, 2026-08-30 시점)의 조직·저장소 `selected-actions` 엔드포인트 설명 어디에도 상하위 관계가 적혀 있지 않고, 저장소 설정 문서는 이렇게만 말한다.

> You might not be able to manage these settings if your organization has an overriding policy or is managed by an enterprise that has overriding policy.

조직 목록이 저장소 목록을 **대체**한다면 (1)의 10줄이 곧 Afternote-FE 의 새 경계가 되고, #1538 이 좁혀 둔 것이 도로 넓어진다. 실측으로 가리려면 조직 정책을 실제로 켜야 하는데 그건 하위 저장소 전부에 걸리는 변경이라, 확인되지 않은 채로는 손대지 않는다.

**(3) `patterns_allowed` 는 공개 저장소에만 적용된다.** REST 스펙의 `selected-actions` 스키마 주석이다.

> The `patterns_allowed` setting only applies to public repositories.

`demo-repository` 는 private 이라, 조직 목록을 만들어도 그 저장소의 서드파티 액션은 patterns 로 막히지 않는다. 조직 목록으로 얻는 실익에 애초에 구멍이 있다.

정리하면 — 액션 허용 범위는 저장소마다 다르고 저장소가 가장 잘 안다. Afternote-FE 는 이미 5줄로 좁혀져 있고, Afternote-BE 는 자기 12종에 맞춰 저장소 수준에서 좁히면 된다(BE 몫). 조직 수준에서 얻을 것은 `sha_pinning_required` 하나다.

### 곁가지 — `enabled_repositories`

지금은 `all` 이다. `demo-repository` 는 2026-08-27 에 만들어진 GitHub 데모 저장소로 run 이 단 1건(`Proof HTML`)이고 실제 파이프라인이 아니다. 이 저장소를 정책의 사정권에서 빼는 가장 싼 방법은 워크플로를 고치는 게 아니라 Actions 자체를 끄는 것이다.

```bash
# demo-repository 의 Actions 를 끈다 (SHA 고정 대상에서 사라진다)
gh api --method PUT repos/Afternote/demo-repository/actions/permissions -F enabled=false
```

## 5. 조직 정책을 켜는 절차

### 선행 조건 (전부 충족돼야 한다)

1. **`Afternote-BE` 의 floating 참조 32건을 0으로 만든다.** 이 저장소(FE)에서 고칠 수 없는 몫이다. BE 워크플로 6개(`ci.yml`, `codeql.yml`, `dependency-submission.yml`, `deploy.yml`, `quality.yml`, `tls-expiry.yml`)를 SHA 고정으로 옮기는 작업이 선행돼야 한다. 값은 위 «해석된 커밋 SHA» 표에 있다.
2. **`demo-repository` 의 floating 참조 2건을 처리한다.** SHA 고정으로 옮기거나, 위처럼 Actions 를 끄거나, 저장소를 지운다.
3. **재감사에서 floating 0건을 확인한다.** 열린 PR 은 계속 새로 생긴다 — 이 감사 중에도 [#1602](https://github.com/Afternote/Afternote-FE/pull/1602) 가 시작 13초 뒤에 열렸다. 스냅샷은 금방 낡으므로 **켜기 직전에 다시 돌린다.**

```bash
GH_TOKEN=$(gh auth token) node .github/scripts/audit-org-action-pinning.mjs --org Afternote
echo "exit=$?"   # 0 이어야 한다
```

### 켜기 전 대조군 확보

조직 정책이 실제로 도는지 가리려면, **저장소 수준 pinning 이 꺼져 있는 저장소**에서 실측해야 한다. Afternote-FE 는 이미 저장소 수준에서 켜져 있어 그대로는 조직 정책과 저장소 정책을 구별할 수 없다. 자기 저장소 안에서 닫는 절차는 이렇다.

```bash
# 1) FE 의 저장소 수준 pinning 을 잠시 끈다 (조직 정책만 남긴다)
gh api --method PUT repos/Afternote/Afternote-FE/actions/permissions \
  -F enabled=true -f allowed_actions=selected -F sha_pinning_required=false

# 2) fixture 브랜치에 floating 참조 워크플로를 올려 run 이 success 인 것을 본다 (대조군)
#    예: uses: actions/checkout@v7  ← 목록 안 액션이지만 태그 참조
```

이 창이 열려 있는 동안은 FE 에 floating 참조가 머지될 수 있다. 몇 분 안에 닫는다.

### 켜는 명령

```bash
gh api --method PUT orgs/Afternote/actions/permissions \
  -f enabled_repositories=all \
  -f allowed_actions=all \
  -F sha_pinning_required=true

# 확인
gh api orgs/Afternote/actions/permissions
# 기대: {"enabled_repositories":"all","allowed_actions":"all","sha_pinning_required":true}
```

`enabled_repositories` 는 필수 필드다. `allowed_actions` 도 함께 보내 지금 값(`all`)을 명시적으로 유지한다.

### 켠 뒤 검증

1. 대조군으로 쓴 같은 fixture 를 다시 밀어 결과가 뒤집히는지 본다. 기대는 태그 참조에 대한 `The action ... is not allowed in ... because all actions must be pinned to a full-length commit SHA.` 다.
2. FE 의 저장소 수준 pinning 을 되돌린다.

```bash
gh api --method PUT repos/Afternote/Afternote-FE/actions/permissions \
  -F enabled=true -f allowed_actions=selected -F sha_pinning_required=true
```

3. fixture 브랜치를 지운다.
4. BE 의 CI·배포 워크플로가 `startup_failure` 없이 도는지 실제 run 으로 확인한다.

### 되돌리기

```bash
gh api --method PUT orgs/Afternote/actions/permissions \
  -f enabled_repositories=all \
  -f allowed_actions=all \
  -F sha_pinning_required=false
```

## 6. 지금 상태 (2026-08-30)

| 범위 | `allowed_actions` | `sha_pinning_required` |
| --- | --- | --- |
| `orgs/Afternote` | `all` | `false` |
| `repos/Afternote/Afternote-FE` | `selected` (github_owned + verified + 패턴 5줄) | `true` |
| `repos/Afternote/Afternote-BE` | `all` | `false` |
| `repos/Afternote/demo-repository` | `all` | `false` |

Afternote-FE 저장소의 허용 목록은 [#1590](https://github.com/Afternote/Afternote-FE/pull/1590) 에서 `.github/scripts/supply-chain-policy.test.mjs` 에 미러링된다. 목록 밖 액션은 CI 가 아니라 run 시작 시점에 죽어 로그에 사유가 남지 않기 때문에, 그 침묵을 diff 옆의 red 로 바꾸는 장치다.
