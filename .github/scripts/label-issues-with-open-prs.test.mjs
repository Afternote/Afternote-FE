import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
    DEFAULT_LABEL,
    applyPlan,
    commentMarker,
    ensureLabelExists,
    fetchLabeledIssueNumbers,
    planIssueLabelChanges,
    renderLinkComment,
    renderSummary,
    resolveIssueNumbers,
} from "./label-issues-with-open-prs.mjs";

const workflow = await readFile(new URL("../workflows/conflict-label.yml", import.meta.url), "utf8");

/** 테스트 출력이 CI 로그에서 실제 조작처럼 보이지 않도록 삼킨다. */
const silent = { log() {} };

function fakeApi({ responses = {}, failOn = null } = {}) {
    const calls = [];
    const api = async (apiPath, options = {}) => {
        calls.push({ apiPath, method: options.method ?? "GET", body: options.body });
        if (failOn && failOn(apiPath, options)) {
            throw new Error("boom");
        }
        for (const [pattern, value] of Object.entries(responses)) {
            if (apiPath.startsWith(pattern)) {
                return typeof value === "function" ? value(apiPath, options) : value;
            }
        }
        return options.allowNotFound ? null : {};
    };
    api.calls = calls;
    return api;
}

function pullRequest(overrides = {}) {
    return {
        number: 100,
        title: "fix(ci): 무언가를 고친다 (#42)",
        baseRefName: "develop",
        isDraft: false,
        ...overrides,
    };
}

function plan(overrides = {}) {
    return planIssueLabelChanges({
        pullRequests: [],
        labeledIssueNumbers: [],
        defaultBranch: "develop",
        ...overrides,
    });
}

test("열린 PR 의 대표 이슈에 라벨 계획이 잡힌다", () => {
    const result = plan({ pullRequests: [pullRequest()] });

    assert.deepEqual(result.toLabel.map((entry) => entry.issueNumber), [42]);
    assert.deepEqual(result.toUnlabel, []);
});

test("이미 라벨이 붙어 있으면 다시 붙이지 않는다", () => {
    // 리컨사일러는 develop push 마다 돈다. 이미 맞는 상태를 다시 쓰면 호출만 늘고 이슈
    // 타임라인에 같은 라벨 이벤트가 쌓인다 (#1465).
    const result = plan({ pullRequests: [pullRequest()], labeledIssueNumbers: [42] });

    assert.deepEqual(result.toLabel, []);
    assert.deepEqual(result.toUnlabel, []);
});

test("열린 PR 이 사라진 이슈에서 라벨을 뗀다", () => {
    const result = plan({ pullRequests: [], labeledIssueNumbers: [42, 43] });

    assert.deepEqual(result.toUnlabel, [42, 43]);
});

test("사람이 손으로 붙인 라벨도 다음 실행에서 정본으로 되돌아간다", () => {
    // 라벨의 뜻이 «열린 PR 이 있다» 하나로 유지되려면 리컨사일러가 유일한 저자여야 한다.
    // 손으로 붙인 것을 존중하면 라벨이 «누군가 그렇게 생각했다» 로 흐려진다.
    const result = plan({ pullRequests: [pullRequest()], labeledIssueNumbers: [42, 999] });

    assert.deepEqual(result.toUnlabel, [999]);
    assert.deepEqual(result.toLabel, []);
});

test("제목이 대표 이슈 형식이 아니면 건너뛴다", () => {
    // 본문의 느슨한 `Refs #N` 까지 주우면 «참고로 언급했을 뿐인 이슈» 에도 라벨이 붙어
    // 라벨의 뜻이 무너진다. 판정은 Repository Quality 가 강제하는 제목 형식 하나로만 한다.
    const result = plan({
        pullRequests: [pullRequest({ title: "fix(ci): 이슈 번호가 없다" })],
    });

    assert.deepEqual(result.toLabel, []);
    assert.deepEqual(result.skipped.map((item) => item.number), [100]);
});

test("제목 끝이 아닌 (#N) 은 대표 이슈로 보지 않는다", () => {
    const result = plan({
        pullRequests: [pullRequest({ title: "fix(ci): (#42) 를 언급만 한다" })],
    });

    assert.deepEqual(result.toLabel, []);
});

test("한 이슈를 여러 PR 이 가리켜도 라벨은 한 번만 계획된다", () => {
    const result = plan({
        pullRequests: [pullRequest({ number: 100 }), pullRequest({ number: 101 })],
    });

    assert.equal(result.toLabel.length, 1);
    assert.deepEqual(result.toLabel[0].pullRequests.map((item) => item.number), [100, 101]);
});

test("draft PR 도 라벨 대상이다", () => {
    // draft 는 «리뷰를 받을 상태가 아니다» 이지 «아무도 안 하고 있다» 가 아니다. 이 라벨이
    // 답하는 질문은 «이미 누가 손대고 있는가» 이므로 draft 를 빼면 오인이 그대로 남는다.
    const result = plan({ pullRequests: [pullRequest({ isDraft: true })] });

    assert.deepEqual(result.toLabel.map((entry) => entry.issueNumber), [42]);
});

test("base 가 기본 브랜치면 링크 코멘트를 달지 않는다", () => {
    // 이 경우 GitHub 이 Development 칸에 PR 을 이미 띄운다 — 같은 말을 코멘트로 한 번 더 하면 소음이다.
    const result = plan({ pullRequests: [pullRequest({ baseRefName: "develop" })] });

    assert.deepEqual(result.comments, []);
});

test("base 가 기본 브랜치가 아니면 링크 코멘트를 계획한다", () => {
    const result = plan({
        pullRequests: [pullRequest({ baseRefName: "feat/1497-detail-name-cache" })],
    });

    assert.deepEqual(
        result.comments.map((entry) => [entry.issueNumber, entry.pullRequest.number]),
        [[42, 100]],
    );
});

test("링크 코멘트는 PR 상태를 문장으로 박지 않는다", () => {
    // GitHub 이 `#100` 을 open·merged·closed 배지와 함께 렌더한다. 상태를 글로 적으면 PR 이
    // 닫힌 뒤 코멘트만 거짓말을 하게 되고, 그걸 고치려면 실행마다 코멘트를 다시 쓰게 된다.
    const body = renderLinkComment({
        pullRequest: pullRequest({ number: 1511, baseRefName: "feat/1497-detail-name-cache" }),
        defaultBranch: "develop",
    });

    assert.match(body, /^<!-- issue-pr-open:1511 -->/);
    assert.match(body, /\*\*#1511\*\*/);
    assert.match(body, /feat\/1497-detail-name-cache/);
    assert.doesNotMatch(body, /열려 있다|머지됐다|닫혔다/);
});

test("라벨을 붙이기 전에 대상이 이슈인지 확인한다", async () => {
    // 제목의 `(#N)` 이 PR 번호일 수 있다. 게이트가 그런 PR 을 막지만 리컨사일러는 게이트를
    // 통과하지 못한 PR 까지 훑으므로 여기서 한 번 더 가린다.
    const api = fakeApi({
        responses: {
            "/graphql": { data: { repository: { n42: { __typename: "Issue" }, n77: { __typename: "PullRequest" } } } },
        },
    });

    const issues = await resolveIssueNumbers(api, "o/r", [42, 77]);

    assert.deepEqual([...issues], [42]);
    assert.equal(api.calls.length, 1, "번호마다 따로 묻지 않고 한 번에 판정한다");
});

test("이슈가 아닌 대상에는 라벨도 코멘트도 쓰지 않는다", async () => {
    const api = fakeApi();
    const target = plan({
        pullRequests: [pullRequest({ title: "fix(ci): PR 을 가리킨다 (#77)", baseRefName: "feat/x" })],
    });

    const failures = await applyPlan(api, "o/r", target, {
        defaultBranch: "develop",
        realIssues: new Set(),
        logger: silent,
    });

    assert.deepEqual(failures, []);
    assert.deepEqual(api.calls.filter((call) => call.method !== "GET"), []);
});

test("라벨 보유 이슈 조회는 PR 을 제외한다", async () => {
    // 이 엔드포인트는 라벨이 붙은 PR 도 함께 돌려준다. 걸러 내지 않으면 그 PR 번호가
    // «열린 PR 이 없는 이슈» 로 읽혀 매 실행마다 있지도 않은 라벨을 지우려 든다.
    const api = fakeApi({
        responses: {
            "/repos/o/r/issues?labels=": [
                { number: 42 },
                { number: 100, pull_request: { url: "…" } },
            ],
        },
    });

    const numbers = await fetchLabeledIssueNumbers(api, "o/r", DEFAULT_LABEL);

    assert.deepEqual(numbers, [42]);
});

test("라벨 보유 이슈 조회는 닫힌 이슈까지 본다", async () => {
    // PR 이 살아 있는 채로 이슈가 먼저 닫히면 라벨이 남는다. state=open 만 보면 그 이슈가
    // 목록에 없어 «뗄 것이 없다» 로 읽혀 라벨이 영구히 붙어 있게 된다.
    const api = fakeApi({ responses: { "/repos/o/r/issues?labels=": [] } });

    await fetchLabeledIssueNumbers(api, "o/r", DEFAULT_LABEL);

    assert.match(api.calls[0].apiPath, /state=all/);
});

test("dry run 은 아무것도 쓰지 않는다", async () => {
    const api = fakeApi();
    const target = plan({
        pullRequests: [pullRequest({ baseRefName: "feat/x" })],
        labeledIssueNumbers: [999],
    });

    await applyPlan(api, "o/r", target, {
        defaultBranch: "develop",
        dryRun: true,
        realIssues: new Set([42]),
        logger: silent,
    });

    assert.deepEqual(api.calls, []);
});

test("같은 PR 의 링크 코멘트가 이미 있으면 다시 달지 않는다", async () => {
    const api = fakeApi({
        responses: {
            "/repos/o/r/issues/42/comments": [{ body: `${commentMarker(100)}\n이전 실행` }],
        },
    });
    const target = plan({ pullRequests: [pullRequest({ baseRefName: "feat/x" })] });

    await applyPlan(api, "o/r", target, {
        defaultBranch: "develop",
        realIssues: new Set([42]),
        logger: silent,
    });

    assert.deepEqual(api.calls.filter((call) => call.method === "POST" && call.apiPath.endsWith("/comments")), []);
});

test("한 이슈의 실패가 나머지 처리를 막지 않는다", async () => {
    const api = fakeApi();
    const failing = fakeApi({ failOn: (apiPath) => apiPath.endsWith("/issues/42/labels") });
    const target = plan({
        pullRequests: [pullRequest({ number: 100 }), pullRequest({ number: 101, title: "fix: 다른 것 (#43)" })],
    });

    const failures = await applyPlan(failing, "o/r", target, {
        defaultBranch: "develop",
        realIssues: new Set([42, 43]),
        logger: silent,
    });

    assert.equal(failures.length, 1);
    assert.match(failures[0], /#42/);
    assert.ok(failing.calls.some((call) => call.apiPath.endsWith("/issues/43/labels")));
    assert.equal(api.calls.length, 0);
});

test("라벨이 없으면 만든다", async () => {
    const api = fakeApi({ responses: { "/repos/o/r/labels/": null } });

    await ensureLabelExists(api, "o/r", DEFAULT_LABEL);

    const created = api.calls.find((call) => call.method === "POST");
    assert.equal(created.apiPath, "/repos/o/r/labels");
    assert.equal(created.body.name, DEFAULT_LABEL);
    assert.ok(created.body.description.length > 0);
});

test("요약은 부착·제거·코멘트·미판정을 모두 센다", () => {
    const target = plan({
        pullRequests: [pullRequest({ baseRefName: "feat/x" }), pullRequest({ number: 101, title: "번호 없음" })],
        labeledIssueNumbers: [999],
    });

    const summary = renderSummary({ plan: target, dryRun: false });

    assert.match(summary, /라벨 부착: 1건 — #42/);
    assert.match(summary, /라벨 제거: 1건 — #999/);
    assert.match(summary, /링크 코멘트 대상\(base≠기본 브랜치\): 1건 — #42/);
    assert.match(summary, /대표 이슈 미판정: 1건 — #101/);
});

test("리컨사일 워크플로가 이 스크립트와 테스트를 실제로 부른다", () => {
    // 스크립트만 있고 아무도 부르지 않으면 달라지는 것이 없다.
    assert.match(workflow, /node --test(?:[\s\S]*?)label-issues-with-open-prs\.test\.mjs/);
    assert.match(workflow, /node \.github\/scripts\/label-issues-with-open-prs\.mjs \|\| failures=1/);
});

test("리컨사일 job 은 이슈 쓰기 권한과 기본 브랜치 이름을 넘긴다", () => {
    assert.match(workflow, /^\s{6}issues: write$/m);
    assert.match(workflow, /GITHUB_DEFAULT_BRANCH: \$\{\{ github\.event\.repository\.default_branch \}\}/);
});
