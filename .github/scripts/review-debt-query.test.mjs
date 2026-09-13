import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { spawnSync } from "node:child_process";
import test from "node:test";

const guard = await readFile(new URL("../workflows/review-debt-guard.yml", import.meta.url), "utf8");
const cutoff = "2026-09-10T00:00:00Z";
const owners = "koongmai,1hyok";

function runQuery(query, data, args = []) {
    return spawnSync("jq", ["-r", ...args, query], { input: JSON.stringify(data), encoding: "utf8" });
}

for (const endpoint of ["issues", "pulls"]) {
    test(`${endpoint} comments retain author context and count only later owner responses`, () => {
        const start = guard.indexOf(`repos/$REPO/${endpoint}/$pn/comments?per_page=100`);
        assert.ok(start >= 0);
        const raw = guard.slice(start).match(/--jq "([\s\S]*?)" \\\n/)[1];
        const query = raw.replaceAll("$pr_owners", owners).replaceAll("$blocked_at", cutoff)
            .replaceAll('\\"', '"').replaceAll('\\$', '$');
        const comments = [
            { id: 1, user: { login: "KoongMai" }, created_at: "2026-09-11T00:00:00Z" },
            { id: 2, user: { login: "1hyok" }, created_at: "2026-09-12T00:00:00Z" },
            { id: 3, user: { login: "koongmai" }, created_at: cutoff },
            { id: 4, user: { login: "koong" }, created_at: "2026-09-11T00:00:00Z" },
            { id: 5, user: { login: "other" }, created_at: "2026-09-11T00:00:00Z" },
        ];
        // gh --paginate executes its query separately on each page.
        const pages = [comments.slice(0, 1), comments.slice(1), []];
        const output = pages.map(page => {
            const result = runQuery(query, page);
            assert.equal(result.status, 0, result.stderr);
            return result.stdout;
        }).join("");
        assert.equal(output, "1\n2\n");
    });
}

const bodyQuery = guard.match(/--arg cutoff "\$blocked_at" '([\s\S]*?)' <<< "\$edits_json"/)[1];
function edits(nodes, hasPreviousPage = false) {
    return { data: { repository: { pullRequest: { userContentEdits: { nodes, pageInfo: { hasPreviousPage } } } } } };
}
const args = ["--arg", "owners", `,${owners},`, "--arg", "cutoff", cutoff];

test("body edit evidence keeps editor context, cutoff, and exact owner membership", () => {
    const result = runQuery(bodyQuery, edits([
        { editor: { login: "KOONGMAI" }, editedAt: "2026-09-11T00:00:00Z" },
        { editor: { login: "1hyok" }, editedAt: "2026-09-12T00:00:00Z" },
        { editor: { login: "koongmai" }, editedAt: cutoff },
        { editor: { login: "koong" }, editedAt: "2026-09-11T00:00:00Z" },
        { editor: null, editedAt: "2026-09-11T00:00:00Z" },
    ]), args);
    assert.equal(result.status, 0, result.stderr);
    assert.equal(result.stdout, "2\n");
    assert.equal(runQuery(bodyQuery, edits([]), args).stdout, "0\n");
});

test("truncated body edit history still fails closed", () => {
    const result = runQuery(bodyQuery, edits([], true), args);
    assert.notEqual(result.status, 0);
    assert.match(result.stderr, /최근 50건보다 많은 본문 편집 이력/);
});
