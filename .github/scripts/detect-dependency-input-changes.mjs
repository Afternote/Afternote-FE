import { appendFile, readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

const DEPENDENCY_WORKFLOWS = new Set([
  '.github/workflows/dependency-review.yml',
  '.github/workflows/dependency-submission-upload.yml',
  '.github/workflows/dependency-submission.yml',
  '.github/scripts/detect-dependency-input-changes.mjs',
]);

function normalizePath(path) {
  return String(path).replaceAll('\\', '/').replace(/^\.\/+/, '');
}

export function isDependencyInputPath(path) {
  const normalized = normalizePath(path);
  const segments = normalized.split('/');
  const basename = segments.at(-1) ?? '';

  return (
    DEPENDENCY_WORKFLOWS.has(normalized) ||
    normalized.startsWith('gradle/') ||
    segments.includes('build-logic') ||
    segments.includes('buildSrc') ||
    basename === 'gradle.properties' ||
    basename === 'gradlew' ||
    basename === 'gradlew.bat' ||
    basename.endsWith('.gradle') ||
    basename.endsWith('.gradle.kts') ||
    basename.endsWith('.lockfile')
  );
}

export function dependencyInputPaths(files) {
  return files
    .flatMap(({ filename, previous_filename: previousFilename }) => [filename, previousFilename])
    .filter((path) => typeof path === 'string')
    .map(normalizePath)
    .filter(isDependencyInputPath);
}

function nextPage(linkHeader) {
  if (!linkHeader) return undefined;

  for (const entry of linkHeader.split(',')) {
    const match = /<([^>]+)>;\s*rel="([^"]+)"/.exec(entry.trim());
    if (match?.[2] === 'next') return match[1];
  }
  return undefined;
}

export async function listPullRequestFiles({
  apiUrl,
  repository,
  pullRequestNumber,
  token,
  fetchImpl = fetch,
}) {
  let url = `${apiUrl}/repos/${repository}/pulls/${pullRequestNumber}/files?per_page=100`;
  const files = [];

  while (url) {
    const response = await fetchImpl(url, {
      headers: {
        Accept: 'application/vnd.github+json',
        Authorization: `Bearer ${token}`,
        'X-GitHub-Api-Version': '2022-11-28',
      },
    });

    if (!response.ok) {
      const detail = (await response.text()).slice(0, 500);
      throw new Error(`GitHub pull files API returned ${response.status}: ${detail}`);
    }

    const page = await response.json();
    if (!Array.isArray(page)) {
      throw new TypeError('GitHub pull files API returned a non-array response');
    }
    files.push(...page);
    url = nextPage(response.headers.get('link'));
  }

  return files;
}

async function expectedChangedFileCount(eventPath) {
  if (!eventPath) return undefined;

  const event = JSON.parse(await readFile(eventPath, 'utf8'));
  const count = event.pull_request?.changed_files;
  return Number.isInteger(count) ? count : undefined;
}

function escapeWorkflowCommand(value) {
  return String(value).replaceAll('%', '%25').replaceAll('\r', '%0D').replaceAll('\n', '%0A');
}

async function main() {
  const {
    GITHUB_API_URL: apiUrl = 'https://api.github.com',
    GITHUB_EVENT_PATH: eventPath,
    GITHUB_OUTPUT: outputPath,
    GITHUB_REPOSITORY: repository,
    GITHUB_TOKEN: token,
    PR_NUMBER: pullRequestNumber,
  } = process.env;

  if (!outputPath) throw new Error('GITHUB_OUTPUT is required');

  let changed = true;
  try {
    if (!repository?.includes('/') || !token || !/^\d+$/.test(pullRequestNumber ?? '')) {
      throw new Error('GITHUB_REPOSITORY, GITHUB_TOKEN, and numeric PR_NUMBER are required');
    }

    const files = await listPullRequestFiles({
      apiUrl,
      repository,
      pullRequestNumber,
      token,
    });
    const expected = await expectedChangedFileCount(eventPath);
    if (expected !== undefined && files.length < expected) {
      throw new Error(`GitHub returned ${files.length} of ${expected} changed files`);
    }

    const inputs = dependencyInputPaths(files);
    changed = inputs.length > 0;
    if (changed) {
      console.log(`Dependency graph inputs changed: ${[...new Set(inputs)].join(', ')}`);
    } else {
      console.log(`No dependency graph inputs changed across ${files.length} pull request files.`);
    }
  } catch (error) {
    console.log(
      `::warning title=Dependency input detection failed::${escapeWorkflowCommand(error.message)}. Generating the dependency graph as a fail-closed fallback.`,
    );
  }

  await appendFile(outputPath, `changed=${changed}\n`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  await main();
}
