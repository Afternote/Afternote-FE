import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
    collectViewedAutoMergeActions,
    decideViewedAutoMerge,
    decideViewedAutoMergeCancel,
    isAuthoredByViewer,
} from "./find-viewed-auto-merge-targets.mjs";

const viewed = (path) => ({ path, viewerViewedState: "VIEWED" });

function pullRequest(overrides = {}) {
    return {
        number: 1,
        isDraft: false,
        headRefOid: "0123456789abcdef0123456789abcdef01234567",
        isInMergeQueue: false,
        autoMergeRequest: null,
        changedFiles: 2,
        ...overrides,
    };
}

test("모든 파일이 Viewed 이고 승인 상태면 예약한다", () => {
    assert.equal(decideViewedAutoMerge(pullRequest(), [viewed("a.kt"), viewed("b.kt")]).enable, true);
});

test("하나라도 Viewed 가 아니면 예약하지 않는다 — 체크 뒤 바뀐 파일(DISMISSED) 포함", () => {
    for (const state of ["UNVIEWED", "DISMISSED"]) {
        const files = [viewed("a.kt"), { path: "b.kt", viewerViewedState: state }];
        assert.equal(decideViewedAutoMerge(pullRequest(), files).enable, false, state);
    }
});

test("리뷰 상태와 무관하게 Viewed 만으로 예약한다", () => {
    for (const reviewDecision of [null, "APPROVED", "CHANGES_REQUESTED", "REVIEW_REQUIRED"]) {
        const decision = decideViewedAutoMerge(pullRequest({ reviewDecision }), [viewed("a.kt"), viewed("b.kt")]);
        assert.equal(decision.enable, true, String(reviewDecision));
    }
});

test("파일 목록을 다 못 읽었으면 다 봤다고 단정하지 않는다", () => {
    assert.equal(decideViewedAutoMerge(pullRequest({ changedFiles: 3 }), [viewed("a.kt"), viewed("b.kt")]).enable, false);
    assert.equal(decideViewedAutoMerge(pullRequest({ changedFiles: 0 }), []).enable, false);
});

test("Draft·이미 예약·큐에 있는 PR 은 건드리지 않는다", () => {
    const files = [viewed("a.kt"), viewed("b.kt")];
    assert.equal(decideViewedAutoMerge(pullRequest({ isDraft: true }), files).enable, false);
    assert.equal(decideViewedAutoMerge(pullRequest({ autoMergeRequest: { enabledAt: "2026-10-04T00:00:00Z" } }), files).enable, false);
    assert.equal(decideViewedAutoMerge(pullRequest({ isInMergeQueue: true }), files).enable, false);
});

const scheduled = { autoMergeRequest: { enabledAt: "2026-10-04T00:00:00Z" } };

test("예약·큐 등록이 없으면 Viewed 와 무관하게 해제하지 않는다", () => {
    const decision = decideViewedAutoMergeCancel(pullRequest(), [viewed("a.kt"), { path: "b.kt", viewerViewedState: "UNVIEWED" }]);
    assert.equal(decision.disableAutoMerge, false);
    assert.equal(decision.dequeue, false);
});

test("예약 뒤 새 push 로 Viewed 가 풀리면 예약을 끈다 — DISMISSED·UNVIEWED", () => {
    for (const viewerViewedState of ["DISMISSED", "UNVIEWED"]) {
        const files = [viewed("a.kt"), { path: "b.kt", viewerViewedState }];
        const decision = decideViewedAutoMergeCancel(pullRequest(scheduled), files);
        assert.equal(decision.disableAutoMerge, true, viewerViewedState);
        assert.equal(decision.dequeue, false, viewerViewedState);
    }
});

test("merge queue 에 있는데 Viewed 가 풀리면 큐에서 빼고, 예약도 있으면 함께 끈다", () => {
    const files = [viewed("a.kt"), { path: "b.kt", viewerViewedState: "DISMISSED" }];
    assert.deepEqual(
        pickActions(decideViewedAutoMergeCancel(pullRequest({ isInMergeQueue: true }), files)),
        { disableAutoMerge: false, dequeue: true },
    );
    assert.deepEqual(
        pickActions(decideViewedAutoMergeCancel(pullRequest({ ...scheduled, isInMergeQueue: true }), files)),
        { disableAutoMerge: true, dequeue: true },
    );
});

test("리뷰·Draft 상태와 무관하게 Viewed 로만 해제를 가른다", () => {
    const files = [viewed("a.kt"), { path: "b.kt", viewerViewedState: "UNVIEWED" }];
    for (const overrides of [{ reviewDecision: "APPROVED" }, { reviewDecision: "CHANGES_REQUESTED" }, { isDraft: true }]) {
        assert.equal(decideViewedAutoMergeCancel(pullRequest({ ...scheduled, ...overrides }), files).disableAutoMerge, true);
    }
});

test("파일 목록을 다 못 읽었으면 다 봤다고 단정하지 않고 해제한다", () => {
    assert.equal(decideViewedAutoMergeCancel(pullRequest({ ...scheduled, changedFiles: 3 }), [viewed("a.kt"), viewed("b.kt")]).disableAutoMerge, true);
    assert.equal(decideViewedAutoMergeCancel(pullRequest({ isInMergeQueue: true, changedFiles: 0 }), []).dequeue, true);
});

test("예약·큐에 있고 모든 파일이 Viewed 면 그대로 둔다", () => {
    const files = [viewed("a.kt"), viewed("b.kt")];
    for (const overrides of [scheduled, { isInMergeQueue: true }, { ...scheduled, isInMergeQueue: true }]) {
        assert.deepEqual(pickActions(decideViewedAutoMergeCancel(pullRequest(overrides), files)), { disableAutoMerge: false, dequeue: false });
    }
});

function pickActions({ disableAutoMerge, dequeue }) {
    return { disableAutoMerge, dequeue };
}

test("VIEWED_BY 본인이 작성한 PR 만 대상이다 — 대소문자 무시, 작성자 없음은 제외", () => {
    assert.equal(isAuthoredByViewer({ author: { login: "1hyok" } }, "1hyok"), true);
    assert.equal(isAuthoredByViewer({ author: { login: "1Hyok" } }, "1hyok"), true);
    for (const author of [{ login: "koongmai" }, { login: "Sadturtleman" }, { login: "dependabot" }, null, undefined]) {
        assert.equal(isAuthoredByViewer({ author }, "1hyok"), false, JSON.stringify(author));
    }
});

// 파일이 한 페이지에 다 들어 있어 추가 조회가 없다. 작성자가 아니면 파일을 읽지도 않는다.
function listedPullRequest(number, login, overrides = {}, states = ["VIEWED", "VIEWED"]) {
    const nodes = states.map((viewerViewedState, index) => ({ path: `f${index}.kt`, viewerViewedState }));
    return pullRequest({
        number,
        id: `PR_${number}`,
        author: login === null ? null : { login },
        changedFiles: nodes.length,
        files: { pageInfo: { hasNextPage: false, endCursor: null }, nodes },
        ...overrides,
    });
}

const noRequest = async () => {
    throw new Error("파일 추가 조회는 없어야 한다");
};

test("팀원·봇 PR 은 큐에 있고 Viewed 가 0 이어도 해제하지 않고, 승인·Viewed 가 다 차도 예약하지 않는다", async () => {
    const pullRequests = [
        listedPullRequest(2262, "dependabot", { isInMergeQueue: true }, ["UNVIEWED", "UNVIEWED"]),
        listedPullRequest(2269, "koongmai", { ...scheduled, isInMergeQueue: true }, ["UNVIEWED", "DISMISSED"]),
        listedPullRequest(2300, "Sadturtleman", { isInMergeQueue: true, changedFiles: 0 }, []),
        listedPullRequest(2301, null, scheduled, ["UNVIEWED"]),
        listedPullRequest(2302, "koongmai"),
    ];
    const actions = await collectViewedAutoMergeActions(noRequest, "Afternote", "Afternote-FE", "1hyok", pullRequests);
    assert.deepEqual(actions, { targets: [], cancels: [] });
});

test("본인 PR 은 지금처럼 Viewed 가 풀리면 해제하고, 승인·Viewed 가 다 차면 예약한다", async () => {
    const pullRequests = [
        listedPullRequest(10, "1hyok", { isInMergeQueue: true }, ["UNVIEWED", "UNVIEWED"]),
        listedPullRequest(11, "1Hyok", scheduled, ["VIEWED", "DISMISSED"]),
        listedPullRequest(12, "1hyok"),
        listedPullRequest(13, "dependabot", { isInMergeQueue: true }, ["UNVIEWED", "UNVIEWED"]),
    ];
    const actions = await collectViewedAutoMergeActions(noRequest, "Afternote", "Afternote-FE", "1hyok", pullRequests);
    assert.deepEqual(actions.cancels, [
        { number: 10, id: "PR_10", disableAutoMerge: false, dequeue: true },
        { number: 11, id: "PR_11", disableAutoMerge: true, dequeue: false },
    ]);
    assert.deepEqual(actions.targets, [{ number: 12, headSha: pullRequest().headRefOid }]);
});

test("GraphQL 조회가 작성자를 가져온다", async () => {
    const script = await readFile(new URL("./find-viewed-auto-merge-targets.mjs", import.meta.url), "utf8");
    const fields = script.slice(script.indexOf("const PULL_REQUEST_FIELDS"), script.indexOf("`;", script.indexOf("const PULL_REQUEST_FIELDS")));
    assert.match(fields, /author \{ login \}/);
});

test("워크플로는 해제 대상의 예약을 끄고 큐에서 빼며, 해제 실패는 red 로 남긴다", async () => {
    const workflow = await readFile(new URL("../workflows/viewed-auto-merge.yml", import.meta.url), "utf8");
    assert.match(workflow, /echo 'cancels=\[\]' >> "\$GITHUB_OUTPUT"/);
    assert.match(workflow, /CANCELS: \$\{\{ steps\.find\.outputs\.cancels \}\}/);
    assert.match(workflow, /gh pr merge "\$number" --disable-auto/);
    assert.match(workflow, /dequeuePullRequest\(input: \{ id: \$id \}\)/);
    assert.match(workflow, /exit "\$failed"/);
    // 해제는 예약보다 먼저 돈다.
    assert.ok(workflow.indexOf("--disable-auto") < workflow.indexOf("--match-head-commit"));
});

test("워크플로는 본인 토큰으로 읽고 사람 토큰으로 승인한 HEAD 만 예약한다", async () => {
    const workflow = await readFile(new URL("../workflows/viewed-auto-merge.yml", import.meta.url), "utf8");
    assert.match(workflow, /^permissions: \{\}$/m);
    assert.match(workflow, /VIEWED_TOKEN: \$\{\{ secrets\.VIEWED_TOKEN \}\}/);
    assert.match(workflow, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(workflow, /persist-credentials: false/);
    // GITHUB_TOKEN 으로 예약·투입하면 merge_group CI 가 뜨지 않아 큐가 60분 막힌다(1005 #2208).
    // 해제 단계는 CI 가 필요 없어 GITHUB_TOKEN 을 써도 되므로, 예약 단계만 본다.
    const enableStep = workflow.slice(workflow.indexOf("- name: Enable auto-merge"));
    assert.match(enableStep, /GH_TOKEN: \$\{\{ secrets\.MERGE_QUEUE_TOKEN \}\}/);
    assert.doesNotMatch(enableStep, /GH_TOKEN: \$\{\{ github\.token \}\}/);
    assert.match(enableStep, /if \[ -z "\$GH_TOKEN" \]; then[\s\S]*?exit 0/);
    assert.match(workflow, /--match-head-commit "\$head_sha"/);
    // 우회 머지 금지. merge queue 와 required check 를 그대로 탄다.
    assert.doesNotMatch(workflow, /--admin/);
    // PR 코드를 체크아웃하지 않는다.
    assert.doesNotMatch(workflow, /github\.event\.pull_request\.head|refs\/pull/);
});
