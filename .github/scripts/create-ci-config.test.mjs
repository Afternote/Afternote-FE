import assert from "node:assert/strict";
import { execFile as execFileCallback } from "node:child_process";
import { mkdtemp, readFile, readdir, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join, resolve } from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

import {
    CI_CONFIG_MODE,
    ciEnvironment,
    createCiConfig,
    renderGitHubEnvironment,
    renderGoogleServicesJson,
} from "./create-ci-config.mjs";

const scriptsDirectory = dirname(fileURLToPath(import.meta.url));
const githubDirectory = resolve(scriptsDirectory, "..");
const execFile = promisify(execFileCallback);

async function createTemporaryWorkspace() {
    const workspace = await mkdtemp(join(tmpdir(), "afternote-ci-config-"));
    return {
        workspace,
        githubEnv: join(workspace, "github-env"),
    };
}

test("creates the same secretless app configuration in clean workspaces", async () => {
    const first = await createTemporaryWorkspace();
    const second = await createTemporaryWorkspace();

    try {
        await createCiConfig(first);
        await createCiConfig(second);

        const firstGoogleServices = await readFile(
            join(first.workspace, "app/google-services.json"),
            "utf8",
        );
        const secondGoogleServices = await readFile(
            join(second.workspace, "app/google-services.json"),
            "utf8",
        );
        assert.equal(firstGoogleServices, secondGoogleServices);
        assert.equal(firstGoogleServices, renderGoogleServicesJson());
        assert.equal(await readFile(first.githubEnv, "utf8"), renderGitHubEnvironment());
    } finally {
        await Promise.all([
            rm(first.workspace, { recursive: true, force: true }),
            rm(second.workspace, { recursive: true, force: true }),
        ]);
    }
});

test("uses inert placeholders with the production Android package contract", () => {
    const googleServices = JSON.parse(renderGoogleServicesJson());
    const serialized = JSON.stringify(googleServices);

    assert.equal(
        googleServices.client[0].client_info.android_client_info.package_name,
        "com.afternote.afternote_fe",
    );
    assert.equal(ciEnvironment.AFTERNOTE_CI_CONFIG_MODE, CI_CONFIG_MODE);
    assert.match(ciEnvironment.GOOGLE_WEB_CLIENT_ID, /\.invalid$/);
    assert.doesNotMatch(serialized, /AIza[0-9A-Za-z_-]{30,}/);
    assert.doesNotMatch(serialized, /apps\.googleusercontent\.com/);
});

test("supports the composite action command-line contract", async () => {
    const temporary = await createTemporaryWorkspace();

    try {
        await execFile(process.execPath, [
            join(scriptsDirectory, "create-ci-config.mjs"),
            "--workspace",
            temporary.workspace,
            "--github-env",
            temporary.githubEnv,
        ]);
        assert.equal(
            await readFile(join(temporary.workspace, "app/google-services.json"), "utf8"),
            renderGoogleServicesJson(),
        );
        assert.equal(await readFile(temporary.githubEnv, "utf8"), renderGitHubEnvironment());
    } finally {
        await rm(temporary.workspace, { recursive: true, force: true });
    }
});

test("keeps pull request validation secretless and release credentials isolated", async () => {
    const workflowsDirectory = join(githubDirectory, "workflows");
    const workflowNames = (await readdir(workflowsDirectory)).filter(
        (name) => name.endsWith(".yml") || name.endsWith(".yaml"),
    );
    const workflows = new Map(
        await Promise.all(
            workflowNames.map(async (name) => [
                name,
                await readFile(join(workflowsDirectory, name), "utf8"),
            ]),
        ),
    );

    for (const [name, workflow] of workflows) {
        assert.doesNotMatch(
            workflow,
            /^\s*pull_request_target\s*:/m,
            `${name} must not use pull_request_target`,
        );
        if (!/^\s*pull_request\s*:/m.test(workflow)) {
            continue;
        }

        const secretReferences = [
            ...workflow.matchAll(
                /\bsecrets(?:\.([A-Za-z0-9_]+)|\[['"]([^'"]+)['"]\])/g,
            ),
        ].map((match) => match[1] ?? match[2]);
        assert.deepEqual(
            [...new Set(secretReferences)].filter((name_) => name_ !== "GITHUB_TOKEN"),
            [],
            `${name} must not depend on repository or environment secrets`,
        );
    }

    const actionReference = "uses: ./.github/actions/setup-ci-config";
    for (const name of [
        "lint.yml",
        "unit-test.yml",
        "screenshot.yml",
        "android-managed-device.yml",
    ]) {
        assert.match(workflows.get(name), new RegExp(actionReference.replaceAll(".", "\\.")));
    }

    const releaseWorkflow = workflows.get("release-distribution.yml");
    assert.doesNotMatch(releaseWorkflow, /\.\/\.github\/actions\/setup-ci-config/);
    for (const secretName of [
        "GOOGLE_SERVICES_JSON_B64",
        "KAKAO_NATIVE_APP_KEY",
        "GOOGLE_WEB_CLIENT_ID",
        "RELEASE_STORE_FILE_B64",
        "FIREBASE_SERVICE_ACCOUNT_JSON",
    ]) {
        assert.match(releaseWorkflow, new RegExp(`secrets\\.${secretName}`));
    }
});
