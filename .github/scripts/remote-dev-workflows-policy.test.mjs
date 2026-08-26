import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const workflowDirectory = new URL("../workflows/", import.meta.url);
const readWorkflow = (name) => readFile(new URL(name, workflowDirectory), "utf8");

const screenshotModules = [
    ":core:ui",
    ":feature:home:presentation",
    ":feature:receiver:presentation",
    ":feature:onboarding:presentation",
    ":feature:afternote:presentation",
    ":feature:mindrecord:presentation",
];

test("baseline generation executes PR code without write credentials", async () => {
    const source = await readWorkflow("screenshot-baseline-generate.yml");

    assert.match(source, /^\s{2}workflow_dispatch:/m);
    assert.match(source, /^permissions:\n\s{2}contents: read\n\s{2}pull-requests: read$/m);
    assert.doesNotMatch(source, /^\s{2}contents: write$/m);
    assert.match(source, /pullRequest\.head\.sha/);
    assert.match(source, /pullRequest\.head\.repo\?\.full_name/);
    assert.match(source, /persist-credentials: false/);
    assert.match(source, /platforms: linux\/amd64/);
    assert.match(source, /docker run --rm --platform linux\/amd64/);
    assert.match(source, /--rerun --no-daemon/);
    for (const module of screenshotModules) {
        assert.ok(source.includes(`${module}:updateScreenshotTest`), `${module} baseline is not updated`);
        assert.ok(source.includes(`${module}:validateScreenshotTest`), `${module} baseline is not validated`);
    }
});

test("privileged baseline apply is a workflow-run bridge restricted to PNG baselines", async () => {
    const source = await readWorkflow("screenshot-baseline-apply.yml");

    assert.match(source, /^\s{2}workflow_run:/m);
    assert.match(source, /workflows: \["Generate Screenshot Baselines"\]/);
    assert.match(source, /github\.event\.workflow_run\.event == 'workflow_dispatch'/);
    assert.match(source, /^\s{2}contents: write$/m);
    assert.match(source, /persist-credentials: false/);
    assert.match(source, /Rejected non-baseline path/);
    assert.match(source, /89504e470d0a1a0a/);
    assert.match(source, /pullRequest\.head\.sha !== metadata\.headSha/);
    assert.match(source, /force: false/);
    for (const workflow of ["pr-validation.yml", "codeql.yml", "merge-order-guard.yml"]) {
        assert.ok(source.includes(`workflow_id: '${workflow}'`), `${workflow} is not redispatched`);
    }
});

test("managed device QA can target an exact labeled or manually selected PR", async () => {
    const source = await readWorkflow("android-managed-device.yml");

    assert.match(source, /^\s{2}pull_request:\n\s{4}types: \[labeled, synchronize\]$/m);
    assert.match(source, /contains\(github\.event\.pull_request\.labels\.\*\.name, 'android-test'\)/);
    assert.match(source, /pull_request_number:/);
    assert.match(source, /ref: \$\{\{ steps\.target\.outputs\.sha \}\}/);
    assert.match(source, /persist-credentials: false/);
});

test("stack refresh only executes trusted default-branch code", async () => {
    const source = await readWorkflow("stack-refresh.yml");

    assert.match(source, /^\s{2}workflow_dispatch:/m);
    assert.match(source, /github\.ref_name == github\.event\.repository\.default_branch/);
    assert.match(source, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(source, /persist-credentials: false/);
    assert.match(source, /node --test \.github\/scripts\/refresh-pr-stack\.test\.mjs/);
    assert.match(source, /MAX_DEPTH: \$\{\{ inputs\.max_depth \}\}/);
});

test("required checks expose manual dispatch for token-authored commits", async () => {
    const validation = await readWorkflow("pr-validation.yml");
    const guard = await readWorkflow("merge-order-guard.yml");

    assert.match(validation, /^\s{2}workflow_dispatch:$/m);
    assert.match(guard, /^\s{2}workflow_dispatch:\n\s{4}inputs:/m);
    assert.match(guard, /github\.event_name == 'pull_request' \|\| github\.event_name == 'workflow_dispatch'/);
});
