import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const workflowDirectory = new URL("../workflows/", import.meta.url);
const readWorkflow = (name) => readFile(new URL(name, workflowDirectory), "utf8");

test("untrusted pull request and stack-base signals have no write permission", async () => {
    const probe = await readWorkflow("base-freshness-probe.yml");

    assert.match(probe, /^name: Base Freshness Probe$/m);
    assert.match(probe, /^\s{2}pull_request:\n\s{4}types: \[opened, reopened, synchronize, edited\]$/m);
    assert.match(probe, /^\s{2}merge_group:\n\s{4}types: \[checks_requested\]$/m);
    assert.match(probe, /^\s{2}push:\s*$/m);
    assert.match(probe, /^\s{2}workflow_dispatch:\n\s{4}inputs:\n\s{6}pull_request_number:/m);
    assert.match(probe, /^permissions: \{\}$/m);
    assert.doesNotMatch(probe, /pull_request_target:/);
    assert.doesNotMatch(probe, /statuses: write/);
    assert.match(probe, /name: Base Freshness/);
    assert.match(probe, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(probe, /persist-credentials: false/);
    assert.match(probe, /BASE_FRESHNESS_MODE: merge-group/);
});

test("only the trusted publisher receives the narrow status permission", async () => {
    const publisher = await readWorkflow("base-freshness.yml");

    assert.match(publisher, /^name: Base Freshness Publisher$/m);
    assert.match(publisher, /^\s{2}workflow_run:\n\s{4}workflows: \["Base Freshness Probe"\]/m);
    assert.match(publisher, /^\s{2}push:\n\s{4}branches: \[develop, main\]$/m);
    assert.match(publisher, /^permissions: \{\}$/m);
    assert.match(publisher, /^  group: base-freshness-publisher$/m);
    assert.match(publisher, /^  cancel-in-progress: false$/m);
    assert.match(publisher, /^\s{6}statuses: write$/m);
    assert.match(publisher, /^\s{6}pull-requests: read$/m);
    assert.doesNotMatch(publisher, /contents: write/);
    assert.doesNotMatch(publisher, /pull-requests: write/);
    assert.match(publisher, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(publisher, /persist-credentials: false/);
    assert.match(publisher, /node --test \.github\/scripts\/evaluate-base-freshness\.test\.mjs/);
    assert.match(publisher, /node \.github\/scripts\/evaluate-base-freshness\.mjs/);
});

test("the gate writes one stable required context from live ancestry comparisons", async () => {
    const script = await readFile(new URL("evaluate-base-freshness.mjs", import.meta.url), "utf8");

    assert.match(script, /STATUS_CONTEXT = "Base Freshness"/);
    assert.match(script, /compare\//);
    assert.match(script, /state: fresh \? "success" : "failure"/);
    assert.match(script, /state: "pending"/);
    assert.match(script, /api\.getBranchHead\(current\.baseRef\)/);
    assert.match(script, /verifiedBaseSha !== baseSha/);
    assert.match(script, /finalBaseSha !== baseSha/);
    assert.match(script, /finalCurrent\.headSha !== current\.headSha/);
    assert.match(script, /const pullRequests = await api\.listPullRequests\(\)/);
    assert.match(script, /sweeps all open PRs/);
    assert.match(script, /new AggregateError/);
    assert.doesNotMatch(script, /pull_request_target/);
});
