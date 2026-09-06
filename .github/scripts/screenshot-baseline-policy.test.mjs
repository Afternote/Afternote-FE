import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const verify = await readFile(new URL("../workflows/screenshot.yml", import.meta.url), "utf8");
const generate = await readFile(
    new URL("../workflows/screenshot-baseline-generate.yml", import.meta.url),
    "utf8",
);

const PARTITION_COMMAND = /ci-expected-failures\.mjs\s*\\?\s*\n?\s*partition-screenshot/;

test("both screenshot lanes partition against the same expected-failure source", () => {
    // 검증 lane 만 xfail 목록을 알고 생성 lane 이 모르면, core:ui 를 건드린 PR 은
    // 역의존으로 끌려온 남의 기대 실패 모듈에서 컴파일이 죽어 골든을 통째로 못 받는다
    // (#1448 — PR #1442 가 그렇게 막혔다). 한쪽만 고치면 이 테스트가 그 자리에서 깨진다.
    assert.match(verify, PARTITION_COMMAND);
    assert.match(generate, PARTITION_COMMAND);
});

test("the generate lane runs Gradle only on the partitioned normal tasks", () => {
    // 파티션을 부르고도 결과를 안 쓰면 아무것도 달라지지 않는다. update·validate 태스크가
    // impact 출력이 아니라 normal_tasks 에서 나오는지 본다.
    assert.match(generate, /NORMAL_TASKS: \$\{\{ steps\.expected-failures\.outputs\.normal_tasks \}\}/);
    assert.match(generate, /read -r -a normal <<< "\$NORMAL_TASKS"/);
    assert.match(generate, /update_tasks\+=\("\$\{module\}:updateScreenshotTest"\)/);
    assert.match(generate, /validate_tasks\+=\("\$\{module\}:validateScreenshotTest"\)/);

    // 파티션 이후 단계가 impact 의 원본 모듈 목록을 다시 집으면 제외가 무효가 된다.
    // 파티션 스텝 자신만 그 목록을 읽어야 한다.
    const screenshotModulesUses = [...generate.matchAll(/SCREENSHOT_MODULES: \$\{\{ steps\.impact\.outputs\.screenshot_modules \}\}/g)];
    assert.equal(
        screenshotModulesUses.length,
        1,
        "impact 의 원본 모듈 목록은 파티션 스텝에서만 읽어야 한다",
    );
});

test("the packaged baseline paths come from the same partition", () => {
    // 제외된 모듈의 reference 경로를 스테이징하면 골든을 만든 적 없는 자리를 담게 된다.
    assert.match(generate, /NORMAL_TASKS: \$\{\{ steps\.expected-failures\.outputs\.normal_tasks \}\}\n\s+TARGET_BRANCH:/);
    assert.match(generate, /module_path="\$\{task%:updateScreenshotTest\}"/);
});

test("excluded modules are reported instead of silently dropped", () => {
    // 조용히 빠지면 「내 모듈 골든이 왜 안 왔지」로 다시 헤맨다.
    assert.match(generate, /EXPECTED_TASKS: \$\{\{ steps\.expected-failures\.outputs\.expected_tasks \}\}/);
    assert.match(generate, /골든 생성에서 제외된 모듈/);
    assert.match(generate, /GITHUB_STEP_SUMMARY/);
});

test("a run that can generate nothing fails loudly", () => {
    // 영향 범위가 전부 기대 실패면 골든이 하나도 안 나온다. 초록으로 끝내면 라벨을 단
    // 사람은 생성이 끝난 줄 안다.
    assert.match(generate, /if \[ -z "\$NORMAL_TASKS" \]/);
    assert.match(generate, /::error::영향 범위의 screenshot 모듈이 전부 기대 실패 목록에 있어/);
});
