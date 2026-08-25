import assert from "node:assert/strict";
import { readdir, readFile } from "node:fs/promises";
import test from "node:test";

const workflowDirectory = new URL("../workflows/", import.meta.url);
const GATE_WORKFLOW = "pr-validation.yml";
// ci-gate 가 결과를 모아야 하는 검증들. 여기서 빠지면 그 검증은 머지를 막지 못한다.
const VALIDATION_WORKFLOWS = ["lint.yml", "unit-test.yml", "screenshot.yml", "repository-quality.yml"];

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

function needsOf(source, job) {
    const pattern = new RegExp(`^ {2}${job}:$[\\s\\S]*?^ {4}needs:\\s*\\[([^\\]]*)\\]`, "m");
    const match = pattern.exec(source);
    return match ? match[1].split(",").map((entry) => entry.trim()) : [];
}

test("pull request validation has exactly one entry point", async () => {
    // 검증 워크플로가 스스로 pull_request 를 듣고 있으면 같은 검사가 두 벌 돌고,
    // ci-gate 가 보지 않는 쪽이 별개 체크로 남는다.
    const entryPoints = (await workflows())
        .filter(([name, source]) => /^on:\n(?:[^\n]*\n)*?\s{2}pull_request:/m.test(source) && VALIDATION_WORKFLOWS.includes(name))
        .map(([name]) => name);

    assert.deepEqual(entryPoints, []);
    assert.match(await readWorkflow(GATE_WORKFLOW), /^on:\n\s{2}pull_request:$/m);
});

test("every validation workflow is reachable only as a reusable call", async () => {
    for (const name of VALIDATION_WORKFLOWS) {
        assert.match(await readWorkflow(name), /^\s{2}workflow_call:$/m, `${name} must be callable`);
    }
});

test("ci-gate aggregates every validation job in the entry workflow", async () => {
    const gate = await readWorkflow(GATE_WORKFLOW);
    const jobs = jobNames(gate);

    assert.ok(jobs.includes("ci-gate"));
    const validationJobs = jobs.filter((job) => job !== "ci-gate");
    assert.equal(validationJobs.length, VALIDATION_WORKFLOWS.length);
    // 검증 job 을 추가하고 needs 갱신을 잊으면 그 검증은 게이트 밖에 남는다.
    assert.deepEqual(needsOf(gate, "ci-gate").sort(), [...validationJobs].sort());

    for (const workflow of VALIDATION_WORKFLOWS) {
        assert.ok(gate.includes(`uses: ./.github/workflows/${workflow}`), `${workflow} is not called`);
    }
});

test("ci-gate runs after failures and judges each conclusion explicitly", async () => {
    const gate = await readWorkflow(GATE_WORKFLOW);

    assert.match(gate, /^\s{4}if: always\(\)$/m);
    // always() 로 돌기만 하고 아무것도 검사하지 않으면 게이트가 항상 초록이 된다.
    assert.match(gate, /toJSON\(needs\)/);
    assert.match(gate, /\.value\.result != "success"/);
    assert.match(gate, /exit 1/);
});

test("stale runs are cancelled per pull request", async () => {
    const gate = await readWorkflow(GATE_WORKFLOW);

    assert.match(gate, /^concurrency:\n\s{2}group: pr-validation-\$\{\{ github\.event\.pull_request\.number \|\| github\.ref \}\}\n\s{2}cancel-in-progress: true$/m);
});

test("the entry point keeps no pull_request branch filter", async () => {
    // #683: base 변경(feat/* → develop)은 기본 activity type 에 없어 재트리거되지
    // 않는다. 필터가 있으면 스택 PR 이 검증을 한 번도 거치지 않고 머지된다.
    const gate = await readWorkflow(GATE_WORKFLOW);
    const trigger = /^on:\n\s{2}pull_request:\n([\s\S]*?)^\S/m.exec(gate)?.[1] ?? "";

    assert.doesNotMatch(trigger, /branches/);
});

test("repository quality still runs on develop and main pushes", async () => {
    const source = await readWorkflow("repository-quality.yml");

    assert.match(source, /^\s{2}push:\n\s{4}branches:\s*\[develop, main\]$/m);
});
