import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const guard = await readFile(new URL("../workflows/review-debt-guard.yml", import.meta.url), "utf8");
const requestAll = await readFile(new URL("../workflows/review-request-all.yml", import.meta.url), "utf8");

test("team membership is judged by real permission, not author_association", () => {
    // 웹훅 페이로드의 author_association 은 조직 멤버십이 비공개면 MEMBER 대신
    // CONTRIBUTOR 를 내린다. 이 조직은 공개 멤버가 0명이라, 레포에 직접 초대된 한
    // 계정만 COLLABORATOR 로 찍혀 검사를 받고 나머지 팀원은 전원 건너뛰어졌다.
    // 가드가 한 사람만 막는 상태였다 (8/25 #1152·#1154 통과 / #1112 만 닫힘).
    // 주석은 이력이라 남기고, 실제로 값을 끌어오는 자리만 금지한다.
    assert.doesNotMatch(guard, /^\s*[A-Z_]+:\s*\$\{\{[^\n]*author_association/m);
    assert.doesNotMatch(guard, /\$ASSOCIATION/);
    assert.match(guard, /collaborators\/\$AUTHOR\/permission/);
});

test("only write-capable accounts are subject to the guard", () => {
    // read 권한만 있는 계정은 리뷰로 판정을 낼 수 없으니 빚을 질 수도 없다.
    assert.match(guard, /^\s*admin\|maintain\|write\)/m);
    // 빈 값·예상 밖 값까지 «권한 없음» 으로 접으면 조회 실패가 통과로 둔갑한다.
    assert.match(guard, /^\s*read\|none\)[^\n]*exit 0/m);
    assert.match(guard, /^\s*\*\) echo "::error::권한 값을 알 수 없다[^\n]*exit 1/m);
});

test("a failed permission lookup fails the run instead of passing the pull request", () => {
    // 조회 실패를 «권한 없음» 으로 접으면 가드 전체가 조용히 무력해진다.
    assert.match(guard, /if ! perm=\$\(gh api[^\n]*\n\s*echo "::error::[^\n]*\n\s*exit 1/);
});

test("forks are excluded by head repository, not by contributor status", () => {
    // fork PR 은 토큰이 read-only 라 닫지도 못한다. 그것이 원래 걸러내려던 것이고,
    // author_association 은 그 판정에 쓸 수 없는 축이었다.
    assert.match(guard, /\[ "\$HEAD_REPO" != "\$REPO" \]/);
});

test("the debt sweep still fails closed when open pull requests cannot be listed", () => {
    // 조회 실패를 «빚 없음» 으로 오인하면 새 PR 이 전부 통과한다.
    assert.match(guard, /if ! open_prs=\$\(gh api[^\n]*\n(?:[^\n]*\n)*?\s*exit 1/);
});

test("a pull request is never counted as debt against its own author", () => {
    // 자기 PR 은 자신이 리뷰할 수 없다.
    assert.match(guard, /select\(\.user\.login != \\"\$AUTHOR\\"\)/);
});

test("the dead review_request_removed path is gone", () => {
    // 빚 기준은 여전히 열린 PR 전체다. 변경요청 뒤 명시적 재요청 여부를 확인하더라도
    // 요청 제거 자체를 별도 감사하는 job 은 가드 런 19건 동안 발동한 적이 없었다.
    // 주석은 이력이라 남기고, 실제로 트리거를 걸고 job 을 세우는 자리만 금지한다.
    assert.doesNotMatch(guard, /^\s*types:[^\n]*review_request_removed/m);
    assert.doesNotMatch(guard, /^\s{2}bypass-audit:/m);
});

test("changes requested becomes debt only after explicit rerequest and a fix", () => {
    // 수정 커밋만으로 리뷰 의무를 만들지 않는다. 변경요청을 낸 리뷰어가 현재 요청
    // 목록에 다시 들어온 경우에만 반영 여부를 보고 빚으로 센다.
    assert.match(guard, /CHANGES_REQUESTED/);
    assert.match(guard, /requested_reviewers\[\]\.login/);
    assert.match(guard, /blocked_by/);
    assert.match(guard, /명시적 재리뷰 요청 없음/);
    assert.match(guard, /sort_by\(\.t\) \| last/);
    assert.doesNotMatch(guard, /group_by\(\.u/);
    assert.match(guard, /\(\.parents \| length\) < 2/);
    // 반영 판정의 기준 시각은 여전히 최신 변경요청 시각이다. 집계가 jq 에서 awk 로
    // 옮겨 갔을 뿐, 그 이전 커밋을 반영으로 세지 않는다.
    assert.match(guard, /-v cutoff="\$blocked_at"/);
    assert.match(guard, /\$4 > cutoff/);
});

test("a reviewer's own commit is never counted as the author's fix", () => {
    // 누가 올렸는지를 안 보면 리뷰어가 미는 CI 재트리거 커밋이 «작성자가 반영했다» 가
    // 된다. 8/28 에 리뷰어가 건 +0/-0 커밋 하나로 koongmai PR 3건(#1379·#1365·#882)이
    // 전부 가짜 빚이 됐고, 재트리거한 리뷰어가 그 대가로 자기 PR 을 못 열었다 (#1459).
    assert.match(guard, /\(\.author\.login \/\/ ""\)/);
    assert.match(guard, /login == target/);
    // 계정이 연결되지 않은 커밋은 login 이 비어 가릴 수 없다. 같은 PR 에서 작성자
    // 것으로 확인된 커밋의 이메일을 폴백 신원으로 쓴다.
    assert.match(guard, /\(\.commit\.author\.email \/\/ ""\)/);
    assert.match(guard, /email in own/);
});

test("an empty commit is not a fix", () => {
    // pulls/{n}/commits 응답에는 파일 정보가 없다. 작성자 커밋으로 좁힌 후보에 한해
    // commits/{sha} 를 조회해 바뀐 파일이 0건이면 버린다.
    assert.match(guard, /repos\/\$REPO\/commits\/\$sha/);
    assert.match(guard, /\(\.files \/\/ \[\]\) \| length/);
    assert.match(guard, /\[ "\$changed" -gt 0 \] \|\| continue/);
    // 조회 실패나 깨진 응답을 반영·미반영 어느 쪽으로도 접지 않는다. 근거가 완전하지
    // 않으면 가드를 실패시켜 사람의 정상 PR 을 닫는 오탐을 막는다.
    assert.doesNotMatch(guard, /changed=1/);
    assert.match(guard, /변경 파일 조회에 실패했다/);
    assert.match(guard, /변경 파일 수가 올바르지 않다/);
});

test("review evidence API failures stop the guard before it can close a pull request", () => {
    // 리뷰·커밋·댓글을 못 읽은 상태는 «응답 없음» 이 아니다. 각 근거 조회가 실패하면
    // 즉시 종료하고, `|| true` 나 stderr 폐기로 빈 결과를 만들어서는 안 된다.
    for (const message of [
        "리뷰 조회에 실패했다",
        "커밋 조회에 실패했다",
        "변경 파일 조회에 실패했다",
        "작성자 일반 응답 조회에 실패했다",
        "작성자 리뷰 응답 조회에 실패했다",
        "본문 편집 이력 조회에 실패했다",
        "본문 편집 이력이 불완전하다",
    ]) {
        assert.match(guard, new RegExp(message));
    }
    assert.doesNotMatch(guard, /gh api[^\n]*(?:\n[^\n]*){0,4}\|\| true/);
    assert.doesNotMatch(guard, /gh api[^\n]*2>\/dev\/null/);
});

test("a fix delivered by a merge commit still counts as a response", () => {
    // 비병합 커밋만 세면 base 를 끌어와 충돌을 풀며 반영한 PR 이 «미반영» 이 된다.
    // #1316 은 리뷰가 지목한 파일이 실제로 바뀌고 작성자 응답 코멘트도 2건 달린
    // 채로 빚에서 빠져 있었다. 커밋과 함께 작성자의 응답을 본다 (#1450).
    assert.match(guard, /issues\/\$pn\/comments/);
    assert.match(guard, /pulls\/\$pn\/comments/);
    assert.match(guard, /select\(\.user\.login == \\"\$pr_author\\"\)/);
    assert.match(guard, /\[ "\$\{fixed:-0\}" -gt 0 \] \|\| \[ "\$responses" -gt 0 \]/);
});

test("counts are taken per item so paginated pull requests do not break the comparison", () => {
    // --paginate 는 페이지마다 jq 를 돌린다. 페이지 단위로 length 를 뽑으면 커밋이
    // 100건을 넘는 PR 에서 숫자가 여러 줄로 나와 -gt 비교가 죽는다.
    assert.doesNotMatch(guard, /\| length" 2>\/dev\/null/);
    assert.match(guard, /\| wc -l \| tr -d ' '/);
});

test("rerequests are automated so silence cannot pass the guard", () => {
    // 가드는 변경요청을 낸 리뷰어에게 요청이 다시 걸린 경우에만 빚으로 센다. 그
    // 되살리기를 작성자 손에 맡기면 아무도 걸지 않아 가드가 통째로 무력해진다 —
    // 8/29 실측에서 반영까지 끝난 7건이 전원 «빚 아님» 이었다 (#1450).
    assert.match(requestAll, /^\s*types: \[opened, ready_for_review, reopened, synchronize, edited\]/m);
    assert.match(requestAll, /^\s{2}rerequest:/m);
    assert.match(requestAll, /github\.event\.action == 'synchronize'/);
    assert.match(requestAll, /github\.event\.action == 'edited'/);
    assert.match(requestAll, /github\.event\.changes\.body != null/);
    assert.match(
        requestAll,
        /github\.event\.sender\.login == github\.event\.pull_request\.user\.login/,
    );
    assert.match(requestAll, /github\.event\.pull_request\.state == 'open'/);
    assert.match(requestAll, /--add-reviewer "\$blocked"/);
    // 기존 전원 요청은 반영 커밋이나 본문 편집마다 다시 돌지 않는다.
    assert.match(requestAll, /github\.event\.action != 'synchronize'/);
    assert.match(requestAll, /github\.event\.action != 'edited'/);
});

test("rerequest review lookup fails closed because body edits are one-shot events", () => {
    assert.match(requestAll, /리뷰 조회에 실패했다 — 재리뷰 요청을 누락시키지 않도록 재실행할 것/);
    assert.match(requestAll, /최신 리뷰 판정에 실패했다 — 재리뷰 요청을 누락시키지 않도록 재실행할 것/);
    assert.doesNotMatch(requestAll, /gh api[^\n]*(?:\n[^\n]*){0,4}2>\/dev\/null/);
    assert.doesNotMatch(requestAll, /gh api[^\n]*(?:\n[^\n]*){0,4}\|\| true/);
});

test("an edited event cannot be attributed to a later change request", () => {
    assert.match(requestAll, /EVENT_AT: \$\{\{ github\.event\.pull_request\.updated_at \}\}/);
    assert.match(requestAll, /\$action != "edited" or \.t < \$event_at/);
    assert.match(requestAll, /본문 편집 이벤트 시각이 없다/);
});

test("author body edits are durable review evidence and fail closed when truncated", () => {
    // updated_at 은 댓글·라벨까지 섞이고 lastEditedAt 은 마지막 편집자만 남긴다. 영속
    // userContentEdits 에서 최신 변경요청 뒤 PR 작성자의 편집만 세어야 한다.
    assert.match(guard, /userContentEdits\(last:50\)/);
    assert.match(guard, /editor\{login\}/);
    assert.match(guard, /\.editor\.login/);
    assert.match(guard, /\.editedAt > \$cutoff/);
    assert.match(guard, /ascii_downcase\) == \(\$author \| ascii_downcase/);
    assert.match(guard, /pageInfo\.hasPreviousPage != false/);
    assert.match(guard, /\[ "\$body_edits" -gt 0 \]/);
});

test("the rerequest job and the guard judge by the same latest decision", () => {
    // 한쪽만 «PR 전체의 최신 판정» 을 보면 자동 재요청이 빚으로 이어지지 않거나,
    // 이미 승인된 PR 에 요청을 되살린다.
    for (const wf of [guard, requestAll]) {
        assert.match(wf, /sort_by\(\.t\) \| last/);
        assert.match(wf, /CHANGES_REQUESTED/);
    }
    // 봇·fork 는 토큰이 read-only 라 요청을 걸 수 없다.
    assert.match(requestAll, /\[ "\$HEAD_REPO" != "\$REPO" \]/);
});

function spaceSeparatedEnv(workflow, name) {
    const match = new RegExp(`^\\s*${name}: ([^\\n#]+)$`, "m").exec(workflow);
    assert.ok(match, `${name} 환경변수를 찾지 못했다`);
    return match[1].trim().split(/\s+/).filter(Boolean).map((login) => login.toLowerCase());
}

test("the gate exemption is judged before any debt is counted", () => {
    // 면제를 빚 계산 뒤에 두면, 면제받은 사람의 PR 때문에 조회가 실패했을 때 가드가 그를
    // 대신해 죽는다. 봇·fork·권한 판정과 같은 자리, 열린 PR 을 훑기 전에 둔다 (#1910).
    const exemptAt = guard.indexOf("리뷰 게이트 면제 작성자");
    const sweepAt = guard.indexOf("if ! open_prs=$(gh api");
    assert.ok(exemptAt > 0, "면제 경로가 없다");
    assert.ok(sweepAt > exemptAt, "면제가 빚 계산 뒤에 있다");
    assert.match(guard, /리뷰 게이트 면제 작성자\(\$AUTHOR\)[^\n]*exit 0/);
});

test("exempt logins are matched case-insensitively", () => {
    // GitHub 로그인은 대소문자를 가리지 않는다. 그대로 비교하면 웹훅이 Koongmai 를 내리는
    // 순간 면제가 조용히 풀리고, 그 사람의 PR 이 닫힌다.
    assert.match(guard, /author_lc=\$\(printf '%s' "\$AUTHOR" \| tr '\[:upper:\]' '\[:lower:\]'\)/);
    assert.match(guard, /exempt_lc=\$\(printf '%s' "\$REVIEW_GATE_EXEMPT_AUTHORS" \| tr '\[:upper:\]' '\[:lower:\]'\)/);
});

test("a change request from an exempt reviewer is not charged to the rest of the team", () => {
    // 면제된 사람은 리뷰할 의무가 없다. 그가 자발적으로 낸 변경요청 뒤 침묵하면 그 침묵의
    // 대가를 나머지 팀원이 "새 PR 을 못 연다" 로 치른다. 그래도 그 PR 이 그냥 머지되지는
    // 않는다. 승인 1건은 required-approval 룰셋이 계속 요구한다.
    assert.match(guard, /case " \$exempt_lc " in\n\s*\*" \$blocked_by "\*\)/);
    assert.match(guard, /면제 리뷰어 @\$blocked_by/);
});

test("nobody is exempt from the gate while still on the automatic review roster", () => {
    // 리뷰 의무가 없는 사람에게 요청만 계속 걸면, 그 요청은 아무도 응답하지 않는 알림으로
    // 쌓이고 두 워크플로가 서로 다른 팀 명단을 갖게 된다.
    const exempt = spaceSeparatedEnv(guard, "REVIEW_GATE_EXEMPT_AUTHORS");
    const team = spaceSeparatedEnv(requestAll, "TEAM");
    for (const login of exempt) {
        assert.ok(!team.includes(login), `${login} 이 면제이면서 자동 요청 대상이다`);
    }
    assert.ok(team.length > 0, "자동 요청 대상이 비었다");
});
