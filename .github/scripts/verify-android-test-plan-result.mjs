#!/usr/bin/env node

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

function decodeXml(value) {
    return value
        .replaceAll("&quot;", '"')
        .replaceAll("&apos;", "'")
        .replaceAll("&lt;", "<")
        .replaceAll("&gt;", ">")
        .replaceAll("&amp;", "&");
}

function attributes(fragment) {
    return new Map(
        [...fragment.matchAll(/([A-Za-z_:][\w:.-]*)="([^"]*)"/g)].map((match) => [
            match[1],
            decodeXml(match[2]),
        ]),
    );
}

async function collectXmlFiles(root) {
    const files = [];
    async function visit(directory) {
        let entries;
        try {
            entries = await fs.readdir(directory, { withFileTypes: true });
        } catch (error) {
            if (error?.code === "ENOENT") return;
            throw error;
        }
        for (const entry of entries) {
            const target = path.join(directory, entry.name);
            if (entry.isDirectory()) await visit(target);
            else if (entry.isFile() && entry.name.endsWith(".xml")) files.push(target);
        }
    }
    await visit(root);
    return files;
}

export function verifySelectedAndroidTests(selectors, documents) {
    const pendingMethods = new Set(selectors.filter((selector) => selector.includes("#")));
    const classSelectors = new Set(selectors.filter((selector) => !selector.includes("#")));
    const matchedClasses = new Set();
    for (const { file, xml } of documents) {
        for (const match of xml.matchAll(/<testcase\b([^>]*?)(?:\/>|>([\s\S]*?)<\/testcase>)/g)) {
            const fields = attributes(match[1]);
            const className = fields.get("classname") ?? fields.get("class") ?? "";
            const testName = (fields.get("name") ?? "").replace(/\[[^\]]*\]$/, "");
            const selector = `${className}#${testName}`;
            const matchesMethod = pendingMethods.has(selector);
            const matchesClass = classSelectors.has(className);
            if (!matchesMethod && !matchesClass) continue;
            if (/<(?:failure|error|skipped)\b/.test(match[2] ?? "")) {
                throw new Error(`선택 계측 테스트가 성공하지 않았습니다: ${selector} (${file})`);
            }
            pendingMethods.delete(selector);
            if (matchesClass) matchedClasses.add(className);
        }
    }
    const missing = [
        ...pendingMethods,
        ...[...classSelectors].filter((selector) => !matchedClasses.has(selector)),
    ];
    if (missing.length > 0) {
        throw new Error(`Managed Device XML에서 실행 결과를 찾지 못했습니다: ${missing.join(", ")}`);
    }
    return selectors.length;
}

export async function verifyAndroidTestPlanResult(selectors, reportRoot) {
    const files = await collectXmlFiles(reportRoot);
    if (files.length === 0) {
        throw new Error(`Managed Device XML 결과가 없습니다: ${reportRoot}`);
    }
    const documents = await Promise.all(
        files.map(async (file) => ({ file, xml: await fs.readFile(file, "utf8") })),
    );
    return verifySelectedAndroidTests(selectors, documents);
}

async function main() {
    const [selectorsJson, reportRoot] = process.argv.slice(2);
    if (!selectorsJson || !reportRoot) {
        throw new Error("selector JSON 배열과 Managed Device XML 경로가 필요합니다.");
    }
    const selectors = JSON.parse(selectorsJson);
    if (!Array.isArray(selectors) || selectors.some((value) => typeof value !== "string")) {
        throw new Error("selector 인수는 JSON 문자열 배열이어야 합니다.");
    }
    const count = await verifyAndroidTestPlanResult(selectors, reportRoot);
    console.log(`Selected androidTest results verified: ${count}`);
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.message : error);
        process.exitCode = 1;
    });
}
