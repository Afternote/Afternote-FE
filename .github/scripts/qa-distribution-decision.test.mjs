import assert from "node:assert/strict";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import { collectQaDistributionContext, decideQaDistribution, runQaDistributionDecision } from "./qa-distribution-decision.mjs";

const headSha = "a".repeat(40);
const sourceSha = "b".repeat(40);
const mainSha = "c".repeat(40);
const NOW = Date.parse("2026-10-03T00:00:00Z");
const APP = "feature/setting/presentation/src/main/kotlin/Profile.kt";

function context(overrides = {}) {
    return {
        headSha,
        changedPaths: [APP],
        runtimePullRequests: [{ number: 1, title: "refactor(setting): profile state", mergedAt: "2026-10-02T00:00:00Z" }],
        unmappedCommits: [],
        ...overrides,
    };
}

test("CI and test churn does not recommend an APK, even after many PRs", () => {
    const result = decideQaDistribution(context({
        changedPaths: [".github/workflows/lint.yml", "docs/release/distribution.md", "feature/setting/src/test/kotlin/ProfileTest.kt"],
        runtimePullRequests: Array.from({ length: 100 }, (_, number) => ({ number, title: "fix(ci): check", mergedAt: "2026-08-01T00:00:00Z" })),
    }), NOW);
    assert.equal(result.decision, "hold");
});

test("a runtime fix is due, but a data path alone does not claim high risk", () => {
    assert.equal(decideQaDistribution(context({ runtimePullRequests: [{ number: 1, title: "fix(setting): restore photo", mergedAt: "2026-10-02T00:00:00Z" }] }), NOW).decision, "deploy");
    const result = decideQaDistribution(context({ changedPaths: ["core/data/src/main/kotlin/Mapper.kt"] }), NOW);
    assert.equal(result.decision, "hold");
    assert.equal(result.readyToDistribute, "not_evaluated");
});

test("an old unshipped app change reaches its cap without another merge", () => {
    const pending = context({ runtimePullRequests: [{ number: 1, title: "refactor(setting): profile state", mergedAt: "2026-09-26T00:00:00Z" }] });
    assert.equal(decideQaDistribution(pending, NOW - 1).decision, "hold");
    assert.equal(decideQaDistribution(pending, NOW).decision, "deploy");
});

test("the pending app batch is bounded independently from raw commits", () => {
    const runtimePullRequests = Array.from({ length: 5 }, (_, number) => ({ number, title: "refactor(setting): simplify", mergedAt: "2026-10-02T00:00:00Z" }));
    assert.equal(decideQaDistribution(context({ runtimePullRequests }), NOW).decision, "deploy");
    assert.equal(decideQaDistribution(context({ changedPaths: [APP, "feature/home/src/main/kotlin/Home.kt", "feature/receiver/src/main/kotlin/List.kt"] }), NOW).decision, "deploy");
});

test("unassociated runtime changes cannot masquerade as a small safe batch", () => {
    assert.equal(decideQaDistribution(context({ unmappedCommits: [headSha] }), NOW).decision, "unknown");
    assert.equal(decideQaDistribution(context({ runtimePullRequests: [] }), NOW).decision, "unknown");
});

test("invalid or future merge timestamps fail instead of silently holding", () => {
    for (const mergedAt of [null, "not-a-date", "2027-01-01T00:00:00Z"]) {
        assert.throws(() => decideQaDistribution(context({ runtimePullRequests: [{ title: "fix: change", mergedAt }] }), NOW));
    }
});

function collectorFixture(count = 1, netPaths = [APP]) {
    const commits = Array.from({ length: count }, (_, i) => (i + 1).toString(16).padStart(40, "0"));
    const pulls = commits.map((sha, i) => ({ number: i + 1, title: "refactor(setting): state", merged_at: "2026-10-02T00:00:00Z", base: { ref: "develop" }, merge_commit_sha: sha }));
    const calls = [];
    async function api(endpoint, paginate) {
        assert.equal(paginate, true);
        calls.push(endpoint);
        if (endpoint.includes("/actions/")) return [{ workflow_runs: [{ id: 7, head_sha: mainSha, head_branch: "main", event: "push", conclusion: "success", updated_at: "2026-09-03T00:00:00Z", html_url: "https://example.com/run" }] }];
        if (endpoint.includes("/commits/")) return [[{ merged_at: "2026-09-03T00:00:00Z", base: { ref: "main" }, head: { ref: "develop", sha: sourceSha } }]];
        return [pulls.slice(0, 50), pulls.slice(50)];
    }
    function git(args) {
        if (args[0] === "rev-list" && args.at(-1).endsWith("..refs/remotes/origin/main")) return "";
        if (args[0] === "rev-list") return [...commits].reverse().concat(sourceSha).join("\n");
        if (args.includes("--raw")) {
            const index = commits.indexOf(args.at(-2));
            return `:100644 100644 ${String(index).padStart(40, "0")} ${String(index + 1).padStart(40, "0")} M\0${APP}\0`;
        }
        if (args[0] === "diff") return (args.includes(sourceSha) ? netPaths : [APP]).join("\0") + "\0";
        return "";
    }
    return { repository: "example/android", headSha, api, git, calls, commits, pulls };
}

test("maps a successful main deployment to its develop source and collects over 50 PRs", async () => {
    const fixture = collectorFixture(75);
    const collected = await collectQaDistributionContext(fixture);
    assert.equal(collected.baseline.sourceSha, sourceSha);
    assert.equal(collected.baseline.mainSha, mainSha);
    assert.equal(collected.pendingPullRequestCount, 75);
    assert.equal(collected.runtimePullRequests.length, 75);
    assert.equal(collected.unmappedCommits.length, 0);
    assert.equal(decideQaDistribution(collected, NOW).decision, "deploy");
});

test("a run list that omits newer main deployments still resolves the newest baseline", async () => {
    const fixture = collectorFixture();
    const [staleMain, failedMain, deployedMain] = ["d", "e", "f"].map((c) => c.repeat(40));
    const staleSource = "9".repeat(40);
    const run = (id, sha, conclusion) => ({ id, head_sha: sha, head_branch: "main", event: "push", conclusion, updated_at: `2026-09-0${id}T00:00:00Z`, html_url: `https://example.com/${id}` });
    const api = fixture.api;
    fixture.api = async (endpoint, paginate) => {
        if (endpoint.includes("/actions/")) {
            fixture.calls.push(endpoint);
            if (endpoint.endsWith(`head_sha=${failedMain}`)) return [{ workflow_runs: [run(3, failedMain, "failure")] }];
            if (endpoint.endsWith(`head_sha=${deployedMain}`)) return [{ workflow_runs: [run(2, deployedMain, "success")] }];
            return [{ workflow_runs: [run(1, staleMain, "success")] }];
        }
        if (endpoint.includes(`/commits/${staleMain}/`)) return [[{ merged_at: "2026-09-01T00:00:00Z", base: { ref: "main" }, head: { ref: "develop", sha: staleSource } }]];
        return api(endpoint, paginate);
    };
    const git = fixture.git;
    fixture.git = (args) => {
        if (args[0] === "rev-list" && args.at(-1) === `${staleMain}..refs/remotes/origin/main`) return [failedMain, deployedMain].join("\n");
        return git(args);
    };
    const collected = await collectQaDistributionContext(fixture);
    assert.equal(collected.baseline.runId, 2);
    assert.equal(collected.baseline.sourceSha, sourceSha);
    assert.ok(fixture.calls.every((endpoint) => !endpoint.includes("status=")));
});

test("a fully reverted runtime batch has no remaining APK change", async () => {
    const fixture = collectorFixture(75, []);
    const collected = await collectQaDistributionContext(fixture);
    assert.equal(decideQaDistribution(collected, NOW).decision, "hold");
    assert.equal(fixture.calls.length, 2);
});

test("a reverted old feature cannot make a fresh same-file refactor immediately due", async () => {
    const fixture = collectorFixture(3);
    fixture.pulls[0].title = "feat(setting): old feature";
    fixture.pulls[0].merged_at = "2026-09-03T00:00:00Z";
    fixture.pulls[1].title = "revert: remove old feature";
    const git = fixture.git;
    fixture.git = (args) => {
        if (!args.includes("--raw")) return git(args);
        const index = fixture.commits.indexOf(args.at(-2));
        const [before, after] = [[0, 1], [1, 0], [0, 2]][index];
        return `:100644 100644 ${String(before).padStart(40, "0")} ${String(after).padStart(40, "0")} M\0${APP}\0`;
    };
    const result = decideQaDistribution(await collectQaDistributionContext(fixture), NOW);
    assert.deepEqual(result.runtimePullRequests.map((pr) => pr.number), [3]);
    assert.equal(result.decision, "hold");
});

test("all rebase-merged commits resolve to one PR instead of permanent unknown", async () => {
    const fixture = collectorFixture(2);
    const api = fixture.api;
    const rebasedPr = fixture.pulls[1];
    fixture.api = (endpoint, paginate) => {
        if (endpoint.includes(`/commits/${fixture.commits[0]}/`)) return [[rebasedPr]];
        if (endpoint.includes("/pulls?state=")) return [[rebasedPr]];
        return api(endpoint, paginate);
    };
    const collected = await collectQaDistributionContext(fixture);
    assert.deepEqual(collected.unmappedCommits, []);
    assert.deepEqual(collected.runtimePullRequests.map((pr) => pr.number), [2]);
    assert.equal(decideQaDistribution(collected, NOW).decision, "hold");
});

test("Firebase versionCode producers are release changes, personal tool config is neutral", () => {
    for (const file of ["resolve-firebase-version-code.mjs", "version-code-bands.mjs"]) {
        assert.equal(decideQaDistribution(context({ changedPaths: [`.github/scripts/${file}`] }), NOW).decision, "deploy");
    }
    assert.equal(decideQaDistribution(context({ changedPaths: [".mcp.json"] }), NOW).decision, "hold");
});

test("a divergent deployed baseline is an error, not an empty pending batch", async () => {
    const fixture = collectorFixture();
    const git = fixture.git;
    fixture.git = (args) => {
        if (args[0] === "merge-base") throw Object.assign(new Error("not ancestor"), { status: 1 });
        return git(args);
    };
    await assert.rejects(collectQaDistributionContext(fixture), /not an ancestor/);
});

test("a pagination failure produces unknown artifacts with no raw API response", async () => {
    const directory = await mkdtemp(path.join(os.tmpdir(), "qa-decision-"));
    try {
        const fixture = collectorFixture();
        const api = fixture.api;
        fixture.api = (endpoint, paginate) => {
            if (endpoint.includes("/pulls?state=")) throw new Error("secret response body");
            return api(endpoint, paginate);
        };
        const result = await runQaDistributionDecision({
            env: { GITHUB_REPOSITORY: fixture.repository, QA_DISTRIBUTION_HEAD_SHA: headSha, QA_DISTRIBUTION_OUTPUT_DIR: directory },
            collect: () => collectQaDistributionContext(fixture), now: NOW,
        });
        assert.equal(result.decision, "unknown");
        const json = await readFile(path.join(directory, "decision.json"), "utf8");
        const summary = await readFile(path.join(directory, "summary.md"), "utf8");
        assert.doesNotMatch(json + summary, /secret response body/);
        assert.match(summary, /판정 불가/);
        assert.equal(JSON.parse(json).readyToDistribute, "not_evaluated");
    } finally {
        await rm(directory, { recursive: true, force: true });
    }
});

test("the workflow publishes artifacts with read-only GitHub permissions", async () => {
    const workflow = await readFile(new URL("../workflows/qa-distribution-decision.yml", import.meta.url), "utf8");
    assert.doesNotMatch(workflow, /pull_request_target|issues:\s*write|pull-requests:\s*write|contents:\s*write|checks:\s*write/);
    assert.match(workflow, /branches: \[develop\]/);
    assert.match(workflow, /schedule:/);
    assert.match(workflow, /workflow_run:/);
    assert.match(workflow, /github.run_attempt == 1/);
    assert.doesNotMatch(workflow, /^concurrency:/m);
    assert.match(workflow, /^    concurrency:/m);
    assert.match(workflow, /if: always\(\)/);
    assert.match(workflow, /actions\/upload-artifact@[a-f0-9]{40}/);
});
