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

async function xmlFiles(root) {
    const files = [];
    async function visit(directory) {
        let entries;
        try {
            entries = await fs.readdir(directory, { withFileTypes: true });
        } catch (error) {
            if (error.code === "ENOENT") return;
            throw error;
        }
        for (const entry of entries) {
            const target = path.join(directory, entry.name);
            if (entry.isDirectory()) {
                await visit(target);
            } else if (entry.isFile() && entry.name.endsWith(".xml")) {
                files.push(target);
            }
        }
    }
    await visit(root);
    return files;
}

export function verifyAndroidTestXml(testRef, documents) {
    const match = /\/([^/#]+)\.kt#([A-Za-z_][A-Za-z0-9_]*)$/.exec(testRef);
    if (!match) {
        throw new Error(`올바르지 않은 androidTest testRef: ${testRef}`);
    }
    const [, expectedClass, expectedName] = match;

    for (const { file, xml } of documents) {
        for (const testcase of xml.matchAll(/<testcase\b([^>]*?)(?:\/>|>([\s\S]*?)<\/testcase>)/g)) {
            const fields = attributes(testcase[1]);
            const className = fields.get("classname") ?? fields.get("class") ?? "";
            const testName = fields.get("name") ?? "";
            const simpleClassName = className.split(".").at(-1)?.split("$")[0] ?? "";
            if (simpleClassName !== expectedClass || testName !== expectedName) {
                continue;
            }
            if (/<(?:failure|error|skipped)\b/.test(testcase[2] ?? "")) {
                throw new Error(`선언한 계측 테스트가 성공하지 않았습니다: ${testRef} (${file})`);
            }
            return { file, className, testName };
        }
    }

    throw new Error(`Managed Device XML에서 선언한 계측 테스트 실행 결과를 찾지 못했습니다: ${testRef}`);
}

export async function verifyAndroidTestResult(testRef, reportRoot) {
    const files = await xmlFiles(reportRoot);
    if (files.length === 0) {
        throw new Error(`Managed Device XML 결과가 없습니다: ${reportRoot}`);
    }
    const documents = await Promise.all(
        files.map(async (file) => ({ file, xml: await fs.readFile(file, "utf8") })),
    );
    return verifyAndroidTestXml(testRef, documents);
}

async function main() {
    const [testRef, reportRoot] = process.argv.slice(2);
    if (!testRef || !reportRoot) {
        throw new Error("testRef와 Managed Device XML 경로가 필요합니다.");
    }
    const result = await verifyAndroidTestResult(testRef, reportRoot);
    console.log(`Declared androidTest executed: ${result.className}#${result.testName}`);
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error.message);
        process.exitCode = 1;
    });
}
