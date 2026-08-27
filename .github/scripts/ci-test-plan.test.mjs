import assert from "node:assert/strict";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import {
    ciTestPlanDigest,
    inspectAndroidTestImpact,
    inspectCiTestPlan,
    inspectPullRequestCiTestPlan,
    validateCiTestPlanImpact,
    validateCiTestPlanSources,
} from "./ci-test-plan.mjs";
import { resolveAndroidTestPlan } from "./resolve-android-test-plan.mjs";
import { validatePullRequestCiTestPlan } from "./validate-pr-ci-test-plan.mjs";

const selectedBody = `
## CI Test Plan

\`\`\`json
{
  "androidTest": {
    "mode": "selected",
    "reason": "변경된 receiver 완료 경계를 실제 Android 런타임에서 확인",
    "tests": [{
      "path": "app/src/androidTest/kotlin/com/example/ReceiverTest.kt",
      "selector": "com.example.ReceiverRuntimeTest#completion",
      "device": "api30"
    }]
  }
}
\`\`\`
`;

test("none, selected, full 세 모드만 받는다", () => {
    const selected = inspectCiTestPlan(selectedBody, { pullRequestNumber: 7 });
    assert.equal(selected.valid, true);
    assert.equal(selected.plan.androidTest.mode, "selected");

    const invalid = inspectCiTestPlan(selectedBody.replace('"selected"', '"maybe"'));
    assert.equal(invalid.valid, false);
});

test("selected는 명시적 파일, FQCN#method, lane을 요구한다", () => {
    const invalid = inspectCiTestPlan(
        selectedBody.replace("com.example.ReceiverRuntimeTest#completion", "ReceiverTest#completion"),
    );
    assert.equal(invalid.valid, false);
    assert.match(invalid.errors.join("\n"), /fully-qualified/);
});

test("선택 selector의 package, class, @Test method를 현재 revision에서 확인한다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "ci-test-plan-"));
    const file = path.join(root, "app/src/androidTest/kotlin/com/example/ReceiverTest.kt");
    await fs.mkdir(path.dirname(file), { recursive: true });
    await fs.writeFile(
        file,
        "package com.example\n\nclass ReceiverRuntimeTest {\n @Test fun completion() = Unit\n}\n",
    );
    const inspection = inspectCiTestPlan(selectedBody);
    await assert.doesNotReject(validateCiTestPlanSources(inspection.plan, { root }));
    await assert.rejects(
        validateCiTestPlanSources(
            {
                androidTest: {
                    ...inspection.plan.androidTest,
                    tests: [
                        {
                            ...inspection.plan.androidTest.tests[0],
                            selector: "com.example.ReceiverRuntimeTest#missing",
                        },
                    ],
                },
            },
            { root },
        ),
        /@Test 메서드/,
    );
});

test("기존 PR도 계획이 없으면 실패한다", () => {
    const pullRequest = { number: 3, created_at: "2026-08-01T00:00:00Z", body: "old" };
    assert.equal(inspectPullRequestCiTestPlan(pullRequest).valid, false);
    assert.throws(() => resolveAndroidTestPlan(pullRequest), /CI Test Plan/);
});

test("도입 이후 PR은 계획이 필수이고 digest는 결정적이다", () => {
    const pullRequest = { number: 4, created_at: "2026-09-01T00:00:00Z", body: "missing" };
    assert.equal(inspectPullRequestCiTestPlan(pullRequest).valid, false);
    const resolved = resolveAndroidTestPlan({ ...pullRequest, body: selectedBody });
    assert.equal(resolved.digest, ciTestPlanDigest(resolved.plan));
});

test("pull_request event wrapper와 direct REST payload를 같은 방식으로 검증한다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "ci-test-plan-wrapper-"));
    const noneBody = selectedBody
        .replace('"selected"', '"none"')
        .replace(/,\s*"tests": \[[\s\S]*?\]\s*\n  /, "\n  ");
    const pullRequest = { number: 1280, created_at: "2026-09-01T00:00:00Z", body: noneBody };

    await assert.doesNotReject(validatePullRequestCiTestPlan(pullRequest, { root }));
    await assert.doesNotReject(validatePullRequestCiTestPlan({ pull_request: pullRequest }, { root }));
});

test("하네스 변경은 full, Android 런타임 경계는 selected 이상을 강제한다", async () => {
    assert.deepEqual(
        inspectAndroidTestImpact([".github/workflows/android-managed-device.yml"]).full,
        [".github/workflows/android-managed-device.yml"],
    );
    assert.deepEqual(
        inspectAndroidTestImpact([
            ".github/scripts/classify-android-managed-device-failure.mjs",
            ".github/workflows/android-managed-device-retry.yml",
        ]).full,
        [
            ".github/scripts/classify-android-managed-device-failure.mjs",
            ".github/workflows/android-managed-device-retry.yml",
        ],
    );
    await assert.rejects(
        validateCiTestPlanImpact(
            { androidTest: { mode: "none", reason: "CI only" } },
            [".github/workflows/android-managed-device.yml"],
        ),
        /mode=full/,
    );
    await assert.rejects(
        validateCiTestPlanImpact(
            { androidTest: { mode: "none", reason: "runtime" } },
            ["app/src/main/AndroidManifest.xml"],
        ),
        /selected 또는 full/,
    );
});

test("변경한 @Test 파일의 selector를 selected 계획에서 빠뜨릴 수 없다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "ci-test-impact-"));
    const testPath = "app/src/androidTest/java/com/example/RuntimeTest.kt";
    await fs.mkdir(path.dirname(path.join(root, testPath)), { recursive: true });
    await fs.writeFile(
        path.join(root, testPath),
        "package com.example\nclass RuntimeTest { @Test fun works() = Unit }\n",
    );
    await assert.rejects(
        validateCiTestPlanImpact(
            {
                androidTest: {
                    mode: "selected",
                    reason: "other test",
                    tests: [
                        {
                            path: "app/src/androidTest/java/com/example/OtherTest.kt",
                            selector: "com.example.OtherTest#works",
                            device: "api30",
                        },
                    ],
                },
            },
            [testPath],
            { root },
        ),
        /변경한 @Test 메서드/,
    );
});
