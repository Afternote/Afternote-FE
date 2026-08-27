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

test("does not automatically retry API 30", () => {
    const result = classifyAndroidManagedDeviceFailure({
        device: "api30",
        outcome: "cancelled",
        log: "> Task :app:pixel2Api30DebugAndroidTest\nemulator boot timed out",
        testResultCount: 0,
    });

    assert.equal(result.retryable, false);
    assert.equal(result.reason, "unsupported-device");
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
