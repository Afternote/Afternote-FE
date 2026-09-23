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
    ".github/workflows/android-managed-device-retry.yml",
    ".github/scripts/classify-android-managed-device-failure.mjs",
    ".github/scripts/ci-test-plan.mjs",
    ".github/scripts/resolve-android-test-plan.mjs",
    ".github/scripts/render-android-test-results.mjs",
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
            ...kotlinCodeWithoutLiterals(source).matchAll(
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

// 이름 자리의 키워드는 이름이 아니다. `companion object` 다음 줄의 `class Inner` 를 삼키지 않게 한다.
// 이름 없는 companion 은 JVM 이름이 `Companion` 이다. interface·companion 은 중첩 class 의 바깥 이름으로 쓰인다.
const CLASS_DECLARATION_PATTERN =
    /(?<![\w.:])(?:(?:class|interface|object)\s+(?!(?:class|interface|object|fun|val|var|typealias)\b)([A-Za-z_][A-Za-z0-9_]*)|companion\s+object(?=\s*[{:]))/g;
// `init` 은 `by init` 처럼 식별자로도 쓰여 넣지 않는다. 본문 없는 class 가 뒤따르는 init 블록을
// 본문으로 잡아도 그 안에는 @Test 멤버가 올 수 없다.
const CLASS_HEADER_END_WORDS = new Set(["class", "interface", "object", "fun", "val", "var", "typealias"]);
const WORD_PATTERN = /[A-Za-z_][A-Za-z0-9_]*/y;

// 주석과 문자·문자열 리터럴 안쪽을 공백으로 지운다. 줄바꿈과 길이는 그대로라 인덱스가 원문과 같다.
function kotlinCodeWithoutLiterals(source) {
    const code = source.split("");
    const blank = (from, to) => {
        for (let index = from; index < to; index += 1) {
            if (code[index] !== "\n") code[index] = " ";
        }
    };
    // `${...}` 안은 다시 코드이고 그 안에 문자열이 또 올 수 있어, 돌아갈 문자열 종류와 중괄호 깊이를 쌓는다.
    // 템플릿 안의 리터럴·주석은 바깥 리터럴을 지울 때 함께 지워진다.
    const templates = [];
    let mode = "code";
    // `$$"..."` 처럼 여는 따옴표 앞 `$` 개수만큼 이어져야 템플릿이 열린다(Kotlin 2.2 다중 달러 보간).
    let interpolation = 1;
    let literalStart = 0;
    let commentStart = 0;
    let commentDepth = 0;
    let index = 0;
    const enterLiteral = (literalMode, width) => {
        let dollars = 0;
        while (source[index - 1 - dollars] === "$") dollars += 1;
        interpolation = Math.max(dollars, 1);
        if (templates.length === 0) literalStart = index + width;
        mode = literalMode;
        index += width;
    };
    const leaveLiteral = (contentEnd, width) => {
        if (templates.length === 0) blank(literalStart, contentEnd);
        mode = "code";
        index = contentEnd + width;
    };

    while (index < source.length) {
        const char = source[index];
        const next = source[index + 1];
        if (mode === "comment") {
            if (char === "/" && next === "*") {
                commentDepth += 1;
                index += 2;
            } else if (char === "*" && next === "/") {
                commentDepth -= 1;
                index += 2;
                if (commentDepth === 0) {
                    if (templates.length === 0) blank(commentStart, index);
                    mode = "code";
                }
            } else {
                index += 1;
            }
        } else if (mode === "string" || mode === "raw") {
            if (char === "$") {
                let run = 1;
                while (source[index + run] === "$") run += 1;
                if (source[index + run] === "{" && run >= interpolation) {
                    templates.push({ mode, interpolation, depth: 0 });
                    mode = "code";
                    index += run + 1;
                } else {
                    index += run;
                }
            } else if (mode === "string" && char === "\\") {
                index += 2;
            } else if (mode === "string" && char === '"') {
                leaveLiteral(index, 1);
            } else if (mode === "raw" && source.startsWith('"""', index)) {
                // `""""` 처럼 따옴표가 셋보다 많으면 마지막 셋이 닫고 앞쪽은 내용이다.
                let end = index + 3;
                while (source[end] === '"') end += 1;
                leaveLiteral(end - 3, 3);
            } else {
                index += 1;
            }
        } else if (char === "/" && next === "/") {
            let lineEnd = index;
            while (lineEnd < source.length && source[lineEnd] !== "\n" && source[lineEnd] !== "\r") lineEnd += 1;
            if (templates.length === 0) blank(index, lineEnd);
            index = lineEnd;
        } else if (char === "/" && next === "*") {
            commentStart = index;
            commentDepth = 1;
            mode = "comment";
            index += 2;
        } else if (source.startsWith('"""', index)) {
            enterLiteral("raw", 3);
        } else if (char === '"') {
            enterLiteral("string", 1);
        } else if (char === "'" || char === "`") {
            let end = index + 1;
            while (end < source.length && source[end] !== char && source[end] !== "\n" && source[end] !== "\r") {
                end += char === "'" && source[end] === "\\" ? 2 : 1;
            }
            if (templates.length === 0) blank(index + 1, end);
            index = end + 1;
        } else if (templates.length > 0 && char === "{") {
            templates.at(-1).depth += 1;
            index += 1;
        } else if (templates.length > 0 && char === "}") {
            const template = templates.at(-1);
            if (template.depth === 0) {
                templates.pop();
                mode = template.mode;
                interpolation = template.interpolation;
            } else {
                template.depth -= 1;
            }
            index += 1;
        } else {
            index += 1;
        }
    }
    if (templates.length > 0 || mode === "string" || mode === "raw") {
        blank(literalStart, source.length);
    } else if (mode === "comment") {
        blank(commentStart, source.length);
    }
    return code.join("");
}

function braceBlocks(code) {
    const blocks = [];
    const open = [];
    for (let index = 0; index < code.length; index += 1) {
        if (code[index] === "{") {
            open.push(blocks.length);
            blocks.push({ start: index, end: code.length });
        } else if (code[index] === "}" && open.length > 0) {
            blocks[open.pop()].end = index;
        }
    }
    return blocks;
}

function innermostBlock(blocks, position) {
    let found = null;
    for (const block of blocks) {
        if (block.start >= position) break;
        if (block.end > position) found = block;
    }
    return found;
}

// 선언 뒤 괄호 밖 첫 `{` 가 본문이다. 그 전에 다음 선언이 시작되면 본문 없는 class 다.
function classBodyStart(code, from) {
    let parentheses = 0;
    for (let index = from; index < code.length; index += 1) {
        const char = code[index];
        if (char === "(") {
            parentheses += 1;
        } else if (char === ")") {
            parentheses -= 1;
            if (parentheses < 0) return -1;
        } else if (parentheses > 0) {
            continue;
        } else if (char === "{") {
            return index;
        } else if (char === "}" || char === ";" || char === "=") {
            return -1;
        } else if (/[A-Za-z_]/.test(char) && !/[A-Za-z0-9_]/.test(code[index - 1])) {
            WORD_PATTERN.lastIndex = index;
            const word = WORD_PATTERN.exec(code)[0];
            if (CLASS_HEADER_END_WORDS.has(word)) return -1;
            index += word.length - 1;
        }
    }
    return -1;
}

// selector 가 쓰는 JVM 이름(`Outer$Inner`)으로 class 를 모으고, 본문이 있으면 여는 `{` 위치로 찾게 둔다.
function kotlinClassBodies(code, blocks) {
    const binaryNames = new Set();
    const binaryNameByBodyStart = new Map();
    for (const match of code.matchAll(CLASS_DECLARATION_PATTERN)) {
        const parent = innermostBlock(blocks, match.index);
        const outerName = parent === null ? "" : binaryNameByBodyStart.get(parent.start);
        // 함수 본문 같은 class 밖 블록에 선언된 local class 는 selector 로 가리킬 수 없다.
        if (outerName === undefined) continue;
        const name = match[1] ?? "Companion";
        const binaryName = outerName ? `${outerName}$${name}` : name;
        binaryNames.add(binaryName);
        const bodyStart = classBodyStart(code, match.index + match[0].length);
        if (bodyStart !== -1) binaryNameByBodyStart.set(bodyStart, binaryName);
    }
    return { binaryNames, binaryNameByBodyStart };
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
        const binaryName = classParts.at(-1);
        const expectedPackage = classParts.slice(0, -1).join(".");
        const actualPackage = /^\s*package\s+([A-Za-z_][A-Za-z0-9_.]*)/m.exec(source)?.[1] ?? "";
        if (actualPackage !== expectedPackage) {
            throw new Error(
                `선택 테스트 package가 selector와 다릅니다: ${test.path} (${actualPackage} != ${expectedPackage})`,
            );
        }
        const code = kotlinCodeWithoutLiterals(source);
        const blocks = braceBlocks(code);
        const { binaryNames, binaryNameByBodyStart } = kotlinClassBodies(code, blocks);
        if (!binaryNames.has(binaryName)) {
            throw new Error(`선택 테스트 class가 파일에 없습니다: ${test.selector}`);
        }
        const methodPattern = new RegExp(
            `@Test(?:\\s*\\([^)]*\\))?\\s*` +
                `(?:@[\\w:.]+(?:\\([^\\n]*\\))?\\s*)*` +
                `fun\\s+${escapeRegex(method)}\\s*\\(`,
            "g",
        );
        // 한 파일에 class 가 여럿이면 메서드가 파일에 있어도 다른 class 소속일 수 있다(#2153).
        // 그러면 계측 실행기는 0건을 돌리므로 @Test 를 바로 감싸는 블록이 selector class 본문이어야 한다.
        const owners = [...code.matchAll(methodPattern)].map((match) =>
            binaryNameByBodyStart.get(innermostBlock(blocks, match.index)?.start),
        );
        if (owners.length === 0) {
            throw new Error(`선택 테스트 @Test 메서드가 파일에 없습니다: ${test.selector}`);
        }
        if (!owners.includes(binaryName)) {
            const actual = [...new Set(owners.filter(Boolean))].map(
                (owner) => `${expectedPackage}.${owner}#${method}`,
            );
            throw new Error(
                `선택 테스트 @Test 메서드가 selector class 본문에 없습니다: ${test.selector}` +
                    (actual.length > 0 ? ` (메서드가 있는 class: ${actual.join(", ")})` : ""),
            );
        }
    }
}
