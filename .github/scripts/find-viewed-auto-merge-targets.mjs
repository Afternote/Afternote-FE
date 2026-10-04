// Viewed 기반 자동 머지 대상 판정 (#2248)
//
// 지정한 한 사람(VIEWED_BY)이 PR 의 모든 파일을 Viewed 로 체크했고 PR 이 승인 상태면 «Merge when ready»
// 를 예약할 대상으로 고른다. 실제 머지는 GitHub 가 required check·승인 수·merge queue 를 통과시킨 뒤에만
// 한다.
//
// viewerViewedState 는 토큰 주인 본인의 체크만 돌려준다. 그래서 VIEWED_BY 본인의 토큰(VIEWED_TOKEN)으로
// 읽고, 토큰 주인이 VIEWED_BY 와 다르면 판정하지 않는다. 쓰기(예약)는 워크플로의 GITHUB_TOKEN 이 한다.
// 체크한 뒤 바뀐 파일은 GitHub 가 DISMISSED 로 되돌리므로 새 커밋은 다시 봐야 대상이 된다.
import { appendFileSync } from "node:fs";
import { pathToFileURL } from "node:url";

const PULL_REQUEST_FIELDS = `
    number isDraft headRefOid reviewDecision isInMergeQueue changedFiles
    autoMergeRequest { enabledAt }
    files(first: 100) { pageInfo { hasNextPage endCursor } nodes { path viewerViewedState } }
`;

/**
 * @param {object} pullRequest GraphQL PullRequest 노드
 * @param {Array<{ path: string, viewerViewedState: string }>} files PR 의 전체 변경 파일
 * @returns {{ enable: boolean, reason: string }}
 */
export function decideViewedAutoMerge(pullRequest, files) {
    if (pullRequest.isDraft) return { enable: false, reason: "Draft PR" };
    if (pullRequest.autoMergeRequest || pullRequest.isInMergeQueue) {
        return { enable: false, reason: "이미 자동 머지 예약 또는 merge queue 에 있다" };
    }
    if (pullRequest.reviewDecision !== "APPROVED") {
        return { enable: false, reason: `reviewDecision 이 ${pullRequest.reviewDecision ?? "없음"}` };
    }
    // 파일 목록을 다 못 읽었으면 «다 봤다» 고 단정하지 않는다.
    if (files.length === 0 || files.length !== pullRequest.changedFiles) {
        return { enable: false, reason: `파일 ${files.length}/${pullRequest.changedFiles} 만 읽었다` };
    }
    const unviewed = files.filter((file) => file.viewerViewedState !== "VIEWED");
    if (unviewed.length > 0) {
        return { enable: false, reason: `Viewed 안 된 파일 ${unviewed.length}/${files.length}` };
    }
    return { enable: true, reason: `모든 파일 Viewed·승인 — 자동 머지 예약` };
}

async function graphql(token, apiUrl, query, variables) {
    const response = await fetch(`${apiUrl}/graphql`, {
        method: "POST",
        headers: { authorization: `bearer ${token}`, "content-type": "application/json" },
        body: JSON.stringify({ query, variables }),
    });
    const payload = await response.json();
    if (!response.ok || payload.errors) {
        throw new Error(`GraphQL 실패 (${response.status}): ${JSON.stringify(payload.errors ?? payload)}`);
    }
    return payload.data;
}

async function readAllFiles(request, owner, name, pullRequest) {
    const files = [...pullRequest.files.nodes];
    let { hasNextPage, endCursor } = pullRequest.files.pageInfo;
    while (hasNextPage) {
        const data = await request(
            `query($owner: String!, $name: String!, $number: Int!, $after: String) {
                repository(owner: $owner, name: $name) {
                    pullRequest(number: $number) {
                        files(first: 100, after: $after) { pageInfo { hasNextPage endCursor } nodes { path viewerViewedState } }
                    }
                }
            }`,
            { owner, name, number: pullRequest.number, after: endCursor },
        );
        const page = data.repository.pullRequest.files;
        files.push(...page.nodes);
        ({ hasNextPage, endCursor } = page.pageInfo);
    }
    return files;
}

async function main() {
    const token = process.env.VIEWED_TOKEN;
    const viewedBy = process.env.VIEWED_BY;
    const baseBranch = process.env.BASE_BRANCH;
    const [owner, name] = (process.env.GITHUB_REPOSITORY ?? "").split("/");
    if (!token || !viewedBy || !baseBranch || !owner || !name) {
        throw new Error("VIEWED_TOKEN·VIEWED_BY·BASE_BRANCH·GITHUB_REPOSITORY 가 필요합니다.");
    }
    const request = (query, variables) =>
        graphql(token, process.env.GITHUB_API_URL ?? "https://api.github.com", query, variables);

    const data = await request(
        `query($owner: String!, $name: String!, $base: String!) {
            viewer { login }
            repository(owner: $owner, name: $name) {
                pullRequests(states: OPEN, baseRefName: $base, first: 50) { nodes { ${PULL_REQUEST_FIELDS} } }
            }
        }`,
        { owner, name, base: baseBranch },
    );
    if (data.viewer.login.toLowerCase() !== viewedBy.toLowerCase()) {
        throw new Error(`VIEWED_TOKEN 주인이 ${data.viewer.login} 입니다 — ${viewedBy} 의 토큰이어야 합니다.`);
    }

    const targets = [];
    for (const pullRequest of data.repository.pullRequests.nodes) {
        const files = await readAllFiles(request, owner, name, pullRequest);
        const decision = decideViewedAutoMerge(pullRequest, files);
        console.log(`#${pullRequest.number}: ${decision.reason}`);
        if (decision.enable) targets.push({ number: pullRequest.number, headSha: pullRequest.headRefOid });
    }
    if (process.env.GITHUB_OUTPUT) {
        appendFileSync(process.env.GITHUB_OUTPUT, `targets=${JSON.stringify(targets)}\n`);
    }
}

const isDirectExecution = process.argv[1]
    && import.meta.url === pathToFileURL(process.argv[1]).href;

if (isDirectExecution) {
    try {
        await main();
    } catch (error) {
        console.error(`::error::${error.message}`);
        process.exitCode = 1;
    }
}
