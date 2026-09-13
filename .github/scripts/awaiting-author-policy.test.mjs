import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

import {
    judgeAwaitingAuthor,
    parseReviewGateExemptAuthors,
    planAwaitingAuthorLabels,
} from "./label-awaiting-author-prs.mjs";

const guard = await readFile(new URL("../workflows/review-debt-guard.yml", import.meta.url), "utf8");
const reconcile = await readFile(new URL("../workflows/conflict-label.yml", import.meta.url), "utf8");
const script = await readFile(new URL("./label-awaiting-author-prs.mjs", import.meta.url), "utf8");
const authorDebtCheck = await readFile(new URL("./check-author-debt.mjs", import.meta.url), "utf8");
const exemptAuthors = parseReviewGateExemptAuthors(guard);

test("가드는 작성자 무조치를 공통 판정 CLI 로 검사한다", () => {
    // 라벨은 주기 리컨사일 결과라 입장 시점에 스테일할 수 있다. 가드는 live PR 목록을 읽는
    // CLI 를 호출하고, CLI 는 라벨 리컨사일러와 같은 judgeAwaitingAuthor 를 재사용해야 한다.
    assert.match(guard, /node \.github\/scripts\/check-author-debt\.mjs/);
    assert.match(guard, /AUTHOR_DEBT_FILE="\$author_debt_file"/);
    assert.match(authorDebtCheck, /findAuthorDebts/);
    assert.match(script, /export function findAuthorDebts/);
});

test("라벨 판정은 입장 가드에 선언된 작성자 면제 목록을 직접 읽는다", () => {
    assert.match(guard, /^\s*REVIEW_GATE_EXEMPT_AUTHORS:/m);
    assert.match(guard, /\$REVIEW_GATE_EXEMPT_AUTHORS/);
    assert.match(script, /new URL\("\.\.\/workflows\/review-debt-guard\.yml", import\.meta\.url\)/);
    assert.doesNotMatch(script, /review-gate-policy\.json/);
    const mainAt = script.indexOf("async function main()");
    assert.ok(mainAt > 0);
    const main = script.slice(mainAt);
    assert.match(main, /parseReviewGateExemptAuthors\(/);
    assert.match(main, /planAwaitingAuthorLabels\(\{[^}]*\bexemptAuthors\b/);
});

test("workflow 파일이 없는 가드 checkout에서도 작성자 빚 판정을 import할 수 있다", async () => {
    // data URL 에는 상대 workflow 경로가 없다. 모듈을 불러오는 순간 목록을 읽으면
    // scripts 만 checkout 하는 오래된 가드와 마찬가지로 import가 실패한다.
    const module = await import(`data:text/javascript;base64,${Buffer.from(script).toString("base64")}`);

    assert.equal(typeof module.findAuthorDebts, "function");
    assert.equal(typeof module.judgeAwaitingAuthor, "function");
});

test("가드의 면제 목록에 있는 작성자는 라벨 계획에서도 빠진다", () => {
    const pullRequests = exemptAuthors.map((login, index) => ({
        number: index + 1,
        isDraft: false,
        author: { login: login.toUpperCase() },
        labels: { nodes: [{ name: "awaiting-author" }] },
        reviews: {
            nodes: [{
                state: "CHANGES_REQUESTED",
                submittedAt: "2026-08-29T00:00:00Z",
                authorCanPushToRepository: true,
                author: { login: "reviewer" },
            }],
        },
    }));
    const plan = planAwaitingAuthorLabels({ pullRequests, exemptAuthors });

    assert.deepEqual(plan.toLabel, []);
    assert.deepEqual(plan.toUnlabel.map((entry) => entry.number), pullRequests.map(({ number }) => number));
    assert.ok(plan.toUnlabel.every(({ reason }) => reason === "리뷰 게이트 면제 작성자"));
});

test("리뷰어 빚과 작성자 빚은 한 번의 안내와 종료로 합친다", () => {
    // 두 경로가 각각 댓글·close 를 수행하면 같은 opened 이벤트에서 중복 코멘트와 경합이
    // 생긴다. 두 TSV 를 모두 만든 뒤 한 번만 최종 결정을 내려야 한다.
    assert.match(guard, /\[ ! -s "\$debt_file" \] && \[ ! -s "\$author_debt_file" \]/);
    assert.equal((guard.match(/gh pr close/g) ?? []).length, 1);
    assert.equal((guard.match(/> \/tmp\/debt-comment\.md/g) ?? []).length, 1);
});

test("두 판정은 «PR 전체의 최신 결정 리뷰» 라는 같은 축을 쓴다", () => {
    // 한쪽만 리뷰어별로 묶으면, 승인으로 끝난 PR 에 더 오래된 변경요청 때문에 라벨이
    // 붙거나 그 반대가 된다.
    assert.match(guard, /sort_by\(\.t\) \| last/);
    assert.doesNotMatch(guard, /group_by\(\.u/);
    assert.match(script, /export function latestDecision/);
    assert.doesNotMatch(script, /groupBy/);
});

test("두 판정 모두 병합 커밋을 작성자 반영으로 세지 않는다", () => {
    // 병합 커밋의 diff 는 base 에서 들어온 변경을 통째로 담아 반영분과 구별되지 않는다.
    assert.match(guard, /\(\.parents \| length\) < 2/);
    assert.match(script, /parents\?\.totalCount \?\? 0\) >= 2/);
});

test("두 판정 모두 빈 커밋을 반영으로 세지 않는다", () => {
    // 리뷰어가 미는 +0/-0 CI 재트리거 커밋이 «작성자가 반영했다» 가 되면, 방치된 PR 의
    // 라벨이 조용히 떨어진다 (#1459).
    assert.match(guard, /\(\.files \/\/ \[\]\) \| length/);
    assert.match(guard, /\[ "\$changed" -gt 0 \] \|\| continue/);
    assert.match(script, /changedFilesIfAvailable/);
    assert.match(script, /changed === 0/);
});

test("두 판정 모두 커밋 작성자를 확인하고 이메일 폴백을 쓴다", () => {
    // 누가 올렸는지를 보지 않으면 리뷰어 커밋이 작성자 반영으로 둔갑한다. 계정이 연결되지
    // 않은 커밋은 login 이 비어 같은 PR 의 작성자 이메일로만 가릴 수 있다.
    assert.match(guard, /\(\.author\.login \/\/ ""\)/);
    assert.match(guard, /email in own/);
    assert.match(script, /ownEmails/);
});

test("두 판정 모두 커밋과 함께 작성자의 응답을 본다", () => {
    // 커밋만 세면 base 를 merge 로 끌어와 반영한 PR 이 «무조치» 가 된다 (#1316 → #1450).
    assert.match(guard, /issues\/\$pn\/comments/);
    assert.match(guard, /\[ "\$\{fixed:-0\}" -gt 0 \] \|\| \[ "\$responses" -gt 0 \]/);
    assert.match(script, /export function countAuthorResponses/);
    assert.match(script, /fixes > 0 \|\| responses > 0/);
});

test("두 판정 모두 변경요청 뒤 작성자의 PR 본문 편집을 조치로 본다", () => {
    // CI Test Plan 처럼 PR 본문 자체가 리뷰 대상인 지적은 새 커밋 없이 해결된다. 일반
    // updatedAt 이 아니라 편집자와 편집 시각이 남는 userContentEdits 를 사용해야 한다.
    assert.match(guard, /userContentEdits/);
    assert.match(script, /userContentEdits\(last: 50\)/);
    assert.match(script, /export function countAuthorBodyEdits/);
    assert.match(script, /bodyEdits > 0/);
});

test("두 판정 모두 draft·봇·fork 를 대상에서 뺀다", () => {
    assert.match(guard, /\[ "\$HEAD_REPO" != "\$REPO" \]/);
    assert.match(guard, /select\(\.draft == false\)/);
    assert.match(guard, /봇 PR — 건너뜀/);

    for (const [name, pullRequest] of [
        ["draft", { isDraft: true }],
        ["bot", { author: { login: "dependabot[bot]" } }],
        ["fork", { headRepository: { nameWithOwner: "someone/fork" } }],
    ]) {
        const verdict = judgeAwaitingAuthor(
            {
                isDraft: false,
                author: { login: "author" },
                headRepository: { nameWithOwner: "Afternote/Afternote-FE" },
                reviews: {
                    nodes: [
                        {
                            state: "CHANGES_REQUESTED",
                            submittedAt: "2026-08-29T00:00:00Z",
                            authorCanPushToRepository: true,
                            author: { login: "r" },
                        },
                    ],
                },
                commits: { nodes: [] },
                comments: { nodes: [] },
                ...pullRequest,
            },
            { repository: "Afternote/Afternote-FE" },
        );
        assert.equal(verdict.awaiting, false, `${name} 은 대상이 아니어야 한다`);
    }
});

test("라벨은 리컨사일러로만 관리된다 — 붙이는 경로와 떼는 경로가 갈라지지 않는다", () => {
    // 이벤트로 붙이고 다른 이벤트로 떼면 떼는 쪽이 새고 스테일 라벨이 남는다. 한 번의
    // 차이 계산으로 양쪽을 처리하는 구조를 강제한다.
    assert.match(script, /export function planAwaitingAuthorLabels/);
    assert.match(script, /toUnlabel/);

    // 전용 워크플로를 새로 세우지 않는다. 트리거·concurrency·quota 처리가 이미 허브에 있다.
    assert.match(reconcile, /label-awaiting-author-prs\.test\.mjs/);
    assert.match(reconcile, /node \.github\/scripts\/label-awaiting-author-prs\.mjs/);
    assert.match(reconcile, /awaiting-author-policy\.test\.mjs/);
});

test("리컨사일러 실패가 다른 리컨사일러를 가리지 않는다", () => {
    // 허브는 실패를 모아 마지막에 exit 한다. `&&` 로 이으면 앞이 죽을 때 뒤가 아예 돌지 않는다.
    assert.match(reconcile, /node \.github\/scripts\/label-awaiting-author-prs\.mjs \|\| failures=1/);
});
