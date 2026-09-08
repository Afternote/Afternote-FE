#!/usr/bin/env node

// KDoc 의 `[com.afternote…]` FQN 링크가 실제 선언을 가리키는지 센다 (#1889).
//
// 어느 게이트도 안 보던 자리다 — 패키지 이동(#1092 평탄화, #1724 수신자 홈 이동)마다 끊긴 링크가
// 수십 건씩 쌓였고 0903 에 손으로 30건을 찾아 고쳤다. 링크는 IDE 에서만 경고라 CI 가 세지 않으면
// 다음 이동에서 그대로 되풀이된다.
//
// 해석 순서 — ① 패키지 링크(`[com.afternote.feature.afternote.domain]`) ② 가장 긴 패키지 접두를
// 뗀 나머지를 [타입[.중첩][.멤버]] 사슬로 ③ 그것도 아니면 최상위 callable(Composable 함수 등).
// 0903 스캔의 오탐 두 종을 미리 막는다 — 확장 함수 수신자(`fun Foo<T>?.bar`)를 심볼로 오인하지 않게
// 마지막 이름만 취하고, 패키지 링크는 접두 탐색 «전에» 패키지 집합에서 본다.
//
// 사용: node verify-kdoc-links.mjs [<repo-root>] [--json]
import { readdirSync, readFileSync, statSync } from "node:fs";
import { dirname, join, relative, resolve } from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

const DEFAULT_ROOT = resolve(dirname(fileURLToPath(import.meta.url)), "..", "..");
// 빌드 산출물과 점 디렉터리(중첩 워크트리 .claude/.codex 포함)는 걷지 않는다.
const SKIP_DIRS = new Set(["build", "node_modules"]);

function* kotlinFiles(dir) {
    for (const entry of readdirSync(dir)) {
        if (SKIP_DIRS.has(entry) || entry.startsWith(".")) continue;
        const full = join(dir, entry);
        const st = statSync(full);
        if (st.isDirectory()) yield* kotlinFiles(full);
        else if (entry.endsWith(".kt")) yield full;
    }
}

const MODIFIERS = "(?:(?:public|internal|private|protected|open|abstract|sealed|data|enum|annotation|value|inline|inner|companion|expect|actual|const|lateinit|override|external|tailrec|operator|infix|suspend)\\s+)*";
// 타입 선언 — 중첩 포함(들여쓰기 무관). typealias 도 타입이다.
const TYPE_DECL = new RegExp(`^\\s*${MODIFIERS}(?:class|interface|object|typealias)\\s+\`?(\\w+)\`?`, "gm");
// 최상위·멤버 fun/val/var — 확장 수신자(Foo<T>?.)와 제네릭(<T>)을 걷고 마지막 이름만 취한다(오탐 ①).
const CALLABLE_DECL = new RegExp(`^\\s*${MODIFIERS}(?:fun|val|var)\\s+(?:<[^>]*>\\s*)?(?:[\\w.<>?,\\s*]+?\\.)?\`?(\\w+)\`?\\s*[(:=<{]`, "gm");
const PACKAGE = /^package\s+([\w.]+)/m;
const KDOC = /\/\*\*([\s\S]*?)\*\//g;
const LINK = /\[(com\.afternote\.[\w.]+)\]/g;

export function scanKdocLinks(root = DEFAULT_ROOT) {
const files = [...kotlinFiles(root)];
const pkgOf = new Map();          // file -> package
const textOf = new Map();
const typesIn = new Map();        // package -> Set<typeName>
const callablesIn = new Map();    // package -> Set<name>  (최상위+멤버 뭉뚱그림 — 멤버는 아래 타입 파일 텍스트로 재확인)
const typeFiles = new Map();      // `${pkg}.${type}` -> [file]
for (const f of files) {
    const text = readFileSync(f, "utf8");
    const pkg = PACKAGE.exec(text)?.[1];
    if (!pkg) continue;
    textOf.set(f, text); pkgOf.set(f, pkg);
    const ts = typesIn.get(pkg) ?? new Set(); const cs = callablesIn.get(pkg) ?? new Set();
    for (const m of text.matchAll(TYPE_DECL)) { ts.add(m[1]); const k = `${pkg}.${m[1]}`; typeFiles.set(k, [...(typeFiles.get(k) ?? []), f]); }
    for (const m of text.matchAll(CALLABLE_DECL)) cs.add(m[1]);
    typesIn.set(pkg, ts); callablesIn.set(pkg, cs);
}
const packages = new Set(pkgOf.values());
const packagePrefixes = new Set();
for (const p of packages) { const parts = p.split("."); for (let i = 1; i <= parts.length; i++) packagePrefixes.add(parts.slice(0, i).join(".")); }

// 링크 해석 — ② 패키지 링크 먼저. 그다음 가장 긴 패키지 접두를 떼고 남은 사슬을
// [타입…][.멤버] 또는 [최상위 callable] 로 푼다.
function resolve_(fqn) {
    if (packagePrefixes.has(fqn)) return { ok: true, kind: "package" };
    const parts = fqn.split(".");
    for (let i = parts.length - 1; i >= 1; i--) {
        const pkg = parts.slice(0, i).join(".");
        if (!packages.has(pkg)) continue;
        const chain = parts.slice(i);
        const head = chain[0];
        if (typesIn.get(pkg)?.has(head)) {
            if (chain.length === 1) return { ok: true, kind: "type" };
            // 멤버·중첩: 그 타입을 선언한 파일 텍스트에서 이름을 느슨히 확인
            const rest = chain.slice(1);
            const filesOfType = typeFiles.get(`${pkg}.${head}`) ?? [];
            const seen = filesOfType.some((f) => rest.every((name) => new RegExp(`\\b${name}\\b`).test(textOf.get(f))));
            return seen ? { ok: true, kind: "member" } : { ok: false, reason: `타입 ${pkg}.${head} 에 ${rest.join(".")} 없음` };
        }
        if (chain.length === 1 && callablesIn.get(pkg)?.has(head)) return { ok: true, kind: "callable" };
        return { ok: false, reason: `패키지 ${pkg} 에 ${head} 선언 없음` };
    }
    return { ok: false, reason: "일치하는 패키지 접두 없음" };
}

// 같은 단순 이름의 현재 선언 위치(이동 후보)
function candidates(fqn) {
    const parts = fqn.split(".");
    let head = parts.findLast((p) => /^[A-Z]/.test(p)) ?? parts.at(-1);
    const out = [];
    for (const [pkg, ts] of typesIn) if (ts.has(head)) out.push(`${pkg}.${head}`);
    if (out.length === 0) for (const [pkg, cs] of callablesIn) if (cs.has(parts.at(-1))) out.push(`${pkg}.${parts.at(-1)}`);
    return out;
}

const broken = [];
let total = 0;
for (const [f, text] of textOf) {
    const seenInFile = new Set();
    for (const kd of text.matchAll(KDOC)) for (const l of kd[1].matchAll(LINK)) {
        const fqn = l[1]; if (seenInFile.has(fqn)) continue; seenInFile.add(fqn); total++;
        const r = resolve_(fqn);
        if (!r.ok) broken.push({ file: relative(root, f).replaceAll("\\", "/"), link: fqn, reason: r.reason, candidates: candidates(fqn) });
    }
}
return { total, broken: broken.sort((a, b) => a.file.localeCompare(b.file) || a.link.localeCompare(b.link)) };
}

function main() {
    const args = process.argv.slice(2);
    const asJson = args.includes("--json");
    const root = args.find((a) => !a.startsWith("--")) ?? DEFAULT_ROOT;
    const { total, broken } = scanKdocLinks(root);
    if (asJson) { console.log(JSON.stringify({ total, broken }, null, 2)); }
    else {
        console.log(`KDoc FQN 링크 ${total}개 중 끊김 ${broken.length}개`);
        for (const b of broken) {
            const cand = b.candidates.length ? ` → 후보: ${b.candidates.slice(0, 2).join(" | ")}${b.candidates.length > 2 ? ` (+${b.candidates.length - 2})` : ""}` : " → 선언 없음(링크를 걷고 코드 스팬으로)";
            console.log(`  ${b.file}\n      [${b.link}]\n      ${b.reason}${cand}`);
        }
        if (broken.length) console.error(`::error::KDoc FQN 링크 ${broken.length}개가 실제 선언을 가리키지 않습니다. 이동이면 새 위치로, 소멸이면 링크를 걷으세요 (#1889).`);
    }
    process.exitCode = broken.length ? 1 : 0;
}

const isDirectExecution = process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isDirectExecution) main();
