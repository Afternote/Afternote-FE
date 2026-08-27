import { createHash } from "node:crypto";
import fs from "node:fs/promises";
import path from "node:path";

export const ANDROID_TEST_MODES = ["none", "selected", "full"];
export const ANDROID_TEST_DEVICES = ["api30", "api34"];

const MAX_REASON_LENGTH = 1_000;
const MAX_SELECTED_TESTS = 20;
const PLACEHOLDERS = new Set(["", "-", "...", "none", "n/a", "todo", "tbd", "없음", "해당 없음"]);
const TEST_PATH_PATTERN = /^(?!\/)(?!.*(?:^|\/)\.\.(?:\/|$)).+\/src\/androidTest\/.+\.kt$/;
const SELECTOR_PATTERN = /^(?:[A-Za-z_][A-Za-z0-9_]*\.)+[A-Za-z_][A-Za-z0-9_$]*#[A-Za-z_][A-Za-z0-9_]*$/;
const FULL_REQUIRED_PATHS = new Set([
    ".github/workflows/android-managed-device.yml",
    ".github/scripts/ci-test-plan.mjs",
    ".github/scripts/resolve-android-test-plan.mjs",
    ".github/scripts/validate-pr-ci-test-plan.mjs",
    ".github/scripts/verify-android-test-plan-result.mjs",
    "app/build.gradle.kts",
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradle.properties",
]);

function text(value) {
    return typeof value === "string" ? value.trim() : "";
}

function section(body) {
    const lines = String(body ?? "").split(/\r?\n/);
    const collected = [];
    let capturing = false;

    for (const line of lines) {
        const heading = /^(#{1,6})\s+(.+?)\s*$/.exec(line);
        if (heading) {
            if (capturing) break;
            capturing = /ci\s*(?:test\s*plan|테스트\s*계획)/i.test(heading[2]);
            continue;
        }
        if (capturing) collected.push(line);
    }
    return collected.join("\n").replace(/<!--[\s\S]*?-->/g, "");
}

export function hasCiTestPlanSection(body) {
    return Boolean(section(body).trim());
}

export function extractCiTestPlan(body) {
    const source = section(body);
    if (!source.trim()) {
        throw new Error("`CI Test Plan` 섹션이 없습니다.");
    }
    const blocks = [...source.matchAll(/```(?:json)?\s*\r?\n([\s\S]*?)```/gi)];
    if (blocks.length !== 1) {
        throw new Error("`CI Test Plan`에는 JSON 코드 블록이 정확히 하나 있어야 합니다.");
    }
    let parsed;
    try {
        parsed = JSON.parse(blocks[0][1]);
    } catch {
        throw new Error("`CI Test Plan` JSON을 해석할 수 없습니다.");
    }
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
        throw new Error("`CI Test Plan` 최상위 값은 JSON 객체여야 합니다.");
    }
    return parsed;
}

function rejectUnknownKeys(value, allowed, location, errors) {
    for (const key of Object.keys(value)) {
        if (!allowed.has(key)) {
            errors.push(`\`${location}.${key}\`는 지원하지 않는 필드입니다.`);
        }
    }
}

export function inspectCiTestPlan(body, { pullRequestNumber = "?" } = {}) {
    let raw;
    try {
        raw = extractCiTestPlan(body);
    } catch (error) {
        return { valid: false, plan: null, errors: [`PR #${pullRequestNumber}: ${error.message}`] };
    }

    const errors = [];
    rejectUnknownKeys(raw, new Set(["androidTest"]), "CI Test Plan", errors);
    const decision = raw.androidTest;
    if (!decision || typeof decision !== "object" || Array.isArray(decision)) {
        errors.push("`androidTest` 객체가 필요합니다.");
        return {
            valid: false,
            plan: null,
            errors: errors.map((error) => `PR #${pullRequestNumber}: ${error}`),
        };
    }
    rejectUnknownKeys(decision, new Set(["mode", "reason", "tests"]), "androidTest", errors);

    const mode = text(decision.mode).toLowerCase();
    const reason = text(decision.reason);
    if (!ANDROID_TEST_MODES.includes(mode)) {
        errors.push(`\`androidTest.mode\`는 ${ANDROID_TEST_MODES.join(", ")} 중 하나여야 합니다.`);
    }
    if (PLACEHOLDERS.has(reason.toLowerCase())) {
        errors.push("`androidTest.reason`에 변경 경계와 실행 선택 근거를 적어야 합니다.");
    } else if (reason.length > MAX_REASON_LENGTH) {
        errors.push(`\`androidTest.reason\`은 ${MAX_REASON_LENGTH}자 이하여야 합니다.`);
    }

    const rawTests = decision.tests;
    const tests = [];
    if (mode === "selected") {
        if (!Array.isArray(rawTests) || rawTests.length === 0) {
            errors.push("`selected` 모드에는 `androidTest.tests`가 하나 이상 필요합니다.");
        } else if (rawTests.length > MAX_SELECTED_TESTS) {
            errors.push(`\`androidTest.tests\`는 ${MAX_SELECTED_TESTS}개 이하여야 합니다.`);
        }
    } else if (rawTests !== undefined && (!Array.isArray(rawTests) || rawTests.length > 0)) {
        errors.push("`none` 또는 `full` 모드에는 선택 테스트를 함께 둘 수 없습니다.");
    }

    if (Array.isArray(rawTests)) {
        const identities = new Set();
        rawTests.forEach((item, index) => {
            if (!item || typeof item !== "object" || Array.isArray(item)) {
                errors.push(`\`androidTest.tests[${index}]\`는 객체여야 합니다.`);
                return;
            }
            rejectUnknownKeys(
                item,
                new Set(["path", "selector", "device"]),
                `androidTest.tests[${index}]`,
                errors,
            );
            const testPath = text(item.path);
            const selector = text(item.selector);
            const device = text(item.device).toLowerCase();
            if (!TEST_PATH_PATTERN.test(testPath)) {
                errors.push(
                    `\`androidTest.tests[${index}].path\`는 저장소 안의 \`src/androidTest/*.kt\`여야 합니다.`,
                );
            }
            if (!SELECTOR_PATTERN.test(selector)) {
                errors.push(
                    `\`androidTest.tests[${index}].selector\`는 fully-qualified \`Class#method\` 형식이어야 합니다.`,
                );
            }
            if (!ANDROID_TEST_DEVICES.includes(device)) {
                errors.push(
                    `\`androidTest.tests[${index}].device\`는 ${ANDROID_TEST_DEVICES.join(", ")} 중 하나여야 합니다.`,
                );
            }
            const identity = `${device}:${selector}`;
            if (identities.has(identity)) {
                errors.push(`중복 선택 테스트입니다: ${identity}`);
            }
            identities.add(identity);
            tests.push({ path: testPath, selector, device });
        });
    }

    const plan = { androidTest: { mode, reason } };
    if (mode === "selected") {
        plan.androidTest.tests = tests;
    }
    return {
        valid: errors.length === 0,
        plan: errors.length === 0 ? plan : null,
        errors: errors.map((error) => `PR #${pullRequestNumber}: ${error}`),
    };
}

export function inspectPullRequestCiTestPlan(pullRequest) {
    const number = pullRequest?.number ?? "?";
    return inspectCiTestPlan(pullRequest?.body, { pullRequestNumber: number });
}

export function ciTestPlanDigest(plan) {
    return createHash("sha256").update(JSON.stringify(plan)).digest("hex");
}

export function inspectAndroidTestImpact(changedPaths) {
    const full = [];
    const selected = [];
    const changedTestSources = [];
    for (const rawPath of changedPaths) {
        const filePath = String(rawPath ?? "").replaceAll("\\", "/").replace(/^\.\//, "");
        if (!filePath) continue;
        if (
            FULL_REQUIRED_PATHS.has(filePath) ||
            filePath.startsWith(".github/actions/setup-ci-config/") ||
            filePath.startsWith("build-logic/") ||
            filePath.startsWith("gradle/")
        ) {
            full.push(filePath);
            continue;
        }
        if (/(^|\/)src\/androidTest\//.test(filePath)) {
            selected.push(filePath);
            if (filePath.endsWith(".kt")) changedTestSources.push(filePath);
            continue;
        }
        if (
            /(^|\/)src\/(?:main|debug)\/AndroidManifest\.xml$/.test(filePath) ||
            /^(?:app|feature\/[^/]+\/presentation)\/src\/main\/(?:java|kotlin)\/.+\/navigation\//.test(filePath) ||
            /^app\/src\/main\/(?:java|kotlin)\/.+\/(?:MainActivity|AfternoteApplication)\.kt$/.test(filePath)
        ) {
            selected.push(filePath);
        }
    }
    return {
        full: [...new Set(full)].sort(),
        selected: [...new Set(selected)].sort(),
        changedTestSources: [...new Set(changedTestSources)].sort(),
    };
}

export async function validateCiTestPlanImpact(
    plan,
    changedPaths,
    { root = process.cwd() } = {},
) {
    const impact = inspectAndroidTestImpact(changedPaths);
    const mode = plan?.androidTest?.mode;
    if (impact.full.length > 0 && mode !== "full") {
        throw new Error(
            `계측 하네스·전역 빌드 변경은 androidTest.mode=full이어야 합니다: ${impact.full.join(", ")}`,
        );
    }
    if (impact.selected.length > 0 && mode === "none") {
        throw new Error(
            `Android 런타임 경계 변경은 androidTest.mode=selected 또는 full이어야 합니다: ${impact.selected.join(", ")}`,
        );
    }
    if (mode !== "selected") return;

    const declaredMethodsByPath = new Map();
    for (const test of plan.androidTest.tests) {
        const methods = declaredMethodsByPath.get(test.path) ?? new Set();
        methods.add(test.selector.split("#", 2)[1]);
        declaredMethodsByPath.set(test.path, methods);
    }
    for (const testPath of impact.changedTestSources) {
        let source;
        try {
            source = await fs.readFile(path.resolve(root, testPath), "utf8");
        } catch {
            continue;
        }
        const declaredMethods = declaredMethodsByPath.get(testPath) ?? new Set();
        const testMethods = [
            ...source.matchAll(
                /@Test(?:\s*\([^)]*\))?\s*(?:@[\w:.]+(?:\([^\n]*\))?\s*)*fun\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(/g,
            ),
        ].map((match) => match[1]);
        const omitted = [...new Set(testMethods)].filter((method) => !declaredMethods.has(method));
        if (omitted.length > 0) {
            throw new Error(
                `변경한 @Test 메서드를 모두 selector로 선언해야 합니다: ${testPath}#${omitted.join(", #")}`,
            );
        }
    }
}

function escapeRegex(value) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

export async function validateCiTestPlanSources(plan, { root = process.cwd() } = {}) {
    if (plan?.androidTest?.mode !== "selected") return;

    for (const test of plan.androidTest.tests) {
        const absolute = path.resolve(root, test.path);
        const relative = path.relative(root, absolute);
        if (relative.startsWith("..") || path.isAbsolute(relative)) {
            throw new Error(`선택 테스트가 저장소 밖을 가리킵니다: ${test.path}`);
        }
        let stat;
        try {
            stat = await fs.stat(absolute);
        } catch {
            throw new Error(`선택 테스트 파일이 현재 revision에 없습니다: ${test.path}`);
        }
        if (!stat.isFile()) {
            throw new Error(`선택 테스트 경로가 파일이 아닙니다: ${test.path}`);
        }

        const source = await fs.readFile(absolute, "utf8");
        const [qualifiedClass, method] = test.selector.split("#", 2);
        const classParts = qualifiedClass.split(".");
        const declaredClass = classParts.at(-1).split("$").at(-1);
        const expectedPackage = classParts.slice(0, -1).join(".");
        const actualPackage = /^\s*package\s+([A-Za-z_][A-Za-z0-9_.]*)/m.exec(source)?.[1] ?? "";
        if (actualPackage !== expectedPackage) {
            throw new Error(
                `선택 테스트 package가 selector와 다릅니다: ${test.path} (${actualPackage} != ${expectedPackage})`,
            );
        }
        if (!new RegExp(`\\b(?:class|object)\\s+${escapeRegex(declaredClass)}\\b`).test(source)) {
            throw new Error(`선택 테스트 class가 파일에 없습니다: ${test.selector}`);
        }
        const methodPattern = new RegExp(
            `@Test(?:\\s*\\([^)]*\\))?\\s*` +
                `(?:@[\\w:.]+(?:\\([^\\n]*\\))?\\s*)*` +
                `fun\\s+${escapeRegex(method)}\\s*\\(`,
        );
        if (!methodPattern.test(source)) {
            throw new Error(`선택 테스트 @Test 메서드가 파일에 없습니다: ${test.selector}`);
        }
    }
}
