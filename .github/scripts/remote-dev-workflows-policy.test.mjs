import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { copyFile, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const workflowDirectory = new URL("../workflows/", import.meta.url);
const readWorkflow = (name) => readFile(new URL(name, workflowDirectory), "utf8");

const screenshotModules = [
    ":core:ui",
    ":feature:home:presentation",
    ":feature:receiver:presentation",
    ":feature:onboarding:presentation",
    ":feature:afternote:presentation",
    ":feature:mindrecord:presentation",
];

test("baseline generation executes PR code without write credentials", async () => {
    const source = await readWorkflow("screenshot-baseline-generate.yml");

    assert.match(source, /^\s{2}pull_request:\n\s{4}types: \[labeled\]$/m);
    assert.match(source, /^permissions:\n\s{2}contents: read\n\s{2}pull-requests: read$/m);
    assert.doesNotMatch(source, /^\s{2}contents: write$/m);
    assert.match(source, /github\.event\.label\.name == 'screenshot-baseline'/);
    assert.match(source, /github\.event\.pull_request\.head\.sha/);
    assert.match(source, /github\.event\.pull_request\.head\.repo\.full_name == github\.repository/);
    assert.match(source, /persist-credentials: false/);
    assert.match(source, /platforms: linux\/amd64/);
    assert.match(source, /docker run --rm --platform linux\/amd64/);
    assert.match(source, /--rerun --no-daemon/);
    assert.match(source, /gh api --paginate --slurp/);
    assert.match(source, /resolve-pr-impact\.mjs/);
    assert.match(source, /update_tasks\+=\("\$\{module\}:updateScreenshotTest"\)/);
    assert.match(source, /validate_tasks\+=\("\$\{module\}:validateScreenshotTest"\)/);
    assert.match(source, /SCREENSHOT_MODULES: \$\{\{ steps\.impact\.outputs\.screenshot_modules \}\}/);
});

test("privileged baseline apply is a workflow-run bridge restricted to PNG baselines", async () => {
    const source = await readWorkflow("screenshot-baseline-apply.yml");

    assert.match(source, /^\s{2}workflow_run:/m);
    assert.match(source, /workflows: \["Generate Screenshot Baselines"\]/);
    assert.match(source, /github\.event\.workflow_run\.event == 'pull_request'/);
    assert.match(source, /^\s{2}contents: write$/m);
    assert.doesNotMatch(source, /actions\/checkout@/);
    assert.doesNotMatch(source, /git apply/);
    assert.doesNotMatch(source, /child_process/);
    assert.match(source, /files\.json/);
    assert.match(source, /Rejected non-baseline or duplicate path/);
    assert.match(source, /89504e470d0a1a0a/);
    assert.match(source, /pullRequest\.head\.sha !== metadata\.headSha/);
    assert.match(source, /force: false/);
    for (const workflow of ["pr-validation.yml", "codeql.yml", "merge-order-guard.yml"]) {
        assert.ok(source.includes(`workflow_id: '${workflow}'`), `${workflow} is not redispatched`);
    }
    assert.match(source, /if: steps\.commit\.outputs\.changed == 'true'/);
    assert.match(source, /workflow_id: 'codeql\.yml',[\s\S]*inputs: \{ pull_request_number: process\.env\.TARGET_PR \}/);
});

test("managed device keeps required contexts but boots only CI Test Plan lanes", async () => {
    const source = await readWorkflow("android-managed-device.yml");

    assert.match(source, /^\s{2}pull_request:\n\s{4}types: \[opened, reopened, edited, synchronize\]$/m);
    assert.doesNotMatch(source, /^\s{2}workflow_call:/m);
    assert.doesNotMatch(source, /contains\(github\.event\.pull_request\.labels\.\*\.name, 'android-test'\)/);
    assert.match(source, /github\.event_name == 'pull_request'/);
    assert.match(source, /github\.event\.pull_request\.head\.repo\.full_name == github\.repository/);
    assert.match(source, /inputs\.pull_request_number > 0/);
    assert.match(source, /github\.ref_name == github\.event\.repository\.default_branch/);
    assert.match(source, /cancel-in-progress: true/);
    assert.match(source, /^\s{6}pull_request_number:\n/m);
    assert.match(source, /^\s{6}expected_head_sha:\n/m);
    assert.match(source, /^\s{6}expected_plan_digest:\n/m);
    assert.match(source, /head_repository.*!=.*GITHUB_REPOSITORY/);
    assert.match(source, /target_sha.*!=.*EXPECTED_HEAD_SHA/);
    assert.match(source, /target_branch.*!=.*DISPATCH_REF_NAME/);
    assert.match(source, /target_sha.*!=.*EXECUTION_SHA/);
    assert.match(
        source,
        /ref: \$\{\{ github\.event_name == 'pull_request' && github\.event\.pull_request\.head\.sha \|\| github\.sha \}\}/,
    );
    assert.doesNotMatch(source, /ref: \$\{\{ steps\.target\.outputs\.sha \}\}/);
    assert.match(source, /actual_sha.*!=.*EXPECTED_SHA/);
    const trustedCheckout = source.indexOf("Clone trusted target policy");
    const policyStaging = source.indexOf("Stage trusted Android test policy");
    const targetCheckout = source.indexOf("Clone tested revision");
    const targetVerification = source.indexOf("Verify checked out revision");
    const bootstrapRenderer = source.indexOf("Stage bootstrap result renderer from the tested revision");
    const bootstrapVerifier = source.indexOf("Stage bootstrap result verifier from the tested revision");
    assert.ok(trustedCheckout >= 0 && trustedCheckout < policyStaging);
    assert.ok(policyStaging < targetCheckout);
    assert.ok(targetCheckout < targetVerification && targetVerification < bootstrapRenderer);
    assert.ok(bootstrapRenderer < bootstrapVerifier);
    assert.match(source, /source=trusted/);
    assert.match(source, /source=bootstrap/);
    assert.doesNotMatch(source, /source=target/);
    assert.match(source, /renderer=trusted/);
    assert.match(source, /renderer=bootstrap/);
    assert.match(source, /Android test policy가 부분 설치된 상태입니다/);
    assert.match(source, /bootstrap mode에서는 expected_plan_digest를 사용할 수 없습니다/);
    assert.match(source, /bootstrap mode에서는 selected selector를 신뢰된 parser 없이 실행할 수 없습니다/);
    assert.match(source, /policy bootstrap full run/);
    assert.match(source, /bootstrap result renderer가 tested revision에 없습니다/);
    assert.match(source, /bootstrap result verifier가 tested revision에 없습니다/);
    assert.match(source, /resolve-android-test-plan\.mjs/);
    assert.match(source, /Validate selected tests in the tested revision/);
    assert.match(source, /Verify selected androidTest results/);
    assert.match(source, /Summarize androidTest results/);
    assert.match(source, /verify-android-test-plan-result\.mjs/);
    assert.match(source, /if: steps\.target\.outputs\.run_lane == 'true'/);
    assert.match(source, /selectors_json='\[\]'/);
    assert.match(source, /persist-credentials: false/);

    const bootstrapRendererStep = source.slice(bootstrapRenderer, bootstrapVerifier);
    assert.match(bootstrapRendererStep, /steps\.target\.outputs\.run_lane == 'true'/);
    assert.doesNotMatch(bootstrapRendererStep, /selectors_json != '\[\]'/);
});

test("managed device summarizes XML and uploads the full Gradle log before failing", async () => {
    const source = await readWorkflow("android-managed-device.yml");

    assert.match(source, /- name: Run managed-device androidTest\n\s+id: android_test/);
    assert.match(source, /--console=plain \\\n\s+--stacktrace > "\$gradle_log" 2>&1/);
    assert.match(source, /echo "exit_code=\$status"/);
    assert.match(
        source,
        /- name: Verify selected androidTest results\n\s+id: selected_android_test\n\s+if: >-\n\s+always\(\) &&[\s\S]*?steps\.target\.outputs\.selectors_json != '\[\]'/,
    );
    assert.match(
        source,
        /- name: Summarize androidTest results\n\s+id: android_test_results\n\s+if: always\(\) && steps\.target\.outputs\.run_lane == 'true'/,
    );
    const summaryStep = source.slice(
        source.indexOf("- name: Summarize androidTest results\n"),
        source.indexOf("- name: Upload androidTest reports and failure evidence"),
    );
    assert.doesNotMatch(summaryStep, /selectors_json != '\[\]'/);
    assert.match(
        summaryStep,
        /ANDROID_TEST_ANNOTATION_DIR: \$\{\{ runner\.temp \}\}\/android-test-annotations/,
    );
    assert.match(summaryStep, /SELECTED_VERIFIER_EXIT_CODE/);
    assert.match(summaryStep, /render-android-test-results\.mjs/);
    assert.match(source, /\$\{\{ runner\.temp \}\}\/android-test-logs\//);
    assert.match(source, /\$\{\{ runner\.temp \}\}\/android-test-annotations\//);

    const verify = source.indexOf("Verify selected androidTest results");
    const summary = source.indexOf("Summarize androidTest results\n");
    const annotationSteps = [1, 2, 3, 4, 5].map((index) =>
        source.indexOf(`- name: Publish androidTest annotations ${index}\n`),
    );
    const upload = source.indexOf("Upload androidTest reports and failure evidence");
    const restore = source.indexOf("Restore managed-device androidTest exit code");
    assert.equal([...source.matchAll(/- name: Publish androidTest annotations \d+\n/g)].length, 5);
    assert.ok(verify >= 0 && verify < summary);
    annotationSteps.forEach((stepStart, offset) => {
        const index = offset + 1;
        const next = annotationSteps[offset + 1] ?? upload;
        assert.ok(stepStart > summary && stepStart < next);
        const step = source.slice(stepStart, next);
        assert.match(step, /if: >-\n\s+always\(\) &&/);
        assert.match(step, /steps\.target\.outputs\.run_lane == 'true'/);
        assert.ok(
            step.includes(
                `fromJSON(steps.android_test_results.outputs.annotation_chunks || '0') >= ${index}`,
            ),
        );
        assert.ok(
            step.includes(`run: cat "$RUNNER_TEMP/android-test-annotations/chunk-${index}.log"`),
        );
    });
    assert.ok(annotationSteps.at(-1) < upload && upload < restore);
    assert.match(source, /ANDROID_TEST_EXIT_CODE: \$\{\{ steps\.android_test\.outputs\.exit_code \}\}/);
    assert.match(source, /exit "\$ANDROID_TEST_EXIT_CODE"/);
    assert.match(source, /SELECTED_VERIFIER_EXIT_CODE: \$\{\{ steps\.selected_android_test\.outputs\.exit_code \}\}/);
    assert.match(source, /RESULT_RENDERER_EXIT_CODE: \$\{\{ steps\.android_test_results\.outputs\.exit_code \}\}/);
});

test("managed device stages every local dependency of its trusted Android policy", async () => {
    const source = await readWorkflow("android-managed-device.yml");
    const stageStart = source.indexOf("- name: Stage trusted Android test policy");
    const stageEnd = source.indexOf("- name: Resolve and verify tested revision");
    assert.ok(stageStart >= 0 && stageStart < stageEnd);

    const stagedSource = source.slice(stageStart, stageEnd);
    const stagedFiles = new Set(
        [...stagedSource.matchAll(/\.github\/scripts\/([a-z0-9-]+\.mjs)/g)]
            .map((match) => match[1]),
    );
    const scriptsDirectory = new URL("../scripts/", import.meta.url);
    for (const stagedFile of stagedFiles) {
        const script = await readFile(new URL(stagedFile, scriptsDirectory), "utf8");
        for (const match of script.matchAll(/from\s+["']\.\/([^"']+\.mjs)["']/g)) {
            assert.ok(
                stagedFiles.has(match[1]),
                `${stagedFile} imports ${match[1]}, but the workflow does not stage it`,
            );
        }
    }

    const stagedDirectory = await mkdtemp(path.join(tmpdir(), "android-test-policy-"));
    try {
        await Promise.all(
            [...stagedFiles].map((file) => copyFile(
                fileURLToPath(new URL(file, scriptsDirectory)),
                path.join(stagedDirectory, file),
            )),
        );
        const payloadPath = path.join(stagedDirectory, "pull-request.json");
        await writeFile(payloadPath, JSON.stringify({
            number: 1,
            body: `## CI Test Plan

\`\`\`json
{
  "androidTest": {
    "mode": "selected",
    "reason": "스테이징한 신뢰 정책을 실제 selected 계획으로 실행합니다.",
    "tests": [
      {
        "path": "app/src/androidTest/java/com/afternote/afternote_fe/AccessibilitySmokeAndroidTest.kt",
        "selector": "com.afternote.afternote_fe.AccessibilitySmokeAndroidTest#welcomeAndLogin_haveNoAutomatedAccessibilityErrors",
        "device": "api30"
      }
    ]
  }
}
\`\`\``,
        }));
        execFileSync(
            process.execPath,
            [path.join(stagedDirectory, "validate-pr-ci-test-plan.mjs"), payloadPath, process.cwd()],
            { stdio: "pipe" },
        );
    } finally {
        await rm(stagedDirectory, { recursive: true, force: true });
    }
});

test("screenshot cleanup tolerates cancellation before Gradle setup", async () => {
    const source = await readWorkflow("screenshot.yml");

    assert.match(source, /sudo chown -R .*\$GITHUB_WORKSPACE/);
    assert.match(source, /if \[\[ -e "\$HOME\/\.gradle" \]\]; then/);
    assert.match(source, /sudo chown -R .*\$HOME\/\.gradle/);
});

test("stack refresh only executes trusted default-branch code", async () => {
    const source = await readWorkflow("stack-refresh.yml");

    assert.match(source, /^\s{2}workflow_dispatch:/m);
    assert.match(source, /github\.ref_name == github\.event\.repository\.default_branch/);
    assert.match(source, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
    assert.match(source, /persist-credentials: false/);
    assert.match(source, /node --test \.github\/scripts\/refresh-pr-stack\.test\.mjs/);
    assert.match(source, /MAX_DEPTH: \$\{\{ inputs\.max_depth \}\}/);
});

test("required checks expose manual dispatch for token-authored commits", async () => {
    const validation = await readWorkflow("pr-validation.yml");
    const guard = await readWorkflow("merge-order-guard.yml");

    assert.match(validation, /^\s{2}workflow_dispatch:$/m);
    assert.match(guard, /^\s{2}workflow_dispatch:\n\s{4}inputs:/m);
    assert.match(guard, /github\.event_name == 'pull_request' \|\| github\.event_name == 'workflow_dispatch'/);
});
