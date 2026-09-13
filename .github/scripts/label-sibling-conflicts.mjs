#!/usr/bin/env node

// 열린 PR 끼리 같은 파일을 서로 다르게 고친 «형제 충돌» 에 라벨을 붙인다 (#1750).
//
// `conflict` 라벨(#1028)은 PR 과 base 의 충돌만 본다. 열린 PR 둘이 같은 파일을 서로 다르게
// 고쳐도 한쪽이 base 에 들어가기 전까지는 어디서도 신호가 없고, 들어간 뒤에야 나머지 쪽이
// CONFLICTING 이 된다. merge-order-guard 는 이슈 blocked_by 만 보고, 스택은 의존 관계용이라
// 독립 이슈 둘을 묶을 자리가 아니다. 머지 자체는 GitHub 이 막으므로(나중 쪽이 merge queue 에
// 못 들어간다) 비어 있는 것은 «머지 전에 미리 아는 것» 뿐이다.
//
// 이 스크립트는 그 알림이다 — 차단이 아니다. 둘 다 옳은 PR 이라 어느 쪽을 red 로 만들지 정할
// 근거가 없고, 작성자가 손쓸 수 없는 red 는 #1059 가 걷어낸 상태다.
//
// 판정. 두 head 를 `git merge-tree --write-tree` 로 합쳐 충돌 파일을 구하고, 그중 **두 PR 의
// 자기 커밋이 모두 고친 파일** 만 남긴다. 한쪽만 고친 파일에서 난 충돌은 상대가 품고 있는 trunk
// 커밋과의 충돌이라 `conflict` 라벨 몫이다. 스택 PR 의 «자기 커밋» 은 부모 PR head 와의 공통
// 조상부터의 diff 이고, 부모 체인이 이미 같은 상대와 부딪히는 파일은 자식에서 뺀다 — 충돌은 체인의
// 가장 아래 PR 한 곳에 귀속되고, 그 PR 이 풀면 다음 실행이 자식을 다시 판정한다. 같은 체인
// (부모·자식)은 비교하지 않는다 — 그쪽은 base 축이다.
//
// 한계. 파일 이름이 겹치지 않는 충돌(한쪽이 디렉터리를 옮기고 다른 쪽이 옛 자리에 파일을 더한
// «file location» 충돌)은 잡지 않는다. 내용 충돌만 본다.
//
// 이벤트가 아니라 리컨사일러인 이유는 다른 라벨 스크립트와 같다. 매 실행이 «지금 충돌하는 쌍» 을
// 다시 계산해 차이만 쓰므로 붙이기와 떼기가 갈라지지 않고 스테일 라벨이 남지 않는다. 코멘트는
// PR 당 하나를 마커로 찾아 현재 상태로 고쳐 쓴다 — 쌍마다 새로 달면 상대가 바뀔 때마다 쌓여
// 소음이 되고, 한 번만 달면 상대·파일이 바뀐 뒤 낡은 안내가 남는다.

import { spawnSync } from "node:child_process";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

export const DEFAULT_LABEL = "sibling-conflict";
export const COMMENT_MARKER = "<!-- sibling-conflict -->";

const LABEL_COLOR = "F9D0C4";
const LABEL_DESCRIPTION = "열린 형제 PR 과 같은 파일에서 충돌 — 나중에 머지되는 쪽이 base 를 병합해야 한다. 해소되면 자동으로 떨어진다";
const PULL_REQUEST_PAGE_SIZE = 50;
const COMMENT_PAGE_SIZE = 100;

const OPEN_PULL_REQUESTS_QUERY = `
query($owner: String!, $name: String!, $cursor: String, $pageSize: Int!) {
    repository(owner: $owner, name: $name) {
        pullRequests(states: OPEN, first: $pageSize, after: $cursor) {
            pageInfo { hasNextPage endCursor }
            nodes {
                number
                baseRefName
                headRefName
                headRefOid
                isDraft
                headRepository { nameWithOwner }
                labels(first: 30) { nodes { name } }
            }
        }
    }
}`;

/**
 * `git merge-tree --write-tree --name-only -z` 출력을 트리 OID 와 충돌 파일 목록으로 가른다.
 *
 * 형식은 `<tree>NUL <file>NUL ... NUL <informational messages>` 다. 충돌 파일 구간은 빈 항목으로
 * 끝나고 그 뒤는 사람이 읽는 메시지라 버린다. `--name-only` 라 같은 파일이 stage 마다 반복되지 않는다.
 */
export function parseMergeTreeOutput(stdout) {
    const parts = stdout.split("\0");
    const tree = (parts[0] ?? "").trim();
    const conflictedFiles = [];
    for (const part of parts.slice(1)) {
        if (part === "") {
            break;
        }
        conflictedFiles.push(part);
    }
    return { tree, conflictedFiles };
}

/**
 * 판정에 필요한 git 질의만 노출한다. 테스트는 임시 저장소에 실제 git 을 돌린다.
 */
export function createGit({ cwd = process.cwd(), spawn = spawnSync } = {}) {
    function run(args) {
        const result = spawn("git", args, { cwd, encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });
        if (result.error) {
            throw result.error;
        }
        return { status: result.status, stdout: result.stdout ?? "", stderr: result.stderr ?? "" };
    }

    return {
        commitExists(ref) {
            return run(["rev-parse", "--verify", "--quiet", `${ref}^{commit}`]).status === 0;
        },
        mergeBase(a, b) {
            const result = run(["merge-base", a, b]);
            return result.status === 0 ? result.stdout.trim() : null;
        },
        isAncestor(ancestor, descendant) {
            return run(["merge-base", "--is-ancestor", ancestor, descendant]).status === 0;
        },
        changedFiles(from, to) {
            const result = run(["diff", "--name-only", "-z", from, to]);
            if (result.status !== 0) {
                throw new Error(`git diff ${from} ${to} 실패: ${result.stderr.trim()}`);
            }
            return result.stdout.split("\0").filter(Boolean);
        },
        /** 종료 코드 0 은 깨끗, 1 은 충돌, 그 밖은 오류(공통 조상 없음 등)다. */
        mergeTreeConflicts(a, b) {
            const result = run(["merge-tree", "--write-tree", "--name-only", "-z", a, b]);
            if (result.status === 0) {
                return { conflictedFiles: [], error: null };
            }
            if (result.status === 1) {
                return { conflictedFiles: parseMergeTreeOutput(result.stdout).conflictedFiles, error: null };
            }
            return { conflictedFiles: [], error: result.stderr.trim() || `exit ${result.status}` };
        },
        fetchPullRequestHead(number) {
            return run(["fetch", "--quiet", "origin", `refs/pull/${number}/head`]).status === 0;
        },
    };
}

function sortedPair(a, b) {
    return a < b ? [a, b] : [b, a];
}

/**
 * 열린 PR 들 사이의 형제 충돌 쌍을 찾는다.
 *
 * 각 PR 의 «자기 파일» 은 upstream(부모 PR head, 없으면 trunk)과의 공통 조상부터 head 까지의
 * diff 다. 쌍은 자기 파일이 겹칠 때만 merge-tree 를 돌리고, 충돌 파일 중 양쪽 자기 파일에 모두
 * 든 것만 남긴다. 같은 체인은 부모 링크와 git 조상 관계 두 축으로 걸러 낸다 — base 를 develop 로
 * 옮긴 뒤 아직 rebase 하지 않은 자식은 부모 링크가 끊겨도 커밋 조상으로는 이어져 있다.
 */
export function detectSiblingConflicts({ pullRequests, trunk, git }) {
    const byHead = new Map();
    for (const pullRequest of pullRequests) {
        if (pullRequest.sameRepository !== false) {
            byHead.set(pullRequest.headRefName, pullRequest);
        }
    }

    const parentOf = (pullRequest) => {
        if (pullRequest.sameRepository === false) {
            return null;
        }
        const parent = byHead.get(pullRequest.baseRefName);
        return parent && parent.number !== pullRequest.number ? parent : null;
    };

    const isChainAncestor = (ancestor, pullRequest) => {
        const seen = new Set();
        for (let current = parentOf(pullRequest); current && !seen.has(current.number); current = parentOf(current)) {
            if (current.number === ancestor.number) {
                return true;
            }
            seen.add(current.number);
        }
        return false;
    };

    const eligible = [];
    const skipped = [];
    for (const pullRequest of pullRequests) {
        if (!git.commitExists(pullRequest.headRefOid)) {
            skipped.push({ number: pullRequest.number, reason: `head ${pullRequest.headRefOid.slice(0, 7)} 을 찾지 못함` });
            continue;
        }
        const parent = parentOf(pullRequest);
        const upstreamLabel = parent && git.commitExists(parent.headRefOid) ? `#${parent.number}` : trunk;
        const upstream = parent && git.commitExists(parent.headRefOid) ? parent.headRefOid : trunk;
        const ownBase = git.mergeBase(upstream, pullRequest.headRefOid);
        if (!ownBase) {
            skipped.push({ number: pullRequest.number, reason: `${upstreamLabel} 와 공통 조상 없음` });
            continue;
        }
        eligible.push({ ...pullRequest, ownFiles: new Set(git.changedFiles(ownBase, pullRequest.headRefOid)) });
    }

    // merge-tree 는 쌍마다 한 번이면 된다 — 부모 체인 제외에서 같은 쌍을 다시 묻는다.
    const rawCache = new Map();
    const rawConflicts = (a, b) => {
        const key = a.headRefOid < b.headRefOid ? `${a.headRefOid}:${b.headRefOid}` : `${b.headRefOid}:${a.headRefOid}`;
        if (!rawCache.has(key)) {
            rawCache.set(key, git.mergeTreeConflicts(a.headRefOid, b.headRefOid));
        }
        return rawCache.get(key);
    };
    const availableParentOf = (pullRequest) => {
        const parent = parentOf(pullRequest);
        return parent && git.commitExists(parent.headRefOid) ? parent : null;
    };
    // 부모 체인이 같은 상대와 이미 부딪히는 파일은 자식의 것이 아니다.
    const withoutInherited = (files, self, other) => {
        const parent = availableParentOf(self);
        if (!parent || files.length === 0) {
            return files;
        }
        const inherited = new Set(rawConflicts(parent, other).conflictedFiles);
        return files.filter((file) => !inherited.has(file));
    };

    const pairs = [];
    const skippedPairs = [];
    for (let i = 0; i < eligible.length; i += 1) {
        for (let j = i + 1; j < eligible.length; j += 1) {
            const a = eligible[i];
            const b = eligible[j];
            const overlap = [...a.ownFiles].filter((file) => b.ownFiles.has(file));
            if (overlap.length === 0) {
                continue;
            }
            if (isChainAncestor(a, b) || isChainAncestor(b, a)) {
                continue;
            }
            if (git.isAncestor(a.headRefOid, b.headRefOid) || git.isAncestor(b.headRefOid, a.headRefOid)) {
                continue;
            }
            const { conflictedFiles, error } = rawConflicts(a, b);
            if (error) {
                skippedPairs.push({ numbers: sortedPair(a.number, b.number), reason: error });
                continue;
            }
            let files = conflictedFiles.filter((file) => a.ownFiles.has(file) && b.ownFiles.has(file)).sort();
            files = withoutInherited(files, a, b);
            files = withoutInherited(files, b, a);
            if (files.length > 0) {
                pairs.push({ numbers: sortedPair(a.number, b.number), files });
            }
        }
    }

    pairs.sort((left, right) => left.numbers[0] - right.numbers[0] || left.numbers[1] - right.numbers[1]);
    return { pairs, skipped, skippedPairs };
}

/**
 * 충돌 쌍을 라벨·코멘트 계획으로 바꾼다. 라벨은 «지금 어떤 쌍에든 든 PR» 집합과의 차이만 쓰고,
 * 코멘트는 그 집합의 PR 마다 현재 상대 목록 하나를 계획한다. 집합에서 빠진 PR 은 라벨을 떼면서
 * 남아 있는 코멘트를 «해소됨» 으로 고쳐 쓴다.
 */
export function planSiblingLabelChanges({ pullRequests, pairs, label = DEFAULT_LABEL }) {
    const involved = new Map();
    for (const pair of pairs) {
        const [a, b] = pair.numbers;
        for (const [self, sibling] of [[a, b], [b, a]]) {
            if (!involved.has(self)) {
                involved.set(self, []);
            }
            involved.get(self).push({ sibling, files: pair.files });
        }
    }

    const toLabel = [];
    const toUnlabel = [];
    const comments = [];
    for (const pullRequest of pullRequests) {
        const labeled = (pullRequest.labels ?? []).includes(label);
        const entries = involved.get(pullRequest.number);
        if (entries) {
            if (!labeled) {
                toLabel.push(pullRequest);
            }
            comments.push({
                number: pullRequest.number,
                siblings: [...entries].sort((left, right) => left.sibling - right.sibling),
            });
            continue;
        }
        if (labeled) {
            toUnlabel.push(pullRequest);
        }
    }

    return { toLabel, toUnlabel, comments };
}

export function renderSiblingComment({ siblings, label = DEFAULT_LABEL }) {
    return [
        COMMENT_MARKER,
        "열린 PR 중 이 PR 과 같은 파일을 서로 다르게 고친 것이 있다. **어느 쪽이 먼저 머지되든 나중 쪽은 base 를 병합해야 통과한다** (#1750).",
        "",
        ...siblings.map((entry) => `- #${entry.sibling} — ${entry.files.map((file) => `\`${file}\``).join(", ")}`),
        "",
        "머지 자체는 GitHub 이 막는다 — 한쪽이 들어가면 다른 쪽은 CONFLICTING 이 되어 merge queue 에 못 들어간다.",
        "이 라벨은 그 전에 순서를 정하라는 신호다. 먼저 갈 쪽을 정하고, 나중 쪽은 그 머지 뒤에 base 를 병합한다.",
        "상대 PR 에도 같은 안내가 달린다.",
        "",
        `이 코멘트는 리컨사일러가 매 실행 현재 상태로 고쳐 쓴다. 해소되면 \`${label}\` 라벨은 자동으로 떨어진다.`,
    ].join("\n");
}

export function renderResolvedComment() {
    return [
        COMMENT_MARKER,
        "형제 충돌이 해소됐다 — 지금은 이 PR 과 같은 파일을 서로 다르게 고친 열린 PR 이 없다 (#1750).",
        "다시 생기면 이 코멘트를 현재 상태로 고쳐 쓴다.",
    ].join("\n");
}

export function renderSummary({ pairs, plan, skipped = [], skippedPairs = [], label, dryRun }) {
    const lines = [
        `## 형제 충돌 PR 라벨 (\`${label}\`)${dryRun ? " — dry run" : ""}`,
        "",
        `- 충돌 쌍: ${pairs.length}건`,
        ...pairs.map((pair) => `  - #${pair.numbers[0]} ↔ #${pair.numbers[1]} — ${pair.files.map((file) => `\`${file}\``).join(", ")}`),
        `- 라벨 부착: ${plan.toLabel.length}건${formatNumbers(plan.toLabel)}`,
        `- 라벨 제거: ${plan.toUnlabel.length}건${formatNumbers(plan.toUnlabel)}`,
        `- 코멘트 계획: ${plan.comments.length}건`,
        `- 판정 보류: ${skipped.length}건${skipped.map((item) => ` — #${item.number} (${item.reason})`).join("")}`,
    ];
    if (skippedPairs.length > 0) {
        lines.push(`- 비교 실패: ${skippedPairs.length}건${skippedPairs.map((item) => ` — #${item.numbers[0]} ↔ #${item.numbers[1]} (${item.reason})`).join("")}`);
    }
    return lines.join("\n");
}

function formatNumbers(pullRequests) {
    if (pullRequests.length === 0) {
        return "";
    }
    return ` — ${pullRequests.map((pullRequest) => `#${pullRequest.number}`).join(", ")}`;
}

export function normalizePullRequest(node, repository) {
    return {
        number: node.number,
        baseRefName: node.baseRefName,
        headRefName: node.headRefName,
        headRefOid: node.headRefOid,
        isDraft: node.isDraft,
        sameRepository: (node.headRepository?.nameWithOwner ?? "").toLowerCase() === repository.toLowerCase(),
        labels: (node.labels?.nodes ?? []).map((item) => item.name),
    };
}

function createApi(token) {
    return async function api(apiPath, { method = "GET", body, allowNotFound = false } = {}) {
        const response = await fetch(`https://api.github.com${apiPath}`, {
            method,
            headers: {
                accept: "application/vnd.github+json",
                authorization: `Bearer ${token}`,
                "content-type": "application/json",
                "x-github-api-version": "2022-11-28",
            },
            body: body === undefined ? undefined : JSON.stringify(body),
        });

        if (allowNotFound && response.status === 404) {
            return null;
        }
        if (!response.ok) {
            const detail = await response.text();
            throw new Error(`GitHub API ${method} ${apiPath} 실패: ${response.status} ${detail}`);
        }
        if (response.status === 204) {
            return null;
        }
        return response.json();
    };
}

async function graphql(api, query, variables) {
    const payload = await api("/graphql", { method: "POST", body: { query, variables } });
    if (payload?.errors?.length) {
        throw new Error(`GraphQL 실패: ${JSON.stringify(payload.errors)}`);
    }
    return payload.data;
}

export async function fetchOpenPullRequests(api, repository) {
    const [owner, name] = repository.split("/");
    const pullRequests = [];
    let cursor = null;

    for (;;) {
        const data = await graphql(api, OPEN_PULL_REQUESTS_QUERY, {
            owner,
            name,
            cursor,
            pageSize: PULL_REQUEST_PAGE_SIZE,
        });
        const page = data.repository.pullRequests;
        pullRequests.push(...page.nodes.map((node) => normalizePullRequest(node, repository)));

        if (!page.pageInfo.hasNextPage) {
            return pullRequests;
        }
        cursor = page.pageInfo.endCursor;
    }
}

/**
 * 전체 히스토리 checkout 은 저장소 안 브랜치를 모두 가져오지만, 조회 직전에 push 된 커밋이나 포크
 * PR 의 head 는 없을 수 있다. 없는 것만 `refs/pull/N/head` 로 채운다 — 실패해도 판정 보류로 남길
 * 뿐 실행을 멈추지 않는다.
 */
export function ensureHeadsAvailable(pullRequests, git, logger = console) {
    for (const pullRequest of pullRequests) {
        if (git.commitExists(pullRequest.headRefOid)) {
            continue;
        }
        if (!git.fetchPullRequestHead(pullRequest.number)) {
            logger.log(`#${pullRequest.number} head 를 가져오지 못했다 — 판정 보류`);
        }
    }
}

export async function ensureLabelExists(api, repository, label) {
    const existing = await api(`/repos/${repository}/labels/${encodeURIComponent(label)}`, {
        allowNotFound: true,
    });
    if (existing) {
        return;
    }
    await api(`/repos/${repository}/labels`, {
        method: "POST",
        body: { name: label, color: LABEL_COLOR, description: LABEL_DESCRIPTION },
    });
}

async function findMarkerComment(api, repository, number) {
    for (let page = 1; ; page += 1) {
        const comments = await api(`/repos/${repository}/issues/${number}/comments?per_page=${COMMENT_PAGE_SIZE}&page=${page}`);
        const found = (comments ?? []).find((comment) => (comment.body ?? "").includes(COMMENT_MARKER));
        if (found) {
            return { id: found.id, body: found.body ?? "" };
        }
        if (!comments || comments.length < COMMENT_PAGE_SIZE) {
            return null;
        }
    }
}

const normalizeBody = (body) => body.replace(/\r\n/g, "\n").trimEnd();

/** 마커 코멘트가 없으면 만들고, 있으면 내용이 다를 때만 고쳐 쓴다. 같으면 아무것도 쓰지 않는다. */
async function upsertMarkerComment(api, repository, number, body) {
    const existing = await findMarkerComment(api, repository, number);
    if (!existing) {
        await api(`/repos/${repository}/issues/${number}/comments`, { method: "POST", body: { body } });
        return "created";
    }
    if (normalizeBody(existing.body) === normalizeBody(body)) {
        return "unchanged";
    }
    await api(`/repos/${repository}/issues/comments/${existing.id}`, { method: "PATCH", body: { body } });
    return "updated";
}

// `logger` 를 받는 이유: 테스트가 실제 동작처럼 보이는 줄을 CI 로그에 찍지 않게 하기 위해서다.
export async function applyPlan(api, repository, plan, { label, dryRun, logger = console }) {
    const failures = [];

    for (const pullRequest of plan.toLabel) {
        try {
            if (dryRun) {
                logger.log(`[dry-run] #${pullRequest.number} 라벨 부착`);
                continue;
            }
            await api(`/repos/${repository}/issues/${pullRequest.number}/labels`, {
                method: "POST",
                body: { labels: [label] },
            });
            logger.log(`#${pullRequest.number} 라벨 부착`);
        } catch (error) {
            failures.push(`#${pullRequest.number} 라벨 부착 실패: ${error.message}`);
        }
    }

    for (const comment of plan.comments) {
        const siblings = comment.siblings.map((entry) => `#${entry.sibling}`).join(", ");
        try {
            if (dryRun) {
                logger.log(`[dry-run] #${comment.number} 코멘트 (상대 ${siblings})`);
                continue;
            }
            const outcome = await upsertMarkerComment(
                api,
                repository,
                comment.number,
                renderSiblingComment({ siblings: comment.siblings, label }),
            );
            if (outcome !== "unchanged") {
                logger.log(`#${comment.number} 코멘트 ${outcome === "created" ? "작성" : "갱신"} (상대 ${siblings})`);
            }
        } catch (error) {
            failures.push(`#${comment.number} 코멘트 실패 (상대 ${siblings}): ${error.message}`);
        }
    }

    for (const pullRequest of plan.toUnlabel) {
        try {
            if (dryRun) {
                logger.log(`[dry-run] #${pullRequest.number} 라벨 제거`);
                continue;
            }
            await api(`/repos/${repository}/issues/${pullRequest.number}/labels/${encodeURIComponent(label)}`, {
                method: "DELETE",
                allowNotFound: true,
            });
            // 남아 있는 안내를 «해소됨» 으로 고쳐 써야 낡은 상대 목록이 PR 에 남지 않는다. 없으면 만들지 않는다.
            const existing = await findMarkerComment(api, repository, pullRequest.number);
            if (existing && normalizeBody(existing.body) !== normalizeBody(renderResolvedComment())) {
                await api(`/repos/${repository}/issues/comments/${existing.id}`, {
                    method: "PATCH",
                    body: { body: renderResolvedComment() },
                });
            }
            logger.log(`#${pullRequest.number} 라벨 제거`);
        } catch (error) {
            failures.push(`#${pullRequest.number} 라벨 제거 실패: ${error.message}`);
        }
    }

    return failures;
}

async function main() {
    const token = process.env.GITHUB_TOKEN;
    const repository = process.env.GITHUB_REPOSITORY;
    const defaultBranch = process.env.GITHUB_DEFAULT_BRANCH;
    if (!token || !repository || !defaultBranch) {
        throw new Error("GITHUB_TOKEN, GITHUB_REPOSITORY, GITHUB_DEFAULT_BRANCH 가 필요합니다.");
    }

    const label = process.env.SIBLING_CONFLICT_LABEL ?? DEFAULT_LABEL;
    const dryRun = process.env.DRY_RUN === "true";
    const trunk = process.env.TRUNK_REF ?? `origin/${defaultBranch}`;
    const api = createApi(token);
    const git = createGit();

    if (!git.commitExists(trunk)) {
        throw new Error(`trunk '${trunk}' 를 찾지 못했다 — 전체 히스토리 checkout 이 필요하다.`);
    }

    const pullRequests = await fetchOpenPullRequests(api, repository);
    ensureHeadsAvailable(pullRequests, git);
    const { pairs, skipped, skippedPairs } = detectSiblingConflicts({ pullRequests, trunk, git });
    const plan = planSiblingLabelChanges({ pullRequests, pairs, label });

    if (!dryRun && plan.toLabel.length > 0) {
        await ensureLabelExists(api, repository, label);
    }

    const failures = await applyPlan(api, repository, plan, { label, dryRun });
    const summary = renderSummary({ pairs, plan, skipped, skippedPairs, label, dryRun });
    console.log(summary);

    if (process.env.GITHUB_STEP_SUMMARY) {
        const { appendFile } = await import("node:fs/promises");
        await appendFile(process.env.GITHUB_STEP_SUMMARY, `${summary}\n`);
    }

    if (failures.length > 0) {
        // 개별 실패가 나머지 PR 처리를 막지 않게 모아 두었다가 여기서 한 번에 드러낸다.
        throw new Error(failures.join("\n"));
    }
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error.message);
        process.exitCode = 1;
    });
}
