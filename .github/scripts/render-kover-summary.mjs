#!/usr/bin/env node

import { execFileSync } from "node:child_process";
import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

const COVERAGE_SCOPES = ["app", "core", "feature"];
const SOURCE_SETS = ["main", "debug"];

function decodeXml(value) {
    return String(value)
        .replaceAll("&quot;", '"')
        .replaceAll("&apos;", "'")
        .replaceAll("&lt;", "<")
        .replaceAll("&gt;", ">")
        .replaceAll("&amp;", "&");
}

function attribute(attributes, name) {
    const match = new RegExp(`\\b${name}="([^"]*)"`).exec(attributes);
    return match ? decodeXml(match[1]) : null;
}

function emptyCounters() {
    return {
        LINE: { missed: 0, covered: 0 },
        BRANCH: { missed: 0, covered: 0 },
    };
}

function countersFrom(fragment) {
    const counters = emptyCounters();
    const counterPattern = /<counter\b([^>]*)\/?\s*>/g;
    let match;
    while ((match = counterPattern.exec(fragment)) !== null) {
        const type = attribute(match[1], "type");
        if (!(type in counters)) {
            continue;
        }
        counters[type] = {
            missed: Number(attribute(match[1], "missed")),
            covered: Number(attribute(match[1], "covered")),
        };
    }
    return counters;
}

export function parseKoverXml(xml) {
    const sources = new Map();
    const packagePattern = /<package\b([^>]*)>([\s\S]*?)<\/package>/g;
    let packageMatch;
    while ((packageMatch = packagePattern.exec(xml)) !== null) {
        const packageName = (attribute(packageMatch[1], "name") ?? "").replaceAll("/", ".");
        const sourcePattern = /<sourcefile\b([^>]*)>([\s\S]*?)<\/sourcefile>/g;
        let sourceMatch;
        while ((sourceMatch = sourcePattern.exec(packageMatch[2])) !== null) {
            const sourceName = attribute(sourceMatch[1], "name");
            if (!sourceName) {
                throw new Error(`Kover sourcefile without a name in package ${packageName}`);
            }
            const key = `${packageName}:${sourceName}`;
            if (sources.has(key)) {
                throw new Error(`Ambiguous Kover source key: ${key}`);
            }
            sources.set(key, countersFrom(sourceMatch[2]));
        }
    }

    const lastPackageEnd = xml.lastIndexOf("</package>");
    const reportTail = lastPackageEnd >= 0 ? xml.slice(lastPackageEnd + "</package>".length) : xml;
    return { sources, aggregate: countersFrom(reportTail) };
}

async function walk(directory, predicate, results = []) {
    let entries;
    try {
        entries = await fs.readdir(directory, { withFileTypes: true });
    } catch (error) {
        if (error?.code === "ENOENT") {
            return results;
        }
        throw error;
    }
    for (const entry of entries) {
        const absolute = path.join(directory, entry.name);
        if (entry.isDirectory()) {
            if (!["build", ".gradle", ".git"].includes(entry.name)) {
                await walk(absolute, predicate, results);
            }
        } else if (predicate(absolute)) {
            results.push(absolute);
        }
    }
    return results;
}

export async function discoverCoverageModules(root) {
    const modules = [];
    for (const scope of COVERAGE_SCOPES) {
        const scopeRoot = path.join(root, scope);
        const buildFiles = await walk(scopeRoot, (file) => path.basename(file) === "build.gradle.kts");
        for (const buildFile of buildFiles) {
            modules.push(path.relative(root, path.dirname(buildFile)).split(path.sep).join("/"));
        }
    }
    return [...new Set(modules)].sort();
}

export function selectChangedModules(changedFiles, modules) {
    const longestFirst = [...modules].sort((left, right) => right.length - left.length);
    const selected = new Set();
    for (const file of changedFiles) {
        const normalized = file.split(path.sep).join("/");
        const module = longestFirst.find(
            (candidate) => normalized === candidate || normalized.startsWith(`${candidate}/`),
        );
        if (module) {
            selected.add(module);
        }
    }
    return [...selected].sort();
}

export async function collectModuleSourceKeys(root, module) {
    const keys = new Set();
    for (const sourceSet of SOURCE_SETS) {
        const sourceRoot = path.join(root, module, "src", sourceSet);
        const sources = await walk(sourceRoot, (file) => /\.(?:kt|java)$/.test(file));
        for (const source of sources) {
            const content = await fs.readFile(source, "utf8");
            const packageName = /^\s*package\s+([A-Za-z_][A-Za-z0-9_.]*)/m.exec(content)?.[1] ?? "";
            keys.add(`${packageName}:${path.basename(source)}`);
        }
    }
    return keys;
}

function addCounters(target, source) {
    for (const type of ["LINE", "BRANCH"]) {
        target[type].missed += source[type]?.missed ?? 0;
        target[type].covered += source[type]?.covered ?? 0;
    }
}

export function coverageForSourceKeys(report, sourceKeys) {
    const counters = emptyCounters();
    let matchedSources = 0;
    for (const sourceKey of sourceKeys) {
        const sourceCounters = report.sources.get(sourceKey);
        if (sourceCounters) {
            matchedSources += 1;
            addCounters(counters, sourceCounters);
        }
    }
    return { counters, matchedSources, declaredSources: sourceKeys.size };
}

function formatCounter(counter) {
    const total = counter.covered + counter.missed;
    if (total === 0) {
        return "N/A (0/0)";
    }
    return `${((counter.covered / total) * 100).toFixed(2)}% (${counter.covered}/${total})`;
}

export function renderSummary({ aggregate, modules, artifactUrl }) {
    const lines = [
        "## Kover JVM unit-test coverage",
        "",
        `[Download the full aggregate HTML/XML report](${artifactUrl})`,
        "",
        `Aggregate line: **${formatCounter(aggregate.LINE)}** · branch: **${formatCounter(aggregate.BRANCH)}**`,
        "",
        "### Changed modules",
        "",
    ];

    if (modules.length === 0) {
        lines.push("No app/core/feature Gradle module changed in this pull request.");
    } else {
        lines.push("| Module | Line | Branch | Mapped source files |");
        lines.push("| --- | ---: | ---: | ---: |");
        for (const module of modules) {
            lines.push(
                `| \`${module.name}\` | ${formatCounter(module.coverage.counters.LINE)} | ${formatCounter(module.coverage.counters.BRANCH)} | ${module.coverage.matchedSources}/${module.coverage.declaredSources} |`,
            );
        }
    }

    lines.push("", "> Report-only baseline: no coverage percentage threshold is enforced.", "");
    return lines.join("\n");
}

function changedFilesBetween(baseSha, headSha) {
    const output = execFileSync(
        "git",
        ["diff", "--name-only", "--diff-filter=ACMRT", `${baseSha}...${headSha}`],
        { encoding: "utf8" },
    );
    return output.split(/\r?\n/).filter(Boolean);
}

async function main() {
    const root = process.cwd();
    const baseSha = process.env.KOVER_BASE_SHA;
    const headSha = process.env.KOVER_HEAD_SHA;
    const artifactUrl = process.env.KOVER_ARTIFACT_URL;
    const outputPath = process.env.GITHUB_STEP_SUMMARY;
    const configuredModules = process.env.KOVER_COVERAGE_MODULES ?? "all";
    if (!baseSha || !headSha || !artifactUrl || !outputPath) {
        throw new Error(
            "KOVER_BASE_SHA, KOVER_HEAD_SHA, KOVER_ARTIFACT_URL, and GITHUB_STEP_SUMMARY are required",
        );
    }

    let report;
    let modules;
    if (configuredModules === "all") {
        const reportXml = await fs.readFile(
            path.join(root, "build", "reports", "kover", "reportCi.xml"),
            "utf8",
        );
        report = parseKoverXml(reportXml);
        const allModules = await discoverCoverageModules(root);
        const changedModules = selectChangedModules(changedFilesBetween(baseSha, headSha), allModules);
        modules = [];
        for (const module of changedModules) {
            const sourceKeys = await collectModuleSourceKeys(root, module);
            modules.push({ name: module, coverage: coverageForSourceKeys(report, sourceKeys) });
        }
    } else {
        report = { aggregate: emptyCounters() };
        modules = [];
        for (const projectPath of configuredModules.split(/\s+/).filter(Boolean)) {
            const module = projectPath.replace(/^:/, "").replaceAll(":", "/");
            const reportXml = await fs.readFile(
                path.join(root, module, "build", "reports", "kover", "reportCi.xml"),
                "utf8",
            );
            const moduleReport = parseKoverXml(reportXml);
            addCounters(report.aggregate, moduleReport.aggregate);
            modules.push({
                name: module,
                coverage: {
                    counters: moduleReport.aggregate,
                    matchedSources: moduleReport.sources.size,
                    declaredSources: moduleReport.sources.size,
                },
            });
        }
    }

    await fs.appendFile(
        outputPath,
        renderSummary({ aggregate: report.aggregate, modules, artifactUrl }),
        "utf8",
    );
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.stack : error);
        process.exitCode = 1;
    });
}
