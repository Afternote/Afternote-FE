import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

import {
    collectXmlFiles,
    parseAndroidTestXml,
    renderAndroidTestResults,
    renderAndroidTestSummary,
    renderAnnotations,
    resolveFailureLocation,
    summarizeAndroidTestDocuments,
} from "./render-android-test-results.mjs";

const SCRIPT = fileURLToPath(new URL("./render-android-test-results.mjs", import.meta.url));

// Run 33084637682의 AGP Managed Device XML 구조와 실패 본문을 축약 없이 재현한 fixture다.
const REPORT_16_TESTS_3_FAILURES = `<?xml version='1.0' encoding='UTF-8' ?>
<testsuites tests="16" failures="3" errors="0" skipped="0">
  <testsuite name="com.afternote.afternote_fe.TimeLetterFlowAndroidTest" tests="3" failures="1" errors="0" skipped="0">
    <properties><property name="device" value="pixel2Api30" /></properties>
    <testcase name="draftSave_usesDraftStatusWithoutSchedule" classname="com.afternote.afternote_fe.TimeLetterFlowAndroidTest" />
    <testcase name="scheduledSave_failureThenRetry_keepsExactPayload" classname="com.afternote.afternote_fe.TimeLetterFlowAndroidTest">
      <failure>org.junit.ComparisonFailure: expected:&lt;2026-09-03T14:35:00[]&gt; but was:&lt;2026-09-03T14:35:00[Z]&gt;
at org.junit.Assert.assertEquals(Assert.java:117)
at com.afternote.afternote_fe.TimeLetterFlowAndroidTest.scheduledSave_failureThenRetry_keepsExactPayload(TimeLetterFlowAndroidTest.kt:97)
</failure>
    </testcase>
    <testcase name="registerWithoutReceiver_isBlockedAndShownToUser" classname="com.afternote.afternote_fe.TimeLetterFlowAndroidTest" />
  </testsuite>
  <testsuite name="com.afternote.afternote_fe.TimeLetterLifecycleAndroidTest" tests="5" failures="1" errors="0" skipped="0">
    <testcase name="drafts_selectionDeleteReentryAndDeleteAll_reloadDurableRepositoryState" classname="com.afternote.afternote_fe.TimeLetterLifecycleAndroidTest">
      <failure>java.lang.AssertionError: Failed to perform checkIsDisplayed check: Expected at most 1 node but found 2 nodes that satisfy (Text + InputText contains '발송 예정일 2026. 10. 01.')
at androidx.compose.ui.test.AssertionsKt.assertIsDisplayed(Assertions.kt:33)
at com.afternote.afternote_fe.TimeLetterLifecycleAndroidTest.drafts_selectionDeleteReentryAndDeleteAll_reloadDurableRepositoryState(TimeLetterLifecycleAndroidTest.kt:188)
</failure>
    </testcase>
    <testcase name="senderList_loadingErrorSuccessFilterAndDeleteRetry_keepRepositoryBoundary" classname="com.afternote.afternote_fe.TimeLetterLifecycleAndroidTest" />
    <testcase name="recipientListAndDetail_errorRetry_recoversBothRepositoryBoundaries" classname="com.afternote.afternote_fe.TimeLetterLifecycleAndroidTest" />
    <testcase name="editingExistingLetter_showsLoadedStateAndSendsExactUpdatePayload" classname="com.afternote.afternote_fe.TimeLetterLifecycleAndroidTest" />
    <testcase name="draftReturnResult_refreshesRepositoryExactlyOnce" classname="com.afternote.afternote_fe.TimeLetterLifecycleAndroidTest" />
  </testsuite>
  <testsuite name="com.afternote.afternote_fe.TimeLetterMindRecordCompletionAndroidTest" tests="8" failures="1" errors="0" skipped="0">
    <testcase name="memorySpace_supportedSuccess_opensAndClosesDetailThenNavigatesBack" classname="com.afternote.afternote_fe.TimeLetterMindRecordCompletionAndroidTest" />
    <testcase name="mindRecordHome_dailyQuestionLoadingEmptyAndErrorRetrySuccess_areRendered" classname="com.afternote.afternote_fe.TimeLetterMindRecordCompletionAndroidTest" />
    <testcase name="diaryDraftRow_routesIdAndMonthThenPrefillsAndPublishesWithPatch" classname="com.afternote.afternote_fe.TimeLetterMindRecordCompletionAndroidTest" />
    <testcase name="timeLetterRecipientSelector_roundTripPreservesTitleTextAndExactReceiverId" classname="com.afternote.afternote_fe.TimeLetterMindRecordCompletionAndroidTest" />
    <testcase name="timeLetterSenderDetail_routeIdLoadingFailureRetryAndSuccess_areConnected" classname="com.afternote.afternote_fe.TimeLetterMindRecordCompletionAndroidTest" />
    <testcase name="dailyQuestionWrite_successRefreshesExistingListWithCreatedAnswer" classname="com.afternote.afternote_fe.TimeLetterMindRecordCompletionAndroidTest" />
    <testcase name="timeLetterWrite_uiValidationAndRapidRegister_preserveInputAndCreateOnce" classname="com.afternote.afternote_fe.TimeLetterMindRecordCompletionAndroidTest">
      <failure>org.junit.ComparisonFailure: expected:&lt;2026-09-14T09:35:00[]&gt; but was:&lt;2026-09-14T09:35:00[Z]&gt;
at org.junit.Assert.assertEquals(Assert.java:117)
at com.afternote.afternote_fe.TimeLetterMindRecordCompletionAndroidTest.timeLetterWrite_uiValidationAndRapidRegister_preserveInputAndCreateOnce(TimeLetterMindRecordCompletionAndroidTest.kt:192)
</failure>
    </testcase>
    <testcase name="weeklyReport_errorRetryThenEmptyAndComplete_preservesRequestedWeekContract" classname="com.afternote.afternote_fe.TimeLetterMindRecordCompletionAndroidTest" />
  </testsuite>
</testsuites>`;

const SOURCES = [
    {
        fileName: "TimeLetterFlowAndroidTest.kt",
        relative: "app/src/androidTest/java/com/afternote/afternote_fe/TimeLetterFlowAndroidTest.kt",
    },
    {
        fileName: "TimeLetterLifecycleAndroidTest.kt",
        relative: "app/src/androidTest/java/com/afternote/afternote_fe/TimeLetterLifecycleAndroidTest.kt",
    },
    {
        fileName: "TimeLetterMindRecordCompletionAndroidTest.kt",
        relative: "app/src/androidTest/java/com/afternote/afternote_fe/TimeLetterMindRecordCompletionAndroidTest.kt",
    },
];


function cliEnvironment(overrides = {}) {
    const environment = { ...process.env, ...overrides };
    delete environment.GITHUB_STEP_SUMMARY;
    return environment;
}

function failingReport(count, { incomplete = false } = {}) {
    const testcases = Array.from({ length: count }, (unused, index) => `
      <testcase classname="com.example.BulkTest" name="method${index}">
        <failure>java.lang.AssertionError: failure ${index}
at com.example.BulkTest.method${index}(BulkTest.kt:${index + 1})</failure>
      </testcase>`).join("");
    const systemError = incomplete
        ? "<system-err>Test run failed to complete. Expected 82 tests, received 21</system-err>"
        : "";
    return `<testsuite>${testcases}${systemError}</testsuite>`;
}

test("실제 형태의 16개 결과에서 세 실패의 메시지와 테스트 소스 위치를 요약한다", () => {
    const summary = summarizeAndroidTestDocuments(
        [{ file: "TEST-pixel2Api30-_app-.xml", xml: REPORT_16_TESTS_3_FAILURES }],
        { sourceFiles: SOURCES },
    );

    assert.equal(summary.executed, 16);
    assert.equal(summary.failed, 3);
    assert.equal(summary.skipped, 0);
    assert.deepEqual(
        summary.failures.map(({ selector, location }) => ({ selector, location })),
        [
            {
                selector: "TimeLetterFlowAndroidTest#scheduledSave_failureThenRetry_keepsExactPayload",
                location: {
                    file: "app/src/androidTest/java/com/afternote/afternote_fe/TimeLetterFlowAndroidTest.kt",
                    line: 97,
                },
            },
            {
                selector:
                    "TimeLetterLifecycleAndroidTest#drafts_selectionDeleteReentryAndDeleteAll_reloadDurableRepositoryState",
                location: {
                    file: "app/src/androidTest/java/com/afternote/afternote_fe/TimeLetterLifecycleAndroidTest.kt",
                    line: 188,
                },
            },
            {
                selector:
                    "TimeLetterMindRecordCompletionAndroidTest#timeLetterWrite_uiValidationAndRapidRegister_preserveInputAndCreateOnce",
                location: {
                    file: "app/src/androidTest/java/com/afternote/afternote_fe/TimeLetterMindRecordCompletionAndroidTest.kt",
                    line: 192,
                },
            },
        ],
    );
    assert.match(summary.failures[0].message, /expected:<2026-09-03T14:35:00\[\]>/);
    assert.doesNotMatch(summary.failures[0].message, /org\.junit\.Assert\.assertEquals/);

    const markdown = renderAndroidTestSummary(summary);
    assert.match(markdown, /Android tests: 16 executed, 3 failed, 0 skipped/);
    assert.match(markdown, /TimeLetterFlowAndroidTest#scheduledSave_failureThenRetry_keepsExactPayload/);
    assert.match(markdown, /TimeLetterFlowAndroidTest\.kt:97/);
    assert.equal(renderAnnotations(summary).length, 3);
});

test("failure와 error를 실패로, skipped를 건너뜀으로 집계하고 XML entity와 CDATA를 복원한다", () => {
    const xml = `<testsuite>
      <testcase classname='com.example.EntityTest' name='failure'>
        <failure message='fallback &quot;message&quot;'>java.lang.AssertionError: A &amp; B &lt; C &#x1F642;
at com.example.EntityTest.failure(EntityTest.kt:12)</failure>
      </testcase>
      <testcase classname="com.example.EntityTest" name="error">
        <error><![CDATA[java.lang.IllegalStateException: <boom> & raw
at com.example.EntityTest.error(EntityTest.kt:18)]]></error>
      </testcase>
      <testcase classname="com.example.EntityTest" name="ignored"><skipped /></testcase>
    </testsuite>`;
    const parsed = parseAndroidTestXml(xml);
    const summary = summarizeAndroidTestDocuments([{ file: "entities.xml", xml }], {
        sourceFiles: [{ fileName: "EntityTest.kt", relative: "app/src/androidTest/java/com/example/EntityTest.kt" }],
    });

    assert.deepEqual(parsed.testcases.map((entry) => entry.status), ["failure", "error", "skipped"]);
    assert.equal(parsed.testcases[0].failure.message, "java.lang.AssertionError: A & B < C 🙂");
    assert.equal(parsed.testcases[1].failure.message, "java.lang.IllegalStateException: <boom> & raw");
    assert.equal(summary.failed, 2);
    assert.equal(summary.skipped, 1);
});

test("annotation의 data와 property 제어문자를 GitHub workflow command 규칙으로 escape한다", () => {
    const [annotation] = renderAnnotations({
        executed: 1,
        failed: 1,
        skipped: 0,
        infrastructureFailures: [],
        failures: [
            {
                selector: "EscapingTest#comma,:percent%",
                message: "100% failed\r\n::warning::not a command",
                location: { file: "app/src/androidTest/A,B:C%Test.kt", line: 7 },
            },
        ],
    });

    assert.equal(
        annotation,
        "::error file=app/src/androidTest/A%2CB%3AC%25Test.kt,line=7,title=EscapingTest#comma%2C%3Apercent%25::100%25 failed%0D%0A::warning::not a command",
    );
    assert.equal(annotation.split("\n").length, 1);
});

test("testcase class frame을 helper보다 우선하고 fallback과 위치 누락을 구분한다", () => {
    const testcase = {
        className: "com.example.RuntimeTest",
        failure: {
            trace: `java.lang.AssertionError
at org.junit.RuntimeTest.assertEquals(RuntimeTest.kt:12)
at com.example.TestRobot.check(TestRobot.kt:42)
at com.example.RuntimeTest.works(RuntimeTest.kt:88)`,
        },
    };
    assert.deepEqual(
        resolveFailureLocation(testcase, [
            { fileName: "TestRobot.kt", relative: "app/src/androidTest/java/com/example/TestRobot.kt" },
            { fileName: "RuntimeTest.kt", relative: "app/src/androidTest/java/com/example/RuntimeTest.kt" },
        ]),
        { file: "app/src/androidTest/java/com/example/RuntimeTest.kt", line: 88 },
    );
    assert.deepEqual(resolveFailureLocation(testcase, []), { file: "RuntimeTest.kt", line: 88 });
    assert.deepEqual(
        resolveFailureLocation(
            { className: "com.example.RuntimeTest", failure: { trace: "java.lang.AssertionError" } },
            [{ fileName: "RuntimeTest.kt", relative: "app/src/androidTest/java/com/example/RuntimeTest.kt" }],
        ),
        { file: "app/src/androidTest/java/com/example/RuntimeTest.kt", line: 0 },
    );
    assert.equal(
        resolveFailureLocation(
            {
                className: "com.example.RuntimeTest",
                failure: { trace: "java.lang.AssertionError\nat org.junit.Assert.fail(Assert.java:1)" },
            },
            [],
        ),
        null,
    );
});

test("Managed Device 결과 루트를 재귀 탐색하고 실제 workspace의 상대 소스 위치를 연결한다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "android-test-summary-"));
    try {
        const report = path.join(root, "reports", "managedDevice", "debug", "pixel2Api30", "TEST.xml");
        const source = path.join(
            root,
            "app",
            "src",
            "androidTest",
            "java",
            "com",
            "afternote",
            "afternote_fe",
            "TimeLetterFlowAndroidTest.kt",
        );
        await fs.mkdir(path.dirname(report), { recursive: true });
        await fs.mkdir(path.dirname(source), { recursive: true });
        await fs.writeFile(report, REPORT_16_TESTS_3_FAILURES);
        await fs.writeFile(source, "package com.afternote.afternote_fe\n");

        assert.deepEqual(await collectXmlFiles(path.join(root, "reports")), [report]);
        const rendered = await renderAndroidTestResults(path.join(root, "reports"), root);
        assert.equal(rendered.summary.executed, 16);
        assert.deepEqual(rendered.summary.failures[0].location, {
            file: "app/src/androidTest/java/com/afternote/afternote_fe/TimeLetterFlowAndroidTest.kt",
            line: 97,
        });
        assert.equal(rendered.summary.failures[1].location.file, "TimeLetterLifecycleAndroidTest.kt");
    } finally {
        await fs.rm(root, { recursive: true, force: true });
    }
});

test("incomplete system-err를 testcase 실패와 별도의 인프라 실패로 노출한다", () => {
    const xml = `<testsuites tests="19" failures="0" errors="0">
      <testsuite><testcase classname="com.example.RuntimeTest" name="works" /></testsuite>
      <system-err>Test run failed to complete. Expected 82 tests, received 19</system-err>
    </testsuites>`;
    const summary = summarizeAndroidTestDocuments([{ file: "incomplete.xml", xml }]);

    assert.equal(summary.executed, 1);
    assert.equal(summary.failed, 0);
    assert.deepEqual(summary.infrastructureFailures, [
        {
            file: "incomplete.xml",
            message: "Test run failed to complete. Expected 82 tests, received 19",
        },
    ]);
    assert.match(renderAndroidTestSummary(summary), /Test run infrastructure failures/);
    assert.equal(
        renderAnnotations(summary)[0],
        "::error title=Android test run incomplete::Test run failed to complete. Expected 82 tests, received 19",
    );
});

test("XML은 성공이어도 selected verifier가 누락 selector로 실패하면 red summary와 annotation 뒤 nonzero가 된다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "android-test-verification-"));
    try {
        const report = path.join(root, "reports", "TEST.xml");
        const verificationLog = path.join(root, "selected-verification.log");
        await fs.mkdir(path.dirname(report), { recursive: true });
        await fs.writeFile(
            report,
            '<testsuite><testcase classname="com.example.RuntimeTest" name="works" /></testsuite>',
        );
        await fs.writeFile(
            verificationLog,
            "Managed Device XML에서 실행 결과를 찾지 못했습니다: com.example.RuntimeTest#missing\n",
        );

        const execution = spawnSync(
            process.execPath,
            [SCRIPT, path.dirname(report), root, verificationLog, "1"],
            { encoding: "utf8", env: cliEnvironment() },
        );

        assert.equal(execution.status, 1);
        assert.match(execution.stdout, /❌ \*\*Android tests: 1 executed, 0 failed, 0 skipped\*\*/);
        assert.match(execution.stdout, /### Selected androidTest result verification failed/);
        assert.match(execution.stdout, /com\.example\.RuntimeTest#missing/);
        assert.match(
            execution.stdout,
            /::error title=Selected androidTest result verification failed::Managed Device XML/,
        );
        assert.match(execution.stderr, /Selected androidTest result verification failed/);
    } finally {
        await fs.rm(root, { recursive: true, force: true });
    }
});

test("annotation 23개를 gate 우선순위대로 10개씩 세 chunk에 기록한다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "android-test-annotation-chunks-"));
    try {
        const report = path.join(root, "reports", "TEST.xml");
        const verificationLog = path.join(root, "selected-verification.log");
        const annotationDirectory = path.join(root, "annotations");
        const githubOutput = path.join(root, "github-output.txt");
        await fs.mkdir(path.dirname(report), { recursive: true });
        await fs.writeFile(report, failingReport(21, { incomplete: true }));
        await fs.writeFile(verificationLog, "Managed Device XML에서 selector를 찾지 못했습니다.\n");
        await fs.writeFile(githubOutput, "existing=value\n");

        const execution = spawnSync(
            process.execPath,
            [SCRIPT, path.dirname(report), root, verificationLog, "1"],
            {
                encoding: "utf8",
                env: cliEnvironment({
                    ANDROID_TEST_ANNOTATION_DIR: annotationDirectory,
                    GITHUB_OUTPUT: githubOutput,
                }),
            },
        );

        assert.equal(execution.status, 1);
        assert.doesNotMatch(execution.stdout, /::error/);
        const chunks = await Promise.all(
            [1, 2, 3].map((index) => fs.readFile(path.join(annotationDirectory, `chunk-${index}.log`), "utf8")),
        );
        assert.deepEqual(
            chunks.map((chunk) => chunk.trimEnd().split("\n").length),
            [10, 10, 3],
        );
        const firstChunk = chunks[0].trimEnd().split("\n");
        assert.match(firstChunk[0], /^::error title=Selected androidTest result verification failed::/);
        assert.match(firstChunk[1], /^::error title=Android test run incomplete::/);
        assert.match(firstChunk[2], /title=BulkTest#method0/);
        assert.match(await fs.readFile(githubOutput, "utf8"), /annotation_chunks=3\nannotation_count=23\n$/);
    } finally {
        await fs.rm(root, { recursive: true, force: true });
    }
});

test("annotation 55개는 첫 50개만 다섯 chunk에 기록하고 Summary에는 실패 전량과 cap을 알린다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "android-test-annotation-cap-"));
    try {
        const report = path.join(root, "TEST.xml");
        const annotationDirectory = path.join(root, "annotations");
        const githubOutput = path.join(root, "github-output.txt");
        await fs.writeFile(report, failingReport(55));
        await fs.writeFile(githubOutput, "");

        const execution = spawnSync(process.execPath, [SCRIPT, root, root], {
            encoding: "utf8",
            env: cliEnvironment({
                ANDROID_TEST_ANNOTATION_DIR: annotationDirectory,
                GITHUB_OUTPUT: githubOutput,
            }),
        });

        assert.equal(execution.status, 1);
        assert.equal((execution.stdout.match(/<code>BulkTest#method/g) ?? []).length, 55);
        assert.match(execution.stdout, /전체 55개 중 첫 50개만 Annotation으로 표시/);
        assert.doesNotMatch(execution.stdout, /::error/);
        const chunks = await Promise.all(
            [1, 2, 3, 4, 5].map((index) =>
                fs.readFile(path.join(annotationDirectory, `chunk-${index}.log`), "utf8"),
            ),
        );
        assert.deepEqual(
            chunks.map((chunk) => chunk.trimEnd().split("\n").length),
            [10, 10, 10, 10, 10],
        );
        assert.match(chunks[4], /title=BulkTest#method49/);
        assert.doesNotMatch(chunks.join("\n"), /title=BulkTest#method50/);
        await assert.rejects(fs.access(path.join(annotationDirectory, "chunk-6.log")));
        assert.equal(await fs.readFile(githubOutput, "utf8"), "annotation_chunks=5\nannotation_count=55\n");
    } finally {
        await fs.rm(root, { recursive: true, force: true });
    }
});

test("verification exit code가 비어 있거나 0이면 로그를 읽지 않고 clean 결과를 유지한다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "android-test-verification-clean-"));
    try {
        const report = path.join(root, "TEST.xml");
        await fs.writeFile(
            report,
            '<testsuite><testcase classname="com.example.RuntimeTest" name="works" /></testsuite>',
        );
        for (const verificationExitCode of [undefined, "", "0"]) {
            const rendered = await renderAndroidTestResults(root, root, {
                verificationLog: path.join(root, "does-not-exist.log"),
                verificationExitCode,
            });
            assert.equal(rendered.summary.verificationFailure, null);
            assert.match(renderAndroidTestSummary(rendered.summary), /^## Android tests\n\n✅/);
        }
    } finally {
        await fs.rm(root, { recursive: true, force: true });
    }
});

test("clean XML CLI는 summary를 stdout에 쓰고 0으로 끝난다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "android-test-clean-cli-"));
    try {
        await fs.writeFile(
            path.join(root, "TEST.xml"),
            '<testsuite><testcase classname="com.example.RuntimeTest" name="works" /></testsuite>',
        );
        const execution = spawnSync(process.execPath, [SCRIPT, root, root], {
            encoding: "utf8",
            env: cliEnvironment(),
        });
        assert.equal(execution.status, 0, execution.stderr);
        assert.match(execution.stdout, /✅ \*\*Android tests: 1 executed, 0 failed, 0 skipped\*\*/);
        assert.doesNotMatch(execution.stdout, /::error/);
    } finally {
        await fs.rm(root, { recursive: true, force: true });
    }
});

test("CLI는 summary와 annotation을 먼저 남기고 XML testcase 실패를 nonzero로 만든다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "android-test-cli-"));
    try {
        const reportRoot = path.join(root, "reports");
        const report = path.join(reportRoot, "nested", "TEST.xml");
        const summaryFile = path.join(root, "step-summary.md");
        await fs.mkdir(path.dirname(report), { recursive: true });
        await fs.writeFile(report, REPORT_16_TESTS_3_FAILURES);
        await fs.writeFile(summaryFile, "existing\n");

        const execution = spawnSync(process.execPath, [SCRIPT, reportRoot, root], {
            encoding: "utf8",
            env: {
                ...process.env,
                GITHUB_STEP_SUMMARY: summaryFile,
                GITHUB_WORKSPACE: root,
            },
        });
        assert.equal(execution.status, 1);
        assert.match(execution.stdout, /::error .*TimeLetterFlowAndroidTest/);
        assert.match(execution.stderr, /실패한 테스트가 3개/);
        const written = await fs.readFile(summaryFile, "utf8");
        assert.match(written, /^existing\n## Android tests/m);
        assert.match(written, /16 executed, 3 failed/);
    } finally {
        await fs.rm(root, { recursive: true, force: true });
    }
});

test("XML이 없으면 명확한 summary와 annotation을 출력하고 nonzero로 끝난다", () => {
    const missing = path.join(os.tmpdir(), `missing-android-test-${process.pid}`);
    const execution = spawnSync(process.execPath, [SCRIPT, missing, os.tmpdir()], {
        encoding: "utf8",
        env: cliEnvironment(),
    });

    assert.equal(execution.status, 1);
    assert.match(execution.stdout, /Android test results unavailable/);
    assert.match(execution.stdout, /::error title=Android test results unavailable::/);
    assert.match(execution.stderr, /Managed Device XML 결과가 없습니다/);
});

test("파싱할 수 없는 XML도 summary와 annotation을 남기고 nonzero로 끝난다", async () => {
    const root = await fs.mkdtemp(path.join(os.tmpdir(), "broken-android-test-"));
    try {
        await fs.writeFile(path.join(root, "TEST.xml"), "<testsuite><testcase>");
        const execution = spawnSync(process.execPath, [SCRIPT, root, root], {
            encoding: "utf8",
            env: cliEnvironment(),
        });

        assert.equal(execution.status, 1);
        assert.match(execution.stdout, /Android test results unavailable/);
        assert.match(execution.stdout, /::error title=Android test results unavailable::/);
        assert.match(execution.stderr, /닫히지 않았습니다/);
    } finally {
        await fs.rm(root, { recursive: true, force: true });
    }
});
