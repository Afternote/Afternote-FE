import assert from "node:assert/strict";
import test from "node:test";

import {
    DEFAULT_LABEL,
    applyPlan,
    fetchCurrentDispatchTarget,
    planLabelChanges,
    resolveAndroidTestDecision,
} from "./label-android-test-prs.mjs";

function body(mode) {
    return `## CI Test Plan\n\n\`\`\`json\n${JSON.stringify({
        androidTest: {
            mode,
            reason: `${mode} 모드가 필요한 변경 경계 설명`,
            ...(mode === "selected"
                ? {
                      tests: [
                          {
                              path: "app/src/androidTest/java/com/example/RuntimeTest.kt",
                              selector: "com.example.RuntimeTest#works",
                              device: "api30",
                          },
                      ],
                  }
                : {}),
        },
    })}\n\`\`\``;
}

test("CI Test Plan selected/full만 계측 실행 대상으로 분류한다", () => {
    for (const mode of ["selected", "full"]) {
        const decision = resolveAndroidTestDecision({ number: 1280, body: body(mode) });
        assert.equal(decision.valid, true);
        assert.equal(decision.required, true);
        assert.match(decision.digest, /^[0-9a-f]{64}$/);
    }
    assert.equal(
        resolveAndroidTestDecision({ number: 1280, body: body("none") }).required,
        false,
    );
});

test("plan이 없는 기존 PR도 fail-closed로 분류한다", () => {
    const decision = resolveAndroidTestDecision({
        number: 1200,
        created_at: "2026-08-01T00:00:00Z",
        body: "legacy",
    });
    assert.equal(decision.valid, false);
    assert.equal(decision.required, true);
});

test("라벨 계획은 selected/full same-repository PR만 포함한다", () => {
    const plan = planLabelChanges({
        repository: "o/r",
        pullRequests: [
            { number: 1, headRepository: "o/r", labels: [], androidTestRequired: false },
            {
                number: 2,
                headRepository: "o/r",
                labels: [],
                androidTestRequired: true,
                androidTestMode: "selected",
            },
            {
                number: 3,
                headRepository: "fork/r",
                labels: [],
                androidTestRequired: true,
                androidTestMode: "full",
            },
        ],
    });
    assert.deepEqual(plan.notRequired.map(({ number }) => number), [1]);
    assert.deepEqual(plan.toLabel.map(({ number }) => number), [2]);
    assert.deepEqual(plan.skippedForks.map(({ number }) => number), [3]);
});

test("dispatch 직전에 현재 head와 plan digest를 다시 계산한다", async () => {
    const pullRequest = {
        number: 1280,
        created_at: "2026-09-01T00:00:00Z",
        state: "open",
        body: body("full"),
        head: { ref: "feature/x", sha: "a".repeat(40), repo: { full_name: "o/r" } },
        labels: [{ name: DEFAULT_LABEL }],
    };
    const target = await fetchCurrentDispatchTarget(async () => pullRequest, "o/r", 1280, DEFAULT_LABEL);
    assert.equal(target.headBranch, "feature/x");
    assert.equal(target.headSha, "a".repeat(40));
    assert.match(target.planDigest, /^[0-9a-f]{64}$/);
});

test("재시도 dispatch는 exact head와 plan digest를 전달한다", async () => {
    const calls = [];
    const pullRequest = {
        number: 1280,
        created_at: "2026-09-01T00:00:00Z",
        state: "open",
        body: body("full"),
        head: { ref: "feature/x", sha: "b".repeat(40), repo: { full_name: "o/r" } },
        labels: [{ name: DEFAULT_LABEL }],
    };
    const api = async (apiPath, options = {}) => {
        calls.push({ apiPath, options });
        if (apiPath === "/repos/o/r/pulls/1280") return pullRequest;
        return null;
    };
    const failures = await applyPlan(
        api,
        "o/r",
        {
            toLabel: [],
            toRetry: [{ number: 1280, labels: [DEFAULT_LABEL, "android-test-dispatch-pending"] }],
        },
        { label: DEFAULT_LABEL, dryRun: false, logger: { log() {} } },
    );
    assert.deepEqual(failures, []);
    const dispatch = calls.find(({ apiPath }) => apiPath.endsWith("/dispatches"));
    assert.equal(dispatch.options.body.ref, "feature/x");
    assert.equal(dispatch.options.body.inputs.expected_head_sha, "b".repeat(40));
    assert.match(dispatch.options.body.inputs.expected_plan_digest, /^[0-9a-f]{64}$/);
});
