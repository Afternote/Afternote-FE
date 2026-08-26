import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const guard = await readFile(new URL("../workflows/review-debt-guard.yml", import.meta.url), "utf8");

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
    assert.match(guard, /\(\.parents \| length\) < 2/);
    assert.match(guard, /select\(\.commit\.committer\.date > \\"\$blocked_at\\"\)/);
});
