import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
    COMMENT_MARKER,
    DEFAULT_LABEL,
    applyPlan,
    ensureLabelExists,
    planLabelChanges,
    renderConflictComment,
    renderSummary,
    resolveMergeStates,
} from "./label-conflicting-prs.mjs";

const conflictLabelWorkflow = await readFile(new URL("../workflows/conflict-label.yml", import.meta.url), "utf8");

/**
 * 호출을 기록하는 가짜 API. `responses` 로 특정 경로의 응답을 지정한다.
 */
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
        number: 1,
        mergeable: "MERGEABLE",
        baseRefName: "develop",
        headRefName: "feature/x",
        isDraft: false,
        labels: [],
        ...overrides,
    };
}

test("PR 검증 요청 직후 default branch 에서 충돌 라벨을 다시 판정한다", () => {
    assert.match(conflictLabelWorkflow, /^  workflow_run:\n    workflows: \["PR Validation"\]\n    types: \[requested\]$/m);
    assert.doesNotMatch(conflictLabelWorkflow, /^\s*pull_request_target\s*:/m);
});

test("라벨 조정 job은 PR 라벨 쓰기 권한을 명시한다", () => {
    assert.match(
        conflictLabelWorkflow,
        /^    permissions:\n      actions: write\n      contents: read\n      issues: write\n      pull-requests: write$/m,
    );
});

test("충돌 PR 에 라벨을 붙이고 해소된 PR 에서 뗀다", () => {
    const plan = planLabelChanges({
        pullRequests: [
            pullRequest({ number: 10, mergeable: "CONFLICTING" }),
            pullRequest({ number: 11, mergeable: "MERGEABLE", labels: [DEFAULT_LABEL] }),
        ],
        label: DEFAULT_LABEL,
    });

    assert.deepEqual(plan.toLabel.map((item) => item.number), [10]);
    assert.deepEqual(plan.toUnlabel.map((item) => item.number), [11]);
    assert.deepEqual(plan.skipped, []);
});

test("이미 라벨이 붙은 충돌 PR 은 다시 건드리지 않는다", () => {
    // 실행마다 코멘트가 쌓이면 알림이 소음이 되고, 저자가 라벨 자체를 무시하게 된다.
    const plan = planLabelChanges({
        pullRequests: [pullRequest({ number: 12, mergeable: "CONFLICTING", labels: [DEFAULT_LABEL, "bug"] })],
        label: DEFAULT_LABEL,
    });

    assert.deepEqual(plan.toLabel, []);
    assert.deepEqual(plan.toUnlabel, []);
});

test("라벨이 없는 정상 PR 은 아무 대상도 아니다", () => {
    const plan = planLabelChanges({
        pullRequests: [pullRequest({ number: 13, mergeable: "MERGEABLE" })],
        label: DEFAULT_LABEL,
    });

    assert.deepEqual(plan.toLabel, []);
    assert.deepEqual(plan.toUnlabel, []);
    assert.deepEqual(plan.skipped, []);
});

test("UNKNOWN 은 충돌로 단정하지 않고 보류한다", () => {
    // GitHub 이 mergeable 을 계산 중인 상태다. 멀쩡한 PR 에 라벨이 붙는 오탐은
    // 라벨이 없는 것보다 나쁘다.
    const plan = planLabelChanges({
        pullRequests: [pullRequest({ number: 14, mergeable: "UNKNOWN" })],
        label: DEFAULT_LABEL,
    });

    assert.deepEqual(plan.toLabel, []);
    assert.deepEqual(plan.skipped.map((item) => item.number), [14]);
});

test("UNKNOWN 이면서 라벨이 붙어 있어도 성급히 떼지 않는다", () => {
    const plan = planLabelChanges({
        pullRequests: [pullRequest({ number: 15, mergeable: "UNKNOWN", labels: [DEFAULT_LABEL] })],
        label: DEFAULT_LABEL,
    });

    assert.deepEqual(plan.toUnlabel, []);
    assert.deepEqual(plan.skipped.map((item) => item.number), [15]);
});

test("스택 PR 도 base 가 develop 이 아닐 뿐 같은 규칙으로 본다", () => {
    const plan = planLabelChanges({
        pullRequests: [pullRequest({ number: 16, mergeable: "CONFLICTING", baseRefName: "feat/601" })],
        label: DEFAULT_LABEL,
    });

    assert.deepEqual(plan.toLabel.map((item) => item.baseRefName), ["feat/601"]);
});

test("사용자 지정 라벨 이름을 따른다", () => {
    const plan = planLabelChanges({
        pullRequests: [
            pullRequest({ number: 17, mergeable: "CONFLICTING", labels: ["conflict"] }),
            pullRequest({ number: 18, mergeable: "MERGEABLE", labels: ["needs-rebase"] }),
        ],
        label: "needs-rebase",
    });

    assert.deepEqual(plan.toLabel.map((item) => item.number), [17]);
    assert.deepEqual(plan.toUnlabel.map((item) => item.number), [18]);
});

test("코멘트에 마커와 base 이름이 들어간다", () => {
    const body = renderConflictComment({ baseRefName: "develop" });

    assert.ok(body.startsWith(COMMENT_MARKER));
    assert.ok(body.includes("`develop` 를 병합해"));
    assert.ok(body.includes("#1028"));
});

test("UNKNOWN 이 남아 있으면 다시 조회한다", async () => {
    const pages = [
        [pullRequest({ number: 20, mergeable: "UNKNOWN" })],
        [pullRequest({ number: 20, mergeable: "CONFLICTING" })],
    ];
    let calls = 0;
    const waits = [];

    const resolved = await resolveMergeStates(null, "o/r", {
        attempts: 3,
        delayMs: 5,
        wait: async (ms) => {
            waits.push(ms);
        },
        fetchPage: async () => pages[Math.min(calls++, pages.length - 1)],
    });

    assert.equal(calls, 2);
    assert.deepEqual(waits, [5]);
    assert.equal(resolved[0].mergeable, "CONFLICTING");
});

test("UNKNOWN 이 없으면 한 번만 조회한다", async () => {
    let calls = 0;

    await resolveMergeStates(null, "o/r", {
        attempts: 3,
        delayMs: 5,
        wait: async () => {},
        fetchPage: async () => {
            calls += 1;
            return [pullRequest({ number: 21, mergeable: "MERGEABLE" })];
        },
    });

    assert.equal(calls, 1);
});

test("끝내 UNKNOWN 이면 시도 횟수만큼만 조회하고 보류로 남긴다", async () => {
    let calls = 0;

    const resolved = await resolveMergeStates(null, "o/r", {
        attempts: 3,
        delayMs: 1,
        wait: async () => {},
        fetchPage: async () => {
            calls += 1;
            return [pullRequest({ number: 22, mergeable: "UNKNOWN" })];
        },
    });

    assert.equal(calls, 3);
    assert.deepEqual(planLabelChanges({ pullRequests: resolved, label: DEFAULT_LABEL }).skipped.length, 1);
});

test("라벨을 붙이고 안내 코멘트를 한 번 남긴다", async () => {
    const api = fakeApi({ responses: { "/repos/o/r/issues/40/comments": [] } });

    const failures = await applyPlan(
        api,
        "o/r",
        { toLabel: [{ number: 40, baseRefName: "develop" }], toUnlabel: [] },
        { label: DEFAULT_LABEL, dryRun: false, logger: silent },
    );

    assert.deepEqual(failures, []);
    const writes = api.calls.filter((call) => call.method === "POST");
    assert.deepEqual(writes.map((call) => call.apiPath), [
        "/repos/o/r/issues/40/labels",
        "/repos/o/r/issues/40/comments",
    ]);
    assert.ok(writes[1].body.body.includes(COMMENT_MARKER));
});

test("마커 코멘트가 이미 있으면 다시 달지 않는다", async () => {
    // 사람이 라벨을 손으로 떼었다가 다시 붙는 경우에도 안내가 중복되지 않아야 한다.
    const api = fakeApi({
        responses: { "/repos/o/r/issues/41/comments": [{ body: `${COMMENT_MARKER}\n이전 안내` }] },
    });

    await applyPlan(
        api,
        "o/r",
        { toLabel: [{ number: 41, baseRefName: "develop" }], toUnlabel: [] },
        { label: DEFAULT_LABEL, dryRun: false, logger: silent },
    );

    const posts = api.calls.filter((call) => call.method === "POST").map((call) => call.apiPath);
    assert.deepEqual(posts, ["/repos/o/r/issues/41/labels"]);
});

test("해소된 PR 에서 라벨만 지운다", async () => {
    const api = fakeApi();

    await applyPlan(
        api,
        "o/r",
        { toLabel: [], toUnlabel: [{ number: 42 }] },
        { label: DEFAULT_LABEL, dryRun: false, logger: silent },
    );

    assert.deepEqual(api.calls, [
        { apiPath: "/repos/o/r/issues/42/labels/conflict", method: "DELETE", body: undefined },
    ]);
});

test("dry run 은 아무것도 쓰지 않는다", async () => {
    const api = fakeApi();

    await applyPlan(
        api,
        "o/r",
        { toLabel: [{ number: 43, baseRefName: "develop" }], toUnlabel: [{ number: 44 }] },
        { label: DEFAULT_LABEL, dryRun: true, logger: silent },
    );

    assert.deepEqual(api.calls, []);
});

test("한 PR 이 실패해도 나머지를 처리하고 실패를 모아 돌려준다", async () => {
    const api = fakeApi({
        responses: { "/repos/o/r/issues": [] },
        failOn: (apiPath) => apiPath === "/repos/o/r/issues/50/labels",
    });

    const failures = await applyPlan(
        api,
        "o/r",
        {
            toLabel: [
                { number: 50, baseRefName: "develop" },
                { number: 51, baseRefName: "develop" },
            ],
            toUnlabel: [],
        },
        { label: DEFAULT_LABEL, dryRun: false, logger: silent },
    );

    assert.equal(failures.length, 1);
    assert.match(failures[0], /#50 라벨 부착 실패/);
    assert.ok(api.calls.some((call) => call.apiPath === "/repos/o/r/issues/51/labels"));
});

test("라벨이 없으면 만들고, 있으면 만들지 않는다", async () => {
    const missing = fakeApi();
    await ensureLabelExists(missing, "o/r", DEFAULT_LABEL);
    const created = missing.calls.filter((call) => call.method === "POST");
    assert.equal(created.length, 1);
    assert.equal(created[0].body.name, DEFAULT_LABEL);

    const existing = fakeApi({ responses: { "/repos/o/r/labels/conflict": { name: "conflict" } } });
    await ensureLabelExists(existing, "o/r", DEFAULT_LABEL);
    assert.equal(existing.calls.filter((call) => call.method === "POST").length, 0);
});

test("진행 상황은 주입한 로거로 나간다", async () => {
    // 기본값이 console 이면 테스트 출력이 CI 로그에 «#60 라벨 부착» 으로 섞여
    // 실제 조작과 구분되지 않는다. 주입 지점이 사라지지 않게 고정한다.
    const lines = [];
    const api = fakeApi({ responses: { "/repos/o/r/issues/60/comments": [] } });

    await applyPlan(
        api,
        "o/r",
        { toLabel: [{ number: 60, baseRefName: "develop" }], toUnlabel: [] },
        { label: DEFAULT_LABEL, dryRun: false, logger: { log: (line) => lines.push(line) } },
    );

    assert.deepEqual(lines, ["#60 라벨 부착 (base develop)"]);
});

test("요약에 건수와 PR 번호가 남는다", () => {
    const plan = planLabelChanges({
        pullRequests: [
            pullRequest({ number: 30, mergeable: "CONFLICTING" }),
            pullRequest({ number: 31, mergeable: "MERGEABLE", labels: [DEFAULT_LABEL] }),
            pullRequest({ number: 32, mergeable: "UNKNOWN" }),
        ],
        label: DEFAULT_LABEL,
    });

    const summary = renderSummary({ plan, label: DEFAULT_LABEL, dryRun: true });

    assert.ok(summary.includes("dry run"));
    assert.ok(summary.includes("라벨 부착: 1건 — #30"));
    assert.ok(summary.includes("라벨 제거: 1건 — #31"));
    assert.ok(summary.includes("판정 보류(UNKNOWN): 1건 — #32"));
});
