import assert from "node:assert/strict";
import test from "node:test";
import { classifyAndroidManagedDeviceFailure } from "./classify-android-managed-device-failure.mjs";

test("retries one API 34 timeout proven to be in the managed-device boot phase", () => {
    const result = classifyAndroidManagedDeviceFailure({
        device: "api34",
        outcome: "success",
        exitCode: "124",
        log: "> Task :app:pixel2Api34DebugAndroidTest",
        testResultCount: 0,
    });

    assert.equal(result.retryable, true);
    assert.equal(result.reason, "managed-device-boot-timeout");
});

test("does not retry a test failure after XML results exist", () => {
    const result = classifyAndroidManagedDeviceFailure({
        device: "api34",
        outcome: "success",
        exitCode: "1",
        log: "emulator terminated before boot",
        testResultCount: 1,
    });

    assert.equal(result.retryable, false);
    assert.equal(result.reason, "test-results-exist");
});

test("does not relabel a slow compile as an emulator infrastructure failure", () => {
    const result = classifyAndroidManagedDeviceFailure({
        device: "api34",
        outcome: "cancelled",
        log: "> Task :feature:afternote:presentation:compileDebugKotlin",
        testResultCount: 0,
    });

    assert.equal(result.retryable, false);
    assert.equal(result.reason, "not-proven-infrastructure-failure");
});

test("retries the API 30 pre-test provisioning timeout observed on PR 1312", () => {
    const result = classifyAndroidManagedDeviceFailure({
        device: "api30",
        outcome: "success",
        exitCode: "124",
        log: [
            "> Task :app:pixel2Api30Setup",
            'Preparing "Install Intel x86_64 Atom System Image API 30 (revision 11)".',
            "Installing Intel x86_64 Atom System Image in /sdk/system-images/android-30/default/x86_64",
        ].join("\n"),
        testResultCount: 0,
    });

    assert.equal(result.retryable, true);
    assert.equal(result.reason, "managed-device-boot-timeout");
    assert.equal(result.exitCode, "124");
    assert.deepEqual(result.testExecutionSignals, []);
});

test("does not retry an API 30 compile-only timeout", () => {
    const result = classifyAndroidManagedDeviceFailure({
        device: "api30",
        outcome: "success",
        exitCode: "124",
        log: "> Task :feature:afternote:presentation:compileDebugKotlin",
        testResultCount: 0,
    });

    assert.equal(result.retryable, false);
    assert.equal(result.reason, "not-proven-infrastructure-failure");
});

test("does not retry an API 30 non-timeout even with infrastructure evidence", () => {
    const result = classifyAndroidManagedDeviceFailure({
        device: "api30",
        outcome: "success",
        exitCode: "1",
        log: "Unable to start emulator: boot timed out",
        testResultCount: 0,
    });

    assert.equal(result.retryable, false);
    assert.equal(result.reason, "not-proven-infrastructure-failure");
});

test("does not retry an API 30 timeout after test execution starts", () => {
    const result = classifyAndroidManagedDeviceFailure({
        device: "api30",
        outcome: "success",
        exitCode: "124",
        log: [
            "> Task :app:pixel2Api30Setup",
            "Starting 88 tests on pixel2Api30",
        ].join("\n"),
        testResultCount: 0,
    });

    assert.equal(result.retryable, false);
    assert.equal(result.reason, "test-execution-started");
    assert.deepEqual(result.testExecutionSignals, ["test-execution-started"]);
});

test("retries an explicit emulator boot failure before tests start", () => {
    const result = classifyAndroidManagedDeviceFailure({
        device: "api34",
        outcome: "success",
        exitCode: "1",
        log: "Unable to start emulator: boot timed out",
        testResultCount: 0,
    });

    assert.equal(result.retryable, true);
    assert.equal(result.reason, "explicit-infrastructure-failure");
});

test("does not retry a successful managed-device run", () => {
    const result = classifyAndroidManagedDeviceFailure({
        device: "api34",
        outcome: "success",
        exitCode: "0",
        log: "> Task :app:pixel2Api34DebugAndroidTest",
        testResultCount: 0,
    });

    assert.equal(result.retryable, false);
    assert.equal(result.reason, "not-failed");
});

test("does not retry an unsupported managed device", () => {
    const result = classifyAndroidManagedDeviceFailure({
        device: "api35",
        outcome: "success",
        exitCode: "124",
        log: "Unable to start emulator: boot timed out",
        testResultCount: 0,
    });

    assert.equal(result.retryable, false);
    assert.equal(result.reason, "unsupported-device");
});
