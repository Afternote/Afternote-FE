#!/usr/bin/env node

// 조직(orgs/<org>) 하위 전 저장소의 워크플로·composite action 이 쓰는 `uses:` 를
// 전수로 모아 floating(태그·브랜치) 참조와 40자리 SHA 고정 참조로 가른다.
//
// `orgs/<org>/actions/permissions` 의 `sha_pinning_required` 를 켜면 조직 하위
// 저장소 전부에 즉시 적용된다. floating 참조가 하나라도 남아 있으면 그 저장소의
// run 은 job 이 만들어지기 전에 `startup_failure` 로 죽고 로그에 사유가 남지 않는다.
// 그래서 켜기 전에 이 감사를 돌려 0건인지 확인한다.
//
// 사용법:
//   GH_TOKEN=$(gh auth token) node .github/scripts/audit-org-action-pinning.mjs \
//     --org Afternote --output /tmp/audit.json
//
// 기본 브랜치만이 아니라 **열린 PR 의 head 리비전까지** 훑는다. 정책은 머지 전
// 브랜치의 run 에도 걸리므로, 열린 PR 에 남은 floating 참조는 정책을 켜는 순간
// 그 PR 의 CI 를 죽인다.

import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

const DEFAULT_API_URL = "https://api.github.com";
const MAX_PAGES = 10;
const PAGE_SIZE = 100;

const WORKFLOW_PATH = /^\.github\/workflows\/[^/]+\.ya?ml$/;
const ACTION_MANIFEST_PATH = /(?:^|\/)action\.ya?ml$/;

/**
 * 워크플로·action 매니페스트 원문에서 `uses:` 값을 순서대로 뽑는다.
 *
 * YAML 파서를 쓰지 않는 이유는 `uses:` 가 step 시퀀스·job 레벨(reusable workflow)
 * 양쪽에 나타나고, 주석 처리된 줄은 정책 대상이 아니기 때문이다. 줄 단위로 보는
 * 편이 정책이 보는 범위와 정확히 겹친다.
 */
export function extractActionReferences(source) {
    const references = [];
    for (const line of String(source ?? "").split("\n")) {
        if (/^\s*#/.test(line)) {
            continue;
        }
        const match = /^\s*(?:-\s+)?uses:\s*(.+?)\s*$/.exec(line);
        if (!match) {
            continue;
        }
        const value = stripInlineComment(match[1]);
        if (value.length > 0) {
            references.push(value);
        }
    }
    return references;
}

function stripInlineComment(value) {
    let text = value.trim();
    const quoted = /^(['"])(.*?)\1/.exec(text);
    if (quoted) {
        return quoted[2].trim();
    }
    const comment = /\s+#/.exec(text);
    if (comment) {
        text = text.slice(0, comment.index);
    }
    return text.trim();
}

/**
 * 참조 하나를 정책 관점으로 분류한다.
 *
 * - `local`: `./` 로 시작하는 같은 저장소 참조. 대상 리비전이 저장소 자신이라
 *   SHA 고정 정책의 대상이 아니다.
 * - `docker`: `docker://` 참조. 액션 레지스트리가 아니라 이미지라 대상이 아니다.
 * - `expression`: `${{ }}` 가 섞인 참조. 정적으로 판정할 수 없으니 사람이 본다.
 * - `pinned`: `@` 뒤가 40자리 소문자 hex.
 * - `floating`: 그 밖(태그 `@v4`·브랜치 `@main`·짧은 SHA). 정책을 켜면 막힌다.
 */
export function classifyActionReference(reference) {
    const value = String(reference ?? "").trim();
    if (value.length === 0) {
        return { reference: value, kind: "empty", action: null, version: null };
    }
    if (value.startsWith("./")) {
        return { reference: value, kind: "local", action: value, version: null };
    }
    if (value.startsWith("docker://")) {
        return { reference: value, kind: "docker", action: value, version: null };
    }
    if (value.includes("${{")) {
        return { reference: value, kind: "expression", action: null, version: null };
    }
    const separator = value.lastIndexOf("@");
    if (separator <= 0) {
        return { reference: value, kind: "unversioned", action: value, version: null };
    }
    const action = value.slice(0, separator);
    const version = value.slice(separator + 1);
    const kind = /^[0-9a-f]{40}$/.test(version) ? "pinned" : "floating";
    return { reference: value, kind, action, version };
}

/**
 * `allowed_actions: selected` 목록을 만들 때 필요한, 정책이 실제로 보는 액션 이름
 * 집합이다. GitHub 의 패턴 매칭에서 `*` 는 `/` 를 넘지 않으므로 서브패스 액션은
 * (`gradle/actions/setup-gradle` 처럼) 한 줄씩 필요하다 — 그래서 owner/repo 가
 * 아니라 서브패스까지 포함한 전체 경로로 모은다.
 */
export function collectExternalActionNames(entries) {
    const names = new Set();
    for (const entry of entries) {
        if (entry.kind === "pinned" || entry.kind === "floating" || entry.kind === "unversioned") {
            names.add(entry.action);
        }
    }
    return [...names].sort();
}

/** 감사 결과를 저장소별 집계로 접는다. */
export function summarizeAudit(entries) {
    const byRepository = new Map();
    for (const entry of entries) {
        if (!byRepository.has(entry.repository)) {
            byRepository.set(entry.repository, {
                repository: entry.repository,
                total: 0,
                pinned: 0,
                floating: 0,
                local: 0,
                other: 0,
                floatingReferences: [],
            });
        }
        const bucket = byRepository.get(entry.repository);
        bucket.total += 1;
        if (entry.kind === "pinned") {
            bucket.pinned += 1;
        } else if (entry.kind === "floating") {
            bucket.floating += 1;
            bucket.floatingReferences.push(entry);
        } else if (entry.kind === "local") {
            bucket.local += 1;
        } else {
            bucket.other += 1;
        }
    }
    const repositories = [...byRepository.values()].sort((left, right) =>
        left.repository.localeCompare(right.repository),
    );
    return {
        repositories,
        totals: {
            references: entries.length,
            pinned: repositories.reduce((sum, item) => sum + item.pinned, 0),
            floating: repositories.reduce((sum, item) => sum + item.floating, 0),
            local: repositories.reduce((sum, item) => sum + item.local, 0),
            other: repositories.reduce((sum, item) => sum + item.other, 0),
        },
        externalActions: collectExternalActionNames(entries),
        compatible: entries.every((entry) => entry.kind !== "floating"),
    };
}

export function parseArguments(argv) {
    const options = { org: null, output: null, apiUrl: DEFAULT_API_URL };
    for (let index = 0; index < argv.length; index += 1) {
        const flag = argv[index];
        if (flag === "--org") {
            options.org = argv[(index += 1)];
        } else if (flag === "--output") {
            options.output = argv[(index += 1)];
        } else if (flag === "--api-url") {
            options.apiUrl = argv[(index += 1)];
        } else {
            throw new Error(`unknown argument: ${flag}`);
        }
    }
    if (!options.org) {
        throw new Error("--org is required");
    }
    return options;
}

async function requestJson(url, token) {
    const response = await fetch(url, {
        headers: {
            Accept: "application/vnd.github+json",
            Authorization: `Bearer ${token}`,
            "X-GitHub-Api-Version": "2022-11-28",
        },
    });
    if (!response.ok) {
        const body = (await response.text()).slice(0, 2_000);
        throw new Error(`GET ${url} failed (${response.status}): ${body}`);
    }
    return response.json();
}

async function requestText(url, token) {
    const response = await fetch(url, {
        headers: {
            Accept: "application/vnd.github.raw+json",
            Authorization: `Bearer ${token}`,
            "X-GitHub-Api-Version": "2022-11-28",
        },
    });
    if (!response.ok) {
        const body = (await response.text()).slice(0, 2_000);
        throw new Error(`GET ${url} failed (${response.status}): ${body}`);
    }
    return response.text();
}

async function listPaged(url, token) {
    const items = [];
    for (let page = 1; page <= MAX_PAGES; page += 1) {
        const separator = url.includes("?") ? "&" : "?";
        const pageItems = await requestJson(
            `${url}${separator}per_page=${PAGE_SIZE}&page=${page}`,
            token,
        );
        items.push(...pageItems);
        if (pageItems.length < PAGE_SIZE) {
            return items;
        }
    }
    throw new Error(`more than ${MAX_PAGES * PAGE_SIZE} items require pagination: ${url}`);
}

/**
 * 리비전 하나의 트리에서 워크플로·action 매니페스트 blob 만 고른다.
 *
 * 경로가 아니라 `{path, sha}` 로 돌려주는 이유는 열린 PR 수십 건이 대부분 같은
 * 워크플로 파일을 그대로 물고 있기 때문이다. blob SHA 로 캐시하면 저장소 하나의
 * 파일 내려받기가 리비전 수만큼 곱해지지 않는다.
 */
export function selectPolicyRelevantBlobs(treeEntries) {
    return (treeEntries ?? [])
        .filter((entry) => entry.type === "blob")
        .filter((entry) => WORKFLOW_PATH.test(entry.path) || ACTION_MANIFEST_PATH.test(entry.path))
        .map((entry) => ({ path: entry.path, sha: entry.sha }))
        .sort((left, right) => left.path.localeCompare(right.path));
}

async function scanRevision(apiUrl, repository, revision, label, token, blobCache) {
    const tree = await requestJson(
        `${apiUrl}/repos/${repository}/git/trees/${revision}?recursive=1`,
        token,
    );
    if (tree.truncated) {
        throw new Error(`tree for ${repository}@${revision} was truncated; audit would be partial`);
    }
    const blobs = selectPolicyRelevantBlobs(tree.tree);
    const entries = [];
    for (const blob of blobs) {
        const cacheKey = `${repository}@${blob.sha}`;
        if (!blobCache.has(cacheKey)) {
            blobCache.set(
                cacheKey,
                await requestText(`${apiUrl}/repos/${repository}/git/blobs/${blob.sha}`, token),
            );
        }
        for (const reference of extractActionReferences(blobCache.get(cacheKey))) {
            entries.push({
                repository,
                revision,
                revisionLabel: label,
                path: blob.path,
                ...classifyActionReference(reference),
            });
        }
    }
    return { paths: blobs.map((blob) => blob.path), entries };
}

async function main() {
    const options = parseArguments(process.argv.slice(2));
    const token = process.env.GH_TOKEN ?? process.env.GITHUB_TOKEN;
    if (!token) {
        throw new Error("GH_TOKEN or GITHUB_TOKEN is required");
    }

    const repositories = await listPaged(
        `${options.apiUrl}/orgs/${options.org}/repos?type=all`,
        token,
    );
    const entries = [];
    const scannedRevisions = [];
    const blobCache = new Map();

    for (const repository of repositories) {
        const fullName = repository.full_name;
        const branch = await requestJson(
            `${options.apiUrl}/repos/${fullName}/branches/${encodeURIComponent(repository.default_branch)}`,
            token,
        );
        const revisions = [
            {
                revision: branch.commit.sha,
                label: `${repository.default_branch} (default branch)`,
            },
        ];

        const pullRequests = await listPaged(
            `${options.apiUrl}/repos/${fullName}/pulls?state=open`,
            token,
        );
        for (const pullRequest of pullRequests) {
            revisions.push({
                revision: pullRequest.head.sha,
                label: `PR #${pullRequest.number} (${pullRequest.head.ref})`,
            });
        }

        for (const { revision, label } of revisions) {
            const scanned = await scanRevision(
                options.apiUrl,
                fullName,
                revision,
                label,
                token,
                blobCache,
            );
            scannedRevisions.push({
                repository: fullName,
                revision,
                label,
                files: scanned.paths.length,
                references: scanned.entries.length,
            });
            entries.push(...scanned.entries);
        }
    }

    const report = {
        organization: options.org,
        auditedAt: new Date().toISOString(),
        repositories: repositories.map((repository) => ({
            fullName: repository.full_name,
            defaultBranch: repository.default_branch,
            visibility: repository.visibility,
            archived: repository.archived,
        })),
        scannedRevisions,
        summary: summarizeAudit(entries),
        entries,
    };

    const serialized = `${JSON.stringify(report, null, 2)}\n`;
    if (options.output) {
        await fs.mkdir(path.dirname(path.resolve(options.output)), { recursive: true });
        await fs.writeFile(options.output, serialized, "utf8");
    } else {
        process.stdout.write(serialized);
    }

    const { totals, compatible } = report.summary;
    console.error(
        `scanned ${scannedRevisions.length} revisions across ${repositories.length} repositories: ` +
            `${totals.references} uses (${totals.pinned} pinned, ${totals.floating} floating, ` +
            `${totals.local} local, ${totals.other} other)`,
    );
    if (!compatible) {
        console.error("floating references remain; sha_pinning_required would break these runs");
        process.exitCode = 1;
    }
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    main().catch((error) => {
        console.error(error instanceof Error ? error.stack : error);
        process.exitCode = 1;
    });
}
