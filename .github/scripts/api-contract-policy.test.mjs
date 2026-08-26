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

test('wire smoke uses matching Testcontainers and MockServer versions', () => {
  assert.match(build, /testImplementation\(libs\.testcontainers\.mockserver\)/);
  assert.match(build, /testImplementation\(libs\.mockserver\.client\)/);
  assert.match(smoke, /MOCKSERVER_VERSION = "5\.15\.0"/);
  assert.match(smoke, /DockerClientFactory\.instance\(\)\.isDockerAvailable/);
  assert.match(smoke, /MatchType\.STRICT/);
  assert.match(smoke, /\/api\/v1\/auth\/social\/login/);
  assert.match(smoke, /Authorization", "Bearer contract-token"/);
});
