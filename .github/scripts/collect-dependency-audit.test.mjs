import assert from "node:assert/strict";
import test from "node:test";

import {
    classifyUpdate,
    compareVersions,
    consistencyFindings,
    detectCatalogUsage,
    parseBomPom,
    parseResolvedDependencies,
    parseVersionCatalog,
    renderSummary,
} from "./collect-dependency-audit.mjs";

const catalogText = `
[versions]
kotlin = "2.4.0"
composeBom = "2026.06.01"
runtime = "1.10.6"

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-runtime = { group = "androidx.compose.runtime", name = "runtime", version.ref = "runtime" }
kotlin-gradlePlugin = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin", version.ref = "kotlin" }
unused = { module = "example:unused", version = "1.0.0" }

[plugins]
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
`;

test("parses version catalog coordinates and refs", () => {
    const catalog = parseVersionCatalog(catalogText);
    assert.equal(catalog.versions.kotlin, "2.4.0");
    assert.equal(catalog.libraries["androidx-compose-runtime"].coordinate, "androidx.compose.runtime:runtime");
    assert.equal(catalog.plugins["kotlin-compose"].id, "org.jetbrains.kotlin.plugin.compose");
});

test("finds generated accessors and convention-plugin string aliases only", () => {
    const catalog = parseVersionCatalog(catalogText);
    const usage = detectCatalogUsage(catalog, [
        {
            path: "feature/build.gradle.kts",
            content: "implementation(libs.androidx.compose.runtime)\nalias(libs.plugins.kotlin.compose)",
        },
        {
            path: "build-logic/Compose.kt",
            content: 'val bom = libs.findLibrary("androidx-compose-bom").get()',
        },
    ]);
    assert.deepEqual(Object.keys(usage.libraries).sort(), [
        "androidx-compose-bom",
        "androidx-compose-runtime",
    ]);
    assert.deepEqual(Object.keys(usage.plugins), ["kotlin-compose"]);
    assert.equal(usage.libraries.unused, undefined);
});

test("orders prerelease channels below the matching stable release", () => {
    assert.ok(compareVersions("1.0.0", "1.0.0-rc13") > 0);
    assert.ok(compareVersions("1.0.0-rc13", "1.0.0-rc02") > 0);
    assert.equal(classifyUpdate("1.0.0-rc13", "1.0.0"), "channel");
    assert.equal(classifyUpdate("1.9.0", "2.0.0"), "major");
    assert.equal(classifyUpdate("2.4.0", "2.4.1"), "patch");
});

test("extracts BOM-managed versions", () => {
    const managed = parseBomPom(`
        <project>
          <properties><runtime.version>1.11.4</runtime.version></properties>
          <dependencyManagement><dependencies>
            <dependency>
              <groupId>androidx.compose.runtime</groupId>
              <artifactId>runtime</artifactId>
              <version>\${runtime.version}</version>
            </dependency>
          </dependencies></dependencyManagement>
        </project>
    `);
    assert.equal(managed["androidx.compose.runtime:runtime"], "1.11.4");
});

test("extracts selected Gradle versions after conflict resolution", () => {
    const dependencies = parseResolvedDependencies(
        "+--- androidx.compose.runtime:runtime:1.10.6 -> 1.11.4\n" +
            "+--- unused.constraint:only:9.9.9 (c)\n" +
            "\\--- com.squareup.okhttp3:okhttp:5.4.0",
        "app-runtime.txt",
    );
    assert.deepEqual(dependencies, [
        {
            coordinate: "androidx.compose.runtime:runtime",
            requestedVersion: "1.10.6",
            selectedVersion: "1.11.4",
            source: "app-runtime.txt",
        },
        {
            coordinate: "com.squareup.okhttp3:okhttp",
            requestedVersion: "5.4.0",
            selectedVersion: "5.4.0",
            source: "app-runtime.txt",
        },
    ]);
});

test("flags BOM and resolved-version mismatches", () => {
    const catalog = parseVersionCatalog(catalogText);
    const entries = [
        {
            alias: "androidx-compose-runtime",
            coordinate: "androidx.compose.runtime:runtime",
            currentVersion: "1.10.6",
            versionRef: "runtime",
            bom: {
                alias: "androidx-compose-bom",
                managedVersion: "1.11.4",
            },
        },
    ];
    const findings = consistencyFindings(
        entries,
        [{ coordinate: "androidx.compose.runtime:runtime", version: "1.11.4" }],
        catalog,
    );
    assert.deepEqual(
        findings.map((finding) => finding.type).sort(),
        ["bom-override-mismatch", "declared-resolved-mismatch"],
    );
});

test("flags Kotlin toolchain versions that drift apart", () => {
    const catalog = parseVersionCatalog(catalogText);
    const findings = consistencyFindings(
        [
            {
                alias: "kotlin-gradlePlugin",
                coordinate: "org.jetbrains.kotlin:kotlin-gradle-plugin",
                currentVersion: "2.4.0",
                versionRef: "kotlin",
            },
            {
                alias: "compose-compiler-gradle-plugin",
                coordinate: "org.jetbrains.kotlin:compose-compiler-gradle-plugin",
                currentVersion: "2.3.0",
                versionRef: "kotlin",
            },
        ],
        [],
        catalog,
    );
    assert.match(
        findings.find((finding) => finding.type === "toolchain-version-mismatch").message,
        /Kotlin\/Compose compiler/,
    );
});

test("renders a compact CI summary", () => {
    const summary = renderSummary({
        generatedAt: "2026-08-21T00:00:00.000Z",
        commitSha: "abc123",
        entries: [
            {
                alias: "okhttp",
                currentVersion: "5.4.0",
                latestStable: "6.0.0",
                latestInChannel: "5.5.0",
                updateKind: "major",
                metadata: { url: "https://repo1.maven.org/example" },
            },
        ],
        vulnerabilities: [],
        consistencyFindings: [],
        compatibility: { exitCode: 0 },
        coverage: {
            usedEntries: 1,
            resolvedPackages: 1,
            metadataGaps: 0,
            gaps: [],
        },
    });
    assert.match(summary, /okhttp/);
    assert.match(summary, /현재 호환성 검사: 통과/);
});
