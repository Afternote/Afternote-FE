# 공개 QA 증거 대장

에뮬레이터·실서버 QA 결과의 정본이다. 파일 하나가 검증한 HEAD 하나에 대응하며 파일명은
`<40자리-head-sha>.json`이다. 최초 이관일인 2026-08-27에 로컬 대장 34건을 34개 파일로 옮겼다.

## 공개 스키마

- `head_sha`, `result`, `scenario`, `not_covered`는 모든 파일에 존재한다.
- `source.field_mapping`은 각 값이 이관 전 어느 필드에서 왔는지 나타낸다. 원본에
  `not_covered`가 없으면 공개 값은 빈 배열이고 매핑은 `null`이므로, 이를 전체 범위 검증으로
  해석하면 안 된다.
- `details`에는 원본의 나머지 QA 관찰 중 공개 가능한 내용을 보존한다.
- `source.redactions`에는 제거·치환한 필드 종류만 남긴다. 실제 값은 남기지 않는다.
- 한 파일에 여러 패스가 누적돼 앞선 `not_covered` 사유가 후속 관찰로 갱신된 경우에는
  `source.interpretation_notes`가 읽는 순서를 명시한다. 원래 `not_covered` 값은 이관 증거로 보존한다.
- `manifest.json`은 전체 파일의 원본 대응 관계와 SHA-256을 기록한다.

계정 이메일·별칭·계정/수신자 ID·인증 값·사람 이름·기기 serial/AVD·비공개 네트워크 주소·
스크린샷 비공개 링크는 저장하지 않는다. 공개 GitHub 이슈·PR 링크, 저장소 상대 경로, 커밋 SHA,
화면 크기·density·API level은 재현 근거이므로 보존한다.

## 새 기록 추가와 검증

민감정보가 들어갈 수 있는 원본은 임시 디렉터리에만 두고 아래 변환기를 거친다. 변환기는 기존
공개 기록을 유지하면서 새 HEAD 기록과 manifest를 갱신한다.

```bash
node .github/scripts/qa-evidence.mjs migrate <원본-json-디렉터리> docs/qa/evidence --migrated-on YYYY-MM-DD
node .github/scripts/qa-evidence.mjs validate docs/qa/evidence
node --test .github/scripts/qa-evidence.test.mjs
```

같은 HEAD에서 검증을 다시 했다면 기존 기록을 조용히 덮어쓰지 말고, 시나리오와 결과를 한 파일에
합쳐 이력이 사라지지 않게 한다. 변환기는 내용이 다른 기존 HEAD를 기본적으로 거부한다. 합친 원본을
검토했거나 비식별화 규칙 변경으로 전부 다시 생성할 때만 `--replace-existing`을 명시해 manifest
해시까지 함께 갱신한다.
