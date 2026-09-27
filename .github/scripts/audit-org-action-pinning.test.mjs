import assert from "node:assert/strict";
import test from "node:test";

import {
    classifyActionReference,
    collectExternalActionNames,
    extractActionReferences,
    parseArguments,
    selectPolicyRelevantBlobs,
    summarizeAudit,
} from "./audit-org-action-pinning.mjs";

const PINNED = "actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1";

test("collects uses from step sequences, job-level reusable workflows, and quoted values", () => {
    const source = [
        "jobs:",
        "  lint:",
        "    uses: ./.github/workflows/lint.yml",
        "  build:",
        "    steps:",
        `      - uses: ${PINNED} # v7.0.1`,
        "      - name: setup",
        '        uses: "actions/setup-java@dd06d9cba3e5552c54d9f8ea23572deb30010f7c"',
        "      - uses: ./.github/actions/setup-ci-config",
    ].join("\n");

    assert.deepEqual(extractActionReferences(source).map((entry) => entry.reference), [
        "./.github/workflows/lint.yml",
        PINNED,
        "actions/setup-java@dd06d9cba3e5552c54d9f8ea23572deb30010f7c",
        "./.github/actions/setup-ci-config",
    ]);
});

test("ignores commented-out uses lines and run steps that merely mention uses", () => {
    const source = [
        "      # uses: actions/checkout@v5",
        "      - run: echo 'uses: actions/checkout@v5'",
        `      - uses: ${PINNED}`,
    ].join("\n");

    assert.deepEqual(extractActionReferences(source).map((entry) => entry.reference), [PINNED]);
});

test("separates SHA-pinned references from every floating form the policy rejects", () => {
    assert.equal(classifyActionReference(PINNED).kind, "pinned");
    assert.equal(classifyActionReference("actions/checkout@v7").kind, "floating");
    assert.equal(classifyActionReference("actions/checkout@main").kind, "floating");
    assert.equal(classifyActionReference("appleboy/ssh-action@v1.2.5").kind, "floating");
    // 짧은 SHA 는 «full-length commit SHA» 가 아니라서 정책이 막는다.
    assert.equal(classifyActionReference("actions/checkout@3d3c42e").kind, "floating");
    // 대문자 hex 는 git 이 만드는 형태가 아니고 정책도 받아 주지 않는다.
    assert.equal(
        classifyActionReference("actions/checkout@3D3C42E5AAC5BA805825DA76410C181273BA90B1").kind,
        "floating",
    );
});

test("exempts the reference kinds the pinning policy does not govern", () => {
    assert.equal(classifyActionReference("./.github/actions/setup-ci-config").kind, "local");
    assert.equal(classifyActionReference("./.github/workflows/lint.yml").kind, "local");
    assert.equal(classifyActionReference("docker://alpine:3.20").kind, "docker");
    assert.equal(classifyActionReference("${{ matrix.action }}").kind, "expression");
    assert.equal(classifyActionReference("actions/checkout").kind, "unversioned");
});

test("keeps remote reusable workflows compatible and in the allow-list for every supported ref", () => {
    for (const file of ["build.yml", "build.yaml"]) {
        for (const version of ["v1", "main", "release/v1", "a".repeat(40)]) {
            const action = `octo-org/another-repo/.github/workflows/${file}`;
            const reference = `${action}@${version}`;
            const classified = classifyActionReference(reference, { reusableWorkflow: true });
            const summary = summarizeAudit([{ repository: "fixture", ...classified }]);

            assert.deepEqual(classified, { reference, kind: "reusable-workflow", action, version });
            assert.equal(summary.compatible, true);
            assert.equal(summary.totals.floating, 0);
            assert.equal(summary.totals.reusableWorkflows, 1);
            assert.equal(summary.repositories[0].reusableWorkflows, 1);
            assert.deepEqual(summary.externalActions, [action]);
        }
    }
});

test("still blocks floating actions when reusable workflows appear in the same audit", () => {
    const summary = summarizeAudit([
        "octo-org/another-repo/.github/workflows/build.yml@v1",
        "gradle/actions/setup-gradle@v6",
    ].map((reference) => ({ repository: "fixture", ...classifyActionReference(reference, { reusableWorkflow: true }) })));

    assert.equal(summary.compatible, false);
    assert.equal(summary.totals.reusableWorkflows, 1);
    assert.equal(summary.totals.floating, 1);
    assert.deepEqual(summary.repositories[0].floatingReferences.map((entry) => entry.reference), [
        "gradle/actions/setup-gradle@v6",
    ]);
});

test("does not exempt malformed workflow paths or an empty version", () => {
    for (const reference of [
        "octo-org/another-repo/.github/workflows/build.yml@",
        "octo-org/another-repo/.github/workflows/nested/build.yml@v1",
        "octo-org/another-repo/.github/workflows/build.yml/action.yml@v1",
        "octo-org/another-repo/.github/workflows/build.txt@v1",
        "octo-org/another-repo/sub/.github/workflows/build.yml@v1",
    ]) {
        const entry = classifyActionReference(reference, { reusableWorkflow: true });
        assert.equal(entry.kind, "floating", reference);
        assert.equal(summarizeAudit([{ repository: "fixture", ...entry }]).compatible, false);
    }
});

test("applies the exception only at jobs.<id>.uses in workflow files", () => {
    const reference = "octo-org/another-repo/.github/workflows/build.yml@main";
    const source = [
        "jobs:",
        "  reuse:",
        `    uses: ${reference}`,
        "  action:",
        "    steps:",
        `      - uses: ${reference}`,
        "      - name: another action",
        `        uses: ${reference}`,
        "  'second-reuse':",
        `    uses: ${reference}`,
    ].join("\n");
    const entries = extractActionReferences(source, ".github/workflows/ci.yml").map((entry) => ({
        repository: "fixture", ...classifyActionReference(entry.reference, entry),
    }));
    assert.deepEqual(entries.map((entry) => entry.kind), [
        "reusable-workflow", "floating", "floating", "reusable-workflow",
    ]);
    assert.equal(summarizeAudit(entries).compatible, false);

    const composite = extractActionReferences(`runs:\n  using: composite\n  steps:\n    - uses: ${reference}`, "action.yml");
    assert.equal(classifyActionReference(composite[0].reference, composite[0]).kind, "floating");
    assert.equal(classifyActionReference(reference).kind, "floating");
    assert.ok(extractActionReferences(source, "action.yml").every((entry) => !entry.reusableWorkflow));
});

test("keeps the subpath when naming an action, because allow-list wildcards stop at the slash", () => {
    const reference = classifyActionReference("gradle/actions/setup-gradle@v6");
    assert.equal(reference.action, "gradle/actions/setup-gradle");
    assert.equal(reference.version, "v6");

    assert.deepEqual(
        collectExternalActionNames([
            classifyActionReference("gradle/actions/setup-gradle@v6"),
            classifyActionReference("gradle/actions/dependency-submission@v6"),
            classifyActionReference(PINNED),
            classifyActionReference("./.github/actions/setup-ci-config"),
        ]),
        [
            "actions/checkout",
            "gradle/actions/dependency-submission",
            "gradle/actions/setup-gradle",
        ],
    );
});

test("summarizes per repository and reports incompatibility when any floating reference remains", () => {
    const entries = [
        { repository: "Afternote/Afternote-FE", ...classifyActionReference(PINNED) },
        {
            repository: "Afternote/Afternote-FE",
            ...classifyActionReference("./.github/actions/setup-ci-config"),
        },
        {
            repository: "Afternote/Afternote-BE",
            ...classifyActionReference("actions/checkout@v7"),
        },
    ];

    const summary = summarizeAudit(entries);
    assert.equal(summary.compatible, false);
    assert.deepEqual(summary.totals, {
        references: 3, pinned: 1, floating: 1, reusableWorkflows: 0, local: 1, other: 0,
    });
    assert.deepEqual(
        summary.repositories.map((item) => [item.repository, item.pinned, item.floating]),
        [
            ["Afternote/Afternote-BE", 0, 1],
            ["Afternote/Afternote-FE", 1, 0],
        ],
    );
    assert.equal(summary.repositories[0].floatingReferences[0].reference, "actions/checkout@v7");
});

test("reports compatibility only when nothing floats", () => {
    const summary = summarizeAudit([
        { repository: "Afternote/Afternote-FE", ...classifyActionReference(PINNED) },
        {
            repository: "Afternote/Afternote-FE",
            ...classifyActionReference("./.github/workflows/lint.yml"),
        },
    ]);
    assert.equal(summary.compatible, true);
    assert.deepEqual(summary.externalActions, ["actions/checkout"]);
});

test("scans workflows and composite action manifests, and nothing else", () => {
    const tree = [
        { type: "blob", path: ".github/workflows/ci.yml", sha: "aaa" },
        { type: "blob", path: ".github/workflows/deploy.yaml", sha: "bbb" },
        { type: "blob", path: ".github/actions/setup-ci-config/action.yml", sha: "ccc" },
        { type: "blob", path: ".github/workflows/README.md", sha: "ddd" },
        { type: "blob", path: ".github/dependabot.yml", sha: "eee" },
        { type: "blob", path: "docs/qa/status.md", sha: "fff" },
        { type: "tree", path: ".github/workflows", sha: "ggg" },
    ];

    assert.deepEqual(selectPolicyRelevantBlobs(tree), [
        { path: ".github/actions/setup-ci-config/action.yml", sha: "ccc" },
        { path: ".github/workflows/ci.yml", sha: "aaa" },
        { path: ".github/workflows/deploy.yaml", sha: "bbb" },
    ]);
    assert.deepEqual(selectPolicyRelevantBlobs(undefined), []);
});

test("requires an organization and accepts the optional output and api overrides", () => {
    assert.deepEqual(parseArguments(["--org", "Afternote"]), {
        org: "Afternote",
        output: null,
        apiUrl: "https://api.github.com",
    });
    assert.deepEqual(
        parseArguments([
            "--org",
            "Afternote",
            "--output",
            "/tmp/audit.json",
            "--api-url",
            "https://example.test",
        ]),
        { org: "Afternote", output: "/tmp/audit.json", apiUrl: "https://example.test" },
    );
    assert.throws(() => parseArguments([]), /--org is required/);
    assert.throws(() => parseArguments(["--nope"]), /unknown argument/);
});
