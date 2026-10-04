// 승인 직후 자동 머지 예약 판정
//
// 리뷰어가 승인하면 «Merge when ready» 를 대신 눌러 둔다. 실제 머지는 GitHub 가 required check·
// 승인 수·merge queue 를 모두 통과시킨 뒤에만 하므로, 이 판정은 «예약을 걸어도 되는가» 만 본다.
//
// 리뷰어의 파일 «Viewed» 체크는 조건에 넣지 못한다. GraphQL 의 viewerViewedState 는 토큰 주인
// 본인의 상태만 돌려주므로 GITHUB_TOKEN(github-actions)으로는 리뷰어의 체크를 볼 수 없고,
// 체크 변경을 알리는 webhook 이벤트도 없다. 승인을 «다 봤다» 는 신호로 쓴다.
import { appendFileSync, readFileSync } from "node:fs";
import { pathToFileURL } from "node:url";

/**
 * @param {object} pullRequest GraphQL repository.pullRequest 노드
 * @param {string} defaultBranch 자동 머지를 허용할 base(통합 브랜치)
 * @returns {{ enable: boolean, reason: string, headSha?: string }}
 */
export function decideAutoMerge(pullRequest, defaultBranch) {
    if (!pullRequest) return { enable: false, reason: "PR 을 찾지 못했다" };
    if (pullRequest.state !== "OPEN") return { enable: false, reason: `PR 상태가 ${pullRequest.state}` };
    if (pullRequest.isDraft) return { enable: false, reason: "Draft PR" };
    // 릴리스(main)·스택 상위 PR 은 사람이 머지 시점을 정한다.
    if (pullRequest.baseRefName !== defaultBranch) {
        return { enable: false, reason: `base 가 ${pullRequest.baseRefName} (자동 머지는 ${defaultBranch} 만)` };
    }
    if (pullRequest.autoMergeRequest || pullRequest.isInMergeQueue) {
        return { enable: false, reason: "이미 자동 머지 예약 또는 merge queue 에 있다" };
    }
    // 방금 들어온 리뷰가 승인일 때만 건다. 사람이 예약을 끈 뒤 달린 일반 코멘트 리뷰가 예약을
    // 되살리지 않게 한다.
    const lastReview = pullRequest.reviews?.nodes?.at(-1);
    if (lastReview?.state !== "APPROVED") {
        return { enable: false, reason: `마지막 리뷰가 승인이 아니다 (${lastReview?.state ?? "없음"})` };
    }
    // 최신 판정 리컨사일 뒤의 PR 전체 판정. 변경요청이 남아 있으면 걸지 않는다.
    if (pullRequest.reviewDecision !== "APPROVED") {
        return { enable: false, reason: `reviewDecision 이 ${pullRequest.reviewDecision ?? "없음"}` };
    }
    // 승인 뒤 새 커밋이 올라왔으면 그 커밋은 승인받지 않았다.
    if (lastReview.commit?.oid !== pullRequest.headRefOid) {
        return { enable: false, reason: "승인 뒤 새 커밋이 있다" };
    }
    return { enable: true, reason: "승인됨 — 자동 머지 예약", headSha: pullRequest.headRefOid };
}

function main() {
    const [responsePath] = process.argv.slice(2);
    const defaultBranch = process.env.DEFAULT_BRANCH;
    if (!responsePath || !defaultBranch) {
        throw new Error("사용법: DEFAULT_BRANCH=<branch> node decide-auto-merge-on-approval.mjs <graphql-response.json>");
    }
    const response = JSON.parse(readFileSync(responsePath, "utf8"));
    const decision = decideAutoMerge(response?.data?.repository?.pullRequest, defaultBranch);
    console.log(`auto-merge: ${decision.reason}`);
    if (process.env.GITHUB_OUTPUT) {
        appendFileSync(process.env.GITHUB_OUTPUT, `enable=${decision.enable}\nhead_sha=${decision.headSha ?? ""}\n`);
    }
}

const isDirectExecution = process.argv[1]
    && import.meta.url === pathToFileURL(process.argv[1]).href;

if (isDirectExecution) {
    try {
        main();
    } catch (error) {
        console.error(`::error::${error.message}`);
        process.exitCode = 1;
    }
}
