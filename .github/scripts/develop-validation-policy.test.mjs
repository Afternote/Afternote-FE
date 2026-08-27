import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const workflowDirectory = new URL("../workflows/", import.meta.url);

async function workflow(name) {
    return readFile(new URL(name, workflowDirectory), "utf8");
}

function withoutComments(source) {
    return source
        .split("\n")
        .filter((line) => !/^\s*#/.test(line))
        .join("\n");
}

test("develop validation runs only on develop pushes and cancels obsolete combinations", async () => {
    const source = withoutComments(await workflow("develop-validation.yml"));
    const triggerBlock = source.slice(source.indexOf("on:\n"), source.indexOf("\nconcurrency:"));
    const events = [...triggerBlock.matchAll(/^  ([a-z_]+):/gm)].map((match) => match[1]);

    assert.deepEqual(events, ["push"]);
    assert.match(source, /^on:\n\s{2}push:\n\s{4}branches:\s*\[develop\]/m);
    assert.match(source, /concurrency:\n\s{2}group:\s*\$\{\{ github\.run_attempt == 1/);
    assert.match(source, /develop-validation-current/);
    assert.match(source, /develop-validation-rerun-\{0\}.*github\.run_id/);
    assert.match(source, /cancel-in-progress:\s*true/);
});

test("develop validation calls only the unit and screenshot validation workflows", async () => {
    const source = await workflow("develop-validation.yml");
    const jobs = source.slice(source.indexOf("jobs:\n"));
    const jobNames = [...jobs.matchAll(/^  ([a-z0-9-]+):$/gm)].map((match) => match[1]);

    assert.deepEqual(jobNames, ["unit-test", "screenshot", "report"]);
    assert.match(jobs, /unit-test:\n(?:\s{4}.+\n)*?\s{4}uses:\s*\.\/\.github\/workflows\/unit-test\.yml/);
    assert.match(jobs, /screenshot:\n(?:\s{4}.+\n)*?\s{4}uses:\s*\.\/\.github\/workflows\/screenshot\.yml/);
});

test("develop validation satisfies each reusable workflow permission ceiling exactly", async () => {
    const source = withoutComments(await workflow("develop-validation.yml"));
    const workflowPermissions = source.slice(source.indexOf("permissions:\n"), source.indexOf("\njobs:\n"));
    const unitTestJob = source.slice(source.indexOf("  unit-test:\n"), source.indexOf("\n  screenshot:\n"));
    const screenshotJob = source.slice(source.indexOf("  screenshot:\n"), source.indexOf("\n  report:\n"));

    assert.equal(workflowPermissions.trimEnd(), "permissions:\n  contents: read");

    for (const [jobName, job, workflowName] of [
        ["unit test", unitTestJob, "unit-test.yml"],
        ["screenshot", screenshotJob, "screenshot.yml"],
    ]) {
        const calledSource = withoutComments(await workflow(workflowName));
        const callerPermissions = job.slice(job.indexOf("    permissions:\n"), job.indexOf("\n    uses:"));
        const calledPermissions = calledSource.slice(
            calledSource.indexOf("permissions:\n"),
            calledSource.indexOf("\njobs:\n"),
        );

        assert.equal(
            callerPermissions.replaceAll(/^ {4}/gm, "").trimEnd(),
            calledPermissions.trimEnd(),
            `the caller must satisfy the reusable ${jobName} workflow permission ceiling exactly`,
        );
    }
});

test("develop validation reuses read-only Gradle cache consumers", async () => {
    for (const name of ["unit-test.yml", "screenshot.yml"]) {
        const source = withoutComments(await workflow(name));
        assert.match(source, /cache-read-only:\s*true/, `${name} must keep the Gradle cache read-only`);
        assert.doesNotMatch(source, /cache-read-only:\s*false/, `${name} must not become a develop cache writer`);
    }
});

test("pull-request-only Kover context is skipped on develop pushes", async () => {
    const source = await workflow("unit-test.yml");

    assert.match(
        source,
        /pull_request_number:\n\s+required: false\n\s+default: 0\n\s+type: number/,
        "develop callers must default to no pull request context",
    );

    for (const stepName of [
        "Fetch pull request history for changed-module coverage",
        "Summarize changed-module coverage",
    ]) {
        assert.match(
            source,
            new RegExp(`- name: ${stepName}\\n\\s+if: inputs\\.pull_request_number > 0`),
            `${stepName} must not read pull_request fields on a push event`,
        );
    }
});

test("the reporter is wired to the tested incident state machine with least privilege", async () => {
    const source = await workflow("develop-validation.yml");
    const reportJob = source.slice(source.indexOf("  report:\n"));
    const reportPermissions = reportJob.slice(
        reportJob.indexOf("    permissions:\n"),
        reportJob.indexOf("\n    steps:"),
    );

    assert.match(reportJob, /report:\n(?:\s{4}.+\n)*?\s{4}if:\s*always\(\)/);
    assert.match(reportJob, /needs:\s*\[unit-test, screenshot\]/);
    assert.equal(
        reportPermissions.replaceAll(/^ {4}/gm, "").trimEnd(),
        "permissions:\n  contents: read\n  issues: write",
    );
    assert.match(reportJob, /VALIDATION_RESULTS:\s*\$\{\{ toJSON\(needs\) \}\}/);
    assert.match(reportJob, /develop-validation-incident\.mjs/);
    assert.match(reportJob, /reconcileDevelopValidationIncident/);
});
