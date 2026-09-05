import assert from "node:assert/strict";
import { readdir, readFile } from "node:fs/promises";
import test from "node:test";

import {
    CONFIG_RELATIVE_PATH,
    evaluateAndroidTestResults,
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
    androidTests: [
        {
            className: "com.afternote.afternote_fe.SampleAndroidTest",
            tests: ["flakyScenario"],
            issues: [1439],
            reason: "계측 게이트",
        },
    ],
    screenshotModules: [
        { module: ":feature:setting:presentation", issues: [1360], reason: "Content 경계 분리 대기" },
    ],
};

const workflowDirectory = new URL("../workflows/", import.meta.url);
const INIT_SCRIPT_FLAG = "--init-script .github/ci-expected-failures.init.gradle";

// 주석은 왜 그렇게 했는지를 적어 두는 자리라 태스크 이름과 플래그가 그대로 등장한다.
// 정책은 실제로 실행되는 줄에만 걸어야 한다.
function withoutComments(source) {
    return source
        .split("\n")
        .filter((line) => !/^\s*#/.test(line))
        .join("\n");
}

// 플래그는 «같은 스텝» 에 걸려 있어야 한다 — 파일 전체에서 문자열만 찾으면 다른 스텝에
// 걸린 플래그로 통과한다.
function steps(source) {
    return withoutComments(source).split(/^ {6}- (?=\w[\w-]*:)/m);
}

// Gradle 태스크 토큰만 본다. 토큰 전체를 대조해야 `--tests` 같은 플래그나
// `testRuntimeClasspath` 같은 configuration 이름에 오탐하지 않는다.
const UNIT_TEST_TASK =
    /^(?:(?::[\w-]+)*:(?:test|testDebugUnitTest|koverXmlReportCi|koverHtmlReportCi)|testDebugUnitTest)$/;

function runsUnitTests(step) {
    return step.includes("./gradlew")
        && step.split(/[\s'"\\]+/).some((token) => UNIT_TEST_TASK.test(token));
}

const androidCase = (name, status) => ({
    className: "com.afternote.afternote_fe.SampleAndroidTest",
    name,
    status,
});

test("저장소의 기대 실패 목록은 스키마를 지키고 추적 이슈를 가진다", async () => {
    const config = await loadExpectedFailures();

    for (const entry of [
        ...(config.unitTests ?? []),
        ...(config.androidTests ?? []),
        ...(config.screenshotModules ?? []),
    ]) {
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

test("gradlew 로 unit test 를 돌리는 워크플로 스텝은 모두 제외 init script 를 건다", async () => {
    // #1653: dependency-audit.yml 하나가 이 규약에서 빠져 있어, develop 에 상존하는 의도된
    // 실패가 주간 감사만 깨뜨리고 매주 같은 이슈를 새로 열었다. 규약을 탄 워크플로 목록을
    // 손으로 적어 두면 다음 워크플로가 또 샌다 — 워크플로 전량을 훑어 «unit test 태스크를
    // 돌리는데 플래그가 없는» 스텝을 직접 찾는다.
    const names = (await readdir(workflowDirectory)).filter((name) => name.endsWith(".yml"));
    const offenders = [];
    let detected = 0;

    for (const name of names) {
        const source = await readFile(new URL(name, workflowDirectory), "utf8");
        for (const step of steps(source)) {
            if (!runsUnitTests(step)) continue;
            detected += 1;
            if (!step.includes(INIT_SCRIPT_FLAG)) {
                offenders.push(`${name} (${step.split("\n")[0].trim()})`);
            }
        }
    }

    assert.deepEqual(offenders, [], `제외 init script 가 없는 unit test 스텝: ${offenders.join(", ")}`);
    // 태스크 토큰 판정이 무너지면 검사 대상 0건으로도 통과한다 — 탐지 자체를 단언한다.
    // 태스크를 변수로 넘기는 unit-test.yml 은 여기서 잡히지 않고 아래 전용 테스트가 잡는다.
    assert.ok(detected >= 3, `unit test 스텝 탐지가 무너졌습니다: ${detected}건`);
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

test("androidTest 스키마: FQCN·테스트 목록·추적 이슈가 강제된다", () => {
    assert.throws(
        () => validateExpectedFailuresConfig({ androidTests: [{ ...sampleConfig.androidTests[0], className: "not a class" }] }),
        /FQCN/,
    );
    assert.throws(
        () => validateExpectedFailuresConfig({ androidTests: [{ ...sampleConfig.androidTests[0], tests: [] }] }),
        /비어 있지 않은 배열/,
    );
    assert.throws(
        () => validateExpectedFailuresConfig({ androidTests: [{ ...sampleConfig.androidTests[0], issues: [] }] }),
        /추적 이슈/,
    );
    assert.throws(
        () => validateExpectedFailuresConfig({ androidTests: [{ ...sampleConfig.androidTests[0], devices: [] }] }),
        /비어 있지 않은 배열/,
    );
    assert.doesNotThrow(() => validateExpectedFailuresConfig(sampleConfig));
});

test("androidTest 판정: 등재된 실패는 흡수하고 목록 밖 실패는 red 다", () => {
    const absorbed = evaluateAndroidTestResults([androidCase("flakyScenario", "failure")], sampleConfig, "api30");
    assert.equal(absorbed.failed, false);
    assert.deepEqual(absorbed.absorbed, ["com.afternote.afternote_fe.SampleAndroidTest#flakyScenario"]);
    assert.match(absorbed.lines[0], /기대 실패 게이트 유지/);

    const regression = evaluateAndroidTestResults(
        [{ className: "com.afternote.afternote_fe.OtherAndroidTest", name: "newlyBroken", status: "failure" }],
        sampleConfig,
        "api30",
    );
    assert.equal(regression.failed, true);
    assert.deepEqual(regression.unexpected, ["com.afternote.afternote_fe.OtherAndroidTest#newlyBroken"]);
    assert.match(regression.lines[0], /목록에 없는 androidTest 실패/);
});

test("androidTest 판정: XPASS 는 목록 제거를 요구하는 red 다", () => {
    const verdict = evaluateAndroidTestResults([androidCase("flakyScenario", "passed")], sampleConfig, "api30");

    assert.equal(verdict.failed, true);
    assert.deepEqual(verdict.xpassed, ["com.afternote.afternote_fe.SampleAndroidTest#flakyScenario"]);
    assert.match(verdict.lines[0], new RegExp(CONFIG_RELATIVE_PATH.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
});

test("androidTest 판정: 실행되지 않은 게이트와 건너뛴 테스트는 침묵한다", () => {
    assert.equal(evaluateAndroidTestResults([], sampleConfig, "api30").failed, false);
    assert.equal(evaluateAndroidTestResults([], sampleConfig, "api30").lines.length, 0);

    const skipped = evaluateAndroidTestResults([androidCase("flakyScenario", "skipped")], sampleConfig, "api30");
    assert.equal(skipped.failed, false);
    assert.equal(skipped.absorbed.length, 0);
    assert.equal(skipped.xpassed.length, 0);
});

test("androidTest 판정: devices 를 적으면 그 lane 에서만 흡수한다", () => {
    const scoped = {
        androidTests: [{ ...sampleConfig.androidTests[0], devices: ["api36"] }],
    };

    assert.equal(evaluateAndroidTestResults([androidCase("flakyScenario", "failure")], scoped, "api36").failed, false);
    assert.equal(evaluateAndroidTestResults([androidCase("flakyScenario", "failure")], scoped, "api30").failed, true);
});

test("managed-device job 은 목록을 staging 해 실행 결과를 판정하고 기대 실패만 흡수한다", async () => {
    const workflow = await readFile(new URL("../workflows/android-managed-device.yml", import.meta.url), "utf8");

    assert.match(workflow, /ci-expected-failures\.mjs" \\\n\s+report-android/);
    assert.match(workflow, /expected_failures=trusted/);
    // 도입 PR 이 명령 없는 default branch 사본을 실행해 스스로 red 가 되지 않게, 파일이
    // 아니라 명령의 존재로 가른다.
    assert.match(workflow, /grep -q '"report-android"' \.github\/scripts\/ci-expected-failures\.mjs/);
    assert.match(workflow, /expected_failures=unavailable/);
    assert.match(workflow, /steps\.policy\.outputs\.expected_failures == 'trusted'/);
    assert.match(workflow, /EXPECTED_FAILURES_ROOT/);
    // 목록 밖 실패·XPASS 는 Gradle 이 통과했더라도 red 여야 한다.
    assert.match(workflow, /EXPECTED_FAILURES_EXIT_CODE" != "0"/);
    // 흡수는 기대 실패가 실제로 잡혔을 때만 — 인프라 실패를 삼키지 않는다.
    assert.match(workflow, /EXPECTED_ABSORBED:-0\}" != "0"/);
});
