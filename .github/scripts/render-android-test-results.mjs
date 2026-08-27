#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

const SKIPPED_SOURCE_DIRECTORIES = new Set([
    ".git",
    ".gradle",
    ".idea",
    "build",
    "node_modules",
]);
const MAX_ERROR_MESSAGE_LENGTH = 2_000;
const MAX_GITHUB_ANNOTATIONS = 50;
const ANNOTATIONS_PER_STEP = 10;

function decodeXml(value) {
    const entities = new Map([
        ["quot", '"'],
        ["apos", "'"],
        ["lt", "<"],
        ["gt", ">"],
        ["amp", "&"],
    ]);
    return String(value).replace(/&(#x[\da-f]+|#\d+|quot|apos|lt|gt|amp);/gi, (entity, name) => {
        if (name.startsWith("#x") || name.startsWith("#X")) {
            return String.fromCodePoint(Number.parseInt(name.slice(2), 16));
        }
        if (name.startsWith("#")) {
            return String.fromCodePoint(Number.parseInt(name.slice(1), 10));
        }
        return entities.get(name.toLowerCase()) ?? entity;
    });
}

function findTagEnd(xml, from) {
    let quote = null;
    for (let index = from; index < xml.length; index += 1) {
        const character = xml[index];
        if (quote !== null) {
            if (character === quote) quote = null;
        } else if (character === '"' || character === "'") {
            quote = character;
        } else if (character === ">") {
            return index;
        }
    }
    return -1;
}

function parseOpenTag(fragment) {
    let cursor = 0;
    const nameMatch = /^[A-Za-z_:][\w:.-]*/.exec(fragment);
    if (nameMatch === null) throw new Error(`잘못된 XML 여는 태그: <${fragment}>`);
    const name = nameMatch[0];
    cursor = name.length;
    const attributes = new Map();

    while (cursor < fragment.length) {
        while (/\s/.test(fragment[cursor] ?? "")) cursor += 1;
        if (cursor >= fragment.length) break;
        const attributeMatch = /^[A-Za-z_:][\w:.-]*/.exec(fragment.slice(cursor));
        if (attributeMatch === null) throw new Error(`잘못된 XML 속성: <${fragment}>`);
        const attributeName = attributeMatch[0];
        cursor += attributeName.length;
        while (/\s/.test(fragment[cursor] ?? "")) cursor += 1;
        if (fragment[cursor] !== "=") throw new Error(`XML 속성 ${attributeName}에 값이 없습니다.`);
        cursor += 1;
        while (/\s/.test(fragment[cursor] ?? "")) cursor += 1;
        const quote = fragment[cursor];
        if (quote !== '"' && quote !== "'") {
            throw new Error(`XML 속성 ${attributeName} 값에 따옴표가 없습니다.`);
        }
        const end = fragment.indexOf(quote, cursor + 1);
        if (end === -1) throw new Error(`XML 속성 ${attributeName} 값이 닫히지 않았습니다.`);
        if (attributes.has(attributeName)) throw new Error(`XML 속성 ${attributeName}이 중복됩니다.`);
        attributes.set(attributeName, decodeXml(fragment.slice(cursor + 1, end)));
        cursor = end + 1;
    }
    return { name, attributes };
}

// Managed Device JUnit XML에 필요한 범위만 파싱한다. 외부 entity나 DTD는 해석하지 않는다.
function parseXml(xml) {
    const root = { name: null, attributes: new Map(), content: [] };
    const stack = [root];
    let cursor = xml.charCodeAt(0) === 0xfeff ? 1 : 0;

    while (cursor < xml.length) {
        const nextTag = xml.indexOf("<", cursor);
        if (nextTag === -1) {
            stack.at(-1).content.push(decodeXml(xml.slice(cursor)));
            cursor = xml.length;
            break;
        }
        if (nextTag > cursor) stack.at(-1).content.push(decodeXml(xml.slice(cursor, nextTag)));

        if (xml.startsWith("<!--", nextTag)) {
            const end = xml.indexOf("-->", nextTag + 4);
            if (end === -1) throw new Error("XML 주석이 닫히지 않았습니다.");
            cursor = end + 3;
            continue;
        }
        if (xml.startsWith("<![CDATA[", nextTag)) {
            const end = xml.indexOf("]]>", nextTag + 9);
            if (end === -1) throw new Error("XML CDATA가 닫히지 않았습니다.");
            stack.at(-1).content.push(xml.slice(nextTag + 9, end));
            cursor = end + 3;
            continue;
        }
        if (xml.startsWith("<?", nextTag)) {
            const end = xml.indexOf("?>", nextTag + 2);
            if (end === -1) throw new Error("XML 처리 지시문이 닫히지 않았습니다.");
            cursor = end + 2;
            continue;
        }
        if (xml.startsWith("<!", nextTag)) {
            throw new Error("DTD를 포함한 XML 선언은 지원하지 않습니다.");
        }

        const tagEnd = findTagEnd(xml, nextTag + 1);
        if (tagEnd === -1) throw new Error("XML 태그가 닫히지 않았습니다.");
        let fragment = xml.slice(nextTag + 1, tagEnd).trim();
        if (fragment.startsWith("/")) {
            const closingName = fragment.slice(1).trim();
            if (!/^[A-Za-z_:][\w:.-]*$/.test(closingName)) {
                throw new Error(`잘못된 XML 닫는 태그: </${closingName}>`);
            }
            const current = stack.at(-1);
            if (current === root || current.name !== closingName) {
                throw new Error(`XML 닫는 태그 </${closingName}>의 짝이 맞지 않습니다.`);
            }
            stack.pop();
        } else {
            const selfClosing = fragment.endsWith("/");
            if (selfClosing) fragment = fragment.slice(0, -1).trimEnd();
            const parsed = parseOpenTag(fragment);
            const node = { ...parsed, content: [] };
            stack.at(-1).content.push(node);
            if (!selfClosing) stack.push(node);
        }
        cursor = tagEnd + 1;
    }

    if (stack.length !== 1) throw new Error(`XML 태그 <${stack.at(-1).name}>가 닫히지 않았습니다.`);
    const elements = root.content.filter((item) => typeof item !== "string");
    if (elements.length !== 1) throw new Error("XML 문서에는 루트 요소가 하나 있어야 합니다.");
    return elements[0];
}

function descendants(node, name) {
    const found = [];
    for (const item of node.content) {
        if (typeof item === "string") continue;
        if (item.name === name) found.push(item);
        found.push(...descendants(item, name));
    }
    return found;
}

function directChild(node, names) {
    return node.content.find((item) => typeof item !== "string" && names.has(item.name)) ?? null;
}

function textContent(node) {
    return node.content
        .map((item) => (typeof item === "string" ? item : textContent(item)))
        .join("");
}

function shortenClassName(className) {
    return className.split(".").at(-1)?.split("$")[0] || "UnknownTest";
}

function truncateMessage(message) {
    const trimmed = message.trim();
    if (trimmed.length <= MAX_ERROR_MESSAGE_LENGTH) return trimmed;
    return `${trimmed.slice(0, MAX_ERROR_MESSAGE_LENGTH - 1).trimEnd()}…`;
}

export function extractErrorMessage(trace, attributeMessage = "") {
    const lines = String(trace).replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n");
    const firstFrame = lines.findIndex((line) => /^\s*at\s+\S+\([^)]*\)\s*$/.test(line));
    const prefix = (firstFrame === -1 ? lines : lines.slice(0, firstFrame)).join("\n").trim();
    const message = prefix || attributeMessage.trim() || "오류 메시지가 없습니다.";
    return truncateMessage(message.replaceAll(/\n{3,}/g, "\n\n"));
}

export function extractStackFrames(trace) {
    const frames = [];
    for (const line of String(trace).replaceAll("\r", "").split("\n")) {
        const match = /^\s*at\s+([^\s(]+)\(([^():]+):(\d+)\)\s*$/.exec(line);
        if (match === null) continue;
        frames.push({
            callable: match[1],
            fileName: match[2],
            line: Number(match[3]),
        });
    }
    return frames;
}

function incompleteRunMessage(text) {
    const normalized = String(text).replaceAll(/\s+/g, " ").trim();
    if (!/(?:test run failed to complete|expected \d+ tests?, received \d+|instrumentation_failed|test run (?:aborted|incomplete)|process (?:crashed|terminated))/i.test(normalized)) {
        return null;
    }
    return truncateMessage(normalized);
}

export function parseAndroidTestXml(xml, { file = "JUnit XML" } = {}) {
    let root;
    try {
        root = parseXml(xml);
    } catch (error) {
        throw new Error(`${file}: ${error instanceof Error ? error.message : error}`);
    }

    const testcases = descendants(root, "testcase").map((node) => {
        const className = node.attributes.get("classname") ?? node.attributes.get("class") ?? "";
        const name = node.attributes.get("name") ?? "";
        const result = directChild(node, new Set(["failure", "error", "skipped"]));
        if (result === null || result.name === "skipped") {
            return {
                className,
                name,
                status: result?.name ?? "passed",
                failure: null,
            };
        }
        const trace = textContent(result);
        return {
            className,
            name,
            status: result.name,
            failure: {
                kind: result.name,
                message: extractErrorMessage(trace, result.attributes.get("message") ?? ""),
                trace,
            },
        };
    });

    const infrastructureFailures = descendants(root, "system-err")
        .map((node) => incompleteRunMessage(textContent(node)))
        .filter((message) => message !== null);
    return { testcases, infrastructureFailures };
}

export async function collectXmlFiles(root) {
    const files = [];
    async function visit(directory) {
        let entries;
        try {
            entries = await fs.readdir(directory, { withFileTypes: true });
        } catch (error) {
            if (error?.code === "ENOENT") return;
            throw error;
        }
        for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
            const target = path.join(directory, entry.name);
            if (entry.isDirectory()) await visit(target);
            else if (entry.isFile() && entry.name.endsWith(".xml")) files.push(target);
        }
    }
    await visit(root);
    return files;
}

async function collectTestSources(workspaceRoot) {
    const sources = [];
    async function visit(directory) {
        let entries;
        try {
            entries = await fs.readdir(directory, { withFileTypes: true });
        } catch (error) {
            if (error?.code === "ENOENT" || error?.code === "EACCES") return;
            throw error;
        }
        for (const entry of entries) {
            const target = path.join(directory, entry.name);
            if (entry.isDirectory()) {
                if (!SKIPPED_SOURCE_DIRECTORIES.has(entry.name)) await visit(target);
                continue;
            }
            if (!entry.isFile() || !/\.(?:kt|java)$/.test(entry.name)) continue;
            const relative = path.relative(workspaceRoot, target);
            const segments = relative.split(path.sep);
            const sourceIndex = segments.indexOf("src");
            if (sourceIndex === -1 || !/test/i.test(segments[sourceIndex + 1] ?? "")) continue;
            sources.push({
                fileName: entry.name,
                relative: relative.split(path.sep).join("/"),
            });
        }
    }
    await visit(workspaceRoot);
    return sources.sort((left, right) => left.relative.localeCompare(right.relative));
}

function sourceForFrame(frame, className, sourceFiles) {
    const candidates = sourceFiles.filter((source) => source.fileName === frame.fileName);
    if (candidates.length === 0) return null;
    const owner = frame.callable.slice(0, frame.callable.lastIndexOf("."));
    const framePackage = owner.split(".").slice(0, -1).join("/");
    const exact = candidates.find((source) => source.relative.includes(`/${framePackage}/${frame.fileName}`));
    if (exact !== undefined) return exact;

    // Kotlin이 생성한 callable 이름과 파일명이 다른 경우에는 같은 테스트 package 안에서만 fallback한다.
    const testPackage = className.split(".").slice(0, -1).join("/");
    if (!frame.callable.startsWith(`${className.split(".").slice(0, -1).join(".")}.`)) return null;
    return candidates.find((source) => source.relative.includes(`/${testPackage}/${frame.fileName}`)) ?? null;
}

export function resolveFailureLocation(testcase, sourceFiles) {
    const frames = extractStackFrames(testcase.failure?.trace ?? "");
    const testcaseFrame = frames.find(
        (frame) => frame.callable.startsWith(`${testcase.className}.`) || frame.callable.startsWith(`${testcase.className}$`),
    );
    const shortClass = shortenClassName(testcase.className);
    const testcaseSource = sourceFiles.find(
        (candidate) => candidate.fileName === `${shortClass}.kt` || candidate.fileName === `${shortClass}.java`,
    );

    // Assert/Compose/helper 쪽에 같은 basename이 있어도 실제 testcase frame을 우선한다.
    if (testcaseFrame !== undefined) {
        const exact = sourceForFrame(testcaseFrame, testcase.className, sourceFiles) ?? testcaseSource;
        if (exact !== undefined && exact !== null) return { file: exact.relative, line: testcaseFrame.line };
        return { file: testcaseFrame.fileName, line: testcaseFrame.line };
    }

    for (const frame of frames) {
        const source = sourceForFrame(frame, testcase.className, sourceFiles);
        if (source !== null) return { file: source.relative, line: frame.line };
    }
    if (testcaseSource !== undefined) return { file: testcaseSource.relative, line: 0 };
    return null;
}

export function summarizeAndroidTestDocuments(documents, { sourceFiles = [] } = {}) {
    const testcases = [];
    const infrastructureFailures = [];
    for (const { file, xml } of documents) {
        const parsed = parseAndroidTestXml(xml, { file });
        testcases.push(...parsed.testcases);
        infrastructureFailures.push(...parsed.infrastructureFailures.map((message) => ({ file, message })));
    }
    if (testcases.length === 0 && infrastructureFailures.length === 0) {
        throw new Error("Managed Device XML에 testcase 결과가 없습니다.");
    }
    const failures = testcases
        .filter((testcase) => testcase.status === "failure" || testcase.status === "error")
        .map((testcase) => ({
            selector: `${shortenClassName(testcase.className)}#${testcase.name}`,
            className: testcase.className,
            name: testcase.name,
            kind: testcase.status,
            message: testcase.failure.message,
            location: resolveFailureLocation(testcase, sourceFiles),
        }));
    return {
        executed: testcases.length,
        failed: failures.length,
        skipped: testcases.filter((testcase) => testcase.status === "skipped").length,
        failures,
        infrastructureFailures,
        verificationFailure: null,
    };
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function htmlMessage(message) {
    return escapeHtml(message).replaceAll("\n", "<br>");
}

function renderedLocation(location) {
    if (location === null) return "위치 없음";
    return location.line > 0 ? `${location.file}:${location.line}` : location.file;
}

export function renderAndroidTestSummary(summary) {
    const failed =
        summary.failed > 0 ||
        summary.infrastructureFailures.length > 0 ||
        summary.verificationFailure != null;
    const lines = [
        "## Android tests",
        "",
        `${failed ? "❌" : "✅"} **Android tests: ${summary.executed} executed, ${summary.failed} failed, ${summary.skipped} skipped**`,
    ];
    if (summary.failures.length > 0) {
        lines.push("");
        summary.failures.forEach((failure, index) => {
            lines.push(
                `${index + 1}. <code>${escapeHtml(failure.selector)}</code>`,
                `   - Location: <code>${escapeHtml(renderedLocation(failure.location))}</code>`,
                `   - Error: ${htmlMessage(failure.message)}`,
            );
        });
    }
    if (summary.infrastructureFailures.length > 0) {
        lines.push("", "### Test run infrastructure failures", "");
        for (const failure of summary.infrastructureFailures) {
            lines.push(`- ${htmlMessage(failure.message)}`);
        }
    }
    if (summary.verificationFailure != null) {
        lines.push(
            "",
            "### Selected androidTest result verification failed",
            "",
            htmlMessage(summary.verificationFailure.message),
        );
    }
    const totalAnnotations =
        summary.failures.length +
        summary.infrastructureFailures.length +
        (summary.verificationFailure == null ? 0 : 1);
    if (totalAnnotations > MAX_GITHUB_ANNOTATIONS) {
        lines.push(
            "",
            `> GitHub job Annotation 한도 때문에 전체 ${totalAnnotations}개 중 첫 ${MAX_GITHUB_ANNOTATIONS}개만 Annotation으로 표시합니다. 실패 상세는 위 Summary에 전량 표시됩니다.`,
        );
    }
    return lines.join("\n");
}

export function escapeWorkflowCommandData(value) {
    return String(value).replaceAll("%", "%25").replaceAll("\r", "%0D").replaceAll("\n", "%0A");
}

export function escapeWorkflowCommandProperty(value) {
    return escapeWorkflowCommandData(value).replaceAll(":", "%3A").replaceAll(",", "%2C");
}

export function renderAnnotations(summary) {
    // selected 실행 증거와 실행 중단은 개별 assertion보다 gate 성격이 강하므로 cap 앞쪽에 둔다.
    const annotations = [];
    if (summary.verificationFailure != null) {
        annotations.push(
            `::error title=${escapeWorkflowCommandProperty("Selected androidTest result verification failed")}::${escapeWorkflowCommandData(summary.verificationFailure.message)}`,
        );
    }
    for (const failure of summary.infrastructureFailures) {
        annotations.push(
            `::error title=${escapeWorkflowCommandProperty("Android test run incomplete")}::${escapeWorkflowCommandData(failure.message)}`,
        );
    }
    annotations.push(...summary.failures.map((failure) => {
        const properties = [];
        if (failure.location?.file) properties.push(`file=${escapeWorkflowCommandProperty(failure.location.file)}`);
        if (failure.location?.line > 0) properties.push(`line=${failure.location.line}`);
        properties.push(`title=${escapeWorkflowCommandProperty(failure.selector)}`);
        return `::error ${properties.join(",")}::${escapeWorkflowCommandData(failure.message)}`;
    }));
    return annotations;
}

export async function emitAnnotations(
    annotations,
    {
        annotationDirectory = process.env.ANDROID_TEST_ANNOTATION_DIR,
        githubOutput = process.env.GITHUB_OUTPUT,
    } = {},
) {
    if (!annotationDirectory) {
        for (const annotation of annotations) process.stdout.write(`${annotation}\n`);
        return { annotationChunks: 0, annotationCount: annotations.length };
    }
    if (!githubOutput) {
        throw new Error("ANDROID_TEST_ANNOTATION_DIR를 사용하려면 GITHUB_OUTPUT이 필요합니다.");
    }

    await fs.mkdir(annotationDirectory, { recursive: true });
    const limited = annotations.slice(0, MAX_GITHUB_ANNOTATIONS);
    const chunks = [];
    for (let index = 0; index < limited.length; index += ANNOTATIONS_PER_STEP) {
        chunks.push(limited.slice(index, index + ANNOTATIONS_PER_STEP));
    }
    await Promise.all(
        chunks.map((chunk, index) =>
            fs.writeFile(path.join(annotationDirectory, `chunk-${index + 1}.log`), `${chunk.join("\n")}\n`),
        ),
    );
    await fs.appendFile(
        githubOutput,
        `annotation_chunks=${chunks.length}\nannotation_count=${annotations.length}\n`,
    );
    return { annotationChunks: chunks.length, annotationCount: annotations.length };
}

function renderUnavailable(error) {
    const message = error instanceof Error ? error.message : String(error);
    return {
        summary: ["## Android tests", "", "❌ **Android test results unavailable**", "", htmlMessage(message)].join("\n"),
        annotation: `::error title=${escapeWorkflowCommandProperty("Android test results unavailable")}::${escapeWorkflowCommandData(message)}`,
    };
}

async function loadDocuments(reportRoot) {
    const files = await collectXmlFiles(reportRoot);
    if (files.length === 0) throw new Error(`Managed Device XML 결과가 없습니다: ${reportRoot}`);
    return Promise.all(files.map(async (file) => ({ file, xml: await fs.readFile(file, "utf8") })));
}

async function appendSummary(summary, summaryPath = process.env.GITHUB_STEP_SUMMARY) {
    if (summaryPath) await fs.appendFile(summaryPath, `${summary}\n`);
    else process.stdout.write(`${summary}\n`);
}

function conciseVerificationLog(log) {
    const cleaned = String(log)
        .replaceAll(/\u001b\[[0-9;]*m/g, "")
        .trim();
    if (!cleaned) return "Selected androidTest verifier가 오류 메시지를 남기지 않았습니다.";
    if (cleaned.length <= MAX_ERROR_MESSAGE_LENGTH) return cleaned;
    return `…${cleaned.slice(-(MAX_ERROR_MESSAGE_LENGTH - 1)).trimStart()}`;
}

export async function readVerificationFailure(verificationLog, verificationExitCode) {
    const exitCode = String(verificationExitCode ?? "").trim();
    if (!exitCode || exitCode === "0") return null;
    if (!verificationLog) {
        return {
            exitCode,
            message: `Selected androidTest verifier가 exit code ${exitCode}로 실패했지만 로그 경로가 없습니다.`,
        };
    }
    try {
        return {
            exitCode,
            message: conciseVerificationLog(await fs.readFile(verificationLog, "utf8")),
        };
    } catch (error) {
        return {
            exitCode,
            message:
                `Selected androidTest verifier가 exit code ${exitCode}로 실패했지만 로그를 읽을 수 없습니다: ` +
                `${verificationLog} (${error instanceof Error ? error.message : error})`,
        };
    }
}

export async function renderAndroidTestResults(
    reportRoot,
    workspaceRoot,
    { verificationLog, verificationExitCode } = {},
) {
    const documents = await loadDocuments(reportRoot);
    const [sourceFiles, verificationFailure] = await Promise.all([
        collectTestSources(workspaceRoot),
        readVerificationFailure(verificationLog, verificationExitCode),
    ]);
    const summary = summarizeAndroidTestDocuments(documents, { sourceFiles });
    summary.verificationFailure = verificationFailure;
    return {
        documents,
        summary,
    };
}

async function main() {
    const [reportRoot, workspaceRoot, verificationLog, verificationExitCode] = process.argv.slice(2);
    if (!reportRoot || !workspaceRoot) {
        throw new Error("Managed Device XML 경로와 workspace 경로가 필요합니다.");
    }

    let reportPublished = false;
    try {
        const rendered = await renderAndroidTestResults(reportRoot, workspaceRoot, {
            verificationLog,
            verificationExitCode,
        });
        await appendSummary(renderAndroidTestSummary(rendered.summary));
        reportPublished = true;
        await emitAnnotations(renderAnnotations(rendered.summary));
        if (rendered.summary.failed > 0) {
            throw new Error(`Managed Device XML에 실패한 테스트가 ${rendered.summary.failed}개 있습니다.`);
        }
        if (rendered.summary.infrastructureFailures.length > 0) {
            throw new Error("Managed Device 테스트 실행이 완료되지 않았습니다.");
        }
        if (rendered.summary.verificationFailure != null) {
            throw new Error("Selected androidTest result verification failed");
        }
    } catch (error) {
        if (!reportPublished) {
            const unavailable = renderUnavailable(error);
            await appendSummary(unavailable.summary);
            process.stdout.write(`${unavailable.annotation}\n`);
        }
        throw error;
    }
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.message : error);
        process.exitCode = 1;
    });
}
