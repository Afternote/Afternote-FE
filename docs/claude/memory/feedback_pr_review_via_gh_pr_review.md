# PR 리뷰는 단순 코멘트 X — `gh pr review` 의 formal Review 사용

본인이 "PR 리뷰 남겨줘" / "PR 검토해줘" 류 요청 시. 단순 `gh pr comment` 는 conversation 댓글로만 남고 **Review 카운터에 안 잡힘** → 작성자가 "리뷰 들어왔다" 알림 못 받음.

## 사용할 명령

### General review (verdict 없는 코멘트)
```bash
gh pr review <pr_num> --comment --body "전체 의견..."
```
- "Submit review" 박스의 *Comment* 옵션과 동일
- 일반 피드백 — 승인/거부 아님

### Approve
```bash
gh pr review <pr_num> --approve --body "LGTM. 머지 진행 가능."
```

### Request changes
```bash
gh pr review <pr_num> --request-changes --body "변경 요청..."
```

### 라인 단위 인라인 코멘트 (formal review 안에 묶기)
`gh pr review` CLI 가 인라인 미지원. GraphQL `addPullRequestReview` mutation 직접 호출:
```bash
gh api graphql -f query='mutation { addPullRequestReview(input: {pullRequestId: "...", event: COMMENT, comments: [{path: "...", line: 42, body: "..."}]}) { pullRequestReview { url } } }'
```
또는 REST `POST /repos/{owner}/{repo}/pulls/{num}/reviews` 의 `comments` array.

## Verdict 선택 룰
- 본인이 의도 명시 안 함 → 기본 `--comment` (안전, 결정 강제 X)
- 본인이 "승인해 / approve" 명시 → `--approve`
- 본인이 "변경 요청 / request changes" 명시 → `--request-changes`

## Anti-pattern (이전 실수)
- ❌ `gh pr comment` — review 처리 안 됨, 단순 conversation 댓글
- ❌ 인라인 라인 코멘트를 review 안에 묶지 않고 개별 `gh api .../comments` 로 흩뿌리기 — 리뷰어 알림 분산
