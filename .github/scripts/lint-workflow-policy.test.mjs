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

test("Android Lint runs exactly once per pull request", () => {
    const gradleInvocations = lintWorkflow.match(/^\s*run:.*lintDebug/gm) ?? [];
    assert.equal(gradleInvocations.length, 1);
    assert.match(lintWorkflow, /\.\/gradlew lintDebug[^\n]*--continue/);
});

test("Android Lint reports through the job summary, not a Docker reviewdog action", () => {
    // #279 가 푼 문제(모듈별 위반이 수만 줄 로그에 묻힘)를 되돌리지 않는다 — 자리만 PR
    // 인라인 코멘트에서 job summary 로 옮겼다. 그 도커 액션은 매 run 마다 reviewdog 을
    // 네트워크에서 받아 설치했고, 설치가 조용히 실패하면 lint 위반이 0건이어도 required
    // check 가 red 가 됐다. 이미지는 job 당 한 번만 빌드돼 재시도로도 풀리지 않는다 (#1093).
    assert.match(lintWorkflow, /render-android-lint-summary\.mjs/);
    assert.match(lintWorkflow, /- name: Summarize Android Lint violations\n\s+if: always\(\)/);
    // 주석은 이력이라 남기고, 실제로 액션을 부르는 uses 줄만 금지한다.
    assert.doesNotMatch(lintWorkflow, /^\s*uses:\s*dvdandroid\/action-android-lint/m);
});
