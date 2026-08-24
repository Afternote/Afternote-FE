import assert from "node:assert/strict";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import {
    collectModuleSourceKeys,
    coverageForSourceKeys,
    parseKoverXml,
    renderSummary,
    selectChangedModules,
} from "./render-kover-summary.mjs";

const REPORT = `<?xml version="1.0" encoding="UTF-8"?>
<report name="sample">
  <package name="com/example/app">
    <sourcefile name="Home.kt">
      <line nr="1" mi="0" ci="1" mb="0" cb="0"/>
      <counter type="BRANCH" missed="1" covered="3"/>
      <counter type="LINE" missed="2" covered="8"/>
    </sourcefile>
  </package>
  <package name="com/example/core">
    <sourcefile name="Token.java">
      <counter type="BRANCH" missed="0" covered="0"/>
      <counter type="LINE" missed="5" covered="5"/>
    </sourcefile>
  </package>
  <counter type="INSTRUCTION" missed="10" covered="20"/>
  <counter type="BRANCH" missed="1" covered="3"/>
  <counter type="LINE" missed="7" covered="13"/>
</report>`;

test("parses report-level and source-level line and branch counters", () => {
    const report = parseKoverXml(REPORT);

    assert.deepEqual(report.aggregate.LINE, { missed: 7, covered: 13 });
    assert.deepEqual(report.aggregate.BRANCH, { missed: 1, covered: 3 });
    assert.deepEqual(report.sources.get("com.example.app:Home.kt")?.LINE, {
        missed: 2,
        covered: 8,
    });
});

test("maps changed paths to the deepest app, core, or feature module", () => {
    assert.deepEqual(
        selectChangedModules(
            [
                "README.md",
                "app/src/main/Home.kt",
                "core/network/src/test/TokenTest.kt",
                "feature/timeletter/presentation/build.gradle.kts",
            ],
            ["app", "core/network", "feature/timeletter/domain", "feature/timeletter/presentation"],
        ),
        ["app", "core/network", "feature/timeletter/presentation"],
    );
});

test("collects package and file keys from main and debug source sets", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "kover-summary-"));
    await fs.mkdir(path.join(root, "app/src/main/kotlin/com/example/app"), { recursive: true });
    await fs.mkdir(path.join(root, "app/src/debug/java/com/example/debug"), { recursive: true });
    await fs.writeFile(
        path.join(root, "app/src/main/kotlin/com/example/app/Home.kt"),
        "package com.example.app\nclass Home\n",
    );
    await fs.writeFile(
        path.join(root, "app/src/debug/java/com/example/debug/DebugOnly.java"),
        "package com.example.debug; class DebugOnly {}\n",
    );

    assert.deepEqual(
        [...(await collectModuleSourceKeys(root, "app"))].sort(),
        ["com.example.app:Home.kt", "com.example.debug:DebugOnly.java"],
    );
});

test("sums only source files owned by a changed module", () => {
    const report = parseKoverXml(REPORT);
    const coverage = coverageForSourceKeys(
        report,
        new Set(["com.example.app:Home.kt", "com.example.app:Missing.kt"]),
    );

    assert.deepEqual(coverage.counters.LINE, { missed: 2, covered: 8 });
    assert.deepEqual(coverage.counters.BRANCH, { missed: 1, covered: 3 });
    assert.equal(coverage.matchedSources, 1);
    assert.equal(coverage.declaredSources, 2);
});

test("renders artifact link, aggregate totals, changed modules, and report-only policy", () => {
    const report = parseKoverXml(REPORT);
    const summary = renderSummary({
        aggregate: report.aggregate,
        artifactUrl: "https://example.test/artifact",
        modules: [
            {
                name: "app",
                coverage: coverageForSourceKeys(report, new Set(["com.example.app:Home.kt"])),
            },
        ],
    });

    assert.match(summary, /https:\/\/example\.test\/artifact/);
    assert.match(summary, /Aggregate line: \*\*65\.00% \(13\/20\)\*\*/);
    assert.match(summary, /`app` \| 80\.00% \(8\/10\) \| 75\.00% \(3\/4\)/);
    assert.match(summary, /no coverage percentage threshold is enforced/);
});
