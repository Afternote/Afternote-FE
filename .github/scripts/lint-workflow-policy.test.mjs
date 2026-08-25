import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const lintWorkflow = await readFile(new URL("../workflows/lint.yml", import.meta.url), "utf8");

test("ktlint runs exactly once per pull request", () => {
    // ktlint 를 실제로 돌리는 자리는 Gradle 태스크 하나뿐이다. 검사를 한 번 더 하는 액션이
    // 끼면(과거 ScaCap/action-ktlint) 같은 규칙을 두 번 돌리며 job 의 30% 를 쓴다 (#1012).
    const gradleInvocations = lintWorkflow.match(/^\s*run:.*ktlintCheck/gm) ?? [];
    assert.equal(gradleInvocations.length, 1);
    // 주석은 이력이라 남기고, 실제로 액션을 부르는 uses 줄만 금지한다.
    assert.doesNotMatch(lintWorkflow, /^\s*uses:\s*ScaCap\/action-ktlint/m);
});

test("the ktlint binary version has a single source of truth", () => {
    // 워크플로가 버전을 손으로 적으면 libs.versions.toml 과 표류해 두 검사 결과가 갈린다.
    assert.doesNotMatch(lintWorkflow, /ktlint_version/);
});

test("ktlint still checks the whole repository, not the pull request diff", () => {
    // #875: 파일을 지우면서 남은 파일에 새로 발생하는 파일 단위 위반은 diff 필터로는
    // 보이지 않는다. 저장소 전체를 검사하는 성질이 사라지면 그 회귀가 되돌아온다.
    assert.match(lintWorkflow, /\.\/gradlew ktlintCheck[^\n]*--continue/);
    assert.doesNotMatch(lintWorkflow, /filter_mode/);
});

test("ktlint violations are rendered into the job summary even when the check fails", () => {
    assert.match(lintWorkflow, /render-ktlint-summary\.mjs/);
    const summaryStep = /- name: Summarize ktlint violations\n\s+if: always\(\)/;
    assert.match(lintWorkflow, summaryStep);
});

test("Android Lint keeps its inline reporting — it reuses the XML instead of re-running", () => {
    // #279 가 푼 문제(모듈별 위반이 수만 줄 로그에 묻힘)를 되돌리지 않는다. lintDebug 가
    // 만든 XML 을 재활용하므로 검사가 두 번 돌지 않는다.
    assert.match(lintWorkflow, /lint_xml_file:\s*app\/build\/reports\/lint-results-merged\.xml/);
    assert.match(lintWorkflow, /\.\/gradlew lintDebug/);
});
