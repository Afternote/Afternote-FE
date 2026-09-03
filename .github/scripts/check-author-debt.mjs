#!/usr/bin/env node

// 새 PR 작성자가 변경요청을 받은 자기 PR 을 방치하고 있는지 live 상태로 검사한다.
// 라벨은 스테일할 수 있으므로 입장 판정에 사용하지 않는다. 열린 PR 을 한 번 조회한 뒤
// label-awaiting-author-prs.mjs 의 SSOT 판정을 재사용하고, 워크플로가 댓글과 close 를 한 번만
// 처리할 수 있도록 결과만 TSV 로 넘긴다.

import { writeFile } from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

import {
    createApi,
    fetchOpenPullRequestsByAuthor,
    findAuthorDebts,
} from "./label-awaiting-author-prs.mjs";

function requiredEnv(env, name) {
    const value = env[name];
    if (typeof value !== "string" || value.trim() === "") {
        throw new Error(`${name} 가 필요합니다.`);
    }
    return value;
}

function safeTsvField(value) {
    return String(value ?? "").replace(/[\u0000-\u001f\u007f]+/g, " ").trim();
}

export function renderAuthorDebtTsv(debts) {
    if (!Array.isArray(debts)) {
        throw new TypeError("debts 배열이 필요합니다.");
    }

    const lines = debts.map((debt) =>
        [debt.number, debt.createdDate, debt.reviewer, debt.title]
            .map(safeTsvField)
            .join("\t"),
    );
    return lines.length > 0 ? `${lines.join("\n")}\n` : "";
}

export async function runAuthorDebtCheck({ env = process.env, api, logger = console } = {}) {
    const token = requiredEnv(env, "GITHUB_TOKEN");
    const repository = requiredEnv(env, "GITHUB_REPOSITORY");
    const author = requiredEnv(env, "AUTHOR");
    const currentPullRequestNumber = requiredEnv(env, "CURRENT_PULL_REQUEST_NUMBER");
    const outputFile = requiredEnv(env, "AUTHOR_DEBT_FILE");

    const pullRequests = await fetchOpenPullRequestsByAuthor(
        api ?? createApi(token),
        repository,
        author,
    );
    const debts = findAuthorDebts({
        pullRequests,
        repository,
        author,
        currentPullRequestNumber,
    });

    // 빚이 없어도 파일을 새로 써서 이전 실행의 결과가 남지 않게 한다.
    await writeFile(outputFile, renderAuthorDebtTsv(debts), "utf8");
    logger.log(`작성자 미조치 PR ${debts.length}건`);
    return debts;
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    runAuthorDebtCheck().catch((error) => {
        console.error(error.message);
        process.exitCode = 1;
    });
}
