import assert from "node:assert/strict";
import test from "node:test";

import {
    evaluateDeploymentDecision,
    extractQaPoints,
} from "./evaluate-deployment-decision.mjs";

function context(overrides = {}) {
    const issue = { number: 726, title: "배포 판단", body: "", labels: [] };
    return {
        targetCoveredBySuccessfulDistribution: false,
        baselineDistribution: { headSha: "base" },
        targetPullRequest: {
            number: 800,
            title: "refactor(core): 내부 정리",
            body: "",
            mergeCommitSha: "head",
            closingIssues: [issue],
        },
        pendingPullRequests: [
            {
                number: 800,
                title: "refactor(core): 내부 정리",
                body: "",
                closingIssues: [issue],
            },
        ],
        ...overrides,
    };
}

test("holds when the target is already distributed", () => {
    const result = evaluateDeploymentDecision(
        context({ targetCoveredBySuccessfulDistribution: true }),
        ["feature/home/HomeScreen.kt"],
    );
    assert.equal(result.decision, "hold");
    assert.match(result.reason, /이미 성공한/);
});

test("deploys a high-risk release path change immediately", () => {
    const result = evaluateDeploymentDecision(context(), [".github/workflows/release-distribution.yml"]);
    assert.equal(result.decision, "deploy");
    assert.equal(result.risk, "high");
});

test("deploys a user-visible bug fix", () => {
    const pendingPullRequest = {
        number: 801,
        title: "fix(home): 새로고침 실패 복구",
        body: "",
        closingIssues: [{ number: 704, title: "새로고침 실패", body: "", labels: ["bug"] }],
    };
    const result = evaluateDeploymentDecision(
        context({ pendingPullRequests: [pendingPullRequest] }),
        ["feature/home/HomeScreen.kt"],
    );
    assert.equal(result.decision, "deploy");
    assert.match(result.reason, /결함 수정/);
});

test("holds documentation-only changes", () => {
    const result = evaluateDeploymentDecision(context(), ["README.md", "docs/release.md"]);
    assert.equal(result.decision, "hold");
    assert.match(result.reason, /런타임/);
});

test("deploys when three issues accumulate", () => {
    const pendingPullRequests = [1, 2, 3].map((number) => ({
        number: 810 + number,
        title: `refactor(scope${number}): 정리`,
        body: "",
        closingIssues: [
            {
                number: number === 1 ? 726 : 720 + number,
                title: "정리",
                body: "",
                labels: [],
            },
        ],
    }));
    const result = evaluateDeploymentDecision(
        context({ pendingPullRequests }),
        ["feature/record/RecordState.kt"],
    );
    assert.equal(result.decision, "deploy");
    assert.match(result.reason, /독립 이슈 3건/);
});

test("reads explicit QA points from a PR body", () => {
    assert.deepEqual(
        extractQaPoints("## QA 포인트\n- 저장 후 목록에 표시되는지 확인\n- 재진입 시 값이 유지되는지 확인\n\n## 기타\n- 제외"),
        ["저장 후 목록에 표시되는지 확인", "재진입 시 값이 유지되는지 확인"],
    );
});
