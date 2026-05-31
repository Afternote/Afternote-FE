# Claude Code 워크플로 자산 — 참고용

이 폴더는 **1hyok** 이 본 repo 에서 [Claude Code](https://claude.com/claude-code) 를 쓰면서 누적한 hook · 컨벤션 · 메모리 템플릿이다. **강제 X, 참고 O**. 본인 `.claude/` 는 각자 개인 영역이고 본 폴더와 무관하다.

## 폴더 구성

| 경로 | 내용 |
|------|------|
| `hooks/` | git 워크플로 강제 hook 들 (issue-first, autonomous commit 금지, destructive 차단 등) |
| `CLAUDE.md.example` | 팀 공통 룰만 추출한 `CLAUDE.md` 샘플 |
| `memory/` | 공유 가치 있는 메모리 템플릿 (PR body 양식, 이슈 양식 등) |

## 가장 빠르게 도입 — install 스크립트

본 repo 의 hook 들을 자기 `.claude/hooks/` 로 symlink. **기존 파일이 있으면 skip 해서 본인 hook 덮어쓰기 0**.

```bash
./scripts/install-claude-hooks.sh
```

symlink 라서 본 repo 의 `docs/claude/hooks/*.sh` 가 업데이트되면 자기 hook 도 자동 반영. 다만 hook 등록은 직접 — 자기 `.claude/settings.local.json` (없으면 신설) 의 `hooks` 섹션에 다음 추가:

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          { "type": "command", "command": "bash \"$CLAUDE_PROJECT_DIR/.claude/hooks/git-branch-guard.sh\"" },
          { "type": "command", "command": "bash \"$CLAUDE_PROJECT_DIR/.claude/hooks/git-state-guard.sh\"" },
          { "type": "command", "command": "bash \"$CLAUDE_PROJECT_DIR/.claude/hooks/git-commit-guard.sh\"" }
        ]
      }
    ]
  }
}
```

## 개별 hook 설명

### `git-branch-guard.sh` — issue-first 강제

`git checkout -b <name>` / `git switch -c <name>` / `git branch -m <new>` 가
- 이름 패턴 `feat/<숫자>` 아니면 차단
- 해당 숫자의 GitHub issue 가 없으면 차단

→ Claude 가 마음대로 `git checkout -b fix-something` 같은 brand 부여 못함. 본 repo 의 "이슈 우선 + Assignee/Label/Type" 규약 자동 강제.

### `git-state-guard.sh` — destructive 차단

- `git push --force` / `--force-with-lease` / `+refspec` 차단 (일반 push 와 `push --delete` 는 통과)
- `git reset --hard` 차단
- `git rebase` 차단
- `git branch -D` 차단
- `git clean -f` 차단
- `git checkout -- <file>` / `git restore <file>` (작업 파일 폐기) 차단

→ Claude 가 사용자 작업물 날리는 사고 방지.

### `git-commit-guard.sh` — autonomous commit 금지

`git commit` 자체를 차단. 사용자가 Android Studio 커밋 탭에서 직접 검토·커밋한다는 본 repo 컨벤션 자동 강제.

### `issue-type-guard.sh` — issue type 메타 강제

`gh issue create` 호출 시 type 메타 (Feature / Bug / Task) 누락 차단. issue tracker 의 ownership·종류가 항상 명시.

## CLAUDE.md.example

본 repo 의 팀 공통 룰만 추출한 `CLAUDE.md` 샘플. 사용자별 영역 (작업 어조, 본인 책임 영역 정의 등) 은 제외했다. 자기 프로젝트 루트 `CLAUDE.md` (gitignored) 에 그대로 복사하거나 일부만 가져가서 사용.

## memory/

본인의 memory 디렉토리에서 **다른 사람도 공유할 가치 있는** 항목만 추려둠 — 본 repo 의 PR body 양식, 이슈 본문 양식, branch overlap 검사 등. 자기 `~/.claude/projects/<encoded-path>/memory/` 에 복사해서 사용.

개인적 영역 (사용자 어조, 특정 incident 회상, 본인 책임 영역 등) 은 의도적으로 제외했다.

## 거부·취사선택 정책

- hook 일부만 가져가도 OK (예: `git-state-guard` 만)
- 메모리 일부만 가져가도 OK
- 본인 `.claude/` 와 본 폴더는 완전 분리 — 어느 쪽도 다른 쪽을 강제하지 않는다

## 질문 / 마찰 신고

본 폴더의 hook · 메모리 패턴이 본인 워크플로와 충돌하면 1hyok 에게 슬랙으로. 본 repo 의 git 워크플로 자체에 영향 X (본인 `.claude/` 에 적용한 경우만 본인 Claude 가 hook 받음).
