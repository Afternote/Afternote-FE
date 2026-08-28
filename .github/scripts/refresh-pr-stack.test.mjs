import assert from "node:assert/strict";
import test from "node:test";

import {
    buildLinearStack,
    dispatchRequiredChecks,
    refreshStack,
    renderSummary,
} from "./refresh-pr-stack.mjs";

const repository = "Afternote/Afternote-FE";

function pullRequest(number, base, head, sha = String(number).padStart(40, "a")) {
    return {
        number,
        state: "open",
        base: { ref: base },
        head: { ref: head, sha, repo: { full_name: repository } },
    };
}

function fakeApi(handler) {
    const calls = [];
    const api = async (apiPath, options = {}) => {
        calls.push({ apiPath, method: options.method ?? "GET", body: options.body });
        return handler(apiPath, options, calls);
    };
    api.calls = calls;
    return api;
}

test("루트에서 base 관계를 따라 선형 스택을 만든다", () => {
    const pullRequests = [
        pullRequest(10, "develop", "feat/a"),
        pullRequest(11, "feat/a", "feat/b"),
        pullRequest(12, "feat/b", "feat/c"),
        pullRequest(99, "develop", "unrelated"),
    ];

    assert.deepEqual(buildLinearStack(pullRequests, 10, repository, 5).map((item) => item.number), [10, 11, 12]);
});

test("분기 스택은 어떤 자식을 먼저 갱신할지 추측하지 않는다", () => {
    const pullRequests = [
        pullRequest(10, "develop", "feat/a"),
        pullRequest(11, "feat/a", "feat/b"),
        pullRequest(12, "feat/a", "feat/c"),
    ];

    assert.throws(() => buildLinearStack(pullRequests, 10, repository, 5), /multiple open children/);
});

test("fork, 닫힌 PR, 최대 깊이 초과를 거절한다", () => {
    const fork = pullRequest(10, "develop", "feat/a");
    fork.head.repo.full_name = "someone/fork";
    assert.throws(() => buildLinearStack([fork], 10, repository, 5), /fork PR/);

    const closed = { ...pullRequest(10, "develop", "feat/a"), state: "closed" };
    assert.throws(() => buildLinearStack([closed], 10, repository, 5), /not open/);

    assert.throws(
        () => buildLinearStack([
            pullRequest(10, "develop", "feat/a"),
            pullRequest(11, "feat/a", "feat/b"),
        ], 10, repository, 1),
        /exceeds max depth/,
    );
});

test("이미 최신인 PR 은 update-branch 를 호출하지 않는다", async () => {
    const pr = pullRequest(10, "develop", "feat/a");
    const api = fakeApi((apiPath) => {
        if (apiPath.includes("/compare/")) return { behind_by: 0 };
        return pr;
    });

    const result = await refreshStack(api, repository, [pr]);

    assert.equal(result[0].updated, false);
    assert.equal(api.calls.some((call) => call.method === "PUT"), false);
});

test("뒤처진 PR 을 expected head 로 갱신하고 새 SHA 를 기다린다", async () => {
    const pr = pullRequest(10, "develop", "feat/a");
    const newSha = "b".repeat(40);
    let pullReads = 0;
    const api = fakeApi((apiPath, options) => {
        if (apiPath.includes("/compare/")) return { behind_by: 2 };
        if (options.method === "PUT") return { message: "Updating pull request branch." };
        pullReads += 1;
        return pullReads >= 2 ? { ...pr, head: { ...pr.head, sha: newSha } } : pr;
    });

    const result = await refreshStack(api, repository, [pr], { delayMs: 0, wait: async () => {} });

    const update = api.calls.find((call) => call.method === "PUT");
    assert.deepEqual(update.body, { expected_head_sha: pr.head.sha });
    assert.equal(result[0].sha, newSha);
    assert.equal(result[0].updated, true);
});

test("발견 뒤 HEAD 가 움직이면 업데이트 전에 실패한다", async () => {
    const pr = pullRequest(10, "develop", "feat/a");
    const moved = { ...pr, head: { ...pr.head, sha: "c".repeat(40) } };
    const api = fakeApi(() => moved);

    await assert.rejects(refreshStack(api, repository, [pr]), /moved after stack discovery/);
    assert.equal(api.calls.some((call) => call.method === "PUT"), false);
});

test("필수 workflow 세 개를 각 PR 현재 branch 에 dispatch 한다", async () => {
    const api = fakeApi(() => null);
    await dispatchRequiredChecks(api, repository, [
        { number: 10, branch: "feat/a", sha: "a".repeat(40), updated: true },
        { number: 11, branch: "feat/b", sha: "b".repeat(40), updated: false },
    ]);

    assert.equal(api.calls.length, 6);
    for (const call of api.calls.slice(0, 3)) {
        assert.deepEqual(call.body, {
            ref: "feat/a",
            inputs: { pull_request_number: "10" },
        });
    }
});

test("dry run 은 update 와 workflow dispatch 를 쓰지 않는다", async () => {
    const pr = pullRequest(10, "develop", "feat/a");
    const api = fakeApi((apiPath) => (apiPath.includes("/compare/") ? { behind_by: 1 } : pr));
    const result = await refreshStack(api, repository, [pr], { dryRun: true });
    await dispatchRequiredChecks(api, repository, result, { dryRun: true });

    assert.equal(result[0].updated, true);
    assert.equal(api.calls.some((call) => call.method !== "GET"), false);
    assert.match(renderSummary([pr], result, true), /would update/);
});
