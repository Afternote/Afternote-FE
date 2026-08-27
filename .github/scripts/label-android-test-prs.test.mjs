import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
    DEFAULT_LABEL,
    DEFAULT_PENDING_LABEL,
    applyPlan,
    classifyAndroidTestRequirement,
    ensureLabelExists,
    fetchChangedFiles,
    fetchCurrentDispatchTarget,
    planLabelChanges,
    renderSummary,
} from "./label-android-test-prs.mjs";

const labelWorkflow = await readFile(new URL("../workflows/conflict-label.yml", import.meta.url), "utf8");
const androidWorkflow = await readFile(new URL("../workflows/android-managed-device.yml", import.meta.url), "utf8");
const silent = { log() {} };

function fakeApi({ responses = {}, failOn = null } = {}) {
    const calls = [];
    const api = async (apiPath, options = {}) => {
        calls.push({ apiPath, method: options.method ?? "GET", body: options.body });
        if (failOn && failOn(apiPath, options)) {
            throw new Error("boom");
        }
        for (const [pattern, value] of Object.entries(responses)) {
            if (apiPath.startsWith(pattern)) {
                return typeof value === "function" ? value(apiPath, options) : value;
            }
        }
        return options.allowNotFound ? null : {};
    };
    api.calls = calls;
    return api;
}

function pullRequest(overrides = {}) {
    return {
        number: 1,
        headRefOid: "1111111111111111111111111111111111111111",
        headRepository: "Afternote/Afternote-FE",
        labels: [],
        files: [],
        ...overrides,
    };
}

function currentPullRequest(overrides = {}) {
    return {
        state: "open",
        head: {
            sha: "1111111111111111111111111111111111111111",
            repo: { full_name: "o/r" },
        },
        labels: [{ name: DEFAULT_LABEL }],
        ...overrides,
    };
}

function requiredQaBody(testRef) {
    return `## QA Metadata
\`\`\`json
${JSON.stringify({
    scope: "app-runtime",
    precondition: "로그인한 사용자가 대상 화면에 있다",
    action: "사용자 버튼을 눌러 실제 흐름을 실행한다",
    expected: "완료 화면과 저장 상태가 표시된다",
    risk: "사용자가 핵심 흐름을 완료할 수 없다",
    androidTest: {
        required: true,
        reason: "실제 Activity와 Compose 경계를 검증해야 한다",
        testRef,
    },
    evidence: [
        {
            kind: "test",
            ref: testRef,
            assertion: "같은 입력이 실제 화면 결과를 만든다",
            input: "사용자 버튼 클릭",
            boundary: "AndroidJUnit4 Activity와 Compose",
            observation: "완료 화면과 저장 호출",
        },
    ],
})}
\`\`\``;
}

test("default branch의 PR Validation 요청과 정기 실행에서 라벨을 복구한다", () => {
    assert.match(labelWorkflow, /^  workflow_run:\n    workflows: \["PR Validation"\]\n    types: \[requested\]$/m);
    assert.match(labelWorkflow, /node \.github\/scripts\/label-android-test-prs\.mjs/);
    assert.doesNotMatch(labelWorkflow, /^\s*pull_request_target\s*:/m);
    assert.match(labelWorkflow, /^permissions: \{\}$/m);
    assert.match(labelWorkflow, /^\s{6}actions: write$/m);
    assert.match(
        labelWorkflow,
        /^\s{4}if: github\.event_name != 'workflow_dispatch' \|\| github\.ref_name == github\.event\.repository\.default_branch$/m,
    );
    assert.match(labelWorkflow, /^\s{10}ref: \$\{\{ github\.event\.repository\.default_branch \}\}$/m);
    assert.match(labelWorkflow, /ANDROID_TEST_REDISPATCH_HEAD_SHA:.*workflow_run\.head_sha/);
});

test("자동 라벨 dispatch는 default branch workflow에서 현재 same-repository HEAD를 재검증한다", () => {
    assert.match(androidWorkflow, /^\s{6}pull_request_number:\n/m);
    assert.match(androidWorkflow, /^\s{6}expected_head_sha:\n/m);
    assert.match(androidWorkflow, /^\s{6}expected_test_ref:\n/m);
    assert.match(androidWorkflow, /EXPECTED_HEAD_SHA.*\$\{\{ inputs\.expected_head_sha/);
    assert.match(androidWorkflow, /head_repository.*!=.*GITHUB_REPOSITORY/);
    assert.match(androidWorkflow, /target_sha.*!=.*EXPECTED_HEAD_SHA/);
    assert.match(
        androidWorkflow,
        /github\.event_name == 'workflow_dispatch' &&\n\s+github\.ref_name == github\.event\.repository\.default_branch/,
    );
    assert.match(androidWorkflow, /ref: \$\{\{ steps\.target\.outputs\.sha \}\}/);
    assert.match(androidWorkflow, /persist-credentials: false/);
    assert.match(androidWorkflow, /resolve-android-test-ref\.mjs/);
    assert.match(androidWorkflow, /verify-android-test-result\.mjs/);
});

test("계측 소스, manifest, navigation, 앱 진입점과 managed-device 설정을 대상으로 본다", () => {
    const paths = [
        "app/src/androidTest/java/com/afternote/FlowAndroidTest.kt",
        "feature/timeletter/data/src/main/AndroidManifest.xml",
        "feature/setting/presentation/src/main/kotlin/com/afternote/navigation/SettingNavGraph.kt",
        "app/src/main/java/com/afternote/navigation/AppNavigationActions.kt",
        "app/src/main/kotlin/com/afternote/MainActivity.kt",
        "app/build.gradle.kts",
        ".github/workflows/android-managed-device.yml",
        ".github/actions/setup-ci-config/action.yml",
    ];

    const result = classifyAndroidTestRequirement(paths);

    assert.equal(result.required, true);
    assert.deepEqual(
        result.matches.map((match) => match.id),
        [
            "androidTest-source",
            "android-manifest",
            "runtime-navigation",
            "app-entry-point",
            "app-build-config",
            "managed-device-config",
        ],
    );
});

test("문서, 단위 테스트, screenshot baseline과 화면 내부 로직만으로는 무거운 계측 테스트를 강제하지 않는다", () => {
    const result =
        classifyAndroidTestRequirement([
            "docs/testing/android-managed-device.md",
            "core/data/src/test/kotlin/UserRepositoryTest.kt",
            "feature/mindrecord/presentation/src/screenshotTest/kotlin/MemorySpaceScreenshotTest.kt",
            "feature/mindrecord/presentation/src/screenshotTestDebug/reference/example.png",
            "feature/mindrecord/presentation/src/main/kotlin/com/afternote/screen/DailyQuestionWriteScreen.kt",
            "feature/home/presentation/src/main/kotlin/com/afternote/HomeTabViewModel.kt",
        ]);

    assert.equal(result.required, false);
    assert.deepEqual(result.matches, []);
});

test("경로로 알 수 없는 화면 내부 변경도 QA 메타데이터가 required면 대상으로 본다", () => {
    const result = classifyAndroidTestRequirement(
        ["feature/mindrecord/presentation/src/main/kotlin/screen/WriteScreen.kt"],
        { androidTestRequired: true },
    );

    assert.equal(result.required, true);
    assert.deepEqual(result.matches.map((match) => match.id), ["qa-metadata-decision"]);

    const plan = planLabelChanges({
        pullRequests: [
            pullRequest({
                number: 1266,
                androidTestRequired: true,
                files: ["feature/mindrecord/presentation/src/main/kotlin/screen/WriteScreen.kt"],
            }),
        ],
        repository: "Afternote/Afternote-FE",
    });
    assert.deepEqual(plan.toLabel.map((item) => item.number), [1266]);
});

test("실제 누락 사례 8건은 잡고 unit, docs, screenshot 전용 5건은 제외한다", () => {
    const pullRequests = [
        pullRequest({ number: 440, files: ["feature/timeletter/data/src/main/AndroidManifest.xml"] }),
        pullRequest({ number: 767, files: ["app/src/androidTest/java/TimeLetterLifecycleAndroidTest.kt"] }),
        pullRequest({ number: 771, files: ["app/src/androidTest/java/SettingCompletionAndroidTest.kt"] }),
        pullRequest({
            number: 882,
            files: [
                "feature/setting/presentation/src/main/kotlin/com/afternote/navigation/SettingNavGraph.kt",
            ],
        }),
        pullRequest({ number: 966, files: ["app/src/androidTest/java/MindRecordLifecycleAndroidTest.kt"] }),
        pullRequest({ number: 1197, files: ["app/src/androidTest/java/AppCompletionAndroidTest.kt"] }),
        pullRequest({ number: 1219, files: ["app/src/androidTest/java/AfternoteAuthorAndroidTest.kt"] }),
        pullRequest({ number: 1262, files: [".github/workflows/android-managed-device.yml"] }),
        pullRequest({ number: 1055, files: ["feature/home/presentation/src/test/kotlin/HomeTest.kt"] }),
        pullRequest({ number: 1098, files: ["docs/qa/status.md"] }),
        pullRequest({ number: 1099, files: ["core/data/src/main/java/UserRepositoryImpl.kt"] }),
        pullRequest({ number: 1264, files: ["feature/mindrecord/presentation/src/screenshotTestDebug/reference/a.png"] }),
        pullRequest({ number: 1265, files: ["feature/mindrecord/presentation/src/main/kotlin/screen/WriteScreen.kt"] }),
    ];

    const plan = planLabelChanges({
        pullRequests,
        repository: "Afternote/Afternote-FE",
        label: DEFAULT_LABEL,
    });

    assert.deepEqual(plan.toLabel.map((item) => item.number), [440, 767, 771, 882, 966, 1197, 1219, 1262]);
    assert.deepEqual(plan.notRequired.map((item) => item.number), [1055, 1098, 1099, 1264, 1265]);
});

test("이미 붙은 라벨은 유지하고 자동 제거 계획을 만들지 않는다", () => {
    const plan = planLabelChanges({
        pullRequests: [
            pullRequest({
                number: 10,
                labels: [DEFAULT_LABEL],
                files: ["app/src/androidTest/java/FlowTest.kt"],
            }),
            pullRequest({ number: 11, labels: [DEFAULT_LABEL], files: ["docs/readme.md"] }),
        ],
        repository: "Afternote/Afternote-FE",
        label: DEFAULT_LABEL,
    });

    assert.deepEqual(plan.alreadyLabeled.map((item) => item.number), [10, 11]);
    assert.deepEqual(plan.toRetry, []);
    assert.deepEqual(plan.notRequired, []);
    assert.equal("toUnlabel" in plan, false);
});

test("token-authored PR Validation의 정확한 HEAD만 기존 라벨 상태에서도 재dispatch한다", () => {
    const targetSha = "1313131313131313131313131313131313131313";
    const plan = planLabelChanges({
        pullRequests: [
            pullRequest({
                number: 13,
                headRefOid: targetSha,
                labels: [DEFAULT_LABEL],
                files: ["feature/home/presentation/src/main/kotlin/HomeScreen.kt"],
            }),
            pullRequest({
                number: 14,
                headRefOid: "1414141414141414141414141414141414141414",
                labels: [DEFAULT_LABEL],
                files: ["feature/home/presentation/src/main/kotlin/OtherScreen.kt"],
            }),
        ],
        repository: "Afternote/Afternote-FE",
        redispatchHeadSha: targetSha,
    });

    assert.deepEqual(plan.toRetry.map((item) => item.number), [13]);
    assert.deepEqual(plan.alreadyLabeled.map((item) => item.number), [14]);
});

test("자동 dispatch가 중단된 PR은 안전 라벨을 유지한 채 재시도한다", () => {
    const plan = planLabelChanges({
        pullRequests: [
            pullRequest({
                number: 12,
                labels: [DEFAULT_LABEL, DEFAULT_PENDING_LABEL],
                files: ["app/src/androidTest/java/FlowTest.kt"],
            }),
        ],
        repository: "Afternote/Afternote-FE",
    });

    assert.deepEqual(plan.toLabel, []);
    assert.deepEqual(plan.toRetry.map((item) => item.number), [12]);
    assert.deepEqual(plan.alreadyLabeled, []);
});

test("fork PR은 라벨로 실행되는 척하지 않고 별도 실패 대상으로 남긴다", () => {
    const plan = planLabelChanges({
        pullRequests: [
            pullRequest({
                number: 20,
                headRepository: "contributor/fork",
                files: ["app/src/androidTest/java/ForkTest.kt"],
            }),
        ],
        repository: "Afternote/Afternote-FE",
        label: DEFAULT_LABEL,
    });

    assert.deepEqual(plan.toLabel, []);
    assert.deepEqual(plan.skippedForks.map((item) => item.number), [20]);
});

test("changed files를 끝까지 페이지네이션한다", async () => {
    const pages = new Map([
        ["page=1", [{ filename: "a" }, { filename: "b" }]],
        ["page=2", [{ filename: "c" }]],
    ]);
    const api = async (apiPath) => {
        for (const [marker, response] of pages) {
            if (apiPath.includes(marker)) return response;
        }
        throw new Error(`unexpected ${apiPath}`);
    };

    const files = await fetchChangedFiles(api, "o/r", 30, { pageSize: 2, maxPages: 3 });

    assert.deepEqual(files, ["a", "b", "c"]);
});

test("changed files 한도를 넘으면 일부 파일만으로 판정하지 않고 실패한다", async () => {
    const api = async () => [{ filename: "a" }, { filename: "b" }];

    await assert.rejects(
        fetchChangedFiles(api, "o/r", 31, { pageSize: 2, maxPages: 2 }),
        /changed files가 4개를 넘어/,
    );
});

test("라벨 부착 뒤 다시 읽은 현재 HEAD를 trusted workflow로 dispatch한다", async () => {
    const currentHeadSha = "4141414141414141414141414141414141414141";
    const currentTestRef = "app/src/androidTest/java/com/afternote/FlowAndroidTest.kt#flow_succeeds";
    const api = fakeApi({
        responses: {
            "/repos/o/r/pulls/40": currentPullRequest({
                head: { sha: currentHeadSha, repo: { full_name: "o/r" } },
                body: requiredQaBody(currentTestRef),
            }),
        },
    });
    const failures = await applyPlan(
        api,
        "o/r",
        { toLabel: [{ number: 40, headRefOid: "4040404040404040404040404040404040404040" }] },
        { label: DEFAULT_LABEL, dryRun: false, defaultBranch: "develop", logger: silent },
    );

    assert.deepEqual(failures, []);
    assert.deepEqual(api.calls, [
        {
            apiPath: "/repos/o/r/issues/40/labels",
            method: "POST",
            body: { labels: [DEFAULT_PENDING_LABEL] },
        },
        {
            apiPath: "/repos/o/r/issues/40/labels",
            method: "POST",
            body: { labels: [DEFAULT_LABEL] },
        },
        {
            apiPath: "/repos/o/r/pulls/40",
            method: "GET",
            body: undefined,
        },
        {
            apiPath: "/repos/o/r/actions/workflows/android-managed-device.yml/dispatches",
            method: "POST",
            body: {
                ref: "develop",
                inputs: {
                    pull_request_number: "40",
                    expected_head_sha: currentHeadSha,
                    expected_test_ref: currentTestRef,
                },
            },
        },
        {
            apiPath: "/repos/o/r/issues/40/labels/android-test-dispatch-pending",
            method: "DELETE",
            body: undefined,
        },
    ]);
});

test("현재 PR이 open, same-repository, labeled가 아니면 dispatch 대상을 거부한다", async () => {
    const cases = [
        [currentPullRequest({ state: "closed" }), /open 상태/],
        [
            currentPullRequest({
                head: {
                    sha: "1111111111111111111111111111111111111111",
                    repo: { full_name: "fork/r" },
                },
            }),
            /same-repository/,
        ],
        [currentPullRequest({ labels: [] }), /라벨이 현재 PR에 없습니다/],
        [
            currentPullRequest({ head: { sha: "bad", repo: { full_name: "o/r" } } }),
            /HEAD SHA가 올바르지 않습니다/,
        ],
    ];

    for (const [response, expected] of cases) {
        const api = fakeApi({ responses: { "/repos/o/r/pulls/41": response } });
        await assert.rejects(fetchCurrentDispatchTarget(api, "o/r", 41, DEFAULT_LABEL), expected);
    }
});

test("dispatch 실패 시 안전 라벨과 pending 표식을 남겨 다음 reconcile이 재시도하게 한다", async () => {
    const api = fakeApi({
        responses: { "/repos/o/r/pulls/42": currentPullRequest() },
        failOn: (apiPath) => apiPath.includes("/actions/workflows/android-managed-device.yml/dispatches"),
    });

    const failures = await applyPlan(
        api,
        "o/r",
        { toLabel: [{ number: 42, headRefOid: "4242424242424242424242424242424242424242" }] },
        { label: DEFAULT_LABEL, dryRun: false, defaultBranch: "develop", logger: silent },
    );

    assert.equal(failures.length, 1);
    assert.match(failures[0], /다음 reconcile에서 재시도/);
    assert.equal(api.calls.some((call) => call.method === "DELETE"), false);
    assert.deepEqual(
        api.calls.slice(0, 2).map((call) => call.body),
        [{ labels: [DEFAULT_PENDING_LABEL] }, { labels: [DEFAULT_LABEL] }],
    );
});

test("pending PR 재시도는 android-test를 다시 쓰지 않고 성공 후 표식만 지운다", async () => {
    const api = fakeApi({ responses: { "/repos/o/r/pulls/43": currentPullRequest() } });

    const failures = await applyPlan(
        api,
        "o/r",
        {
            toLabel: [],
            toRetry: [{ number: 43, headRefOid: "4343434343434343434343434343434343434343" }],
        },
        { label: DEFAULT_LABEL, dryRun: false, defaultBranch: "develop", logger: silent },
    );

    assert.deepEqual(failures, []);
    assert.equal(
        api.calls.some(
            (call) => call.method === "POST" && call.apiPath === "/repos/o/r/issues/43/labels",
        ),
        false,
    );
    assert.deepEqual(api.calls.at(-1), {
        apiPath: "/repos/o/r/issues/43/labels/android-test-dispatch-pending",
        method: "DELETE",
        body: undefined,
    });
});

test("dry run은 원격 쓰기를 하지 않는다", async () => {
    const api = fakeApi();

    await applyPlan(
        api,
        "o/r",
        { toLabel: [{ number: 41, headRefOid: "4141414141414141414141414141414141414141" }] },
        { label: DEFAULT_LABEL, dryRun: true, defaultBranch: "develop", logger: silent },
    );

    assert.deepEqual(api.calls, []);
});

test("한 PR 라벨 실패가 나머지 PR 처리를 막지 않는다", async () => {
    const api = fakeApi({
        responses: { "/repos/o/r/pulls/51": currentPullRequest() },
        failOn: (apiPath) => apiPath.endsWith("/50/labels"),
    });

    const failures = await applyPlan(
        api,
        "o/r",
        {
            toLabel: [
                { number: 50, headRefOid: "5050505050505050505050505050505050505050" },
                { number: 51, headRefOid: "5151515151515151515151515151515151515151" },
            ],
        },
        { label: DEFAULT_LABEL, dryRun: false, defaultBranch: "develop", logger: silent },
    );

    assert.equal(failures.length, 1);
    assert.match(failures[0], /#50 자동 처리 실패/);
    assert.ok(api.calls.some((call) => call.apiPath.endsWith("/51/labels")));
});

test("라벨이 없으면 만들고 이미 있으면 만들지 않는다", async () => {
    const missing = fakeApi();
    await ensureLabelExists(missing, "o/r", DEFAULT_LABEL);
    assert.equal(missing.calls.filter((call) => call.method === "POST").length, 1);

    const existing = fakeApi({ responses: { "/repos/o/r/labels/android-test": { name: DEFAULT_LABEL } } });
    await ensureLabelExists(existing, "o/r", DEFAULT_LABEL);
    assert.equal(existing.calls.filter((call) => call.method === "POST").length, 0);
});

test("요약에 라벨 유지와 fork 보류까지 남긴다", () => {
    const summary = renderSummary({
        plan: {
            toLabel: [{ number: 60 }],
            toRetry: [{ number: 64 }],
            alreadyLabeled: [{ number: 61 }],
            notRequired: [{ number: 62 }],
            skippedForks: [{ number: 63 }],
        },
        label: DEFAULT_LABEL,
        dryRun: true,
    });

    assert.ok(summary.includes("dry run"));
    assert.ok(summary.includes("라벨 부착: 1건 — #60"));
    assert.ok(summary.includes("dispatch 재시도: 1건 — #64"));
    assert.ok(summary.includes("이미 유지 중: 1건 — #61"));
    assert.ok(summary.includes("경로 규칙 비대상: 1건 — #62"));
    assert.ok(summary.includes("fork라 실행 불가: 1건 — #63"));
});
