import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const workflowDirectory = new URL("../workflows/", import.meta.url);

// Repository ruleset 21653076 (`required-checks (main)`) 의 required context 와 함께 바꿔야 하는
// 외부 계약이다. 잡 이름을 고치면 룰셋에서도 같이 고쳐야 main 게이트가 조용히 사라진다.
const REQUIRED_MAIN_GATE_CONTEXTS = [
    "Build and verify release AAB",
    "Check MOCK_CLEANUP TODOs",
];

async function workflow(name) {
    return readFile(new URL(name, workflowDirectory), "utf8");
}

function withoutComments(source) {
    return source
        .split("\n")
        .filter((line) => !/^\s*#/.test(line))
        .join("\n");
}

function triggerBlock(source) {
    const start = source.indexOf("on:\n");
    const end = source.indexOf("\npermissions:", start);
    assert.notEqual(start, -1, "on: 블록이 없다");
    assert.notEqual(end, -1, "permissions: 블록이 없다");
    return source.slice(start, end);
}

test("mock cleanup blocks the merge instead of reporting after it", async () => {
    const trigger = triggerBlock(withoutComments(await workflow("mock-cleanup-check.yml")));

    // #684 는 이 게이트를 push(main) 으로 옮겼다. 근거는 「main 이 직접 push 로 갱신된다」였고
    // 그 전제는 낡았다 — 지금 main 은 PR 로 갱신된다 (#1877). PR 에서 돌지 않으면 이미 main 이
    // 그 커밋을 가진 뒤에 red 를 내는 사후 경보가 된다.
    assert.match(
        trigger,
        /^\s{2}pull_request:\n\s{4}branches:\s*\[main\]$/m,
        "머지 전에 막으려면 main 대상 PR 에서 돌아야 한다",
    );

    // 룰셋을 우회한 직접 push 도 여전히 잡아야 한다.
    assert.match(
        trigger,
        /^\s{2}push:\n\s{4}branches:\s*\[main\]$/m,
        "직접 push 안전망을 잃으면 안 된다",
    );
});

test("mock cleanup never widens to develop", async () => {
    const trigger = triggerBlock(withoutComments(await workflow("mock-cleanup-check.yml")));

    // develop 에는 진행 중 마커가 상주할 수 있어, 넓히면 전 PR 이 상시 red 가 된다 (#684).
    assert.doesNotMatch(trigger, /develop/, "develop 으로 넓히는 것은 #684 가 오답으로 못 박았다");
});

test("mock cleanup fails closed on a marker", async () => {
    const source = await workflow("mock-cleanup-check.yml");

    // 경고만 내고 success 로 끝나던 것이 #684 의 결함 셋 중 하나였다.
    assert.match(source, /exit 1/, "마커를 찾으면 실패해야 게이트다");
    assert.doesNotMatch(source, /::warning::/, "경고로 되돌아가면 아무것도 막지 못한다");
});

test("mock cleanup excludes directories by name, not by path", async () => {
    const source = await workflow("mock-cleanup-check.yml");

    // `--exclude-dir` 은 경로가 아니라 디렉터리 이름 glob 을 받는다. `src/test` 처럼 경로를
    // 넘기면 아무것도 제외되지 않는다 (#684).
    for (const name of ["debug", "test", "androidTest"]) {
        assert.match(
            source,
            new RegExp(`--exclude-dir="${name}"`),
            `--exclude-dir 은 디렉터리 이름이어야 한다: ${name}`,
        );
    }
    assert.doesNotMatch(source, /--exclude-dir="[^"]*\//, "경로를 넘기면 제외가 무력해진다");
});

test("release AAB preflight keeps running on main pull requests", async () => {
    const trigger = triggerBlock(withoutComments(await workflow("release-aab-preflight.yml")));

    // 필수 체크로 승격하려면 main 대상 PR 에서 반드시 결과가 나와야 한다.
    assert.match(trigger, /^\s{2}pull_request:\n\s{4}branches:\s*\[main\]$/m);
});

test("both main gates keep the job names the ruleset requires", async () => {
    const sources = [
        await workflow("release-aab-preflight.yml"),
        await workflow("mock-cleanup-check.yml"),
    ].join("\n");

    for (const context of REQUIRED_MAIN_GATE_CONTEXTS) {
        assert.match(
            sources,
            new RegExp(`^\\s{4}name:\\s*${context}$`, "m"),
            `ruleset 21653076 이 요구하는 context 다 — 이름을 바꾸면 룰셋도 함께 고칠 것: ${context}`,
        );
    }
});
