---
name: Issue body must follow existing template
description: Afternote-FE 이슈 본문은 `.github/ISSUE_TEMPLATE/custom.md` 양식을 글자 단위로 따라야 함 — `## ` 헤더 + HTML 주석 힌트 + 빈 섹션은 `No response` 유지.
type: feedback
originSessionId: 40726de3-8856-4590-89dc-2ad8d936ac49
---
Afternote-FE 의 모든 GitHub 이슈 본문은 `.github/ISSUE_TEMPLATE/custom.md` 의 3섹션 양식을 글자 단위로 따른다.

**Why:** 팀이 정한 이슈 컨벤션. 과거에 자유 헤더 (`## 배경`, `## 현상`, `## 원인`, `## 해결 방향`, `## 작업 범위`) 로 작성해서 정정 작업 발생. 또한 직전 세션(#218 검증)에서 `## ` 헤더 누락 + 빈 섹션 omit 으로 양식 불일치 → 재학습.

**How to apply:** 새 이슈 만들 때 양식 파일을 그대로 복사한 뒤 `No response` 부분만 실제 내용으로 치환. 적용 안 되는 optional 섹션은 `No response` 그대로 유지. 헤더·주석·trailing space 임의 변경 금지.

## 양식 (`.github/ISSUE_TEMPLATE/custom.md` 원본 그대로)

```markdown
## 📜 Overview (Required)    
<!-- 이슈에 대해 간략하게 설명해주세요 -->  
No response  

## 📌 Child Issue(Optional)  
<!-- 자식 이슈를 연결해주세요(ex: - #32) -->
No response  

## 📍 Note (Optional) <!-- 특이사항을 적어주세요 -->
No response
```

## 헤더 정확히 그대로 (임의 정규화 금지)

- `## 📜 Overview (Required)` + 끝에 trailing space 4개
- `## 📌 Child Issue(Optional)` — `Issue` 와 `(` 사이 공백 **없음** (양식 그대로)
- `## 📍 Note (Optional) <!-- 특이사항을 적어주세요 -->` — 주석이 헤더와 같은 줄

## HTML 주석 힌트

각 섹션 헤더 아래 `<!-- ... -->` 힌트 줄을 **유지**. 렌더링 시 안 보이지만 raw body 에는 남는 게 양식.

## 빈 optional 섹션

- 📌 Child Issue / 📍 Note 가 비면 `No response` 로 채워둠 (섹션 헤더 자체 omit 금지)
- `No response` 뒤 trailing space 도 양식 그대로 유지

## 자주 빠뜨리는 것

- **자유 헤더 금지**: `## 배경`, `## 현상`, `## 원인`, `## 해결 방향`, `## 작업 범위`, `## 관련` → 모두 📜 Overview 안에 markdown 으로 통합
- **베이스 브랜치 / 검증 방법**: 📍 Note 에 배치
- **`## ` 누락**: 헤더는 반드시 `## ` 접두사 — 양식 그대로
- **PR 본문 양식과 다름**: PR 은 4섹션 (Issues / Work Description / Screenshot / To Reviewers), 이슈는 3섹션 (Overview / Child Issue / Note)
