import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdtemp, mkdir, readFile, readdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";

const verifier = await readFile(new URL("../../scripts/verify-play-release-bundle.sh", import.meta.url), "utf8");
const signaturePolicy = await readFile(new URL("../../scripts/jarsigner-verification-policy.sh", import.meta.url), "utf8");
const versionPolicy = await readFile(new URL("../../build-logic/src/main/kotlin/VersionCode.kt", import.meta.url), "utf8");
const pinnedSha = "a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29";

// ZIP와 실제 검증 스크립트를 실행한다. Gradle/JDK/다운로드만 fake로 대체해 Play 계정 없이
// manifest 응답·주입값·고정 digest·임시 파일 정리를 함께 검증한다.
async function runVerifier({ expected = "202", actual = "202", jarSha = pinnedSha,
    javaStatus = 0, providedJar = true, skipBuild = true, source = verifier,
    policy = versionPolicy, curlStatus = 0 } = {}) {
    const root = await mkdtemp(join(tmpdir(), "afternote-play-bundle-test-"));
    try {
        const bundle = join(root, "app/build/outputs/bundle/release/app-release.aab");
        for (const directory of ["scripts", "bin", "tmp", "build-logic/src/main/kotlin",
            "app/build/outputs/bundle/release", "app/build/outputs/mapping/release", "entries/base/manifest", "entries/base/dex"]) {
            await mkdir(join(root, directory), { recursive: true });
        }
        await writeFile(join(root, "scripts/verify-play-release-bundle.sh"), source);
        await writeFile(join(root, "scripts/jarsigner-verification-policy.sh"), signaturePolicy);
        await writeFile(join(root, "build-logic/src/main/kotlin/VersionCode.kt"), policy);
        await writeFile(join(root, "app/build/outputs/mapping/release/mapping.txt"), "mapping");
        for (const entry of ["BundleConfig.pb", "base/manifest/AndroidManifest.xml", "base/resources.pb", "base/dex/classes.dex"]) {
            await writeFile(join(root, "entries", entry), "fixture");
        }
        const zip = spawnSync("zip", ["-qr", bundle, "."], { cwd: join(root, "entries"), encoding: "utf8" });
        assert.equal(zip.status, 0, zip.stderr);
        await writeFile(join(root, "bundletool.jar"), "fake pinned jar");
        const programs = {
            jarsigner: 'echo "jar verified."',
            keytool: 'echo "SHA256: AA:BB"',
            shasum: 'printf "%s  %s\\n" "$FAKE_JAR_SHA" "$3"',
            java: `
printf '%s\\n' "$*" >> "$FAKE_ROOT/java.calls"
test "$1" = -jar && test "$3" = dump && test "$4" = manifest || exit 91
test "$5" = "--bundle=$FAKE_ROOT/app/build/outputs/bundle/release/app-release.aab" || exit 92
test "$6" = --module=base && test "$7" = --xpath=/manifest/@android:versionCode || exit 93
printf '%s\\n' "$FAKE_MANIFEST_VERSION_CODE"
exit "$FAKE_JAVA_STATUS"`,
            curl: `
printf '%s\\n' "$*" >> "$FAKE_ROOT/curl.calls"
if [ "$FAKE_CURL_STATUS" -ne 0 ]; then exit "$FAKE_CURL_STATUS"; fi
while [ "$#" -gt 0 ]; do
  if [ "$1" = --output ]; then shift; printf 'fake pinned jar' > "$1"; fi
  shift
done`,
        };
        for (const [name, body] of Object.entries(programs)) {
            await writeFile(join(root, "bin", name), `#!/usr/bin/env bash\nset -euo pipefail\n${body}\n`, { mode: 0o755 });
        }
        await writeFile(join(root, "gradlew"), '#!/usr/bin/env bash\nprintf "%s\\n" "$*" > "$FAKE_ROOT/gradle.calls"\n', { mode: 0o755 });
        const env = { ...process.env, PATH: `${join(root, "bin")}:${process.env.PATH}`,
            TMPDIR: join(root, "tmp"), FAKE_ROOT: root, FAKE_JAR_SHA: jarSha,
            FAKE_MANIFEST_VERSION_CODE: actual, FAKE_JAVA_STATUS: String(javaStatus), FAKE_CURL_STATUS: String(curlStatus) };
        delete env.AFTERNOTE_VERSION_CODE;
        delete env.BUNDLETOOL_JAR;
        if (expected !== null) env.AFTERNOTE_VERSION_CODE = expected;
        if (providedJar) env.BUNDLETOOL_JAR = join(root, "bundletool.jar");
        const result = spawnSync("bash", [join(root, "scripts/verify-play-release-bundle.sh"), ...(skipBuild ? ["--skip-build"] : [])],
            { env, encoding: "utf8", timeout: 10_000 });
        assert.equal(result.error, undefined, result.error?.message);
        const readOptional = (name) => readFile(join(root, name), "utf8").catch(() => "");
        const [javaCalls, curlCalls, gradleCalls] = await Promise.all([readOptional("java.calls"), readOptional("curl.calls"), readOptional("gradle.calls")]);
        return { ...result, javaCalls, curlCalls, gradleCalls, temporaryFiles: await readdir(join(root, "tmp")) };
    } finally {
        await rm(root, { recursive: true, force: true });
    }
}

test("manifest versionCode 가 주입값과 같아야 로컬과 skip-build 검증을 통과한다", async () => {
    for (const skipBuild of [false, true]) {
        const result = await runVerifier({ skipBuild });
        assert.equal(result.status, 0, result.stderr);
        assert.match(result.stdout, /versionCode: 202/);
        assert.match(result.javaCalls, /dump manifest/);
        assert.equal(result.curlCalls, "", "이미 고정 digest의 jar가 있으면 다운로드하지 않는다");
        assert.equal(Boolean(result.gradleCalls), !skipBuild);
    }
});

test("manifest versionCode 누락·비정수·불일치와 bundletool 실패는 거부한다", async () => {
    for (const options of [{ actual: "1" }, { actual: "" }, { actual: "abc" }, { actual: "202\n202" }, { javaStatus: 1 }]) {
        const result = await runVerifier(options);
        assert.notEqual(result.status, 0, JSON.stringify(options));
        assert.match(result.stderr, /versionCode|bundletool/, JSON.stringify(options));
    }
});

test("versionCode 미설정은 Gradle 기본값을 읽고 명시된 잘못된 값은 거부한다", async () => {
    const changedDefault = versionPolicy.replace("DEFAULT_AFTERNOTE_VERSION_CODE = 1", "DEFAULT_AFTERNOTE_VERSION_CODE = 7");
    const result = await runVerifier({ expected: null, actual: "7", policy: changedDefault });
    assert.equal(result.status, 0, result.stderr);
    assert.match(result.stdout, /versionCode: 7/);
    for (const expected of ["", "0", "01", "not-a-code", "2100000001", "99999999999999999999999"]) {
        const invalid = await runVerifier({ expected, actual: expected });
        assert.notEqual(invalid.status, 0, expected);
        assert.match(invalid.stderr, /AFTERNOTE_VERSION_CODE/);
    }
    const trimmed = await runVerifier({ expected: " 202 ", actual: "202" });
    assert.equal(trimmed.status, 0, trimmed.stderr);
});

test("bundletool SHA256 불일치는 Java 실행 전에 거부한다", async () => {
    const result = await runVerifier({ jarSha: "0".repeat(64) });
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /bundletool SHA-256/);
    assert.equal(result.javaCalls, "");
});

test("로컬 bundletool 다운로드는 고정 버전을 쓰고 성공·실패 모두 임시 파일을 지운다", async () => {
    for (const options of [{}, { actual: "1" }, { curlStatus: 22 }, { jarSha: "0".repeat(64) }]) {
        const result = await runVerifier({ providedJar: false, ...options });
        assert.equal(result.status === 0, Object.keys(options).length === 0, result.stderr);
        assert.match(result.curlCalls, /https:\/\/github.com\/google\/bundletool\/releases\/download\/1\.18\.3\/bundletool-all-1\.18\.3\.jar/);
        assert.deepEqual(result.temporaryFiles, []);
    }
});
