#!/usr/bin/env node

// develop 에 의도적으로 남긴 red 게이트(.github/ci-expected-failures.json)를 xfail 의미론으로
// 다룬다. 기대 실패는 required check 를 빨갛게 만들지 않고, 통과하기 시작하면(XPASS) 목록
// 제거를 요구하며 job 을 실패시킨다 — 게이트 해제는 담당 구현 머지와 목록 정리로만 가능하다.
//
// 명령:
//   partition-screenshot "<tasks>"        기대 실패 모듈 lane 을 분리해 GITHUB_OUTPUT 형식으로 출력
//   probe-unit "<tasks>"                  실행 범위에 걸린 기대 실패 unit test 만 다시 돌려 XPASS 감시
//   report-screenshot "<ran>" "<xpassed>" screenshot 기대 실패 lane 실행 결과를 판정
//   report-android "<결과 디렉터리>" "<device>"  androidTest 실행 결과 XML 을 목록과 대조해 판정

import { spawnSync } from "node:child_process";
import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { fileURLToPath, pathToFileURL } from "node:url";

export const CONFIG_RELATIVE_PATH = ".github/ci-expected-failures.json";

// 레포에서는 스크립트 위치로 루트를 잡는다. managed-device 워크플로는 이 파일을
// default branch 사본으로 staging 해 쓰므로(신뢰 경계) 그때만 목록 루트를 env 로 준다.
const repositoryRoot = process.env.EXPECTED_FAILURES_ROOT
    ?? path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");

function requireNonEmptyString(value, name) {
    if (typeof value !== "string" || value.trim() === "") {
        throw new Error(`${name} 값이 비어 있습니다.`);
    }
    return value;
}

export function validateExpectedFailuresConfig(config) {
    if (!config || typeof config !== "object" || Array.isArray(config)) {
        throw new Error("기대 실패 목록 최상위 값은 객체여야 합니다.");
    }
    for (const [index, entry] of (config.unitTests ?? []).entries()) {
        const label = `unitTests[${index}]`;
        requireNonEmptyString(entry?.task, `${label}.task`);
        if (!/^(?::[\w-]+)+:testDebugUnitTest$/.test(entry.task)) {
            throw new Error(`${label}.task 는 ':모듈:testDebugUnitTest' 경로여야 합니다: ${entry.task}`);
        }
        requireNonEmptyString(entry?.className, `${label}.className`);
        if (!Array.isArray(entry?.tests) || entry.tests.length === 0) {
            throw new Error(`${label}.tests 는 비어 있지 않은 배열이어야 합니다.`);
        }
        entry.tests.forEach((testName, testIndex) =>
            requireNonEmptyString(testName, `${label}.tests[${testIndex}]`));
        validateIssues(entry, label);
    }
    for (const [index, entry] of (config.androidTests ?? []).entries()) {
        const label = `androidTests[${index}]`;
        requireNonEmptyString(entry?.className, `${label}.className`);
        if (!/^[\w.]+$/.test(entry.className)) {
            throw new Error(`${label}.className 은 FQCN 이어야 합니다: ${entry.className}`);
        }
        if (!Array.isArray(entry?.tests) || entry.tests.length === 0) {
            throw new Error(`${label}.tests 는 비어 있지 않은 배열이어야 합니다.`);
        }
        entry.tests.forEach((testName, testIndex) =>
            requireNonEmptyString(testName, `${label}.tests[${testIndex}]`));
        // device 는 선택 — 생략하면 모든 managed device lane 에서 기대 실패로 본다.
        if (entry.devices !== undefined) {
            if (!Array.isArray(entry.devices) || entry.devices.length === 0) {
                throw new Error(`${label}.devices 는 비어 있지 않은 배열이어야 합니다.`);
            }
            entry.devices.forEach((device, deviceIndex) =>
                requireNonEmptyString(device, `${label}.devices[${deviceIndex}]`));
        }
        validateIssues(entry, label);
    }
    for (const [index, entry] of (config.screenshotModules ?? []).entries()) {
        const label = `screenshotModules[${index}]`;
        requireNonEmptyString(entry?.module, `${label}.module`);
        if (!/^(?::[\w-]+)+$/.test(entry.module)) {
            throw new Error(`${label}.module 은 ':a:b' 형식 project path 여야 합니다: ${entry.module}`);
        }
        validateIssues(entry, label);
    }
    return config;
}

function validateIssues(entry, label) {
    if (!Array.isArray(entry?.issues) || entry.issues.length === 0) {
        throw new Error(`${label}.issues 에 추적 이슈 번호가 하나 이상 필요합니다.`);
    }
    for (const issue of entry.issues) {
        if (!Number.isInteger(issue) || issue <= 0) {
            throw new Error(`${label}.issues 값은 양의 정수여야 합니다: ${issue}`);
        }
    }
    requireNonEmptyString(entry?.reason, `${label}.reason`);
}

export async function loadExpectedFailures(root = repositoryRoot) {
    const raw = await fs.readFile(path.join(root, CONFIG_RELATIVE_PATH), "utf8");
    return validateExpectedFailuresConfig(JSON.parse(raw));
}

function tokenize(tasksString) {
    return String(tasksString ?? "").trim().split(/\s+/).filter(Boolean);
}

function issueReferences(issues) {
    return issues.map((issue) => `#${issue}`).join(" ");
}

// 실행 범위 판정: 기대 실패 task 자신, 같은 모듈의 다른 task(예: 모듈 kover), 또는 전 모듈
// 테스트를 끌고 들어오는 루트 kover aggregate 가 목록에 있으면 그 게이트는 이번 run 에서
// 실행됐을 것이므로 XPASS 감시 대상이다.
export function planUnitProbes(tasksString, config) {
    const tokens = tokenize(tasksString);
    return (config.unitTests ?? [])
        .filter((entry) => {
            const modulePath = entry.task.slice(0, entry.task.lastIndexOf(":"));
            return tokens.some(
                (token) =>
                    token === entry.task ||
                    token.startsWith(`${modulePath}:`) ||
                    token === ":koverXmlReportCi" ||
                    token === ":koverHtmlReportCi",
            );
        })
        .map((entry) => ({
            task: entry.task,
            filters: entry.tests.map((testName) => `${entry.className}.${testName}`),
            issues: entry.issues,
            reason: entry.reason,
        }));
}

export function partitionScreenshotTasks(tasksString, config) {
    const expectedModules = new Set((config.screenshotModules ?? []).map((entry) => entry.module));
    const normal = [];
    const expected = [];
    for (const token of tokenize(tasksString)) {
        const modulePath = token.slice(0, token.lastIndexOf(":"));
        (expectedModules.has(modulePath) ? expected : normal).push(token);
    }
    return { normal, expected };
}

export function reportScreenshotProbes(ranTasksString, xpassedTasksString, config) {
    const ran = new Set(tokenize(ranTasksString));
    const xpassed = new Set(tokenize(xpassedTasksString));
    const unknownXpass = [...xpassed].filter((task) => !ran.has(task));
    if (unknownXpass.length > 0) {
        throw new Error(`실행 목록에 없는 XPASS task: ${unknownXpass.join(" ")}`);
    }

    const lines = [];
    let failed = false;
    for (const entry of config.screenshotModules ?? []) {
        const task = `${entry.module}:validateScreenshotTest`;
        if (!ran.has(task)) continue;
        if (xpassed.has(task)) {
            failed = true;
            lines.push(
                `::error::기대 실패로 등록된 screenshot lane 이 통과합니다 — ${CONFIG_RELATIVE_PATH} 에서 ${entry.module} 항목을 제거하세요 (${issueReferences(entry.issues)}).`,
            );
        } else {
            lines.push(
                `::notice::기대 실패 게이트 유지: ${task} — ${entry.reason} (${issueReferences(entry.issues)})`,
            );
        }
    }
    return { lines, failed };
}

// androidTest 는 unit test 와 달리 개별 재실행 비용이 에뮬레이터 부팅을 포함해 크다.
// 그래서 「다시 돌려 XPASS 를 본다」가 아니라 이미 나온 결과 XML 을 목록과 대조한다.
// 판정은 세 갈래다.
//   목록에 있고 실패     기대 실패 — 흡수한다(notice).
//   목록에 없고 실패     새 회귀 — red.
//   목록에 있고 통과     XPASS — 목록에서 지우라고 요구하며 red(#790·#873 운용과 동일).
// 목록에 있는데 아예 실행되지 않은 항목은 침묵한다 — lane·selector 가 그 테스트를
// 고르지 않았을 뿐이고, 실행 안 된 것을 근거로 게이트를 해제하지 않는다.
export function evaluateAndroidTestResults(testcases, config, device) {
    const applies = (entry) =>
        entry.devices === undefined || entry.devices.includes(device);
    const expected = new Map();
    for (const entry of config.androidTests ?? []) {
        if (!applies(entry)) continue;
        for (const testName of entry.tests) {
            expected.set(`${entry.className}#${testName}`, entry);
        }
    }

    const absorbed = [];
    const unexpected = [];
    const xpassed = [];
    for (const testcase of testcases) {
        const selector = `${testcase.className}#${testcase.name}`;
        const entry = expected.get(selector);
        const isFailure = testcase.status === "failure" || testcase.status === "error";
        if (entry === undefined) {
            if (isFailure) unexpected.push(selector);
            continue;
        }
        if (isFailure) {
            absorbed.push({ selector, entry });
        } else if (testcase.status === "passed") {
            xpassed.push({ selector, entry });
        }
    }

    const lines = [];
    for (const { selector, entry } of absorbed) {
        lines.push(
            `::notice::기대 실패 게이트 유지: ${selector} — ${entry.reason} (${issueReferences(entry.issues)})`,
        );
    }
    for (const { selector, entry } of xpassed) {
        lines.push(
            `::error::기대 실패로 등록된 androidTest 가 통과합니다 — ${CONFIG_RELATIVE_PATH} 에서 ${selector} 를 제거하세요 (${issueReferences(entry.issues)}).`,
        );
    }
    for (const selector of unexpected) {
        lines.push(
            `::error::기대 실패 목록에 없는 androidTest 실패: ${selector} — 수복하거나 추적 이슈와 함께 ${CONFIG_RELATIVE_PATH} 에 등재하세요.`,
        );
    }
    return {
        lines,
        absorbed: absorbed.map(({ selector }) => selector),
        unexpected,
        xpassed: xpassed.map(({ selector }) => selector),
        failed: unexpected.length > 0 || xpassed.length > 0,
    };
}

async function reportAndroidMain(resultsRoot, device, root) {
    const config = await loadExpectedFailures(root);
    // XML 파서는 renderer 가 이미 소유하고 있다. 같은 판정을 두 벌 두지 않으려고
    // 명령이 불릴 때만 동적으로 끌어 쓴다 — 다른 명령들은 이 의존이 없다.
    const renderer = await import(
        pathToFileURL(path.join(path.dirname(fileURLToPath(import.meta.url)), "render-android-test-results.mjs")).href
    );
    const files = await renderer.collectXmlFiles(resultsRoot);
    const testcases = [];
    for (const file of files) {
        const xml = await fs.readFile(file, "utf8");
        testcases.push(...renderer.parseAndroidTestXml(xml, { file }).testcases);
    }
    if (files.length === 0) {
        // 실행 증거가 없으면 판정하지 않는다 — 「실패 0건」 으로 오인해 통과시키지 않기 위해
        // 호출 측(워크플로)이 Gradle 종료 코드를 그대로 복원하도록 침묵한다.
        console.log("androidTest 결과 XML 이 없어 기대 실패 판정을 건너뜁니다.");
        return;
    }

    const verdict = evaluateAndroidTestResults(testcases, config, device);
    verdict.lines.forEach((line) => console.log(line));
    if (process.env.GITHUB_OUTPUT) {
        await fs.appendFile(
            process.env.GITHUB_OUTPUT,
            `absorbed=${verdict.absorbed.length}\nunexpected=${verdict.unexpected.length}\nxpassed=${verdict.xpassed.length}\n`,
        );
    }
    if (verdict.failed) {
        process.exitCode = 1;
    }
}

function runGradle(args, root) {
    return spawnSync(path.join(root, "gradlew"), args, {
        cwd: root,
        stdio: "inherit",
    });
}

async function probeUnitMain(tasksString, root) {
    const config = await loadExpectedFailures(root);
    const probes = planUnitProbes(tasksString, config);
    if (probes.length === 0) {
        console.log("기대 실패 unit 게이트가 이번 실행 범위에 없습니다.");
        return;
    }

    let xpassDetected = false;
    for (const probe of probes) {
        const args = [probe.task, ...probe.filters.flatMap((filter) => ["--tests", filter]), "--build-cache"];
        const result = runGradle(args, root);
        if (result.error) {
            throw result.error;
        }
        if (result.status === 0) {
            xpassDetected = true;
            console.log(
                `::error::기대 실패로 등록된 테스트가 모두 통과합니다 — ${CONFIG_RELATIVE_PATH} 에서 ${probe.task} 항목을 제거하세요 (${issueReferences(probe.issues)}).`,
            );
        } else {
            console.log(
                `::notice::기대 실패 게이트 유지: ${probe.task} ${probe.filters.length}건 — ${probe.reason} (${issueReferences(probe.issues)})`,
            );
        }
    }
    if (xpassDetected) {
        process.exitCode = 1;
    }
}

async function main() {
    const [command, ...rest] = process.argv.slice(2);
    switch (command) {
        case "partition-screenshot": {
            const config = await loadExpectedFailures();
            const { normal, expected } = partitionScreenshotTasks(rest[0], config);
            process.stdout.write(`normal_tasks=${normal.join(" ")}\n`);
            process.stdout.write(`expected_tasks=${expected.join(" ")}\n`);
            return;
        }
        case "probe-unit":
            await probeUnitMain(rest[0], repositoryRoot);
            return;
        case "report-android":
            await reportAndroidMain(
                requireNonEmptyString(rest[0], "결과 디렉터리"),
                requireNonEmptyString(rest[1], "device"),
                repositoryRoot,
            );
            return;
        case "report-screenshot": {
            const config = await loadExpectedFailures();
            const { lines, failed } = reportScreenshotProbes(rest[0], rest[1], config);
            lines.forEach((line) => console.log(line));
            if (failed) {
                process.exitCode = 1;
            }
            return;
        }
        default:
            throw new Error(`알 수 없는 명령: ${command ?? "(없음)"}`);
    }
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.stack : error);
        process.exitCode = 1;
    });
}
