import assert from "node:assert/strict";
import test from "node:test";

import {
    applyReleaseScopeToBody,
    extractQaPoints,
    renderIssueSection,
    summarizeReleaseScope,
} from "./render-release-scope.mjs";

function context(overrides = {}) {
    return {
        alreadyDistributed: false,
        baselineDistribution: { id: 1, headSha: "base", url: "https://example.test/1" },
        releasePullRequest: { number: 1025, headSha: "head", body: "" },
        pendingPullRequests: [
            {
                number: 1002,
                title: "fix(home): 주간 요약 그리드",
                body: "## QA 포인트\n- 홈에서 주간 요약이 실제 기록 수를 보인다",
                closingIssues: [{ number: 562, title: "주간 요약", body: "", labels: [] }],
            },
            {
                number: 1001,
                title: "refactor(core): 인증 예외 정리",
                body: "",
                closingIssues: [{ number: 934, title: "예외 정리", body: "", labels: [] }],
            },
        ],
        ...overrides,
    };
}

test("collects closing issues across pending PRs in ascending order", () => {
    const scope = summarizeReleaseScope(context());
    assert.deepEqual(scope.includedIssues, [562, 934]);
    assert.equal(scope.pullRequestCount, 2);
});

test("carries QA points written in the constituent PR bodies", () => {
    const scope = summarizeReleaseScope(context());
    assert.deepEqual(scope.qaPointsDraft, ["홈에서 주간 요약이 실제 기록 수를 보인다"]);
});

test("stops QA point capture at the next heading", () => {
    assert.deepEqual(
        extractQaPoints("## QA 포인트\n- 첫 항목\n\n## 검증\n- 빌드 통과"),
        ["첫 항목"],
    );
});

test("renders issue numbers in the release note format", () => {
    assert.equal(renderIssueSection([562, 934]), "- #562\n- #934");
    assert.match(renderIssueSection([]), /직접 채워/);
});

test("replaces the included-issue section without touching other sections", () => {
    const body = [
        "## 변경 요약",
        "",
        "develop 누적 반영.",
        "",
        "## 포함 이슈",
        "",
        "- #111",
        "",
        "## 검증",
        "",
        "| 태스크 | 결과 |",
    ].join("\n");

    const updated = applyReleaseScopeToBody(body, {
        issueSection: "- #562\n- #934",
        qaSection: "- 초안",
    });

    assert.match(updated, /## 포함 이슈\n\n- #562\n- #934\n/);
    assert.doesNotMatch(updated, /#111/);
    assert.match(updated, /## 변경 요약\n\ndevelop 누적 반영\./);
    assert.match(updated, /## 검증\n\n\| 태스크 \| 결과 \|/);
});

test("keeps QA points a human already wrote", () => {
    const body = ["## 포함 이슈", "", "- #1", "", "## QA 포인트", "", "- 사람이 쓴 문장"].join("\n");

    const updated = applyReleaseScopeToBody(body, {
        issueSection: "- #562",
        qaSection: "- 자동 초안",
    });

    assert.match(updated, /- 사람이 쓴 문장/);
    assert.doesNotMatch(updated, /자동 초안/);
});

test("fills an empty QA section so the distribution gate cannot fail on it", () => {
    const body = ["## 포함 이슈", "", "- #1", "", "## QA 포인트", ""].join("\n");

    const updated = applyReleaseScopeToBody(body, {
        issueSection: "- #562",
        qaSection: "- 자동 초안",
    });

    assert.match(updated, /## QA 포인트\n\n- 자동 초안/);
});

test("appends both sections when the body has neither", () => {
    const updated = applyReleaseScopeToBody("## 변경 요약\n\n릴리스 후보.", {
        issueSection: "- #562",
        qaSection: "- 자동 초안",
    });

    assert.match(updated, /## 포함 이슈\n\n- #562/);
    assert.match(updated, /## QA 포인트\n\n- 자동 초안/);
    assert.match(updated, /## 변경 요약\n\n릴리스 후보\./);
});

test("leaves no heading inside the rendered sections", () => {
    const updated = applyReleaseScopeToBody("", {
        issueSection: "- #562\n- #934",
        qaSection: "- 자동 초안",
    });
    const lines = updated.split("\n");
    const issueStart = lines.indexOf("## 포함 이슈");
    const qaStart = lines.indexOf("## QA 포인트");

    assert.ok(issueStart >= 0 && qaStart > issueStart);
    assert.deepEqual(
        lines.slice(issueStart + 1, qaStart).filter((line) => line.startsWith("#")),
        [],
    );
});

test("keeps an issue list already in the body when nothing new was collected", () => {
    const body = ["## 포함 이슈", "", "- #506", "- #618", "", "## QA 포인트", "", "- 사람이 쓴 문장"].join("\n");

    const updated = applyReleaseScopeToBody(body, {
        issueSection: renderIssueSection([]),
        qaSection: "- 자동 초안",
        overwriteIssues: false,
    });

    assert.match(updated, /- #506\n- #618/);
    assert.doesNotMatch(updated, /연결된 이슈 없음/);
});

test("still fills an empty issue section when nothing was collected", () => {
    const body = ["## 포함 이슈", "", "## QA 포인트", "", "- 사람이 쓴 문장"].join("\n");

    const updated = applyReleaseScopeToBody(body, {
        issueSection: renderIssueSection([]),
        qaSection: "- 자동 초안",
        overwriteIssues: false,
    });

    assert.match(updated, /## 포함 이슈\n\n- 연결된 이슈 없음/);
});
