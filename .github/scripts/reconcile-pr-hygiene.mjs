#!/usr/bin/env node

import path from 'node:path';
import process from 'node:process';
import { appendFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

export const STALE_LABEL = 'stale';
export const REVIEW_DEBT_LABEL = 'review-debt';

const LABELS = {
  [STALE_LABEL]: {
    color: 'D4C5F9',
    description: '14일 이상 사람의 활동이 없는 PR — 활동이 생기면 자동 해제',
  },
  [REVIEW_DEBT_LABEL]: {
    color: 'FBCA04',
    description: '첫 판정 또는 명시적으로 재요청된 변경요청 검토를 기다리는 PR',
  },
};

const OPEN_PULL_REQUESTS_QUERY = `
query($owner: String!, $name: String!, $cursor: String) {
  repository(owner: $owner, name: $name) {
    pullRequests(states: OPEN, first: 30, after: $cursor, orderBy: {field: CREATED_AT, direction: ASC}) {
      pageInfo { hasNextPage endCursor }
      nodes {
        number
        title
        url
        isDraft
        updatedAt
        author { login }
        labels(first: 50) { nodes { name } }
        reviewRequests(first: 20) {
          nodes { requestedReviewer { ... on User { login } } }
        }
        reviews(last: 100, states: [APPROVED, CHANGES_REQUESTED]) {
          nodes { author { login } state submittedAt }
        }
        commits(last: 100) {
          nodes { commit { committedDate parents(first: 2) { totalCount } } }
        }
      }
    }
  }
}`;

function isBot(login) {
  return !login || login.endsWith('[bot]') || login === 'dependabot' || login === 'github-actions';
}

export function reviewDebtReason(pullRequest) {
  if (pullRequest.isDraft || isBot(pullRequest.author)) {
    return null;
  }
  const reviews = [...(pullRequest.reviews ?? [])]
    .filter((review) => review.submittedAt && review.author)
    .sort((left, right) => left.submittedAt.localeCompare(right.submittedAt));
  if (reviews.length === 0) {
    return '아무도 승인/변경요청 판정을 남기지 않음';
  }

  const latest = reviews.at(-1);
  if (latest.state !== 'CHANGES_REQUESTED') {
    return null;
  }
  const reviewer = latest.author.toLowerCase();
  const requested = new Set((pullRequest.requestedReviewers ?? []).map((login) => login.toLowerCase()));
  if (!requested.has(reviewer)) {
    return null;
  }

  const fixes = (pullRequest.commits ?? []).filter(
    (commit) => commit.parentCount < 2 && commit.committedAt > latest.submittedAt,
  );
  if (fixes.length === 0) {
    return null;
  }
  return `@${latest.author}에게 재리뷰 요청 후 반영 커밋 ${fixes.length}개`;
}

export function planHygiene({ pullRequests, now = new Date(), staleDays = 14 }) {
  if (!Number.isSafeInteger(staleDays) || staleDays < 1) {
    throw new Error('staleDays must be a positive integer');
  }
  const cutoff = now.getTime() - staleDays * 24 * 60 * 60 * 1000;
  const operations = [];
  const stale = [];
  const reviewDebt = [];

  for (const pullRequest of pullRequests) {
    const labels = new Set(pullRequest.labels ?? []);
    const staleByAge = !isBot(pullRequest.author) && Date.parse(pullRequest.updatedAt) <= cutoff;
    if (staleByAge) {
      stale.push(pullRequest);
    }
    if (staleByAge && !labels.has(STALE_LABEL)) {
      operations.push({ action: 'add', label: STALE_LABEL, number: pullRequest.number });
    } else if (!staleByAge && labels.has(STALE_LABEL)) {
      operations.push({ action: 'remove', label: STALE_LABEL, number: pullRequest.number });
    }

    const reason = reviewDebtReason(pullRequest);
    if (reason) {
      reviewDebt.push({ ...pullRequest, reviewDebtReason: reason });
      if (!labels.has(REVIEW_DEBT_LABEL)) {
        operations.push({ action: 'add', label: REVIEW_DEBT_LABEL, number: pullRequest.number });
      }
    } else if (labels.has(REVIEW_DEBT_LABEL)) {
      operations.push({ action: 'remove', label: REVIEW_DEBT_LABEL, number: pullRequest.number });
    }
  }

  return { operations, stale, reviewDebt, staleDays };
}

export function renderSummary(plan, { dryRun = false } = {}) {
  const changes = (action, label) =>
    plan.operations
      .filter((operation) => operation.action === action && operation.label === label)
      .map((operation) => `#${operation.number}`);
  const format = (numbers) => (numbers.length > 0 ? ` — ${numbers.join(', ')}` : '');
  const lines = [
    `## PR hygiene${dryRun ? ' — dry run' : ''}`,
    '',
    `- stale 기준: 마지막 활동 ${plan.staleDays}일`,
    `- stale 라벨 추가: ${changes('add', STALE_LABEL).length}건${format(changes('add', STALE_LABEL))}`,
    `- stale 라벨 제거: ${changes('remove', STALE_LABEL).length}건${format(changes('remove', STALE_LABEL))}`,
    `- review-debt 라벨 추가: ${changes('add', REVIEW_DEBT_LABEL).length}건${format(changes('add', REVIEW_DEBT_LABEL))}`,
    `- review-debt 라벨 제거: ${changes('remove', REVIEW_DEBT_LABEL).length}건${format(changes('remove', REVIEW_DEBT_LABEL))}`,
    '',
    `### 현재 stale PR (${plan.stale.length}건)`,
    ...(plan.stale.length > 0
      ? plan.stale.map((pullRequest) => `- [#${pullRequest.number} ${pullRequest.title}](${pullRequest.url})`)
      : ['- 없음']),
    '',
    `### 현재 리뷰 응답 대기 PR (${plan.reviewDebt.length}건)`,
    ...(plan.reviewDebt.length > 0
      ? plan.reviewDebt.map(
          (pullRequest) =>
            `- [#${pullRequest.number} ${pullRequest.title}](${pullRequest.url}) — ${pullRequest.reviewDebtReason}`,
        )
      : ['- 없음']),
  ];
  return lines.join('\n');
}

function createApi(token) {
  return async function api(apiPath, { method = 'GET', body, allowNotFound = false } = {}) {
    const response = await fetch(`https://api.github.com${apiPath}`, {
      method,
      headers: {
        accept: 'application/vnd.github+json',
        authorization: `Bearer ${token}`,
        'content-type': 'application/json',
        'x-github-api-version': '2022-11-28',
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (allowNotFound && response.status === 404) {
      return null;
    }
    if (!response.ok) {
      throw new Error(`GitHub API ${method} ${apiPath} failed: ${response.status} ${await response.text()}`);
    }
    if (response.status === 204) {
      return null;
    }
    return response.json();
  };
}

async function graphql(api, query, variables) {
  const payload = await api('/graphql', { method: 'POST', body: { query, variables } });
  if (payload.errors?.length) {
    throw new Error(`GraphQL failed: ${JSON.stringify(payload.errors)}`);
  }
  return payload.data;
}

function normalizePullRequest(node) {
  return {
    number: node.number,
    title: node.title,
    url: node.url,
    isDraft: node.isDraft,
    updatedAt: node.updatedAt,
    author: node.author?.login ?? '',
    labels: (node.labels?.nodes ?? []).map((label) => label.name),
    requestedReviewers: (node.reviewRequests?.nodes ?? [])
      .map((request) => request.requestedReviewer?.login)
      .filter(Boolean),
    reviews: (node.reviews?.nodes ?? []).map((review) => ({
      author: review.author?.login ?? '',
      state: review.state,
      submittedAt: review.submittedAt,
    })),
    commits: (node.commits?.nodes ?? []).map(({ commit }) => ({
      committedAt: commit.committedDate,
      parentCount: commit.parents.totalCount,
    })),
  };
}

async function fetchOpenPullRequests(api, repository) {
  const [owner, name] = repository.split('/');
  const pullRequests = [];
  let cursor = null;
  for (;;) {
    const data = await graphql(api, OPEN_PULL_REQUESTS_QUERY, { owner, name, cursor });
    const page = data.repository.pullRequests;
    pullRequests.push(...page.nodes.map(normalizePullRequest));
    if (!page.pageInfo.hasNextPage) {
      return pullRequests;
    }
    cursor = page.pageInfo.endCursor;
  }
}

async function ensureLabel(api, repository, label) {
  if (await api(`/repos/${repository}/labels/${encodeURIComponent(label)}`, { allowNotFound: true })) {
    return;
  }
  await api(`/repos/${repository}/labels`, {
    method: 'POST',
    body: { name: label, ...LABELS[label] },
  });
}

export async function applyPlan(api, repository, plan, { dryRun = false, logger = console } = {}) {
  if (dryRun) {
    for (const operation of plan.operations) {
      logger.log(`[dry-run] #${operation.number} ${operation.label} ${operation.action}`);
    }
    return [];
  }

  const labelsToCreate = new Set(
    plan.operations.filter((operation) => operation.action === 'add').map((operation) => operation.label),
  );
  for (const label of labelsToCreate) {
    await ensureLabel(api, repository, label);
  }

  const failures = [];
  for (const operation of plan.operations) {
    try {
      if (operation.action === 'add') {
        await api(`/repos/${repository}/issues/${operation.number}/labels`, {
          method: 'POST',
          body: { labels: [operation.label] },
        });
      } else {
        await api(
          `/repos/${repository}/issues/${operation.number}/labels/${encodeURIComponent(operation.label)}`,
          { method: 'DELETE', allowNotFound: true },
        );
      }
      logger.log(`#${operation.number} ${operation.label} ${operation.action}`);
    } catch (error) {
      failures.push(`#${operation.number} ${operation.label} ${operation.action} failed: ${error.message}`);
    }
  }
  return failures;
}

async function main() {
  const token = process.env.GITHUB_TOKEN;
  const repository = process.env.GITHUB_REPOSITORY;
  const staleDays = Number(process.env.STALE_DAYS ?? '14');
  const dryRun = process.env.DRY_RUN === 'true';
  if (!token || !repository) {
    throw new Error('GITHUB_TOKEN and GITHUB_REPOSITORY are required');
  }

  const api = createApi(token);
  const pullRequests = await fetchOpenPullRequests(api, repository);
  const plan = planHygiene({ pullRequests, staleDays });
  const failures = await applyPlan(api, repository, plan, { dryRun });
  const summary = renderSummary(plan, { dryRun });
  console.log(summary);
  if (process.env.GITHUB_STEP_SUMMARY) {
    await appendFile(process.env.GITHUB_STEP_SUMMARY, `${summary}\n`);
  }
  if (failures.length > 0) {
    throw new Error(failures.join('\n'));
  }
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : '';
if (import.meta.url === invokedPath) {
  main().catch((error) => {
    console.error(error.message);
    process.exitCode = 1;
  });
}
