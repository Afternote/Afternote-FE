import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import {
    COMMENT_MARKER,
    DEFAULT_LABEL,
    applyPlan,
    createGit,
    detectSiblingConflicts,
    ensureHeadsAvailable,
    ensureLabelExists,
    normalizePullRequest,
    parseMergeTreeOutput,
    planSiblingLabelChanges,
    renderResolvedComment,
    renderSiblingComment,
    renderSummary,
} from "./label-sibling-conflicts.mjs";

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
        number: 1,
        baseRefName: "develop",
        headRefName: "feature/x",
        headRefOid: "0".repeat(40),
        isDraft: false,
        sameRepository: true,
        labels: [],
        ...overrides,
    };
}

const writes = (api) => api.calls.filter((call) => call.method !== "GET");

// ---- 실제 git 저장소 픽스처 ----
//
// 판정의 정본은 git 의 3-way merge 다. 가짜 git 으로는 «같은 파일을 고쳤지만 깨끗이 합쳐진다»
// 같은 경계를 흉내낼 수 없으므로 임시 저장소를 만들어 실제 merge-tree 를 돌린다.

const gitEnv = {
    ...process.env,
    GIT_AUTHOR_NAME: "test",
    GIT_AUTHOR_EMAIL: "test@example.com",
    GIT_COMMITTER_NAME: "test",
    GIT_COMMITTER_EMAIL: "test@example.com",
    GIT_CONFIG_GLOBAL: "/dev/null",
    GIT_CONFIG_NOSYSTEM: "1",
};

async function makeRepository() {
    const dir = await mkdtemp(path.join(os.tmpdir(), "sibling-conflict-"));
    const run = (args) => {
        const result = spawnSync("git", args, { cwd: dir, encoding: "utf8", env: gitEnv });
        if (result.status !== 0) {
            throw new Error(`git ${args.join(" ")} 실패: ${result.stderr}`);
        }
        return result.stdout.trim();
    };
    run(["init", "-q", "-b", "develop"]);
    return {
        dir,
        run,
        write: (name, content) => writeFile(path.join(dir, name), content),
        commit: (message) => {
            run(["add", "-A"]);
            run(["commit", "-q", "-m", message]);
            return run(["rev-parse", "HEAD"]);
        },
        checkout: (branch, from) => run(from ? ["checkout", "-q", "-b", branch, from] : ["checkout", "-q", branch]),
        git: createGit({ cwd: dir }),
        cleanup: () => rm(dir, { recursive: true, force: true }),
    };
}

const LINES = ["a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8"];
const aText = (first = "a1", last = "a8") => [first, ...LINES.slice(1, -1), last].join("\n") + "\n";

/**
 * 픽스처.
 *
 *   develop D0 : a.txt(a1..a8) · b.txt · c.txt
 *   feat/x      (D0)     a1→x              #10 base develop
 *   feat/y      (D0)     a1→y              #11 base develop   → #10 ↔ #11 은 a.txt 충돌
 *   feat/v      (D0)     a8→v              #12 base develop   → #10 과 같은 파일이지만 깨끗이 합쳐진다
 *   feat/z      (D0)     b.txt→z           #13 base develop
 *   feat/x-child(feat/x) a8→xc8 · c.txt→xc #14 base feat/x    → a.txt 의 a1 충돌은 #10 것, a8 충돌은 #14 것
 *   develop D1 : a1→d                                         → #10·#11 과 부딪히는 trunk 전진
 *   feat/w      (D1)     b.txt→w           #15 base develop   → #13 ↔ #15 는 b.txt 충돌,
 *                                                                #11 과는 a.txt 에서 부딪히지만 w 의 자기 파일이 아니다
 */
async function makeFixture() {
    const repo = await makeRepository();
    await repo.write("a.txt", aText());
    await repo.write("b.txt", "b\n");
    await repo.write("c.txt", "c\n");
    const d0 = await repo.commit("D0");

    const heads = {};
    repo.checkout("feat/x", d0);
    await repo.write("a.txt", aText("x"));
    heads.x = await repo.commit("x");

    repo.checkout("feat/y", d0);
    await repo.write("a.txt", aText("y"));
    heads.y = await repo.commit("y");

    repo.checkout("feat/v", d0);
    await repo.write("a.txt", aText("a1", "v"));
    heads.v = await repo.commit("v");

    repo.checkout("feat/z", d0);
    await repo.write("b.txt", "z\n");
    heads.z = await repo.commit("z");

    repo.checkout("feat/x-child", "feat/x");
    await repo.write("a.txt", aText("x", "xc8"));
    await repo.write("c.txt", "xc\n");
    heads.xChild = await repo.commit("x-child");

    repo.checkout("develop");
    await repo.write("a.txt", aText("d"));
    const d1 = await repo.commit("D1");

    repo.checkout("feat/w", d1);
    await repo.write("b.txt", "w\n");
    heads.w = await repo.commit("w");
    repo.checkout("develop");

    const pullRequests = [
        pullRequest({ number: 10, headRefName: "feat/x", headRefOid: heads.x }),
        pullRequest({ number: 11, headRefName: "feat/y", headRefOid: heads.y }),
        pullRequest({ number: 12, headRefName: "feat/v", headRefOid: heads.v }),
        pullRequest({ number: 13, headRefName: "feat/z", headRefOid: heads.z }),
        pullRequest({ number: 14, headRefName: "feat/x-child", headRefOid: heads.xChild, baseRefName: "feat/x" }),
        pullRequest({ number: 15, headRefName: "feat/w", headRefOid: heads.w }),
    ];
    return { repo, heads, pullRequests };
}

function pairNumbers(result) {
    return result.pairs.map((pair) => pair.numbers);
}

const hasPair = (result, a, b) => pairNumbers(result).some((numbers) => numbers[0] === a && numbers[1] === b);

test("형제 충돌 쌍은 양쪽 자기 커밋이 모두 고친 파일에서만 잡힌다", async () => {
    const { repo, pullRequests } = await makeFixture();
    try {
        const result = detectSiblingConflicts({ pullRequests, trunk: "develop", git: repo.git });

        assert.deepEqual(result.pairs, [
            { numbers: [10, 11], files: ["a.txt"] },
            { numbers: [12, 14], files: ["a.txt"] },
            { numbers: [13, 15], files: ["b.txt"] },
        ]);
        assert.deepEqual(result.skipped, []);
        assert.deepEqual(result.skippedPairs, []);
    } finally {
        await repo.cleanup();
    }
});

test("같은 파일을 고쳐도 깨끗이 합쳐지면 충돌이 아니다", async () => {
    // 파일 겹침은 merge-tree 를 돌릴 조건일 뿐이고, 판정은 3-way merge 결과다.
    const { repo, pullRequests } = await makeFixture();
    try {
        const result = detectSiblingConflicts({ pullRequests, trunk: "develop", git: repo.git });
        assert.equal(hasPair(result, 10, 12), false);
    } finally {
        await repo.cleanup();
    }
});

test("스택 자식은 부모의 충돌을 물려받지 않고 자기 충돌만 갖는다", async () => {
    // #14 의 head 는 #10 의 커밋을 품고 있어 #11 과 합치면 a.txt 의 a1 이 충돌한다. 그것은 #10 의
    // 것이다 — 부모 체인이 #11 과 이미 부딪히는 파일이라 #14 에서는 뺀다. 반면 #12 와의 a8 충돌은
    // #14 의 자기 커밋이 만든 것이고 부모는 #12 와 부딪히지 않으므로 #14 에 남는다.
    const { repo, pullRequests } = await makeFixture();
    try {
        const result = detectSiblingConflicts({ pullRequests, trunk: "develop", git: repo.git });
        assert.equal(hasPair(result, 11, 14), false);
        assert.equal(hasPair(result, 12, 14), true);
    } finally {
        await repo.cleanup();
    }
});

test("trunk 전진이 만든 충돌은 형제 충돌이 아니다", async () => {
    // #15 는 D1 에서 갈라져 a1→d 를 품고 있다. #11 과 합치면 a.txt 가 충돌하지만, 그것은 #11 과
    // develop 의 충돌(= conflict 라벨 몫)이지 #15 의 커밋이 만든 것이 아니다.
    const { repo, pullRequests } = await makeFixture();
    try {
        const result = detectSiblingConflicts({ pullRequests, trunk: "develop", git: repo.git });
        assert.equal(hasPair(result, 11, 15), false);
    } finally {
        await repo.cleanup();
    }
});

test("head 를 찾지 못한 PR 은 보류로 남기고 나머지 판정은 계속한다", async () => {
    const { repo, pullRequests } = await makeFixture();
    try {
        const result = detectSiblingConflicts({
            pullRequests: [...pullRequests, pullRequest({ number: 16, headRefName: "feat/fork", headRefOid: "f".repeat(40) })],
            trunk: "develop",
            git: repo.git,
        });

        assert.deepEqual(result.skipped.map((item) => item.number), [16]);
        assert.match(result.skipped[0].reason, /찾지 못함/);
        assert.deepEqual(pairNumbers(result), [[10, 11], [12, 14], [13, 15]]);
    } finally {
        await repo.cleanup();
    }
});

test("base 를 옮긴 뒤 rebase 하지 않은 자식은 커밋 조상으로 같은 체인임을 가린다", async () => {
    // 부모 링크(base 브랜치명)가 끊겨도 #14 의 head 는 #10 의 head 를 조상으로 갖는다.
    // 그 둘 사이의 겹침은 base 축이지 형제 축이 아니다.
    const { repo, pullRequests } = await makeFixture();
    try {
        const retargeted = pullRequests.map((item) => (item.number === 14 ? { ...item, baseRefName: "develop" } : item));
        const result = detectSiblingConflicts({ pullRequests: retargeted, trunk: "develop", git: repo.git });
        assert.equal(hasPair(result, 10, 14), false);
    } finally {
        await repo.cleanup();
    }
});

test("공통 조상이 없는 PR 은 자기 파일을 셀 수 없어 보류로 남긴다", async () => {
    const { repo, pullRequests } = await makeFixture();
    try {
        repo.run(["checkout", "-q", "--orphan", "feat/orphan"]);
        await repo.write("a.txt", "orphan\n");
        const orphan = await repo.commit("orphan");
        repo.checkout("develop");

        const result = detectSiblingConflicts({
            pullRequests: [...pullRequests, pullRequest({ number: 17, headRefName: "feat/orphan", headRefOid: orphan })],
            trunk: "develop",
            git: repo.git,
        });

        assert.deepEqual(result.skipped.map((item) => item.number), [17]);
        assert.match(result.skipped[0].reason, /공통 조상 없음/);
        assert.equal(result.pairs.length, 3);
    } finally {
        await repo.cleanup();
    }
});

test("merge-tree 결과를 종료 코드로 가른다", async () => {
    const { repo, heads } = await makeFixture();
    try {
        assert.deepEqual(repo.git.mergeTreeConflicts(heads.x, heads.v), { conflictedFiles: [], error: null });
        assert.deepEqual(repo.git.mergeTreeConflicts(heads.x, heads.y), { conflictedFiles: ["a.txt"], error: null });

        repo.run(["checkout", "-q", "--orphan", "feat/orphan"]);
        await repo.write("a.txt", "orphan\n");
        const orphan = await repo.commit("orphan");
        const unrelated = repo.git.mergeTreeConflicts(heads.x, orphan);
        assert.deepEqual(unrelated.conflictedFiles, []);
        assert.ok(unrelated.error);
    } finally {
        await repo.cleanup();
    }
});

test("merge-tree -z 출력에서 충돌 파일 구간만 읽는다", () => {
    const stdout = ["abc123", "dir/one.kt", "two.txt", "", "1", "dir/one.kt", "Auto-merging", "Auto-merging dir/one.kt\n"].join("\0");
    assert.deepEqual(parseMergeTreeOutput(stdout), { tree: "abc123", conflictedFiles: ["dir/one.kt", "two.txt"] });
    assert.deepEqual(parseMergeTreeOutput("abc123\0"), { tree: "abc123", conflictedFiles: [] });
    assert.deepEqual(parseMergeTreeOutput("abc123\n"), { tree: "abc123", conflictedFiles: [] });
});

test("없는 head 만 refs/pull/N/head 로 채운다", () => {
    const fetched = [];
    const git = {
        commitExists: (oid) => oid.startsWith("1"),
        fetchPullRequestHead: (number) => {
            fetched.push(number);
            return number !== 30;
        },
    };
    const lines = [];

    ensureHeadsAvailable(
        [
            pullRequest({ number: 20, headRefOid: "1".repeat(40) }),
            pullRequest({ number: 21, headRefOid: "2".repeat(40) }),
            pullRequest({ number: 30, headRefOid: "3".repeat(40) }),
        ],
        git,
        { log: (line) => lines.push(line) },
    );

    assert.deepEqual(fetched, [21, 30]);
    assert.deepEqual(lines, ["#30 head 를 가져오지 못했다 — 판정 보류"]);
});

test("포크 PR 은 같은 저장소가 아니라고 표시한다", () => {
    const own = normalizePullRequest({ number: 1, headRepository: { nameWithOwner: "Org/Repo" }, labels: { nodes: [] } }, "org/repo");
    const fork = normalizePullRequest({ number: 2, headRepository: { nameWithOwner: "someone/repo" }, labels: { nodes: [{ name: "x" }] } }, "org/repo");

    assert.equal(own.sameRepository, true);
    assert.equal(fork.sameRepository, false);
    assert.deepEqual(fork.labels, ["x"]);
});

test("충돌 쌍의 양쪽에 라벨을 붙이고 PR 마다 상대 목록 코멘트를 계획한다", () => {
    const plan = planSiblingLabelChanges({
        pullRequests: [pullRequest({ number: 10 }), pullRequest({ number: 11 }), pullRequest({ number: 12 })],
        pairs: [{ numbers: [10, 11], files: ["a.txt"] }],
        label: DEFAULT_LABEL,
    });

    assert.deepEqual(plan.toLabel.map((item) => item.number), [10, 11]);
    assert.deepEqual(plan.toUnlabel, []);
    assert.deepEqual(plan.comments, [
        { number: 10, siblings: [{ sibling: 11, files: ["a.txt"] }] },
        { number: 11, siblings: [{ sibling: 10, files: ["a.txt"] }] },
    ]);
});

test("이미 라벨이 붙은 PR 은 다시 붙이지 않고, 쌍에서 빠진 PR 에서는 뗀다", () => {
    const plan = planSiblingLabelChanges({
        pullRequests: [
            pullRequest({ number: 10, labels: [DEFAULT_LABEL] }),
            pullRequest({ number: 11 }),
            pullRequest({ number: 12, labels: [DEFAULT_LABEL, "bug"] }),
        ],
        pairs: [{ numbers: [10, 11], files: ["a.txt"] }],
        label: DEFAULT_LABEL,
    });

    assert.deepEqual(plan.toLabel.map((item) => item.number), [11]);
    assert.deepEqual(plan.toUnlabel.map((item) => item.number), [12]);
    // 라벨이 이미 있어도 코멘트는 현재 상태로 다시 계획한다 — 상대 목록이 바뀌었을 수 있다.
    assert.deepEqual(plan.comments.map((item) => item.number), [10, 11]);
});

test("한 PR 이 여러 형제와 충돌하면 상대를 번호순으로 한 코멘트에 모은다", () => {
    const plan = planSiblingLabelChanges({
        pullRequests: [pullRequest({ number: 10 }), pullRequest({ number: 11 }), pullRequest({ number: 12 })],
        pairs: [
            { numbers: [10, 12], files: ["b.txt"] },
            { numbers: [10, 11], files: ["a.txt"] },
        ],
        label: DEFAULT_LABEL,
    });

    assert.deepEqual(plan.comments.find((item) => item.number === 10).siblings, [
        { sibling: 11, files: ["a.txt"] },
        { sibling: 12, files: ["b.txt"] },
    ]);
});

test("코멘트에 마커·상대 번호·충돌 파일·추적 이슈가 들어간다", () => {
    const body = renderSiblingComment({
        siblings: [
            { sibling: 1608, files: ["dir/AfternoteHomeScreen.kt"] },
            { sibling: 1626, files: ["x.kt", "y.kt"] },
        ],
    });

    assert.ok(body.startsWith(COMMENT_MARKER));
    assert.ok(body.includes("- #1608 — `dir/AfternoteHomeScreen.kt`"));
    assert.ok(body.includes("- #1626 — `x.kt`, `y.kt`"));
    assert.ok(body.includes("#1750"));
    assert.ok(body.includes("자동으로 떨어진다"));

    const resolved = renderResolvedComment();
    assert.ok(resolved.startsWith(COMMENT_MARKER));
    assert.ok(resolved.includes("해소"));
});

test("라벨을 붙이고 마커 코멘트가 없으면 만든다", async () => {
    const api = fakeApi({ responses: { "/repos/o/r/issues/40/comments": [], "/repos/o/r/issues/41/comments": [] } });

    const failures = await applyPlan(
        api,
        "o/r",
        {
            toLabel: [{ number: 40 }, { number: 41 }],
            toUnlabel: [],
            comments: [
                { number: 40, siblings: [{ sibling: 41, files: ["a.txt"] }] },
                { number: 41, siblings: [{ sibling: 40, files: ["a.txt"] }] },
            ],
        },
        { label: DEFAULT_LABEL, dryRun: false, logger: silent },
    );

    assert.deepEqual(failures, []);
    assert.deepEqual(writes(api).map((call) => [call.method, call.apiPath]), [
        ["POST", "/repos/o/r/issues/40/labels"],
        ["POST", "/repos/o/r/issues/41/labels"],
        ["POST", "/repos/o/r/issues/40/comments"],
        ["POST", "/repos/o/r/issues/41/comments"],
    ]);
    assert.ok(writes(api)[2].body.body.includes("- #41 — `a.txt`"));
});

test("마커 코멘트가 있고 내용이 다르면 고쳐 쓴다", async () => {
    const api = fakeApi({
        responses: { "/repos/o/r/issues/42/comments": [{ id: 7, body: `${COMMENT_MARKER}\r\n옛 상대 목록` }] },
    });

    await applyPlan(
        api,
        "o/r",
        { toLabel: [], toUnlabel: [], comments: [{ number: 42, siblings: [{ sibling: 43, files: ["a.txt"] }] }] },
        { label: DEFAULT_LABEL, dryRun: false, logger: silent },
    );

    assert.deepEqual(writes(api).map((call) => [call.method, call.apiPath]), [["PATCH", "/repos/o/r/issues/comments/7"]]);
    assert.ok(writes(api)[0].body.body.includes("- #43 — `a.txt`"));
});

test("마커 코멘트가 현재 상태와 같으면 아무것도 쓰지 않는다", async () => {
    const siblings = [{ sibling: 43, files: ["a.txt"] }];
    const current = renderSiblingComment({ siblings, label: DEFAULT_LABEL }).replace(/\n/g, "\r\n");
    const api = fakeApi({ responses: { "/repos/o/r/issues/42/comments": [{ id: 7, body: current }] } });

    await applyPlan(
        api,
        "o/r",
        { toLabel: [], toUnlabel: [], comments: [{ number: 42, siblings }] },
        { label: DEFAULT_LABEL, dryRun: false, logger: silent },
    );

    assert.deepEqual(writes(api), []);
});

test("해소된 PR 에서 라벨을 떼고 남은 안내를 «해소됨» 으로 고쳐 쓴다", async () => {
    const api = fakeApi({
        responses: { "/repos/o/r/issues/45/comments": [{ id: 9, body: `${COMMENT_MARKER}\n옛 상대 목록` }] },
    });

    await applyPlan(
        api,
        "o/r",
        { toLabel: [], toUnlabel: [{ number: 45 }], comments: [] },
        { label: DEFAULT_LABEL, dryRun: false, logger: silent },
    );

    assert.deepEqual(writes(api).map((call) => [call.method, call.apiPath]), [
        ["DELETE", "/repos/o/r/issues/45/labels/sibling-conflict"],
        ["PATCH", "/repos/o/r/issues/comments/9"],
    ]);
    assert.equal(writes(api)[1].body.body, renderResolvedComment());
});

test("해소된 PR 에 안내가 없으면 새로 만들지 않는다", async () => {
    const api = fakeApi({ responses: { "/repos/o/r/issues/46/comments": [] } });

    await applyPlan(
        api,
        "o/r",
        { toLabel: [], toUnlabel: [{ number: 46 }], comments: [] },
        { label: DEFAULT_LABEL, dryRun: false, logger: silent },
    );

    assert.deepEqual(writes(api).map((call) => [call.method, call.apiPath]), [
        ["DELETE", "/repos/o/r/issues/46/labels/sibling-conflict"],
    ]);
});

test("dry run 은 아무것도 쓰지 않는다", async () => {
    const api = fakeApi();

    await applyPlan(
        api,
        "o/r",
        {
            toLabel: [{ number: 46 }],
            toUnlabel: [{ number: 47 }],
            comments: [{ number: 46, siblings: [{ sibling: 48, files: ["a.txt"] }] }],
        },
        { label: DEFAULT_LABEL, dryRun: true, logger: silent },
    );

    assert.deepEqual(api.calls, []);
});

test("한 PR 이 실패해도 나머지를 처리하고 실패를 모아 돌려준다", async () => {
    const api = fakeApi({
        responses: { "/repos/o/r/issues/51/comments": [] },
        failOn: (apiPath) => apiPath === "/repos/o/r/issues/50/labels",
    });

    const failures = await applyPlan(
        api,
        "o/r",
        {
            toLabel: [{ number: 50 }, { number: 51 }],
            toUnlabel: [],
            comments: [{ number: 51, siblings: [{ sibling: 50, files: ["a.txt"] }] }],
        },
        { label: DEFAULT_LABEL, dryRun: false, logger: silent },
    );

    assert.equal(failures.length, 1);
    assert.match(failures[0], /#50 라벨 부착 실패/);
    assert.ok(api.calls.some((call) => call.apiPath === "/repos/o/r/issues/51/comments" && call.method === "POST"));
});

test("라벨이 없으면 만들고, 있으면 만들지 않는다", async () => {
    const missing = fakeApi();
    await ensureLabelExists(missing, "o/r", DEFAULT_LABEL);
    const created = missing.calls.filter((call) => call.method === "POST");
    assert.equal(created.length, 1);
    assert.equal(created[0].body.name, DEFAULT_LABEL);

    const existing = fakeApi({ responses: { "/repos/o/r/labels/sibling-conflict": { name: DEFAULT_LABEL } } });
    await ensureLabelExists(existing, "o/r", DEFAULT_LABEL);
    assert.equal(existing.calls.filter((call) => call.method === "POST").length, 0);
});

test("요약에 쌍·파일·건수·보류 사유가 남는다", () => {
    const pairs = [{ numbers: [10, 11], files: ["a.txt"] }];
    const plan = planSiblingLabelChanges({
        pullRequests: [pullRequest({ number: 10 }), pullRequest({ number: 11 }), pullRequest({ number: 12, labels: [DEFAULT_LABEL] })],
        pairs,
        label: DEFAULT_LABEL,
    });

    const summary = renderSummary({
        pairs,
        plan,
        skipped: [{ number: 16, reason: "head 를 찾지 못함" }],
        skippedPairs: [{ numbers: [10, 17], reason: "no merge base" }],
        label: DEFAULT_LABEL,
        dryRun: true,
    });

    assert.ok(summary.includes("dry run"));
    assert.ok(summary.includes("#10 ↔ #11 — `a.txt`"));
    assert.ok(summary.includes("라벨 부착: 2건 — #10, #11"));
    assert.ok(summary.includes("라벨 제거: 1건 — #12"));
    assert.ok(summary.includes("코멘트 계획: 2건"));
    assert.ok(summary.includes("판정 보류: 1건 — #16 (head 를 찾지 못함)"));
    assert.ok(summary.includes("비교 실패: 1건 — #10 ↔ #17 (no merge base)"));
});

test("리컨사일 워크플로가 전체 히스토리 checkout 으로 이 스크립트와 테스트를 부른다", () => {
    const job = /^  sibling-conflicts:\n([\s\S]*?)(?=^  \w[\w-]*:\n|(?![\s\S]))/m.exec(workflow)?.[1];
    assert.ok(job, "sibling-conflicts job 이 있어야 한다");
    assert.match(job, /^    timeout-minutes: [1-9]\d*$/m);
    assert.match(job, /^          fetch-depth: 0$/m);
    assert.match(job, /^          persist-credentials: false$/m);
    assert.match(job, /node --test \.github\/scripts\/label-sibling-conflicts\.test\.mjs/);
    assert.match(job, /run: node \.github\/scripts\/label-sibling-conflicts\.mjs$/m);
    assert.match(job, /GITHUB_DEFAULT_BRANCH: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(job, /DRY_RUN: \$\{\{ inputs\.dry_run == true && 'true' \|\| 'false' \}\}/);
    assert.match(job, /^      issues: write$/m);
    assert.match(job, /^      pull-requests: write$/m);
});
