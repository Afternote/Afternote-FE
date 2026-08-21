import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { readdir, readFile } from 'node:fs/promises';
import test from 'node:test';

const workflowDirectory = new URL('../workflows/', import.meta.url);

async function workflows() {
  const names = (await readdir(workflowDirectory)).filter((name) => name.endsWith('.yml'));
  return Promise.all(
    names.map(async (name) => [name, await readFile(new URL(name, workflowDirectory), 'utf8')]),
  );
}

test('every workflow that runs the Gradle wrapper uses the pinned setup-gradle action', async () => {
  const gradleWorkflows = (await workflows()).filter(([, source]) => source.includes('./gradlew'));
  assert.ok(gradleWorkflows.length > 0);

  for (const [name, source] of gradleWorkflows) {
    assert.match(
      source,
      /gradle\/actions\/setup-gradle@[0-9a-f]{40} # v\d+\.\d+\.\d+/,
      `${name} must configure Gradle from an immutable action revision`,
    );
    assert.match(source, /cache-provider:\s*basic/, `${name} must use the open-source cache provider`);
    assert.match(source, /validate-wrappers:\s*true/, `${name} must fail closed on wrapper tampering`);
  }
});

test('Gradle caching has a single owner in every workflow', async () => {
  for (const [name, source] of await workflows()) {
    assert.doesNotMatch(source, /^\s+cache:\s*['"]?gradle['"]?\s*$/m, `${name} uses setup-java caching`);
    if (source.includes('./gradlew')) {
      assert.doesNotMatch(source, /uses:\s*actions\/cache@/, `${name} uses a competing Gradle cache`);
    }
  }
});

test('dependency graph generation is immutable, fail closed, and wrapper validated', async () => {
  const source = await readFile(new URL('../workflows/dependency-submission.yml', import.meta.url), 'utf8');
  const actionReferences = source.match(/gradle\/actions\/dependency-submission@[0-9a-f]{40} # v\d+\.\d+\.\d+/g);

  assert.equal(actionReferences?.length, 2);
  assert.equal((source.match(/dependency-graph-continue-on-failure:\s*false/g) ?? []).length, 2);
  assert.equal((source.match(/validate-wrappers:\s*true/g) ?? []).length, 2);
  assert.match(source, /dependency-graph:\s*generate-and-submit/);
  assert.match(source, /dependency-graph:\s*generate-and-upload/);
});

test('the privileged PR graph bridge never checks out or executes pull request code', async () => {
  const source = await readFile(
    new URL('../workflows/dependency-submission-upload.yml', import.meta.url),
    'utf8',
  );

  assert.match(source, /workflow_run:/);
  assert.match(source, /github\.event\.workflow_run\.event == 'pull_request'/);
  assert.match(source, /github\.event\.workflow_run\.conclusion == 'success'/);
  assert.match(source, /actions:\s*read/);
  assert.match(source, /contents:\s*write/);
  assert.match(source, /gradle\/actions\/dependency-submission@[0-9a-f]{40} # v\d+\.\d+\.\d+/);
  assert.match(source, /cache-disabled:\s*true/);
  assert.match(source, /dependency-graph:\s*download-and-submit/);
  assert.doesNotMatch(source, /actions\/checkout@/);
  assert.doesNotMatch(source, /^\s+-?\s*run:/m);
});

test('dependency review blocks high severity changes without enforcing a license allowlist', async () => {
  const source = await readFile(new URL('../workflows/dependency-review.yml', import.meta.url), 'utf8');

  assert.match(source, /actions\/dependency-review-action@[0-9a-f]{40} # v\d+\.\d+\.\d+/);
  assert.match(source, /fail-on-severity:\s*high/);
  assert.match(source, /license-check:\s*true/);
  assert.doesNotMatch(source, /(allow|deny)-licenses:/);
  assert.doesNotMatch(source, /pull_request_target:/);
});

test('Dependabot updates Actions and the screenshot image but not Gradle versions', async () => {
  const source = await readFile(new URL('../dependabot.yml', import.meta.url), 'utf8');

  assert.match(source, /package-ecosystem:\s*github-actions/);
  assert.match(source, /package-ecosystem:\s*docker/);
  assert.doesNotMatch(source, /package-ecosystem:\s*gradle/);
  assert.doesNotMatch(source, /auto-merge|automerge/);
});

test('the Gradle wrapper JAR and distribution stay on the reviewed official checksums', async () => {
  const properties = await readFile(
    new URL('../../gradle/wrapper/gradle-wrapper.properties', import.meta.url),
    'utf8',
  );
  const wrapperJar = await readFile(
    new URL('../../gradle/wrapper/gradle-wrapper.jar', import.meta.url),
  );

  assert.match(
    properties,
    /^distributionUrl=https\\:\/\/services\.gradle\.org\/distributions\/gradle-9\.6\.1-bin\.zip$/m,
  );
  assert.match(
    properties,
    /^distributionSha256Sum=9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14$/m,
  );
  assert.match(properties, /^validateDistributionUrl=true$/m);
  assert.equal(
    createHash('sha256').update(wrapperJar).digest('hex'),
    '76805e32c009c0cf0dd5d206bddc9fb22ea42e84db904b764f3047de095493f3',
  );
});
