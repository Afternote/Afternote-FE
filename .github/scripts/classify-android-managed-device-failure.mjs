import { appendFile, mkdir, readFile, readdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const INFRASTRUCTURE_PATTERNS = [
    ["adb-unavailable", /adb[^\n]*(?:device offline|device not found|no devices|cannot connect)/i],
    ["avd-start-failed", /(?:failed|unable|could not)\s+to\s+(?:launch|start)[^\n]*(?:avd|emulator)/i],
    ["emulator-boot-failed", /emulator[^\n]*(?:boot[^\n]*(?:failed|timeout|timed out)|terminated|offline|not booted)/i],
    ["managed-device-unavailable", /managed[ -]device[^\n]*(?:failed|timeout|timed out|unavailable)/i],
];

const BOOT_PHASE_PATTERNS = [
    ["api34-managed-device-task", /(?:^|\s|:)pixel2Api34(?:DebugAndroidTest|Setup|Check)?\b/im],
    ["gradle-managed-device", /gradle managed device|managed[ -]device/i],
    ["emulator-start", /(?:launching|starting|started)[^\n]*(?:avd|emulator)/i],
    ["system-image-provisioning", /(?:downloading|installing)[^\n]*(?:system image|system-images;android-34)/i],
];

function matchingSignals(log, patterns) {
    return patterns.filter(([, pattern]) => pattern.test(log)).map(([name]) => name);
}

export function classifyAndroidManagedDeviceFailure({
    device,
    exitCode = "",
    log = "",
    outcome,
    testResultCount = 0,
}) {
    const normalizedDevice = String(device ?? "").trim().toLowerCase();
    const normalizedExitCode = String(exitCode ?? "").trim();
    const normalizedOutcome = String(outcome ?? "").trim().toLowerCase();
    const infrastructureSignals = matchingSignals(log, INFRASTRUCTURE_PATTERNS);
    const bootPhaseSignals = matchingSignals(log, BOOT_PHASE_PATTERNS);
    const timedOut = normalizedExitCode === "124" ||
        normalizedOutcome === "cancelled" ||
        normalizedOutcome === "timed_out";
    const failed = normalizedOutcome !== "success" || (
        normalizedExitCode !== "" && normalizedExitCode !== "0"
    );

    let reason = "not-failed";
    if (normalizedDevice !== "api34") {
        reason = "unsupported-device";
    } else if (!failed) {
        reason = "not-failed";
    } else if (testResultCount > 0) {
        reason = "test-results-exist";
    } else if (infrastructureSignals.length > 0) {
        reason = "explicit-infrastructure-failure";
    } else if (timedOut && bootPhaseSignals.length > 0) {
        reason = "managed-device-boot-timeout";
    } else {
        reason = "not-proven-infrastructure-failure";
    }

    return {
        retryable: normalizedDevice === "api34" && testResultCount === 0 && (
            infrastructureSignals.length > 0 || (timedOut && bootPhaseSignals.length > 0)
        ) && failed,
        reason,
        infrastructureSignals,
        bootPhaseSignals,
        testResultCount,
    };
}

async function countXmlFiles(directory) {
    let entries;
    try {
        entries = await readdir(directory, { withFileTypes: true });
    } catch (error) {
        if (error.code === "ENOENT") return 0;
        throw error;
    }

    let count = 0;
    for (const entry of entries) {
        const entryPath = path.join(directory, entry.name);
        if (entry.isDirectory()) {
            count += await countXmlFiles(entryPath);
        } else if (entry.isFile() && entry.name.endsWith(".xml")) {
            count += 1;
        }
    }
    return count;
}

async function main() {
    const [logPath, resultsDirectory, outputPath] = process.argv.slice(2);
    if (!logPath || !resultsDirectory || !outputPath) {
        throw new Error("Usage: classify-android-managed-device-failure.mjs <log> <results-dir> <output>");
    }

    let log = "";
    try {
        log = await readFile(logPath, "utf8");
    } catch (error) {
        if (error.code !== "ENOENT") throw error;
    }
    const testResultCount = await countXmlFiles(resultsDirectory);
    const classification = classifyAndroidManagedDeviceFailure({
        device: process.env.DEVICE,
        exitCode: process.env.ANDROID_TEST_EXIT_CODE,
        log,
        outcome: process.env.ANDROID_TEST_OUTCOME,
        testResultCount,
    });
    const marker = {
        schemaVersion: 1,
        sourceRunId: Number(process.env.GITHUB_RUN_ID),
        runAttempt: Number(process.env.GITHUB_RUN_ATTEMPT),
        headSha: process.env.TARGET_SHA,
        device: process.env.DEVICE,
        ...classification,
    };

    await mkdir(path.dirname(outputPath), { recursive: true });
    await writeFile(outputPath, `${JSON.stringify(marker, null, 2)}\n`);
    if (process.env.GITHUB_OUTPUT) {
        await appendFile(process.env.GITHUB_OUTPUT, [
            `retryable=${classification.retryable}`,
            `reason=${classification.reason}`,
            "",
        ].join("\n"));
    }
    process.stdout.write(`${JSON.stringify(marker)}\n`);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
    await main();
}
