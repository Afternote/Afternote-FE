// Viewed 기반 자동 머지 대상 판정 (#2248)
//
// 지정한 한 사람(VIEWED_BY)이 PR 의 모든 파일을 Viewed 로 체크하면 «Merge when ready» 를 예약할 대상으로
// 고른다. 리뷰 상태는 보지 않는다(#2277, 리뷰 폐지). 실제 머지는 GitHub 가 required check·승인 수·merge queue 를 통과시킨 뒤에만
// 한다.
//
// viewerViewedState 는 토큰 주인 본인의 체크만 돌려준다. 그래서 VIEWED_BY 본인의 토큰(VIEWED_TOKEN)으로
// 읽고, 토큰 주인이 VIEWED_BY 와 다르면 판정하지 않는다. 쓰기(예약)는 워크플로의 GITHUB_TOKEN 이 한다.
// 체크한 뒤 바뀐 파일은 GitHub 가 DISMISSED 로 되돌리므로 새 커밋은 다시 봐야 대상이 된다.
//
// 예약을 건 뒤 새 push 로 Viewed 가 풀려도 GitHub 는 예약·큐 등록을 그대로 두고, develop 룰셋은 push 로 승인을
// 지우지 않는다(dismiss_stale_reviews_on_push false). 그래서 예약됐거나 큐에 있는 PR 중 Viewed 가 다 차지 않은
// 것은 해제 대상으로 고른다(#2252). 누가 걸었는지는 따지지 않는다. 다시 전부 Viewed 가 되면 다음 실행이 다시 건다.
//
// 이 규칙은 VIEWED_BY 개인의 운영 규칙이라 VIEWED_BY 본인이 작성한 PR 에만 적용한다(#2270). 팀원·봇 PR 은
// 예약도 해제도 하지 않는다. 걸지 않으면 VIEWED_BY 가 보지 않은 팀원·dependabot PR 이 큐에서 계속 빠진다.
import { appendFileSync } from "node:fs";
import { pathToFileURL } from "node:url";

const PULL_REQUEST_FIELDS = `
    id number isDraft headRefOid isInMergeQueue changedFiles
    author { login }
    autoMergeRequest { enabledAt }
    files(first: 100) { pageInfo { hasNextPage endCursor } nodes { path viewerViewedState } }
`;

/**
 * 예약·해제를 판정할 PR 인지 가른다. VIEWED_BY 본인이 작성한 PR 만 대상이다(대소문자 무시).
 * 작성자를 알 수 없는 PR(탈퇴 계정 등)은 대상이 아니다.
 *
 * @param {object} pullRequest GraphQL PullRequest 노드
 * @param {string} viewedBy Viewed 를 판정하는 사람의 로그인
 * @returns {boolean}
 */
export function isAuthoredByViewer(pullRequest, viewedBy) {
    const login = pullRequest.author?.login;
    return typeof login === "string" && login.toLowerCase() === viewedBy.toLowerCase();
}

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
    // 파일 목록을 다 못 읽었으면 «다 봤다» 고 단정하지 않는다.
    if (files.length === 0 || files.length !== pullRequest.changedFiles) {
        return { enable: false, reason: `파일 ${files.length}/${pullRequest.changedFiles} 만 읽었다` };
    }
    const unviewed = files.filter((file) => file.viewerViewedState !== "VIEWED");
    if (unviewed.length > 0) {
        return { enable: false, reason: `Viewed 안 된 파일 ${unviewed.length}/${files.length}` };
    }
    return { enable: true, reason: `모든 파일 Viewed — 자동 머지 예약` };
}

/**
 * 자동 머지 예약 또는 merge queue 등록을 풀어야 하는지 판정한다.
 *
 * @param {object} pullRequest GraphQL PullRequest 노드
 * @param {Array<{ path: string, viewerViewedState: string }>} files PR 의 전체 변경 파일
 * @returns {{ disableAutoMerge: boolean, dequeue: boolean, reason: string }}
 */
export function decideViewedAutoMergeCancel(pullRequest, files) {
    const autoMerge = Boolean(pullRequest.autoMergeRequest);
    const queued = Boolean(pullRequest.isInMergeQueue);
    if (!autoMerge && !queued) return { disableAutoMerge: false, dequeue: false, reason: "예약·큐 등록 없음" };
    const keep = (reason) => ({ disableAutoMerge: false, dequeue: false, reason });
    const cancel = (reason) => ({ disableAutoMerge: autoMerge, dequeue: queued, reason });
    // 파일 목록을 다 못 읽었으면 «다 봤다» 고 단정하지 않는다. 예약 쪽과 같은 기준이다.
    if (files.length === 0 || files.length !== pullRequest.changedFiles) {
        return cancel(`파일 ${files.length}/${pullRequest.changedFiles} 만 읽었다 — 예약·큐 해제`);
    }
    const unviewed = files.filter((file) => file.viewerViewedState !== "VIEWED");
    if (unviewed.length > 0) {
        return cancel(`Viewed 안 된 파일 ${unviewed.length}/${files.length} — 예약·큐 해제`);
    }
    return keep("이미 자동 머지 예약 또는 merge queue 에 있고 모든 파일 Viewed");
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

/**
 * 열린 PR 마다 예약·해제를 판정한다. VIEWED_BY 본인 PR 이 아니면 파일도 읽지 않고 건너뛴다.
 *
 * @returns {Promise<{ targets: Array<object>, cancels: Array<object> }>}
 */
export async function collectViewedAutoMergeActions(request, owner, name, viewedBy, pullRequests) {
    const targets = [];
    const cancels = [];
    for (const pullRequest of pullRequests) {
        if (!isAuthoredByViewer(pullRequest, viewedBy)) {
            console.log(`#${pullRequest.number}: 작성자 ${pullRequest.author?.login ?? "없음"} — ${viewedBy} 의 PR 이 아니라 건드리지 않는다`);
            continue;
        }
        const files = await readAllFiles(request, owner, name, pullRequest);
        const cancel = decideViewedAutoMergeCancel(pullRequest, files);
        if (cancel.disableAutoMerge || cancel.dequeue) {
            console.log(`#${pullRequest.number}: ${cancel.reason}`);
            cancels.push({
                number: pullRequest.number,
                id: pullRequest.id,
                disableAutoMerge: cancel.disableAutoMerge,
                dequeue: cancel.dequeue,
            });
            continue;
        }
        const decision = decideViewedAutoMerge(pullRequest, files);
        console.log(`#${pullRequest.number}: ${decision.reason}`);
        if (decision.enable) targets.push({ number: pullRequest.number, headSha: pullRequest.headRefOid });
    }
    return { targets, cancels };
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

    const { targets, cancels } = await collectViewedAutoMergeActions(
        request, owner, name, viewedBy, data.repository.pullRequests.nodes,
    );
    if (process.env.GITHUB_OUTPUT) {
        appendFileSync(
            process.env.GITHUB_OUTPUT,
            `targets=${JSON.stringify(targets)}\ncancels=${JSON.stringify(cancels)}\n`,
        );
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
