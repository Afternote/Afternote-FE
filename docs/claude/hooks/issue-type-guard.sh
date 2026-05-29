#!/usr/bin/env bash
# PostToolUse Bash hook: `gh issue create` 직후 새 이슈의 issueType 검증.
#
# - null 이면 차단 (Claude 가 즉시 GraphQL `updateIssue` 호출로 보정)
# - title prefix 와 actual Type mismatch 면 차단
#   - fix(...) → Bug
#   - feat(...) → Feature
#   - chore/refactor/test/ci/build/docs(...) → Task
#
# Bypass: 본인이 의도적으로 prefix 와 다른 Type 을 명시 후 hook 우회하려면 issue 본문에
# `<!-- type-override: <Task|Bug|Feature> --> marker 를 박아 hook 가 마지막 actual 만 검증.
set -uo pipefail

input="$(cat)"
cmd="$(echo "$input" | jq -r '.tool_input.command // empty')"
output="$(echo "$input" | jq -r '.tool_response.output // empty')"
[ -z "$cmd" ] && exit 0

# `gh issue create` 호출만 대상
if ! [[ "$cmd" =~ (^|[[:space:]]|\;|\&|\|)gh[[:space:]]+issue[[:space:]]+create ]]; then
    exit 0
fi

# 출력에서 새 이슈 URL 추출 (`https://github.com/<owner>/<repo>/issues/<N>`)
url="$(echo "$output" | grep -oE 'https://github\.com/[^/]+/[^/]+/issues/[0-9]+' | head -1)"
[ -z "$url" ] && exit 0

owner_repo="$(echo "$url" | sed -E 's|https://github.com/([^/]+/[^/]+)/issues/.*|\1|')"
issue_num="$(echo "$url" | sed -E 's|.*/issues/([0-9]+)|\1|')"
owner="$(echo "$owner_repo" | cut -d/ -f1)"
repo="$(echo "$owner_repo" | cut -d/ -f2)"

# 새 이슈의 issueType + title 조회
data="$(gh api graphql -f query="
query {
  repository(owner: \"$owner\", name: \"$repo\") {
    issue(number: $issue_num) { title issueType { name } }
  }
}" 2>/dev/null)"

actual_type="$(echo "$data" | jq -r '.data.repository.issue.issueType.name // "null"')"
title="$(echo "$data" | jq -r '.data.repository.issue.title // ""')"

# null = Type 누락
if [ "$actual_type" = "null" ]; then
    jq -nc --arg url "$url" --arg n "$issue_num" \
      '{hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: ("Issue \($url) 의 issueType 누락. Task/Bug/Feature 중 명시 + GraphQL updateIssue 즉시 호출 필요. 예: gh api graphql -f query=\"mutation { updateIssue(input: { id: <issue-node-id>, issueTypeId: <type-node-id> }) { issue { number issueType { name } } } }\". Type 노드 ID: Task=IT_kwDOD_R4ms4B5VUs, Bug=IT_kwDOD_R4ms4B5VUt, Feature=IT_kwDOD_R4ms4B5VUu.")}}'
    exit 0
fi

# title prefix → expected Type 추론
expected=""
case "$title" in
    fix\(*|fix:*) expected="Bug" ;;
    feat\(*|feat:*) expected="Feature" ;;
    chore\(*|chore:*|refactor\(*|refactor:*|test\(*|test:*|ci\(*|ci:*|build\(*|build:*|docs\(*|docs:*) expected="Task" ;;
esac

if [ -n "$expected" ] && [ "$actual_type" != "$expected" ]; then
    jq -nc --arg url "$url" --arg actual "$actual_type" --arg expected "$expected" --arg title "$title" \
      '{hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: ("Issue \($url) 의 issueType=\($actual) 가 title prefix 와 mismatch. Title: \"\($title)\" → expected=\($expected). 의도적이면 issue 본문에 marker `<!-- type-override: \($actual) -->` 추가 후 재시도. 보정하려면 GraphQL updateIssue 호출.")}}'
fi

exit 0
