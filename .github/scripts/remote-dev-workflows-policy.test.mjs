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

    assert.match(source, /^\s{2}pull_request:\n\s{4}types: \[labeled\]$/m);
    assert.match(source, /^permissions:\n\s{2}contents: read\n\s{2}pull-requests: read$/m);
    assert.doesNotMatch(source, /^\s{2}contents: write$/m);
    assert.match(source, /github\.event\.label\.name == 'screenshot-baseline'/);
    assert.match(source, /github\.event\.pull_request\.head\.sha/);
    assert.match(source, /github\.event\.pull_request\.head\.repo\.full_name == github\.repository/);
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
    assert.match(source, /github\.event\.workflow_run\.event == 'pull_request'/);
    assert.match(source, /^\s{2}contents: write$/m);
    assert.doesNotMatch(source, /actions\/checkout@/);
    assert.doesNotMatch(source, /git apply/);
    assert.doesNotMatch(source, /child_process/);
    assert.match(source, /files\.json/);
    assert.match(source, /Rejected non-baseline or duplicate path/);
    assert.match(source, /89504e470d0a1a0a/);
    assert.match(source, /pullRequest\.head\.sha !== metadata\.headSha/);
    assert.match(source, /force: false/);
    for (const workflow of ["pr-validation.yml", "codeql.yml", "merge-order-guard.yml"]) {
        assert.ok(source.includes(`workflow_id: '${workflow}'`), `${workflow} is not redispatched`);
    }
});

test("managed device QA runs through PR validation or the trusted default branch", async () => {
    const source = await readWorkflow("android-managed-device.yml");

    assert.match(source, /^\s{2}workflow_call:\n\s{4}inputs:\n\s{6}pull_request_number:/m);
    assert.doesNotMatch(source, /^\s{2}pull_request:/m);
    assert.doesNotMatch(source, /contains\(github\.event\.pull_request\.labels\.\*\.name, 'android-test'\)/);
    assert.match(source, /github\.event_name == 'pull_request'/);
    assert.match(source, /inputs\.pull_request_number > 0/);
    assert.match(source, /github\.ref_name == github\.event\.repository\.default_branch/);
    assert.match(source, /ref: \$\{\{ github\.event_name == 'pull_request' && github\.event\.pull_request\.head\.sha \|\| github\.sha \}\}/);
    assert.match(source, /persist-credentials: false/);
});

test("screenshot cleanup tolerates cancellation before Gradle setup", async () => {
    const source = await readWorkflow("screenshot.yml");

    assert.match(source, /sudo chown -R .*\$GITHUB_WORKSPACE/);
    assert.match(source, /if \[\[ -e "\$HOME\/\.gradle" \]\]; then/);
    assert.match(source, /sudo chown -R .*\$HOME\/\.gradle/);
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
