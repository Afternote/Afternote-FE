import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { chmod, copyFile, mkdir, mkdtemp, readFile, rm, utimes, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import test from "node:test";

const repository = new URL("../../", import.meta.url);
const bundleRelative = "app/build/outputs/bundle/release/app-release.aab";
const mappingRelative = "app/build/outputs/mapping/release/mapping.txt";

async function executable(path, source) {
    await writeFile(path, source);
    await chmod(path, 0o755);
}

// Run the actual fixture suite and verifier against a tiny ZIP. Only Gradle and JDK commands
// are doubles: ordinary PRs need no Android build or signing key to prove fixture coverage.
async function runFixtures(context, mutateVerifier = (source) => source) {
    const root = await mkdtemp(join(tmpdir(), "afternote-fixture-policy-"));
    context.after(() => rm(root, { recursive: true, force: true }));
    for (const directory of ["scripts", ".github/scripts", "bin", "tmp",
        dirname(bundleRelative), dirname(mappingRelative), "archive/base/manifest", "archive/base/dex"]) {
        await mkdir(join(root, directory), { recursive: true });
    }
    for (const path of ["scripts/jarsigner-verification-policy.sh",
        "scripts/test-jarsigner-verification-policy.sh",
        ".github/scripts/test-release-aab-negative-fixtures.sh"]) {
        await copyFile(new URL(path, repository), join(root, path));
        await chmod(join(root, path), 0o755);
    }
    const verifier = mutateVerifier(await readFile(
        new URL("scripts/verify-play-release-bundle.sh", repository), "utf8",
    )).replace('source "${script_dir}/jarsigner-verification-policy.sh"',
        'source "${script_dir}/jarsigner-verification-policy.sh"\nprintf "%s\\n" "${repo_root##*/}" >> "$FIXTURE_TRACE"');
    await executable(join(root, "scripts/verify-play-release-bundle.sh"), verifier);
    await writeFile(join(root, "app/proguard-rules.pro"), "# original rules\n");
    await writeFile(join(root, mappingRelative), "fixture mapping\n");
    for (const entry of ["BundleConfig.pb", "base/manifest/AndroidManifest.xml",
        "base/resources.pb", "base/dex/classes.dex"]) {
        await writeFile(join(root, "archive", entry), "fixture contents\n");
    }
    // The signature fixture must replace an entry even when its source mtime is not newer.
    // ZIP stores timestamps in two-second ticks; fast runners can otherwise skip the mutation.
    const archiveTime = new Date(Date.now() + 60_000);
    await utimes(join(root, "archive/base/resources.pb"), archiveTime, archiveTime);
    const archive = spawnSync("zip", ["-q", "-r", join(root, bundleRelative), "."],
        { cwd: join(root, "archive"), encoding: "utf8" });
    assert.equal(archive.status, 0, archive.stderr);
    await executable(join(root, "gradlew"), `#!/usr/bin/env bash
set -euo pipefail
grep -Fq -- '-afternote-invalid-keep-rule' "$(dirname "$0")/app/proguard-rules.pro"
echo 'unknown option: -afternote-invalid-keep-rule' >&2
exit 1
`);
    await executable(join(root, "bin/jarsigner"), `#!/usr/bin/env bash
set -euo pipefail
if unzip -p "\${@: -1}" base/resources.pb | grep -Fq tampered; then
    echo 'java.lang.SecurityException: digest error' >&2
    exit 1
fi
echo 'jar verified.'
`);
    await executable(join(root, "bin/keytool"), "#!/usr/bin/env bash\necho 'SHA256: AA:BB'\n");
    const trace = join(root, "trace");
    await writeFile(trace, "");
    const result = spawnSync("bash", [".github/scripts/test-release-aab-negative-fixtures.sh"], {
        cwd: root,
        encoding: "utf8",
        timeout: 20_000,
        env: { ...process.env, PATH: `${join(root, "bin")}:${process.env.PATH}`,
            TMPDIR: join(root, "tmp"), FIXTURE_TRACE: trace },
    });
    assert.equal(result.error, undefined);
    assert.equal(await readFile(join(root, "app/proguard-rules.pro"), "utf8"), "# original rules\n");
    return { ...result, trace: (await readFile(trace, "utf8")).trim().split("\n") };
}

test("negative fixtures execute every AAB verifier failure branch", async (context) => {
    const result = await runFixtures(context);
    assert.equal(result.status, 0, result.stdout + result.stderr);
    assert.deepEqual(result.trace.slice(1), [
        "empty-bundle", "missing-mapping", "missing-entry", "invalid-signature",
    ]);
});

for (const [name, fixture, failure, from, to] of [
    ["empty AAB", "empty-bundle", "Empty-AAB", 'if [[ ! -s "${bundle_path}" ]]; then', 'if false; then'],
    ["missing mapping", "missing-mapping", "Missing-mapping", 'if [[ ! -s "${mapping_path}" ]]; then', 'if false; then'],
    ["required entry", "missing-entry", "Missing-entry", 'if ! grep -Fqx "${required_entry}" <<<"${bundle_entries}"; then', 'if false; then'],
    ["signature", "invalid-signature", "Invalid-signature", 'verify_jarsigner_result "${strict_status}" "${verification_output}"', ":"],
]) {
    test(`negative fixtures reject a verifier mutation that bypasses ${name}`, async (context) => {
        const result = await runFixtures(context, (source) => {
            assert.ok(source.includes(from), `missing mutation target: ${name}`);
            return source.replace(from, to);
        });
        assert.notEqual(result.status, 0, `fixture suite accepted bypassed ${name}\n${result.stdout}`);
        assert.equal(result.trace.at(-1), fixture, `mutation failed before reaching ${fixture}`);
        assert.ok(result.stderr.includes(`${failure} fixture did not fail closed:`), result.stderr);
    });
}
