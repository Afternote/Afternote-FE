import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdir, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import test from "node:test";

const repository = new URL("../../", import.meta.url);
const publisher = ".github/scripts/play-internal-track.mjs";
const verifier = "scripts/verify-play-release-bundle.sh";
const workflow = ".github/workflows/release-play-internal.yml";
const publisherTests = ".github/scripts/play-internal-track.test.mjs";
const verifierTests = ".github/scripts/verify-play-release-bundle.test.mjs";
const policyTests = ".github/scripts/release-play-internal-policy.test.mjs";
const files = [publisher, verifier, workflow, publisherTests, verifierTests, policyTests,
    "scripts/jarsigner-verification-policy.sh", "build-logic/src/main/kotlin/VersionCode.kt"];
const originals = new Map(await Promise.all(files.map(async (file) => [file, await readFile(new URL(file, repository), "utf8")])));

// 실제 회귀 테스트를 복제된 소스에 다시 실행한다. 의도한 가드를 제거했을 때 테스트가 빨간불이
// 되는지 확인하며, 원본 checkout 과 Play 서버는 건드리지 않는다.
for (const [name, file, before, after, suite, pattern] of [
    ["publish 의 tracks 조회 제거", publisher,
        "const tracks = await client.listTracks(editId);\n        const latest =", "const tracks = {};\n        const latest =",
        publisherTests, "업로드 직전 다른 track"],
    ["publish 의 bundles 조회 제거", publisher,
        "const latest = maxVersionCode(bundles, tracks);", "const latest = maxVersionCode({}, tracks);",
        publisherTests, "업로드 직전 더 큰 bundle"],
    ["단조 증가를 동등값만 차단하도록 축소", publisher,
        "if (expected <= latest)", "if (expected === latest)", publisherTests, "업로드 직전 더 큰 bundle"],
    ["같은 versionCode 허용", publisher,
        "if (expected <= latest)", "if (expected < latest)", publisherTests, "업로드 직전 같은 track"],
    ["edit 삭제 시도 제거", publisher,
        "await client.deleteEdit(editId);", "void editId;", publisherTests, "정리 실패"],
    ["삭제 실패로 원래 오류 덮기", publisher,
        'log(`::warning::미완료 edit ${editId} 정리 실패: ${error.message}`);', "throw error;", publisherTests, "정리 실패"],
    ["manifest 불일치 허용", verifier,
        '[[ "${manifest_version_code}" != "${expected_version_code}" ]]', "false", verifierTests, "manifest versionCode 누락"],
    ["bundletool digest 불일치 허용", verifier,
        '[[ "$(sha256_file "${bundletool_jar}")" != "${bundletool_sha256}" ]]', "false", verifierTests, "bundletool SHA256"],
    ["검증 step 의 주입값 제거", workflow,
        "- name: Verify the AAB signature and contents\n        env:\n          AFTERNOTE_VERSION_CODE: ${{ steps.version_code.outputs.version_code }}",
        "- name: Verify the AAB signature and contents", policyTests, "서명 검증기가"],
]) {
    test(`Play publish 회귀 검사가 «${name}» 변이를 거부한다`, async () => {
        const source = originals.get(file);
        assert.equal(source.split(before).length - 1, 1, "변이가 정확히 한 곳에 적용되어야 한다");
        const root = await mkdtemp(join(tmpdir(), "afternote-play-mutation-"));
        try {
            for (const [name, content] of originals) {
                const output = join(root, name);
                await mkdir(dirname(output), { recursive: true });
                await writeFile(output, name === file ? content.replace(before, after) : content);
            }
            const env = { ...process.env };
            // 부모 node:test worker 문맥을 물려주면 자식 runner 가 선택한 테스트를 건너뛴다.
            delete env.NODE_TEST_CONTEXT;
            const result = spawnSync(process.execPath, ["--test", `--test-name-pattern=${pattern}`, join(root, suite)],
                { env, encoding: "utf8", timeout: 30_000 });
            assert.equal(result.error, undefined, result.error?.message);
            assert.notEqual(result.status, 0, `가드가 사라졌는데 회귀 검사가 통과했다:\n${result.stdout}${result.stderr}`);
            assert.match(result.stdout + result.stderr, /AssertionError|ERR_ASSERTION/, "실행 장애가 아닌 회귀 단언이 변이를 거부해야 한다");
        } finally {
            await rm(root, { recursive: true, force: true });
        }
    });
}
