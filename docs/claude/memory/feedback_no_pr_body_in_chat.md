---
name: Don't dump PR body in chat
description: PR 본문은 gh pr create --body 인자로만 전달하고 채팅에는 노출하지 않는다 — GitHub PR 에서 보이므로 중복
type: feedback
originSessionId: unknown
---
PR 생성 시 본문 텍스트(📌Issues·📎Work Description·📷Screenshot·💬To Reviewers 4섹션)를 *채팅 메시지* 에도 출력하지 않는다.

**Why:** PR 이 생성되면 사용자가 GitHub PR 페이지에서 본문을 직접 확인. 채팅에 또 붙이면 동일 텍스트가 두 군데에 노출되어 잡음만 늘어남.

**How to apply:**
- `gh pr create --body "$(cat <<'EOF' ... EOF)"` 형태로 본문은 인자 안에만 작성
- 채팅 응답은 PR URL + 한 줄 요약 정도로 끝
- "미리 본문 초안 보여드릴까요?" 같은 제안도 X — 그냥 만들고 URL 만 보내기
- 본문 양식 자체는 `feedback_pr_body_template.md` 의 4섹션 이모지+italic 그대로 유지
