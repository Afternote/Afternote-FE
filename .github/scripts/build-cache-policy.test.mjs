import assert from "node:assert/strict";
import { readdir, readFile } from "node:fs/promises";
import test from "node:test";

const workflowDirectory = new URL("../workflows/", import.meta.url);

async function workflows() {
    const names = (await readdir(workflowDirectory)).filter((name) => name.endsWith(".yml"));
    return Promise.all(names.map(async (name) => [name, await readFile(new URL(name, workflowDirectory), "utf8")]));
}

// 주석은 왜 그렇게 했는지를 적어 두는 자리라 액션 이름·플래그가 그대로 등장한다.
// 정책은 실제로 실행되는 줄에만 걸어야 한다.
function withoutComments(source) {
    return source
        .split("\n")
        .filter((line) => !/^\s*#/.test(line))
        .join("\n");
}

// Actions 캐시 엔트리는 (키, ref) 로 격리되고, 읽기는 «현재 ref → base → default branch»
// 순으로 매칭된다. 그래서 경쟁이 일어나는 범위는 develop 스코프 하나다 — release·canary
// 워크플로는 main 스코프에 쓰므로 develop PR 이 받는 엔트리를 가리지 않는다.
function runsOnDevelopPush(source) {
    return /^\s{2}push:\n\s{4}branches:\s*\[[^\]]*\bdevelop\b/m.test(source);
}

function runsOnPullRequests(source) {
    return /^on:\n(?:[^\n]*\n)*?\s{2}pull_request:/m.test(source);
}

test("exactly one workflow may write the shared Gradle User Home cache", async () => {
    // Actions 캐시는 같은 키를 덮어쓰지 못한다. writer 가 둘이면 먼저 끝난 쪽이 키를
    // 차지하고, build cache 를 채운 쪽의 저장이 조용히 버려진다 (#996).
    const writers = (await workflows())
        .filter(([, source]) => runsOnDevelopPush(source))
        .filter(([, source]) => /cache-read-only:\s*false/.test(withoutComments(source)))
        .map(([name]) => name);

    assert.deepEqual(writers, ["build-cache-warm.yml"]);
});

test("every other Gradle workflow reads the cache without writing to it", async () => {
    for (const [name, source] of await workflows()) {
        if (name === "build-cache-warm.yml" || !/uses:\s*gradle\/actions\/setup-gradle@/.test(source)) {
            continue;
        }
        if (!runsOnPullRequests(source) && !runsOnDevelopPush(source)) {
            continue;
        }
        assert.match(source, /cache-read-only:\s*true/, `${name} must not write to the shared cache`);
        // 조건식으로 쓰면 pull_request 가 아닌 trigger 에서 writer 로 돌변한다.
        assert.doesNotMatch(
            source,
            /cache-read-only:\s*\$\{\{/,
            `${name} must not decide cache writability from the event`,
        );
    }
});

test("the warming workflow runs on develop pushes and is never cancelled mid-run", async () => {
    const source = await readFile(new URL("build-cache-warm.yml", workflowDirectory), "utf8");

    assert.match(source, /push:\n\s+branches:\s*\[develop\]/);
    // 캐시는 job 의 post 스텝에서 저장된다. 중도 취소되면 그 스텝에 닿지 못한다.
    assert.match(source, /cancel-in-progress:\s*false/);
    assert.match(source, /--build-cache/);
});

test("the warming workflow covers the tasks pull request jobs actually run", async () => {
    const warm = await readFile(new URL("build-cache-warm.yml", workflowDirectory), "utf8");
    const lint = await readFile(new URL("lint.yml", workflowDirectory), "utf8");
    const unitTest = await readFile(new URL("unit-test.yml", workflowDirectory), "utf8");

    for (const task of ["ktlintCheck", "lintDebug"]) {
        assert.match(lint, new RegExp(`\\./gradlew[^\\n]*${task}`), `lint.yml no longer runs ${task}`);
        assert.match(warm, new RegExp(`\\b${task}\\b`), `warming misses ${task}`);
    }
    for (const task of [":koverXmlReportCi", ":konsist:test", ":app:compileDebugAndroidTestKotlin"]) {
        assert.ok(unitTest.includes(task), `unit-test.yml no longer runs ${task}`);
        assert.ok(warm.includes(task), `warming misses ${task}`);
    }
});

test("CodeQL stays out of the build cache in both directions", async () => {
    const source = await readFile(new URL("codeql.yml", workflowDirectory), "utf8");

    // 추출기가 컴파일러 호출을 관찰해야 하므로 FROM-CACHE 로 넘어가면 안 된다 (#1014).
    assert.doesNotMatch(withoutComments(source), /--build-cache/);
    assert.match(source, /cache-read-only:\s*true/);
});

test("the local Gradle default stays uncached", async () => {
    // #996 은 CI 한정이다. 로컬은 손상된 cache 엔트리 사례 때문에 여전히 꺼 둔다.
    const properties = await readFile(new URL("../../gradle.properties", import.meta.url), "utf8");

    assert.match(properties, /^org\.gradle\.caching=false$/m);
});
