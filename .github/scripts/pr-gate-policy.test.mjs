import assert from "node:assert/strict";
import { readdir, readFile } from "node:fs/promises";
import test from "node:test";

const workflowDirectory = new URL("../workflows/", import.meta.url);
const ENTRY_WORKFLOW = "pr-validation.yml";
const VALIDATION_WORKFLOWS = [
    "lint.yml",
    "unit-test.yml",
    "screenshot.yml",
    "repository-quality.yml",
    "android-managed-device.yml",
];
// Repository ruleset 20911039 의 required context 와 함께 바꿔야 하는 외부 계약이다.
const REQUIRED_VALIDATION_CONTEXTS = [
    "Android Managed Device / Pixel 2 API 30 androidTest",
    "Repository Quality / Repository Quality",
    "Screenshot / Validate Compose Preview Screenshots",
    "Static Analysis / Check Code Quality (Ktlint)",
    "Static Analysis / Check Project Issues (Android Lint)",
    "Unit Test / Run Unit Tests",
];

async function workflows() {
    const names = (await readdir(workflowDirectory)).filter((name) => name.endsWith(".yml"));
    return Promise.all(names.map(async (name) => [name, await readFile(new URL(name, workflowDirectory), "utf8")]));
}

function readWorkflow(name) {
    return readFile(new URL(name, workflowDirectory), "utf8");
}

function jobNames(source) {
    const jobsSection = source.slice(source.indexOf("\njobs:\n"));
    return [...jobsSection.matchAll(/^ {2}([A-Za-z][\w-]*):$/gm)].map((match) => match[1]);
}

function displayNameOf(source, job) {
    const pattern = new RegExp(`^ {2}${job}:$[\\s\\S]*?^ {4}name:\\s*(.+)$`, "m");
    return pattern.exec(source)?.[1]?.trim();
}

function calledWorkflowOf(source, job) {
    const pattern = new RegExp(`^ {2}${job}:$[\\s\\S]*?^ {4}uses:\\s*\\./\\.github/workflows/(.+)$`, "m");
    return pattern.exec(source)?.[1]?.trim();
}

test("pull request validation has exactly one entry point", async () => {
    // 검증 워크플로가 스스로 pull_request 를 듣고 있으면 같은 이름의 check 가 두 벌 돈다.
    const entryPoints = (await workflows())
        .filter(([name, source]) => /^on:\n(?:[^\n]*\n)*?\s{2}pull_request:/m.test(source) && VALIDATION_WORKFLOWS.includes(name))
        .map(([name]) => name);

    assert.deepEqual(entryPoints, []);
    assert.match(await readWorkflow(ENTRY_WORKFLOW), /^on:\n\s{2}pull_request:$/m);
});

test("every validation workflow is reachable only as a reusable call", async () => {
    for (const name of VALIDATION_WORKFLOWS) {
        assert.match(await readWorkflow(name), /^\s{2}workflow_call:$/m, `${name} must be callable`);
    }
});

test("the entry workflow calls every validation workflow without an aggregate runner job", async () => {
    const entry = await readWorkflow(ENTRY_WORKFLOW);
    const jobs = jobNames(entry);

    assert.equal(jobs.length, VALIDATION_WORKFLOWS.length);
    assert.doesNotMatch(entry, /^ {2}ci-gate:$/m);
    for (const workflow of VALIDATION_WORKFLOWS) {
        assert.ok(entry.includes(`uses: ./.github/workflows/${workflow}`), `${workflow} is not called`);
    }
});

test("required validation context names stay aligned with the repository ruleset", async () => {
    const entry = await readWorkflow(ENTRY_WORKFLOW);
    const contexts = [];

    for (const job of jobNames(entry)) {
        const callerName = displayNameOf(entry, job);
        const workflow = calledWorkflowOf(entry, job);
        assert.ok(callerName, `${job} has no display name`);
        assert.ok(workflow, `${job} is not a reusable workflow call`);

        const reusable = await readWorkflow(workflow);
        for (const reusableJob of jobNames(reusable)) {
            const reusableName = displayNameOf(reusable, reusableJob);
            assert.ok(reusableName, `${workflow}:${reusableJob} has no display name`);
            contexts.push(`${callerName} / ${reusableName}`);
        }
    }

    assert.deepEqual(contexts.sort(), [...REQUIRED_VALIDATION_CONTEXTS].sort());
});

test("stale runs are cancelled per pull request", async () => {
    const entry = await readWorkflow(ENTRY_WORKFLOW);

    assert.match(
        entry,
        /^concurrency:\n\s{2}group: pr-validation-\$\{\{ github\.event\.pull_request\.number \|\| inputs\.pull_request_number \|\| github\.ref \}\}\n\s{2}cancel-in-progress: true$/m,
    );
});

test("token-authored commits preserve the pull request context on manual dispatch", async () => {
    const entry = await readWorkflow(ENTRY_WORKFLOW);

    assert.match(entry, /^\s{2}workflow_dispatch:\n\s{4}inputs:\n\s{6}pull_request_number:/m);
    assert.equal(
        (entry.match(/pull_request_number: \$\{\{ inputs\.pull_request_number \|\| github\.event\.pull_request\.number \}\}/g) ?? []).length,
        VALIDATION_WORKFLOWS.length,
    );
});

test("the entry point keeps no pull_request branch filter", async () => {
    // #683: base 변경(feat/* → develop)은 기본 activity type 에 없어 재트리거되지
    // 않는다. 필터가 있으면 스택 PR 이 검증을 한 번도 거치지 않고 머지된다.
    const entry = await readWorkflow(ENTRY_WORKFLOW);
    const trigger = /^on:\n\s{2}pull_request:\n([\s\S]*?)^\S/m.exec(entry)?.[1] ?? "";

    assert.doesNotMatch(trigger, /branches/);
});

test("repository quality still runs on develop and main pushes", async () => {
    const source = await readWorkflow("repository-quality.yml");

    assert.match(source, /^\s{2}push:\n\s{4}branches:\s*\[develop, main\]$/m);
});
