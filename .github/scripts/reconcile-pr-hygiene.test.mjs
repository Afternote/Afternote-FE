import assert from 'node:assert/strict';
import test from 'node:test';

import {
  REVIEW_DEBT_LABEL,
  STALE_LABEL,
  applyPlan,
  planHygiene,
  renderSummary,
  reviewDebtReason,
} from './reconcile-pr-hygiene.mjs';

const now = new Date('2026-08-27T00:00:00Z');
const day = 24 * 60 * 60 * 1000;

function pullRequest(overrides = {}) {
  return {
    number: 10,
    title: 'example',
    url: 'https://example.test/pull/10',
    isDraft: false,
    updatedAt: new Date(now.getTime() - day).toISOString(),
    author: 'author',
    labels: [],
    requestedReviewers: [],
    reviews: [{ author: 'reviewer', state: 'APPROVED', submittedAt: '2026-08-20T00:00:00Z' }],
    commits: [],
    ...overrides,
  };
}

test('unreviewed pull requests are review debt but drafts and bots are not', () => {
  assert.match(reviewDebtReason(pullRequest({ reviews: [] })), /아무도/);
  assert.equal(reviewDebtReason(pullRequest({ reviews: [], isDraft: true })), null);
  assert.equal(reviewDebtReason(pullRequest({ reviews: [], author: 'dependabot[bot]' })), null);
});

test('changes requested becomes debt only after explicit rerequest and a non-merge fix', () => {
  const blocked = {
    reviews: [{ author: 'Reviewer', state: 'CHANGES_REQUESTED', submittedAt: '2026-08-20T00:00:00Z' }],
    requestedReviewers: ['reviewer'],
    commits: [
      { committedAt: '2026-08-21T00:00:00Z', parentCount: 1 },
      { committedAt: '2026-08-22T00:00:00Z', parentCount: 2 },
    ],
  };

  assert.match(reviewDebtReason(pullRequest(blocked)), /반영 커밋 1개/);
  assert.equal(reviewDebtReason(pullRequest({ ...blocked, requestedReviewers: [] })), null);
  assert.equal(reviewDebtReason(pullRequest({ ...blocked, commits: [] })), null);
});

test('the latest decisive review controls the debt state', () => {
  const reviews = [
    { author: 'first', state: 'CHANGES_REQUESTED', submittedAt: '2026-08-20T00:00:00Z' },
    { author: 'second', state: 'APPROVED', submittedAt: '2026-08-21T00:00:00Z' },
  ];
  assert.equal(reviewDebtReason(pullRequest({ reviews, requestedReviewers: ['first'] })), null);
});

test('stale is added after 14 days and reconciled away after recent activity', () => {
  const plan = planHygiene({
    pullRequests: [
      pullRequest({ number: 11, updatedAt: new Date(now.getTime() - 15 * day).toISOString() }),
      pullRequest({ number: 12, labels: [STALE_LABEL] }),
    ],
    now,
    staleDays: 14,
  });

  assert.deepEqual(plan.operations, [
    { action: 'add', label: STALE_LABEL, number: 11 },
    { action: 'remove', label: STALE_LABEL, number: 12 },
  ]);
  assert.deepEqual(plan.stale.map(({ number }) => number), [11]);
});

test('review debt labels are reconciled in both directions', () => {
  const plan = planHygiene({
    pullRequests: [
      pullRequest({ number: 13, reviews: [] }),
      pullRequest({ number: 14, labels: [REVIEW_DEBT_LABEL] }),
    ],
    now,
  });

  assert.deepEqual(plan.operations, [
    { action: 'add', label: REVIEW_DEBT_LABEL, number: 13 },
    { action: 'remove', label: REVIEW_DEBT_LABEL, number: 14 },
  ]);
});

function fakeApi() {
  const calls = [];
  const api = async (apiPath, options = {}) => {
    calls.push({ apiPath, method: options.method ?? 'GET', body: options.body });
    return options.allowNotFound ? null : {};
  };
  api.calls = calls;
  return api;
}

test('dry run produces no API writes', async () => {
  const api = fakeApi();
  await applyPlan(
    api,
    'o/r',
    { operations: [{ action: 'add', label: STALE_LABEL, number: 15 }] },
    { dryRun: true, logger: { log() {} } },
  );
  assert.deepEqual(api.calls, []);
});

test('apply creates labels then performs add and remove operations', async () => {
  const api = fakeApi();
  const plan = {
    operations: [
      { action: 'add', label: REVIEW_DEBT_LABEL, number: 16 },
      { action: 'remove', label: REVIEW_DEBT_LABEL, number: 17 },
    ],
  };

  assert.deepEqual(await applyPlan(api, 'o/r', plan, { logger: { log() {} } }), []);
  assert.ok(api.calls.some((call) => call.apiPath === '/repos/o/r/labels'));
  assert.ok(api.calls.some((call) => call.apiPath === '/repos/o/r/issues/16/labels'));
  assert.ok(api.calls.some((call) => call.apiPath.endsWith('/issues/17/labels/review-debt')));
});

test('summary contains the current findings and planned changes', () => {
  const plan = planHygiene({
    pullRequests: [pullRequest({ number: 18, reviews: [] })],
    now,
  });
  const summary = renderSummary(plan, { dryRun: true });

  assert.match(summary, /dry run/);
  assert.match(summary, /review-debt 라벨 추가: 1건 — #18/);
  assert.match(summary, /현재 리뷰 응답 대기 PR \(1건\)/);
});
