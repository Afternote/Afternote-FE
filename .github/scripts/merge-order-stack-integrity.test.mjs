import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import test from "node:test";

import {
    closedUnmergedBlockerNumbers,
    openPullRequestNumbersAboveCurrent,
} from "./merge-order-stack-integrity.mjs";

const MERGED_AT = "2026-08-30T12:00:00Z";

function entry(position, number, state = "OPEN", mergedAt = null) {
    return { position, pullRequest: { number, state, mergedAt } };
}

function pullRequest({
    currentNumber = 1557,
    currentPosition = 3,
    currentState = "OPEN",
    currentMergedAt = null,
    entries = [
        entry(1, 1537, "MERGED", MERGED_AT),
        entry(2, 1555, "CLOSED"),
        entry(3, 1557),
    ],
} = {}) {
    return {
        number: currentNumber,
        state: currentState,
        mergedAt: currentMergedAt,
        stackEntry: { position: currentPosition },
        stack: {
            number: 1556,
            size: entries.length,
            entries: {
                nodes: entries,
                pageInfo: { hasNextPage: false },
            },
        },
    };
}

test("#1556처럼 현재 PR 아래에 닫힌 미병합 항목이 있으면 차단한다", () => {
    assert.deepEqual(closedUnmergedBlockerNumbers(pullRequest()), [1555]);
});

test("병합된 하위 항목은 현재 PR을 차단하지 않는다", () => {
    const value = pullRequest({
        currentNumber: 1584,
        currentPosition: 2,
        entries: [entry(1, 1546, "MERGED", MERGED_AT), entry(2, 1584)],
    });

    assert.deepEqual(closedUnmergedBlockerNumbers(value), []);
});

test("현재 PR 위의 닫힌 미병합 항목은 아래 PR을 차단하지 않는다", () => {
    const value = pullRequest({
        currentNumber: 10,
        currentPosition: 1,
        entries: [entry(1, 10), entry(2, 11, "CLOSED"), entry(3, 12)],
    });

    assert.deepEqual(closedUnmergedBlockerNumbers(value), []);
});

test("스택에 속하지 않은 PR은 두 판정 모두 빈 목록이다", () => {
    const value = {
        number: 20,
        state: "OPEN",
        mergedAt: null,
        stack: null,
        stackEntry: null,
    };

    assert.deepEqual(closedUnmergedBlockerNumbers(value), []);
    assert.deepEqual(openPullRequestNumbersAboveCurrent(value), []);
});

test("부분 페이지와 size 불일치는 문제 없음으로 통과하지 않는다", () => {
    const partialPage = pullRequest();
    partialPage.stack.entries.pageInfo.hasNextPage = true;
    assert.throws(() => closedUnmergedBlockerNumbers(partialPage), /전체 페이지/);

    const wrongSize = pullRequest();
    wrongSize.stack.size += 1;
    assert.throws(() => closedUnmergedBlockerNumbers(wrongSize), /nodes 길이/);
});

test("잘못된 위치와 PR 필드는 문제 없음으로 통과하지 않는다", () => {
    const duplicatePosition = pullRequest();
    duplicatePosition.stack.entries.nodes[2].position = 2;
    assert.throws(() => closedUnmergedBlockerNumbers(duplicatePosition), /position/);

    const wrongCurrent = pullRequest();
    wrongCurrent.stackEntry.position = 2;
    assert.throws(() => closedUnmergedBlockerNumbers(wrongCurrent), /현재 PR/);

    const malformedPullRequest = pullRequest();
    malformedPullRequest.stack.entries.nodes[1].pullRequest.state = "UNKNOWN";
    assert.throws(() => closedUnmergedBlockerNumbers(malformedPullRequest), /state/);
});

test("open-above는 현재 위치보다 위에 있는 열린 PR만 위치순으로 반환한다", () => {
    const value = pullRequest({
        currentNumber: 1555,
        currentPosition: 2,
        currentState: "CLOSED",
        entries: [
            entry(1, 1537, "MERGED", MERGED_AT),
            entry(2, 1555, "CLOSED"),
            entry(3, 1557),
            entry(4, 1558, "CLOSED"),
            entry(5, 1559, "MERGED", MERGED_AT),
            entry(6, 1560),
        ],
    });

    assert.deepEqual(openPullRequestNumbersAboveCurrent(value), [1557, 1560]);
});

test("재오픈된 현재 PR도 위쪽 열린 PR을 반환해 stale red check를 갱신한다", () => {
    const value = pullRequest({
        currentNumber: 1555,
        currentPosition: 2,
        currentState: "OPEN",
        entries: [entry(1, 1537, "MERGED", MERGED_AT), entry(2, 1555), entry(3, 1557)],
    });

    assert.deepEqual(openPullRequestNumbersAboveCurrent(value), [1557]);
});

test("병합된 현재 PR도 기존 안내를 해소할 위쪽 PR을 반환한다", () => {
    const value = pullRequest({
        currentNumber: 1537,
        currentPosition: 1,
        currentState: "MERGED",
        currentMergedAt: MERGED_AT,
        entries: [entry(1, 1537, "MERGED", MERGED_AT), entry(2, 1557)],
    });

    assert.deepEqual(openPullRequestNumbersAboveCurrent(value), [1557]);
});

test("CLI는 stdin JSON을 받아 하위 blocker와 위쪽 open PR을 한 줄에 하나씩 출력한다", () => {
    const script = new URL("./merge-order-stack-integrity.mjs", import.meta.url);
    const brokenStack = pullRequest();
    const blockers = spawnSync(process.execPath, [script.pathname, "blockers"], {
        encoding: "utf8",
        input: JSON.stringify(brokenStack),
    });

    assert.equal(blockers.status, 0, blockers.stderr);
    assert.equal(blockers.stdout, "1555\n");

    const closedMiddle = pullRequest({
        currentNumber: 1555,
        currentPosition: 2,
        currentState: "CLOSED",
    });
    const openAbove = spawnSync(process.execPath, [script.pathname, "open-above"], {
        encoding: "utf8",
        input: JSON.stringify(closedMiddle),
    });

    assert.equal(openAbove.status, 0, openAbove.stderr);
    assert.equal(openAbove.stdout, "1557\n");
});

test("CLI는 잘못된 명령과 불완전한 JSON에서 실패한다", () => {
    const script = new URL("./merge-order-stack-integrity.mjs", import.meta.url);
    const unknownCommand = spawnSync(process.execPath, [script.pathname, "unknown"], {
        encoding: "utf8",
        input: JSON.stringify(pullRequest()),
    });
    assert.notEqual(unknownCommand.status, 0);
    assert.match(unknownCommand.stderr, /사용법/);

    const malformedJson = spawnSync(process.execPath, [script.pathname, "blockers"], {
        encoding: "utf8",
        input: "{",
    });
    assert.notEqual(malformedJson.status, 0);
    assert.match(malformedJson.stderr, /파싱할 수 없습니다/);
});
