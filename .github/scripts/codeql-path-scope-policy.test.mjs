import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

import { inspectModules } from "./resolve-pr-impact.mjs";

const workflowUrl = new URL("../workflows/codeql.yml", import.meta.url);
const repoRoot = path.resolve(fileURLToPath(new URL("../..", import.meta.url)));

function jobBlock(source, jobName, nextJobName) {
    const start = source.indexOf(`  ${jobName}:`);
    assert.notEqual(start, -1, `${jobName} job is missing`);
    const end = nextJobName === undefined ? source.length : source.indexOf(`  ${nextJobName}:`, start + 1);
    assert.notEqual(end, -1, `${nextJobName} job is missing`);
    return source.slice(start, end);
}

test("CodeQL keeps its pull request trigger unfiltered so required checks cannot stay pending", async () => {
    const source = await readFile(workflowUrl, "utf8");
    const trigger = /^on:\n\s{2}pull_request:\n([\s\S]*?)^\s{2}push:/m.exec(source)?.[1] ?? "";

    assert.doesNotMatch(trigger, /paths(?:-ignore)?:/);
    assert.match(source, /^\s{2}merge_group:\n\s{4}types: \[checks_requested\]$/m);
    assert.match(source, /^\s{2}workflow_dispatch:\n\s{4}inputs:\n\s{6}pull_request_number:/m);
});

test("CodeQL classifies every PR file by language and defaults to full analysis", async () => {
    const source = await readFile(workflowUrl, "utf8");
    const classifier = jobBlock(source, "classify-changes", "analyze-actions");

    assert.match(classifier, /gh api --paginate --slurp/);
    assert.match(classifier, /classify-documentation-changes\.mjs/);
    assert.match(classifier, /docs_only=.*classify-documentation-changes\.mjs/s);
    assert.match(classifier, /resolve-pr-impact\.mjs/);
    assert.match(classifier, /actions_required: \$\{\{ steps\.path-scope\.outputs\.codeql_actions \|\| 'true' \}\}/);
    assert.match(classifier, /javascript_typescript_required: \$\{\{ steps\.path-scope\.outputs\.codeql_javascript_typescript \|\| 'true' \}\}/);
    assert.match(classifier, /java_kotlin_required: \$\{\{ steps\.path-scope\.outputs\.codeql_java_kotlin \|\| 'true' \}\}/);
    assert.match(classifier, /codeql_javascript_typescript=false/);
    assert.match(classifier, /codeql_javascript_typescript=true/);
    assert.match(classifier, /if \[ "\$GITHUB_EVENT_NAME" = "workflow_dispatch" \]; then/);
    assert.match(classifier, /if \[ "\$GITHUB_SHA" != "\$head_sha" \]; then/);
    assert.match(classifier, /permissions:\n\s+contents: read\n\s+pull-requests: read/);
    assert.doesNotMatch(classifier, /security-events: write/);
    assert.match(classifier, /persist-credentials: false/);
    assert.match(classifier, /env -u GH_TOKEN -u GITHUB_TOKEN/);
});

test("CodeQL preserves all required context names and skips only an unaffected language", async () => {
    const source = await readFile(workflowUrl, "utf8");
    const actions = jobBlock(source, "analyze-actions", "analyze-javascript-typescript");
    const javascript = jobBlock(source, "analyze-javascript-typescript", "analyze-java-kotlin");
    const kotlin = jobBlock(source, "analyze-java-kotlin");
    const actionsFailClosed = /if: \$\{\{ !cancelled\(\) && \(needs\.classify-changes\.result != 'success' \|\| needs\.classify-changes\.outputs\.actions_required != 'false'\) \}\}/;
    const javascriptFailClosed = /if: \$\{\{ !cancelled\(\) && \(needs\.classify-changes\.result != 'success' \|\| needs\.classify-changes\.outputs\.javascript_typescript_required != 'false'\) \}\}/;
    const kotlinFailClosed = /if: \$\{\{ !cancelled\(\) && \(needs\.classify-changes\.result != 'success' \|\| needs\.classify-changes\.outputs\.java_kotlin_required != 'false'\) \}\}/;

    assert.match(actions, /^\s{4}name: Analyze \(actions\)$/m);
    assert.match(javascript, /^\s{4}name: Analyze \(javascript-typescript\)$/m);
    assert.match(kotlin, /^\s{4}name: Analyze \(java-kotlin\)$/m);
    assert.match(actions, /^\s{4}needs: classify-changes$/m);
    assert.match(javascript, /^\s{4}needs: classify-changes$/m);
    assert.match(kotlin, /^\s{4}needs: classify-changes$/m);
    assert.match(actions, actionsFailClosed);
    assert.match(javascript, javascriptFailClosed);
    assert.match(kotlin, kotlinFailClosed);
    assert.match(actions, /security-events: write/);
    assert.match(javascript, /security-events: write/);
    assert.match(kotlin, /security-events: write/);
    assert.match(javascript, /languages: javascript-typescript/);
    assert.match(javascript, /build-mode: none/);
    assert.match(javascript, /category: \/language:javascript-typescript/);
    assert.match(javascript, /CODEQL_DATABASE_LANGUAGE: javascript/);
    assert.match(javascript, /CODEQL_REQUIRED_PATH_FRAGMENT: \.github\/scripts\//);
    assert.doesNotMatch(source, /^\s{4}strategy:/m);
});

test("CodeQL compiles every domain module with the task for its current platform", async () => {
    const source = await readFile(workflowUrl, "utf8");
    const kotlin = jobBlock(source, "analyze-java-kotlin");
    // 모듈 목록을 여기 손으로 적으면 규약 마이그레이션 때 워크플로와 갈린다. 실제로 #1151 이
    // domain 3모듈을 JVM 으로 옮겼을 때 codeql.yml 은 그대로여서 «task not found» 로 죽었다.
    // 빌드 스크립트에서 판정해, 규약이 바뀌면 워크플로도 따라오도록 강제한다.
    const modules = await inspectModules(repoRoot);
    const domainModules = modules.filter(
        ({ projectPath }) => projectPath.endsWith(":domain") || projectPath === ":core:model",
    );
    assert.ok(domainModules.length > 0, "domain 모듈을 하나도 못 찾았다 — 판정이 망가졌다");

    assert.match(kotlin, /\.\/gradlew compileDebugSources/);
    domainModules.forEach(({ projectPath, android }) => {
        // android 규약을 타면 classes 만으로는 Kotlin 컴파일러가 안 돌아 추출에서 빠진다.
        const task = android ? `${projectPath}:compileDebugKotlin` : `${projectPath}:classes`;
        assert.ok(kotlin.includes(task), `${task} is missing from the CodeQL build`);
    });
});
