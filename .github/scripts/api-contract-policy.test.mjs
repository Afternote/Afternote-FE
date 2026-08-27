import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const workflow = await readFile(
  new URL('../workflows/api-contract-smoke.yml', import.meta.url),
  'utf8',
);
const build = await readFile(
  new URL('../../core/network/build.gradle.kts', import.meta.url),
  'utf8',
);
const smoke = await readFile(
  new URL(
    '../../core/network/src/test/kotlin/com/afternote/core/network/service/ApiWireContractSmokeTest.kt',
    import.meta.url,
  ),
  'utf8',
);

test('contract smoke is explicit, Docker-backed, secretless, and bounded', () => {
  assert.match(workflow, /^name: API Wire Contract Smoke$/m);
  assert.match(workflow, /^  pull_request:\n    paths:/m);
  assert.match(workflow, /^  schedule:/m);
  assert.match(workflow, /^  workflow_dispatch:/m);
  assert.match(workflow, /^permissions:\n  contents: read$/m);
  assert.match(workflow, /timeout-minutes: 15/);
  assert.match(workflow, /RUN_API_CONTRACT_SMOKE: 'true'/);
  assert.match(workflow, /run: docker info/);
  assert.match(workflow, /--tests '\*ApiWireContractSmokeTest'/);
  assert.doesNotMatch(workflow, /secrets\./);
});

test('wire smoke uses Testcontainers with a pinned MockServer REST API image', () => {
  assert.match(build, /testImplementation\(libs\.testcontainers\.mockserver\)/);
  assert.doesNotMatch(build, /mockserver\.client/);
  assert.match(smoke, /MOCKSERVER_VERSION = "7\.6\.0"/);
  assert.match(smoke, /DockerClientFactory\.instance\(\)\.isDockerAvailable/);
  assert.match(smoke, /put\("matchType", "STRICT"\)/);
  assert.match(smoke, /\/api\/v1\/auth\/social\/login/);
  assert.match(smoke, /Authorization", "Bearer contract-token"/);
});

test('REST control plane resets state and verifies exactly one recorded request', () => {
  assert.match(smoke, /controlPut\("\/mockserver\/reset"\)/);
  assert.match(smoke, /controlPut\("\/mockserver\/expectation"/);
  assert.match(smoke, /\/mockserver\/retrieve\?type=REQUESTS/);
  assert.match(smoke, /assertEquals\("\$method \$path must cross the socket exactly once", 1, recorded\.size\)/);
});
