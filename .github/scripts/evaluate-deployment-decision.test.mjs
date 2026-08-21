import assert from "node:assert/strict";
import test from "node:test";

import {
    evaluateDeploymentDecision,
} from "./evaluate-deployment-decision.mjs";

function qaBody(label = "목록 저장") {
    return `## QA 메타데이터
\`\`\`json
{
  "scope": "app-runtime",
  "precondition": "로그인하고 테스트 데이터가 준비된 상태",
  "action": "${label} 동작을 수행한다",
  "expected": "변경한 데이터가 화면에 즉시 반영된다",
  "risk": "저장 결과를 확인할 수 없다",
  "evidence": [
    { "kind": "issue", "ref": "#726", "assertion": "재현 조건과 기대 결과를 정의한다" }
  ]
}
\`\`\``;
}

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

test("holds an unrelated CI workflow-only fix", () => {
    const pendingPullRequest = {
        number: 802,
        title: "fix(ci): Review Assign 자동 assignee 제거",
        body: "앱과 서버 동작 변경 없음",
        closingIssues: [{ number: 801, title: "자동 assignee 제거", body: "", labels: ["bug"] }],
    };
    const result = evaluateDeploymentDecision(
        context({ pendingPullRequests: [pendingPullRequest] }),
        [".github/workflows/PRassign.yml"],
    );

    assert.equal(result.decision, "hold");
    assert.equal(result.risk, "low");
    assert.match(result.reason, /일반 CI/);
});

test("holds a data-layer test-only change", () => {
    const result = evaluateDeploymentDecision(context(), [
        "feature/afternote/data/src/test/java/ReceiverMapperTest.kt",
    ]);

    assert.equal(result.decision, "hold");
    assert.equal(result.risk, "low");
    assert.match(result.reason, /런타임/);
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
    assert.deepEqual(result.qaPoints, []);
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

test("never creates a generic QA fallback", () => {
    const result = evaluateDeploymentDecision(context(), ["feature/home/HomeScreen.kt"]);

    assert.deepEqual(result.qaPoints, []);
    assert.doesNotMatch(JSON.stringify(result), /관련 동작을 재현/);
});

test("uses validated structured QA sources for the accumulation boundary", () => {
    const pendingPullRequests = Array.from({ length: 6 }, (_, index) => ({
        number: 820 + index,
        title: "refactor(home): 상태 정리",
        body: qaBody(`목록 항목 ${index + 1} 저장`),
        closingIssues: [{ number: 726, title: "배포 판단", body: "", labels: [] }],
    }));
    const result = evaluateDeploymentDecision(
        context({ pendingPullRequests }),
        ["feature/home/HomeState.kt"],
    );

    assert.equal(result.decision, "deploy");
    assert.match(result.reason, /구조화된 QA 원천이 6개/);
});
