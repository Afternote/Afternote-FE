import assert from "node:assert/strict";
import test from "node:test";

import {
    renderIssueBody,
    selectActionableFindings,
} from "./publish-dependency-audit-issues.mjs";

function audit(overrides = {}) {
    return {
        generatedAt: "2026-08-21T00:00:00.000Z",
        commitSha: "abc123",
        runUrl: "https://github.com/Afternote/Afternote-FE/actions/runs/1",
        entries: [],
        vulnerabilities: [],
        consistencyFindings: [],
        compatibility: { exitCode: 0 },
        ...overrides,
    };
}

test("creates findings for security, actionable consistency, core major, and compatibility failures", () => {
    const findings = selectActionableFindings(
        audit({
            entries: [
                {
                    kind: "library",
                    alias: "okhttp",
                    coordinate: "com.squareup.okhttp3:okhttp",
                    currentVersion: "5.4.0",
                    latestStable: "6.0.0",
                    latestInChannel: "5.5.0",
                    updateKind: "major",
                    metadata: { url: "https://repo1.maven.org/example" },
                },
                {
                    kind: "library",
                    alias: "googleid",
                    coordinate: "com.google.android.libraries.identity.googleid:googleid",
                    currentVersion: "1.1.1",
                    latestStable: "1.2.0",
                    updateKind: "minor",
                },
            ],
            vulnerabilities: [
                {
                    coordinate: "org.example:vulnerable",
                    version: "1.0.0",
                    aliases: [],
                    vulnerabilities: [{ id: "GHSA-abcd-efgh-ijkl" }],
                },
            ],
            consistencyFindings: [
                {
                    type: "declared-resolved-mismatch",
                    alias: "androidx-compose-runtime",
                    coordinate: "androidx.compose.runtime:runtime",
                    message: "선언 1.10.6이 1.11.4로 해석됩니다.",
                },
                {
                    type: "unresolved-version",
                    alias: "firebase-messaging",
                    coordinate: "com.google.firebase:firebase-messaging",
                    message: "수집 공백",
                },
            ],
            compatibility: { exitCode: 1 },
        }),
    );
    assert.deepEqual(
        findings.map((finding) => finding.key),
        [
            "compatibility:dependency-audit",
            "consistency:androidx.compose.runtime:runtime",
            "major:com.squareup.okhttp3:okhttp:6",
            "security:org.example:vulnerable",
        ],
    );
});

test("does not create issues for ordinary patch or minor updates", () => {
    const findings = selectActionableFindings(
        audit({
            entries: [
                {
                    kind: "library",
                    alias: "okhttp",
                    coordinate: "com.squareup.okhttp3:okhttp",
                    currentVersion: "5.4.0",
                    latestStable: "5.5.0",
                    updateKind: "minor",
                },
            ],
        }),
    );
    assert.deepEqual(findings, []);
});

test("renders the repository template without changing optional No response sections", () => {
    const twoSpaces = "  ";
    const fourSpaces = "    ";
    const template = [
        "---",
        "name: Custom",
        "---",
        "",
        `## 📜 Overview (Required)${fourSpaces}`,
        `<!-- overview -->${twoSpaces}`,
        `No response${twoSpaces}`,
        "",
        `## 📌 Child Issue(Optional)${twoSpaces}`,
        `No response${twoSpaces}`,
        "",
    ].join("\n");
    const [finding] = selectActionableFindings(
        audit({
            consistencyFindings: [
                {
                    type: "declared-resolved-mismatch",
                    alias: "runtime",
                    coordinate: "androidx.compose.runtime:runtime",
                    message: "버전 불일치",
                },
            ],
        }),
    );
    const body = renderIssueBody(template, finding);
    assert.doesNotMatch(body, /^---/);
    assert.match(body, /dependency-audit-key: consistency:androidx\.compose\.runtime:runtime/);
    assert.match(body, /## 📌 Child Issue\(Optional\)[\s\S]*No response  /);
});
