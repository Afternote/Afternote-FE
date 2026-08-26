import assert from 'node:assert/strict';
import test from 'node:test';

import {
  dependencyInputPaths,
  isDependencyInputPath,
  listPullRequestFiles,
} from './detect-dependency-input-changes.mjs';

test('recognizes every Gradle dependency graph input family', () => {
  const inputs = [
    'build.gradle',
    'app/build.gradle.kts',
    'included/settings.gradle.kts',
    'gradle/libs.versions.toml',
    'gradle/wrapper/gradle-wrapper.properties',
    'build-logic/src/main/kotlin/android-library.gradle.kts',
    'buildSrc/src/main/kotlin/Dependencies.kt',
    'gradle.properties',
    'gradlew',
    'gradlew.bat',
    'app/gradle.lockfile',
    '.github/workflows/dependency-review.yml',
    '.github/workflows/dependency-submission.yml',
    '.github/workflows/dependency-submission-upload.yml',
    '.github/scripts/detect-dependency-input-changes.mjs',
  ];

  for (const path of inputs) {
    assert.equal(isDependencyInputPath(path), true, path);
  }
});

test('does not treat ordinary source, documentation, or unrelated CI changes as dependency inputs', () => {
  const ordinaryFiles = [
    'app/src/main/kotlin/com/afternote/MainActivity.kt',
    'README.md',
    'docs/gradle-migration.md',
    '.github/workflows/android-ci.yml',
    'build/reports/tests/index.html',
  ];

  for (const path of ordinaryFiles) {
    assert.equal(isDependencyInputPath(path), false, path);
  }
});

test('checks both sides of a rename so moving a dependency input away still regenerates', () => {
  assert.deepEqual(
    dependencyInputPaths([
      {
        filename: 'docs/old-build-script.txt',
        previous_filename: 'feature/build.gradle.kts',
      },
    ]),
    ['feature/build.gradle.kts'],
  );
});

test('follows GitHub pagination when listing pull request files', async () => {
  const requests = [];
  const responses = [
    new Response(JSON.stringify([{ filename: 'README.md' }]), {
      headers: {
        link: '<https://api.github.test/repos/Afternote/Afternote-FE/pulls/1/files?per_page=100&page=2>; rel="next"',
      },
    }),
    new Response(JSON.stringify([{ filename: 'app/build.gradle.kts' }])),
  ];

  const files = await listPullRequestFiles({
    apiUrl: 'https://api.github.test',
    repository: 'Afternote/Afternote-FE',
    pullRequestNumber: '1',
    token: 'test-token',
    fetchImpl: async (url) => {
      requests.push(url);
      return responses.shift();
    },
  });

  assert.equal(requests.length, 2);
  assert.deepEqual(files.map(({ filename }) => filename), ['README.md', 'app/build.gradle.kts']);
});
