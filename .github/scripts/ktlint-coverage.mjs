#!/usr/bin/env node

// 어떤 Gradle 모듈이 ktlintCheck 태스크를 갖는지 빌드 스크립트만 읽어 판정한다.
//
// resolve-pr-impact.mjs 는 `.kt`·`.kts` 가 바뀐 모듈마다 `<모듈>:ktlintCheck` 를 고르는데
// (PR #1279), 그 태스크가 실재하는지는 보지 않는다. 없으면 Ktlint job 이 태스크 선택 단계에서
// 죽는다 (#1419). 모든 모듈은 자기 build.gradle.kts 를 갖고 그 파일 자체가 `.kts` 라, 위험 범위는
// 「.kt 를 가진 모듈」이 아니라 **등록된 모듈 전부**다.
//
// ktlint 는 두 경로로 붙는다 — 모듈이 직접 적용하거나, ktlint 를 적용하는 컨벤션 플러그인을
// 타거나. 컨벤션은 서로를 apply 하며 사슬을 이루므로(android.feature → android.library.compose →
// android.library → android.lint) 여기서 그 사슬의 전이 폐포를 계산한다. 컨벤션이 새로 생겨도
// 목록을 손으로 고칠 필요가 없다.

import fs from "node:fs/promises";
import path from "node:path";

const KTLINT_PLUGIN_ID = "org.jlleitschuh.gradle.ktlint";

function moduleDirectory(projectPath) {
    return projectPath.replace(/^:/, "").replaceAll(":", "/");
}

/** build-logic 의 gradlePlugin 블록에서 컨벤션 id ↔ 구현 클래스 짝을 읽는다. */
function parseConventionRegistrations(buildLogicSource) {
    const registrations = new Map();
    const blockPattern = /register\("[^"]+"\)\s*\{([^}]*)\}/g;
    for (const [, body] of buildLogicSource.matchAll(blockPattern)) {
        const id = body.match(/id\s*=\s*"([^"]+)"/)?.[1];
        const implementationClass = body.match(/implementationClass\s*=\s*"([^"]+)"/)?.[1];
        if (id && implementationClass) {
            registrations.set(id, implementationClass);
        }
    }
    return registrations;
}

/** 컨벤션 플러그인 소스가 apply 하는 플러그인 id 를 모은다. */
function parseAppliedPlugins(source) {
    return [...source.matchAll(/pluginManager\.apply\("([^"]+)"\)/g)].map((match) => match[1]);
}

/**
 * ktlint 를 결과적으로 적용하는 컨벤션 플러그인 id 집합을 전이 폐포로 구한다.
 * 시드는 ktlint 를 직접 apply 하는 컨벤션이고, 그 컨벤션을 apply 하는 컨벤션도 함께 물든다.
 */
export async function resolveKtlintBearingConventions(root) {
    const buildLogicSource = await fs.readFile(
        path.join(root, "build-logic", "build.gradle.kts"),
        "utf8",
    );
    const registrations = parseConventionRegistrations(buildLogicSource);

    const appliedByConvention = new Map();
    for (const [id, implementationClass] of registrations) {
        const source = await fs.readFile(
            path.join(root, "build-logic", "src", "main", "kotlin", `${implementationClass}.kt`),
            "utf8",
        );
        appliedByConvention.set(id, parseAppliedPlugins(source));
    }

    const bearing = new Set();
    for (const [id, applied] of appliedByConvention) {
        if (applied.includes(KTLINT_PLUGIN_ID)) {
            bearing.add(id);
        }
    }
    // 사슬을 타고 올라간다 — 물든 컨벤션을 apply 하는 컨벤션도 ktlint 를 받는다.
    for (let changed = true; changed; ) {
        changed = false;
        for (const [id, applied] of appliedByConvention) {
            if (bearing.has(id)) continue;
            if (applied.some((appliedId) => bearing.has(appliedId))) {
                bearing.add(id);
                changed = true;
            }
        }
    }
    return bearing;
}

/** 모듈 빌드 스크립트의 plugins 블록에 선언된 id 를 뽑는다. */
function declaredPluginIds(moduleBuildSource) {
    return [...moduleBuildSource.matchAll(/id\("([^"]+)"\)/g)].map((match) => match[1]);
}

/** settings.gradle.kts 에 등록된 모든 모듈의 ktlint 보유 여부를 판정한다. */
export async function inspectKtlintCoverage(root) {
    const settings = await fs.readFile(path.join(root, "settings.gradle.kts"), "utf8");
    const projectPaths = [...settings.matchAll(/include\("(:[^"]+)"\)/g)].map((match) => match[1]);
    const bearingConventions = await resolveKtlintBearingConventions(root);

    const modules = [];
    for (const projectPath of projectPaths) {
        const directory = moduleDirectory(projectPath);
        const source = await fs.readFile(path.join(root, directory, "build.gradle.kts"), "utf8");
        const declared = declaredPluginIds(source);
        modules.push({
            projectPath,
            directory,
            hasKtlint:
                declared.includes(KTLINT_PLUGIN_ID) ||
                declared.some((id) => bearingConventions.has(id)),
        });
    }
    return modules;
}
