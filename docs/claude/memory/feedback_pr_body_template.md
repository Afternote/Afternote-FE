---
name: PR body must follow existing template
description: Afternote-FE 의 모든 PR 본문은 기존 양식(📌 Issues / 📎 Work Description / 📷 Screenshot / 💬 To Reviewers) 따라야 함. 영문 헤더 (Summary/What changed/Why/Test plan) 사용 금지.
type: feedback
originSessionId: 40726de3-8856-4590-89dc-2ad8d936ac49
---
Afternote-FE 의 모든 PR 본문은 다음 한국어 + 이모지 + *유니코드 italic 헤더 글자* (template 의 `𝘐𝘴𝘴𝘶𝘦𝘴`·`𝘞𝘰𝘳𝘬 𝘋𝘦𝘴𝘤𝘳𝘪𝘱𝘵𝘪𝘰𝘯` 등) 양식을 따른다. **본문은 평문** — markdown `_..._` italic 으로 줄을 감싸지 않는다.

**Why:** 팀이 정한 PR 컨벤션 (`.github/PULL_REQUEST_TEMPLATE.md`) 의 헤더가 유니코드 mathematical sans-serif italic 글자라서 시각적으로 italic 처럼 보이는 것뿐. 본문 자체에 italic 을 강제하지 않음. 사용자 정정(2026-05) — 과거에 내가 본문 줄 단위로 `_..._` 를 두른 PR (#217·#229·#230 등) 은 잘못 적용된 양식.

**How to apply:** 새 PR 만들 때 항상 아래 4개 섹션 + 이모지 + 유니코드 italic 헤더 사용. 본문은 평문, 강조가 정말 필요한 단어만 `**bold**`. Closes 키워드는 📌 Issues 섹션 안에 `closed feat: ... #이슈번호` 형태로.

## 양식

```
## 📌𝘐𝘴𝘴𝘶𝘦𝘴

closed <이슈 제목 그대로> #이슈번호

## 📎𝘞𝘰𝘳𝘬 𝘋𝘦𝘴𝘤𝘳𝘪𝘱𝘵𝘪𝘰𝘯

- 한 줄짜리 변경 요약 (커밋 해시 동봉 가능)
- 다른 변경 요약
- ...

## 📷𝘚𝘤𝘳𝘦𝘦𝘯𝘴𝘩𝘰𝘵

스크린샷 또는 "UI 변경 없음/Preview 검증 완료" 같은 설명

## 💬𝘛𝘰 𝘙𝘦𝘷𝘪𝘦𝘸𝘦𝘳𝘴

- 알려둘 점 1 (의도된 차이/서버 협의 사항/후속 작업 등)
- 알려둘 점 2
- 빌드 검증: `./gradlew :모듈:compileDebugKotlin` BUILD SUCCESSFUL
```

**`## ` 마크다운 H2 헤더 필수** — GitHub 가 H2 아래에 자동 underline(섹션 경계선) 을 그려준다. 이전 정정 작업(2026-05) 에서 내가 `## ` 를 빼버려 경계선이 사라진 회귀가 있었음. 헤더 양식은 *`## ` + 이모지 + 유니코드 italic 글자* 셋 다 묶음.

## 헤더 이모지 + 폰트 (정확히 이대로)

- `📌𝘐𝘴𝘴𝘶𝘦𝘴`
- `📎𝘞𝘰𝘳𝘬 𝘋𝘦𝘴𝘤𝘳𝘪𝘱𝘵𝘪𝘰𝘯`
- `📷𝘚𝘤𝘳𝘦𝘦𝘯𝘴𝘩𝘰𝘵`
- `💬𝘛𝘰 𝘙𝘦𝘷𝘪𝘦𝘸𝘦𝘳𝘴`

(영문은 Mathematical Sans-Serif Italic 유니코드 문자 — 이모지 + 코드포인트 그대로 사용. 본문 평문이라도 헤더 자체가 italic 처럼 보이는 *글자 자체의 형태*.)

## 자주 빠뜨리는 것

- **빌드 검증 라인**: `💬 To Reviewers` 마지막에 `빌드 검증: ./gradlew :모듈:compileDebugKotlin BUILD SUCCESSFUL` 형태로 명시
- **이미지 없을 때**: 📷 섹션을 비우지 말고 "UI 변경 없음", "Compose Preview 로 검증", "NavGraph 만 변경이라 시각 변화 없음" 등 사유 적기
- 의존(stack PR base), 백엔드 협의 요청, 후속 작업 메모는 💬 안에 bullet 으로 묶기
