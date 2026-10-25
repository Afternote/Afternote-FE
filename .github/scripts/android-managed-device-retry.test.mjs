import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import path from "node:path";
import test from "node:test";
import { runInNewContext } from "node:vm";

const workflow = await readFile(new URL("../workflows/android-managed-device-retry.yml", import.meta.url), "utf8");
const validationStep = workflow.split("- name: Validate marker and rerun the failed managed-device job")[1];
assert.ok(validationStep, "trusted marker validation step must exist");
const script = validationStep.split("          script: |\n")[1]
    .split("\n").map((line) => line.replace(/^ {12}/, "")).join("\n");

async function recover({ state = "open", sha = "current", repository = "owner/repo", references = [{ number: 7 }], lookupFails = false } = {}) {
    const calls = [];
    const marker = {
        device: "api34",
        schemaVersion: 1,
        sourceRunId: 42,
        runAttempt: 1,
        headSha: "current",
        retryable: true,
        reason: "explicit-infrastructure-failure",
        testResultCount: 0,
        infrastructureSignals: ["emulator process exited"],
        bootPhaseSignals: [],
        testExecutionSignals: [],
    };
    const summary = {
        addHeading() { return this; },
        addRaw() { return this; },
        async write() {},
    };
    await runInNewContext(`(async () => {\n${script}\n})()`, {
        require(name) {
            if (name === "node:path") return path;
            assert.equal(name, "node:fs");
            return {
                lstatSync: () => ({ isFile: () => true, isSymbolicLink: () => false, size: 512 }),
                readFileSync: () => JSON.stringify(marker),
            };
        },
        process: { env: { MARKER_PATH: "/runner/retry.json", MARKER_ARTIFACT_NAME: "android-managed-device-retry-api34-42-1" } },
        context: {
            repo: { owner: "owner", repo: "repo" },
            payload: { workflow_run: { id: 42, run_attempt: 1, head_sha: "current", head_repository: { full_name: "owner/repo" }, pull_requests: references } },
        },
        core: { info() {}, warning() {}, summary },
        github: {
            rest: { pulls: { async get(args) {
                calls.push("lookup");
                assert.equal(args.pull_number, 7);
                if (lookupFails) throw new Error("API unavailable");
                return { data: { state, head: { sha, repo: repository === null ? null : { full_name: repository } } } };
            } } },
            async request(route, args) {
                if (route.startsWith("GET ")) {
                    calls.push("jobs");
                    assert.equal(args.run_id, 42);
                    assert.equal(args.attempt_number, 1);
                    return { data: { jobs: [{ id: 99, name: "Pixel 2 API 34 accessibility smoke", conclusion: "failure" }] } };
                }
                assert.equal(route, "POST /repos/{owner}/{repo}/actions/jobs/{job_id}/rerun");
                assert.equal(args.job_id, 99);
                calls.push("rerun");
            },
        },
    });
    return calls;
}

test("recovery checks the live PR after validating the failed job and before rerunning", async () => {
    assert.deepEqual(await recover(), ["jobs", "lookup", "rerun"]);
});

for (const [name, options] of [
    ["newer PR head", { sha: "newer" }],
    ["closed PR", { state: "closed" }],
    ["different head repository", { repository: "other/repo" }],
    ["deleted head repository", { repository: null }],
    ["failed PR lookup", { lookupFails: true }],
]) {
    test(`recovery skips ${name}`, async () => {
        assert.deepEqual(await recover(options), ["jobs", "lookup"]);
    });
}

for (const [name, references] of [
    ["missing PR link", []],
    ["ambiguous PR links", [{ number: 7 }, { number: 8 }]],
    ["invalid PR number", [{ number: "7" }]],
]) {
    test(`recovery skips ${name}`, async () => {
        assert.deepEqual(await recover({ references }), ["jobs"]);
    });
}
