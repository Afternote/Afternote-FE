# presentation 패키지 구조 규칙

**기능(화면) 폴더가 기본 단위다. 타입(`screen/` · `viewmodel/` · `component/`)으로 먼저 쪼개지 않는다.**

`feature/*/presentation` 모듈에 적용한다. data · domain 계층은 대상이 아니다.

실측 기준 리비전: `efbb1256c` (origin/develop, 2026-08-24).

## 현황

| 모듈 | main 파일 | 1단계 폴더 | 방식 | 최대 깊이 |
|---|---:|---|---|---:|
| `afternote` | 116 | `author` · `shared` · `reporting` | 기능별 | 6 |
| `mindrecord` | 79 | `component` · `screen` · `viewmodel` · `model` · `mapper` · `util` · `hometab` · `navigation` | 타입별 | 2 |
| `receiver` | 65 | `home` · `detail` · `summary` · `recordsbox` · `deliveryverification` · `playlist` · `afternotelist` · `senderdetail` · `navigation` · `reporting` | 기능별 | 2 |
| `setting` | 64 | `component` · `screen` · `viewmodel` · `social` · `navigation` | 타입별 | 1 |
| `timeletter` | 38 | `component` · `screen` · `viewmodel` · `navigation` | 타입별 | 2 |
| `onboarding` | 27 | `signup` · `login` · `findaccount` · `terms` · `navigation` · `reporting` | 기능별 | 2 |
| `home` | 5 | `usecase` · `reporting` | 평면 | 1 |

7개 모듈 중 3개(파일 수로는 208/394)가 이미 기능별이고, 그중엔 가장 큰 `afternote` 가 있다. 소수를 다수에 맞춘다.

## 왜 기능별인가

- **변경이 같이 일어난다** — 화면 하나를 고치면 Screen · ViewModel · UiState 가 거의 항상 함께 바뀐다. 기능별이면 커밋 diff 가 한 폴더에 모인다.
- **삭제가 쉽다** — 기능이 사라지면 폴더째 지운다. 타입별에서는 4곳을 뒤져야 하고, 그래서 참조 0건인 잔재가 남는다(#941).
- **`internal` 이 실제로 쓰인다** — 기능별인 `afternote` 는 top-level 선언 294개 중 56개가 `internal` 이다. 타입별인 `setting` 은 119개 중 4개뿐이다. 폴더가 경계를 안 그으면 가시성도 안 좁혀진다.
- **타입별은 스스로도 일관되지 않는다** — `mindrecord` 는 `DiaryWriteUiState.kt` 가 `viewmodel/` 에, 같은 성격의 `DailyDiary.kt` 는 `model/` 에 있다. 홈 탭은 `hometab/HomeTabMindRecordLazyItems.kt` 와 `component/hometab/RecordCategoryCard.kt` 로 갈렸다. 규칙이 없으니 어디를 봐야 할지 매번 달라진다.

## 목표 구조

```
feature/<name>/presentation/src/main/kotlin/com/afternote/feature/<name>/presentation/
  <기능>/                     # 예: passkey/ · profileedit/ · diarywrite/
    XxxScreen.kt
    XxxViewModel.kt
    XxxUiState.kt
    component/                # 이 기능만 쓰는 컴포넌트
  shared/                     # 2개 이상 기능이 쓰는 것만
    component/
    model/
    util/
  navigation/                 # 모듈에 하나
  reporting/                  # 모듈에 하나 (기존 관례 유지)
```

## 규칙

### R1. 1단계는 기능 폴더다

`screen` · `viewmodel` · `component` · `model` 을 1단계에 두지 않는다. 1단계 이름은 화면 묶음의 이름(`passkey` · `withdraw` · `diarywrite`)이다.

기능 단위는 **함께 바뀌는 화면 묶음**이다. `PassKeyScreen` · `PassKeyListScreen` · `PassKeyMakingScreen` · `PassKeyPasswordScreen` 은 한 흐름이므로 `passkey/` 하나다. 화면마다 폴더를 만들지 않는다.

### R2. 깊이는 2단계까지다

presentation 패키지 루트 기준으로 최대 2단계다.

```
passkey/PassKeyScreen.kt                    # 1단계 — OK
passkey/component/PasskeyListItem.kt        # 2단계 — OK
author/editor/memorial/playlist/Xxx.kt      # 4단계 — 금지
```

2단계에 올 수 있는 이름은 타입 폴더(`component` · `model` · `util`)뿐이다. **기능 폴더 안에 기능 폴더를 두지 않는다.** 기능 폴더가 커져 쪼개고 싶으면 1단계에 형제 폴더로 만든다(`editor/` 가 커지면 `editormemorial/` 이 아니라 `memorial/`).

`afternote` 는 기능별이지만 `shared/body/infinite/content/list/item/` 까지 6단계로 내려간다. 반대 방향으로 불편해진 사례이고, 별도로 평탄화한다.

### R3. `shared/` 는 2개 이상 기능이 쓸 때만이다

한 기능만 쓰면 그 기능 폴더로 내린다. "공용이 될 것 같아서" 는 근거가 아니다 — 두 번째 사용처가 생길 때 옮긴다.

**이관이 끝난 모듈**에서는 사용처의 1단계 폴더를 센다.

```bash
M=feature/receiver/presentation/src/main/kotlin/com/afternote/feature/receiver/presentation
git grep -lw 'HomeSectionCard' -- "$M" | sed "s#$M/##" | cut -d/ -f1 | sort -u
```

출력이 2줄 이상이면 `shared/`, 1줄이면 그 기능 폴더다.

**아직 타입별인 모듈을 이관할 때**는 폴더가 판정 재료가 못 되므로, 그 컴포넌트를 쓰는 화면을 센다.

```bash
M=feature/setting/presentation/src/main/kotlin/com/afternote/feature/setting/presentation
git grep -lw 'PasskeyListItem' -- "$M" | grep -oE '[A-Za-z]+Screen\.kt' | sort -u
```

서로 다른 기능의 화면이 2개 이상이면 `shared/` 다. 출력이 비면 둘 중 하나다 — 컴포넌트끼리만 쓰거나(그 상위 컴포넌트가 속한 기능을 따라간다), 참조가 없다(옮기지 말고 지운다).

### R4. `*UiState` 는 그 화면의 기능 폴더에 둔다

`*UiState` · `*UiEvent` · `*UiEffect` 는 ViewModel 과 같은 폴더다. `viewmodel/` 과 `model/` 로 갈라 두지 않는다.

여러 기능이 공유하는 UI 모델만 `shared/model/` 에 둔다. 판정 기준은 R3 과 같다.

`mapper/` · `util/` 같은 나머지 타입 폴더도 같은 기준이다 — 한 기능만 쓰면 그 기능 폴더 안으로, 여럿이 쓰면 `shared/` 아래로 간다.

### R5. `navigation/` · `reporting/` 은 모듈에 하나다

`Route` · `NavGraph` · `NavActions` 는 모듈 진입점이라 기능별로 쪼개지 않는다. 실패 리포팅(`*FailureReporting.kt`)도 모듈 단위 관례를 유지한다.

## 이관 PR 규칙

파일 이동만이라 위험은 낮지만 diff 가 거대해진다. 리뷰 가능성을 지키기 위해:

- **모듈 하나가 PR 하나다.** 여러 모듈을 한 PR 로 묶지 않는다.
- **그 모듈에 열린 PR 이 없을 때 진행한다.** 이동은 리베이스가 사실상 불가능하다. 착수 전에 확인한다.

  ```bash
  gh pr list --state open --limit 100 --json number,headRefName,files \
    -q '.[] | select(any(.files[]; .path | startswith("feature/setting/presentation/"))) | "\(.number) \(.headRefName)"'
  ```

- **이동 외 변경을 섞지 않는다.** `package` 선언과 `import` 갱신만 허용한다. 로직 · 포맷 변경은 별도 PR 이다. 검증:

  ```bash
  git diff -M origin/develop...HEAD -U0 \
    | grep -E '^[+-]' | grep -vE '^(\+\+\+|---)' \
    | grep -vE '^[+-](package |import )'
  ```

  출력이 비어야 이동 전용 PR 이다. 비어 있지 않으면 그 줄이 리뷰어가 볼 실제 변경이므로 PR 본문에 적는다.

- **GitHub 의 `renamed` 라벨을 근거로 쓰지 않는다.** 유사도 휴리스틱이라 내용 변경을 숨긴다. 위 명령 출력을 근거로 쓴다.

## 이관 현황

- [x] 규칙 문서화 — 이 문서
- [ ] [#1086](https://github.com/Afternote/Afternote-FE/issues/1086) — `feature/timeletter/presentation` (38, 가장 작음 → 파일럿) — @koongmai
- [ ] [#1087](https://github.com/Afternote/Afternote-FE/issues/1087) — `feature/mindrecord/presentation` (80) — @Sadturtleman
- [ ] [#1088](https://github.com/Afternote/Afternote-FE/issues/1088) — `feature/setting/presentation` (64) — @1hyok
- [ ] `feature/afternote/presentation` 깊이 평탄화 검토 (117, 이미 기능별이므로 이관 아님) — @1hyok

`receiver` · `onboarding` 은 이미 규칙을 만족해 대상이 아니다. `home` 은 파일 5개로 아직 폴더를 나눌 규모가 아니며, 화면이 늘면 R1 을 따른다.

새로 만드는 presentation 모듈은 처음부터 이 구조로 만든다.
