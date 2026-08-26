import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const managedDevice = await readFile(
  new URL('../workflows/android-managed-device.yml', import.meta.url),
  'utf8',
);
const hygiene = await readFile(new URL('../workflows/pr-hygiene.yml', import.meta.url), 'utf8');
const lint = await readFile(new URL('../workflows/lint.yml', import.meta.url), 'utf8');
const appBuild = await readFile(new URL('../../app/build.gradle.kts', import.meta.url), 'utf8');

test('accessibility smoke uses an API 34 managed device and only its dedicated class', () => {
  assert.match(appBuild, /create\("pixel2Api34"\)[\s\S]*?apiLevel = 34/);
  assert.match(appBuild, /androidx\.compose\.ui\.test\.junit4\.accessibility/);
  assert.match(managedDevice, /task: pixel2Api34DebugAndroidTest/);
  assert.match(managedDevice, /test_class: com\.afternote\.afternote_fe\.AccessibilitySmokeAndroidTest/);
  assert.match(managedDevice, /android\.testInstrumentationRunnerArguments\.class/);
});

test('manifest policy runs against the AGP merged manifest after generation', () => {
  assert.match(lint, /:app:processDebugMainManifest/);
  assert.match(lint, /verify-android-manifest\.mjs/);
  assert.match(lint, /app\/build\/intermediates\/merged_manifest\/debug\/processDebugMainManifest\/AndroidManifest\.xml/);
});

test('scheduled hygiene executes only trusted default-branch code', () => {
  assert.match(hygiene, /^  schedule:/m);
  assert.match(hygiene, /^  workflow_dispatch:/m);
  assert.match(hygiene, /github\.ref_name == github\.event\.repository\.default_branch/);
  assert.match(hygiene, /ref: \$\{\{ github\.event\.repository\.default_branch \}\}/);
  assert.match(hygiene, /persist-credentials: false/);
  assert.match(hygiene, /STALE_DAYS: '14'/);
  assert.match(hygiene, /node --test \.github\/scripts\/reconcile-pr-hygiene\.test\.mjs/);
});

test('activity path never checks out pull-request code and only removes stale', () => {
  const clearStale = hygiene.split(/^  clear-stale:/m)[1];
  assert.ok(clearStale);
  assert.doesNotMatch(hygiene, /^  pull_request_target:/m);
  assert.doesNotMatch(clearStale, /actions\/checkout@/);
  assert.match(clearStale, /issues\.removeLabel/);
  assert.match(clearStale, /name: 'stale'/);
});
