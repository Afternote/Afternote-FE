#!/usr/bin/env node

import path from "node:path";
import process from "node:process";
import { pathToFileURL } from "node:url";

const PULL_REQUEST_STATES = new Set(["OPEN", "CLOSED", "MERGED"]);

function fail(message) {
    throw new Error(`GraphQL 스택 응답이 불완전합니다: ${message}`);
}

function requireObject(value, name) {
    if (value === null || typeof value !== "object" || Array.isArray(value)) {
        fail(`${name} 객체가 없습니다.`);
    }
    return value;
}

function requirePositiveInteger(value, name) {
    if (!Number.isSafeInteger(value) || value < 1) {
        fail(`${name}은(는) 양의 정수여야 합니다.`);
    }
    return value;
}

function validatePullRequestFields(pullRequest, name) {
    const value = requireObject(pullRequest, name);
    const number = requirePositiveInteger(value.number, `${name}.number`);

    if (!PULL_REQUEST_STATES.has(value.state)) {
        fail(`${name}.state 값이 올바르지 않습니다.`);
    }
    if (
        value.mergedAt !== null &&
        (typeof value.mergedAt !== "string" ||
            value.mergedAt.trim() === "" ||
            Number.isNaN(Date.parse(value.mergedAt)))
    ) {
        fail(`${name}.mergedAt 값이 올바르지 않습니다.`);
    }
    if (value.state === "MERGED" && value.mergedAt === null) {
        fail(`${name}은(는) MERGED인데 mergedAt이 없습니다.`);
    }
    if (value.state !== "MERGED" && value.mergedAt !== null) {
        fail(`${name}은(는) ${value.state}인데 mergedAt이 있습니다.`);
    }

    return {
        number,
        state: value.state,
        mergedAt: value.mergedAt,
    };
}

function samePullRequestState(left, right) {
    return (
        left.number === right.number &&
        left.state === right.state &&
        left.mergedAt === right.mergedAt
    );
}

// GitHub GraphQL 의 PullRequest.stack 은 read-only connection 이다. 이 정책은 일부
// 페이지만 보고 "문제 없음"이라고 판정하지 않도록 한 페이지에 스택 전체가 들어왔는지,
// 그리고 현재 PR 이 그 스택의 정확한 위치에 있는지를 먼저 검증한다.
export function validatePullRequestStack(pullRequest) {
    const current = validatePullRequestFields(pullRequest, "pullRequest");

    if (!("stack" in pullRequest) || !("stackEntry" in pullRequest)) {
        fail("pullRequest.stack 또는 pullRequest.stackEntry 필드가 없습니다.");
    }
    if (pullRequest.stack === null) {
        if (pullRequest.stackEntry !== null) {
            fail("stack은 null인데 stackEntry가 남아 있습니다.");
        }
        return { current, currentPosition: null, entries: [] };
    }
    if (pullRequest.stackEntry === null) {
        fail("stack은 있는데 stackEntry가 없습니다.");
    }

    const stack = requireObject(pullRequest.stack, "pullRequest.stack");
    requirePositiveInteger(stack.number, "pullRequest.stack.number");
    const size = requirePositiveInteger(stack.size, "pullRequest.stack.size");
    const currentPosition = requirePositiveInteger(
        requireObject(pullRequest.stackEntry, "pullRequest.stackEntry").position,
        "pullRequest.stackEntry.position",
    );
    if (currentPosition > size) {
        fail("현재 PR 위치가 스택 크기를 벗어났습니다.");
    }

    const connection = requireObject(stack.entries, "pullRequest.stack.entries");
    const pageInfo = requireObject(
        connection.pageInfo,
        "pullRequest.stack.entries.pageInfo",
    );
    if (pageInfo.hasNextPage !== false) {
        fail("entries 전체 페이지를 조회하지 못했습니다.");
    }
    if (!Array.isArray(connection.nodes)) {
        fail("pullRequest.stack.entries.nodes 배열이 없습니다.");
    }
    if (connection.nodes.length !== size) {
        fail(`stack.size(${size})와 entries.nodes 길이(${connection.nodes.length})가 다릅니다.`);
    }

    const positions = new Set();
    const numbers = new Set();
    const entries = connection.nodes.map((rawEntry, index) => {
        const entry = requireObject(rawEntry, `entries.nodes[${index}]`);
        const position = requirePositiveInteger(
            entry.position,
            `entries.nodes[${index}].position`,
        );
        if (position > size || positions.has(position)) {
            fail(`entries.nodes[${index}].position 값이 중복되었거나 범위를 벗어났습니다.`);
        }
        positions.add(position);

        const entryPullRequest = validatePullRequestFields(
            entry.pullRequest,
            `entries.nodes[${index}].pullRequest`,
        );
        if (numbers.has(entryPullRequest.number)) {
            fail(`PR #${entryPullRequest.number}가 스택에 중복되어 있습니다.`);
        }
        numbers.add(entryPullRequest.number);

        return { position, pullRequest: entryPullRequest };
    });

    for (let position = 1; position <= size; position += 1) {
        if (!positions.has(position)) {
            fail(`스택 position ${position}이 없습니다.`);
        }
    }

    const currentEntry = entries.find(({ position }) => position === currentPosition);
    if (!currentEntry || !samePullRequestState(currentEntry.pullRequest, current)) {
        fail("현재 PR과 stackEntry.position의 항목이 일치하지 않습니다.");
    }
    if (
        entries.filter(({ pullRequest: entryPullRequest }) =>
            samePullRequestState(entryPullRequest, current),
        ).length !== 1
    ) {
        fail("현재 PR이 스택에 정확히 한 번 존재하지 않습니다.");
    }

    return {
        current,
        currentPosition,
        entries: entries.sort((left, right) => left.position - right.position),
    };
}

export function closedUnmergedBlockerNumbers(pullRequest) {
    const { currentPosition, entries } = validatePullRequestStack(pullRequest);
    if (currentPosition === null) return [];

    return entries
        .filter(
            ({ position, pullRequest: entryPullRequest }) =>
                position <= currentPosition &&
                entryPullRequest.state === "CLOSED" &&
                entryPullRequest.mergedAt === null,
        )
        .map(({ pullRequest: entryPullRequest }) => entryPullRequest.number);
}

export function openPullRequestNumbersAboveCurrent(pullRequest) {
    const { currentPosition, entries } = validatePullRequestStack(pullRequest);
    if (currentPosition === null) return [];

    return entries
        .filter(
            ({ position, pullRequest: entryPullRequest }) =>
                position > currentPosition &&
                entryPullRequest.state === "OPEN" &&
                entryPullRequest.mergedAt === null,
        )
        .map(({ pullRequest: entryPullRequest }) => entryPullRequest.number);
}

function renderNumbers(numbers) {
    return numbers.length === 0 ? "" : `${numbers.join("\n")}\n`;
}

export async function runCli({ argv = process.argv.slice(2), input = process.stdin, output = process.stdout } = {}) {
    const [command, ...extraArguments] = argv;
    if (extraArguments.length > 0 || !["blockers", "open-above"].includes(command)) {
        throw new Error("사용법: merge-order-stack-integrity.mjs <blockers|open-above>");
    }

    let source = "";
    for await (const chunk of input) {
        source += chunk;
    }
    if (source.trim() === "") {
        throw new Error("stdin에 pullRequest JSON이 필요합니다.");
    }

    let pullRequest;
    try {
        pullRequest = JSON.parse(source);
    } catch {
        throw new Error("stdin의 pullRequest JSON을 파싱할 수 없습니다.");
    }

    const numbers =
        command === "blockers"
            ? closedUnmergedBlockerNumbers(pullRequest)
            : openPullRequestNumbersAboveCurrent(pullRequest);
    output.write(renderNumbers(numbers));
    return numbers;
}

const invokedPath = process.argv[1] ? pathToFileURL(path.resolve(process.argv[1])).href : "";
if (import.meta.url === invokedPath) {
    runCli().catch((error) => {
        console.error(error.message);
        process.exitCode = 1;
    });
}
