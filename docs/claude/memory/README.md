# 공유 메모리 템플릿

본 폴더의 memory 파일들은 본 repo 워크플로에 강하게 묶인 (= 1hyok 개인 영역이 아니라 팀이 공유해도 가치 있는) 항목만 추려둔 것이다.

## 자기 Claude memory 에 도입

Claude Code 는 사용자별 memory 디렉토리 (`~/.claude/projects/<encoded-project-path>/memory/`) 의 `MEMORY.md` 를 index 로 읽고, 거기 링크된 파일을 컨텍스트에 로딩한다.

도입 절차:

1. 자기 memory 폴더 확인 (Afternote-FE 에서 한 번이라도 Claude Code 를 띄웠다면 자동 생성됨):
   ```bash
   ls ~/.claude/projects/-Users-*-Afternote-FE/memory/  # macOS 경로 인코딩 패턴
   ```
2. 마음에 드는 메모리 파일을 자기 폴더로 복사:
   ```bash
   cp docs/claude/memory/feedback_pr_body_template.md \
      ~/.claude/projects/<your-encoded-path>/memory/
   ```
3. 자기 `MEMORY.md` 에 한 줄 추가:
   ```markdown
   - [PR body template](feedback_pr_body_template.md) — PR 본문은 📌Issues / 📎Work Description / 📷Screenshot / 💬To Reviewers 4섹션 양식
   ```

## 카테고리

### 워크플로 (이슈/브랜치/PR)
- `feedback_issue_first_branch.md` — 이슈 작성 + Assignee/Label/Type + 브랜치 링크 4단계
- `feedback_issue_body_template.md` — `.github/ISSUE_TEMPLATE/custom.md` 양식 (3 섹션 + No response)
- `feedback_branch_base_overlap_check.md` — 새 브랜치 전 이전 PR 변경 파일 확인 → stack 여부 결정
- `feedback_pr_body_template.md` — PR 본문 4 섹션 양식
- `feedback_pr_title_issue_number.md` — PR 제목 끝에 `(#NNN)` 포함
- `feedback_no_pr_body_in_chat.md` — PR 본문을 채팅에 노출 X (URL 만 보고)
- `feedback_pr_labels.md` — 이슈 라벨과 PR 라벨은 독립
- `feedback_pr_review_via_gh_pr_review.md` — PR 리뷰는 `gh pr review` (not `pr comment`)
- `feedback_pr_merge_needs_approval.md` — branch protection 으로 reviewer 승인 강제
- `feedback_cleanup_merged_branches.md` — 머지된 PR 의 원격/로컬 브랜치 자동 삭제

### 코드 컨벤션
- `feedback_annotation_use_site_target.md` — Constructor `val` annotation 은 `@param:` 명시 (KT-73255)
- `feedback_kdoc_dangling_refs.md` — KDoc `[Symbol]` 마커는 resolve 되는지 자체 점검
- `feedback_kdoc_slash_star.md` — KDoc 안 `/*` 중첩 주석 위험
- `feedback_no_network_in_presentation.md` — presentation → core:network 직접 의존 금지
- `feedback_no_throwaway_refactor.md` — 다음 PR 에서 제거될 코드는 본 PR 에 안 박기
- `feedback_check_existing_abstractions.md` — 추상화 추출 전 기존 공통 인터페이스 grep 확인
- `feedback_dead_vs_unimplemented.md` — ViewModel/Screen 호출 0건 = dead 단정 X (NavGraph 미연결 가능성)

## 제외한 영역

다음은 개인 영역이라 본 폴더에 의도적으로 포함하지 않음:

- 사용자 어조 (어디까지 묻기 / 응답 길이 톤)
- 사용자 책임 영역 정의 (홈/온보딩/코어/feat/afternote 같은 1hyok 영역)
- 사용자 작업 흐름 (stash batch, auto-continue after PR 같은 개인 선호)
- 사용자별 incident 회상 (특정 PR 의 실수 메모)

자기 영역·어조에 맞는 메모리는 본인이 따로 작성·운영.
