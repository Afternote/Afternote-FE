import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
    CONFIG_RELATIVE_PATH,
    loadExpectedFailures,
    partitionScreenshotTasks,
    planUnitProbes,
    reportScreenshotProbes,
    validateExpectedFailuresConfig,
} from "./ci-expected-failures.mjs";

const sampleConfig = {
    schemaVersion: 1,
    unitTests: [
        {
            task: ":feature:timeletter:data:testDebugUnitTest",
            className: "com.afternote.feature.timeletter.data.dto.TimeLetterDtoContractTest",
            tests: ["발신 응답의 명시적 null과 빈 배열은 정상 파싱된다"],
            issues: [790],
            reason: "계약 게이트",
        },
    ],
    screenshotModules: [
        { module: ":feature:setting:presentation", issues: [1360], reason: "Content 경계 분리 대기" },
    ],
};

test("저장소의 기대 실패 목록은 스키마를 지키고 추적 이슈를 가진다", async () => {
    const config = await loadExpectedFailures();

    for (const entry of [...(config.unitTests ?? []), ...(config.screenshotModules ?? [])]) {
        assert.ok(entry.issues.length > 0);
        assert.ok(entry.reason.length > 0);
    }
    assert.ok((config.unitTests?.length ?? 0) + (config.screenshotModules?.length ?? 0) > 0);
});

test("스키마 위반은 로드 시점에 거부된다", () => {
    assert.throws(() => validateExpectedFailuresConfig(null), /객체여야/);
    assert.throws(
        () => validateExpectedFailuresConfig({ unitTests: [{ ...sampleConfig.unitTests[0], task: ":feature:x:assemble" }] }),
        /testDebugUnitTest/,
    );
    assert.throws(
        () => validateExpectedFailuresConfig({ unitTests: [{ ...sampleConfig.unitTests[0], tests: [] }] }),
        /tests/,
    );
    assert.throws(
        () => validateExpectedFailuresConfig({ unitTests: [{ ...sampleConfig.unitTests[0], issues: [] }] }),
        /issues/,
    );
    assert.throws(
        () => validateExpectedFailuresConfig({ screenshotModules: [{ module: "feature:setting", issues: [1], reason: "r" }] }),
        /project path/,
    );
    assert.throws(
        () => validateExpectedFailuresConfig({ screenshotModules: [{ module: ":feature:setting:presentation", issues: [1], reason: " " }] }),
        /reason/,
    );
});

test("루트 kover aggregate 는 모든 unit 게이트를 probe 범위로 만든다", () => {
    const probes = planUnitProbes(":koverXmlReportCi :koverHtmlReportCi :konsist:test", sampleConfig);

    assert.equal(probes.length, 1);
    assert.equal(probes[0].task, ":feature:timeletter:data:testDebugUnitTest");
    assert.deepEqual(probes[0].filters, [
        "com.afternote.feature.timeletter.data.dto.TimeLetterDtoContractTest.발신 응답의 명시적 null과 빈 배열은 정상 파싱된다",
    ]);
});

test("게이트 모듈의 kover task 나 test task 자신도 probe 범위다", () => {
    assert.equal(planUnitProbes(":feature:timeletter:data:koverXmlReportCi", sampleConfig).length, 1);
    assert.equal(planUnitProbes(":feature:timeletter:data:testDebugUnitTest", sampleConfig).length, 1);
});

test("게이트 모듈이 실행 범위 밖이면 probe 하지 않는다", () => {
    assert.deepEqual(planUnitProbes(":core:common:koverXmlReportCi :konsist:test", sampleConfig), []);
    assert.deepEqual(planUnitProbes("", sampleConfig), []);
    // ':feature:timeletter:data-legacy' 처럼 접두어만 같은 다른 모듈에 오탐하지 않는다.
    assert.deepEqual(planUnitProbes(":feature:timeletter:data-legacy:testDebugUnitTest", sampleConfig), []);
});

test("screenshot task 는 기대 실패 모듈 lane 만 분리된다", () => {
    const { normal, expected } = partitionScreenshotTasks(
        ":core:ui:validateScreenshotTest :feature:setting:presentation:validateScreenshotTest :feature:home:presentation:validateScreenshotTest",
        sampleConfig,
    );

    assert.deepEqual(normal, [
        ":core:ui:validateScreenshotTest",
        ":feature:home:presentation:validateScreenshotTest",
    ]);
    assert.deepEqual(expected, [":feature:setting:presentation:validateScreenshotTest"]);
});

test("screenshot 판정: 실패 유지는 notice, XPASS 는 목록 제거를 요구하는 error", () => {
    const holding = reportScreenshotProbes(
        ":feature:setting:presentation:validateScreenshotTest",
        "",
        sampleConfig,
    );
    assert.equal(holding.failed, false);
    assert.equal(holding.lines.length, 1);
    assert.match(holding.lines[0], /^::notice::기대 실패 게이트 유지/);
    assert.match(holding.lines[0], /#1360/);

    const xpass = reportScreenshotProbes(
        ":feature:setting:presentation:validateScreenshotTest",
        ":feature:setting:presentation:validateScreenshotTest",
        sampleConfig,
    );
    assert.equal(xpass.failed, true);
    assert.match(xpass.lines[0], /^::error::/);
    assert.match(xpass.lines[0], new RegExp(CONFIG_RELATIVE_PATH.replaceAll(".", "\\.")));
});

test("screenshot 판정: 실행 안 된 게이트는 침묵하고, 목록 밖 XPASS 는 오류다", () => {
    const skipped = reportScreenshotProbes(":core:ui:validateScreenshotTest", "", sampleConfig);
    assert.deepEqual(skipped, { lines: [], failed: false });

    assert.throws(
        () => reportScreenshotProbes("", ":feature:setting:presentation:validateScreenshotTest", sampleConfig),
        /실행 목록에 없는 XPASS/,
    );
});

test("init script 는 같은 목록을 읽어 실패를 fail-closed 로 제외한다", async () => {
    const initScript = await readFile(new URL("../ci-expected-failures.init.gradle", import.meta.url), "utf8");

    assert.ok(initScript.includes(CONFIG_RELATIVE_PATH));
    assert.match(initScript, /throw new GradleException/);
    assert.match(initScript, /excludeTestsMatching/);
    assert.match(initScript, /probe-unit/);
});

test("unit test job 은 제외 init script 와 XPASS probe 를 함께 건다", async () => {
    const unitTest = await readFile(new URL("../workflows/unit-test.yml", import.meta.url), "utf8");

    assert.match(unitTest, /--init-script \.github\/ci-expected-failures\.init\.gradle/);
    assert.match(unitTest, /ci-expected-failures\.mjs probe-unit/);
});

test("screenshot job 은 기대 실패 lane 을 분리 실행하고 XPASS 를 판정한다", async () => {
    const screenshot = await readFile(new URL("../workflows/screenshot.yml", import.meta.url), "utf8");

    assert.match(screenshot, /ci-expected-failures\.mjs partition-screenshot/);
    assert.match(screenshot, /ci-expected-failures\.mjs report-screenshot/);
    assert.match(screenshot, /steps\.expected-failures\.outputs\.normal_tasks != ''/);
    assert.match(screenshot, /steps\.expected-failures\.outputs\.expected_tasks != ''/);
});

test("기대 실패 목록·스크립트 변경은 full validation 을 강제한다", async () => {
    const resolvePrImpact = await readFile(new URL("./resolve-pr-impact.mjs", import.meta.url), "utf8");

    for (const policyPath of [
        ".github/ci-expected-failures.json",
        ".github/ci-expected-failures.init.gradle",
        ".github/scripts/ci-expected-failures.mjs",
        ".github/scripts/ci-expected-failures.test.mjs",
    ]) {
        assert.ok(
            resolvePrImpact.includes(`"${policyPath}"`),
            `${policyPath} 가 IMPACT_POLICY_PATHS 에 없습니다`,
        );
    }
});
