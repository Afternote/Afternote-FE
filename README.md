# Afternote-FE

# 💻 코딩 컨벤션

> **네이밍 컨벤션**
>
- 네이밍 항목 순서는 android-style-guide를 준수한다.
- 단, Layout을 제외한 네이밍은 CamelCase를 사용한다.
    - 예시) `android:id="@+id/tvPostNovelTitle"`
    - 자세한 정보는 아래 링크를 참고하였다.

[](https://github.com/PRNDcompany/android-style-guide/blob/main/Resource.md)

- Coding Style은 객체지향 생활 체조 원칙을 준수한다.
    - 자세한 정보는 아래 링크를 참고하였다.

[[Java] 객체지향 생활 체조 원칙 9가지 (from 소트웍스 앤솔러지)](https://jamie95.tistory.com/99)

# 🦥 깃 전략 및 컨벤션

> **브랜치 전략**
>
- GitHub Flow를 사용한다.
    - 수시로 코드가 변하는 앱잼의 특성을 고려하였다.
    - 브랜치 이름은 다음과 같이 언더바를 사용한다.
        - 예시) `feat/post_novel`
    - 자세한 정보는 아래 링크를 참고하였다.

[[GIT] 📈 깃 브랜치 전략 정리 - Github Flow / Git Flow](https://inpa.tistory.com/entry/GIT-⚡️-github-flow-git-flow-📈-브랜치-전략)

> **Commit 컨벤션**
>
- 사용할 커밋 타입은 다음과 같다.
    - 🍯 [FEAT] 새로운 기능 추가
    - ♻️ [REFACTOR] 코드 리팩토링
    - 🔨 [FIX] 버그 수정
    - 🚧 [BUILD] 빌드 업무 수정, 패키지 매니저 수정
- 커밋 메시지 예시는 다음과 같다.
    - 예시) `feat: color system 구성`
- 커밋 메시지는 한글로 작성하고, 이슈 번호는 별도로 표기하지 않는다.

> **Issue 컨벤션**
>
- 제목 예시는 다음과 같다.
    - 예시) `feat: library view 구현`

```kotlin
## ⚔️ Kind (Required)    <!-- 이슈 종류를 선택해주세요 -->
`FEATURE` `BUG`

## 📜 Overview (Required)    <!-- 이슈에 대해 간략하게 설명해주세요 -->

> **✔️ To do**    <!-- 진행할 작업에 대해 적어주세요 -->
> - [ ] color system 구성 _(예시)_

## 📍 Note (Optional) <!-- 특이사항을 적어주세요 -->
```

> **PR 컨벤션**
>
- 제목 예시는 다음과 같다.
    - 예시) `feat: bottomNavigation color system 적용`

```kotlin
## 📌𝘐𝘴𝘴𝘶𝘦𝘴
- closed #

## 📎𝘞𝘰𝘳𝘬 𝘋𝘦𝘴𝘤𝘳𝘪𝘱𝘵𝘪𝘰𝘯
- 
- 

## 📷𝘚𝘤𝘳𝘦𝘦𝘯𝘴𝘩𝘰𝘵

## 💬𝘛𝘰 𝘙𝘦𝘷𝘪𝘦𝘸𝘦𝘳𝘴
```

> **Code Review 컨벤션 및 추가정보**
>
- Merge는 리뷰 인원 2명의 승인을 받는다.
- 리뷰 인원으로 할당받은 사람은 12시간 이내에 코드리뷰를 완료한다.
- RCA룰을 통해 Prefix를 적고, 코드 리뷰 반영의 우선순위를 표시한다.
    - R (Request Changes) : 적극적으로 반영을 고려해주세요.
    - C (Comment) : 웬만하면 반영해주세요.
    - A (Approve) : 반영해도 좋고, 넘어가도 좋습니다. 사소한 의견입니다.
        - 예시) `R: @Data 어노테이션 사용은 지양해야 할 것 같습니다. 참고자료 별첨합니다.`