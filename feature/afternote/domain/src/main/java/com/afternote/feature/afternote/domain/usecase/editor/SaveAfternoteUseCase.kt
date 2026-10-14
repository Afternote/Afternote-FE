package com.afternote.feature.afternote.domain.usecase.editor

import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.SaveAfternoteCommand
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import javax.inject.Inject

/**
 * 완성된 [SaveAfternoteCommand] 를 해석하는 **단일 경계**.
 *
 * 도입 근거는 「비즈니스 로직이 많다」가 아니라 *해석의 소재지* 다. [SaveAfternoteCommand] 는 도메인이
 * 소유한 닫힌(sealed) 명령이고, 그 5갈래(Create 4종 · Update)를 [AfternoteRepository] 의 어느 메서드로
 * 보낼지는 명령 정의와 짝을 이루는 도메인 지식이다. 명령이 도메인에 있는 한 해석도 도메인에 있어야
 * 명령에 갈래가 늘 때 고칠 곳이 한 곳으로 닫힌다 — 화면이 몇 개든, 호출부가 지금 하나든 상관없다.
 *
 * 과거 `#246` 은 이 UseCase 를 «Repository 프록시» 로 보고 제거했지만, 같은 `when` 은 사라지지 않고
 * `AfternoteEditorViewModel` 로 옮겨갔을 뿐이었다. 즉 제거된 것은 중복이 아니라 경계였다 (`#1694`).
 *
 * 여기서 하지 않는 일 — 입력 검증, UI 폼 매핑, 미디어 입력 선택, 저장 상태·문구·네비게이션.
 * 그 책임들은 명령을 *만드는* 쪽(presentation)에 그대로 남는다.
 */
class SaveAfternoteUseCase
    @Inject
    constructor(
        private val afternoteRepository: AfternoteRepository,
    ) {
        /**
         * Repository 의 [Result] 를 감싸거나 다시 던지지 않고 그대로 돌려준다 — 저장 실패의 해석은
         * 호출부(에러 문구·재시도)의 몫이고, `runCatching` 을 두르지 않아 코루틴 취소도 삼키지 않는다.
         *
         * @return 생성·수정된 애프터노트 id.
         */
        suspend operator fun invoke(command: SaveAfternoteCommand): Result<Long> =
            when (command) {
                is SaveAfternoteCommand.Create -> {
                    when (val input = command.input) {
                        is CreateAfternoteInput.Social -> afternoteRepository.createSocial(input.payload)
                        is CreateAfternoteInput.Business -> afternoteRepository.createBusiness(input.payload)
                        is CreateAfternoteInput.Gallery -> afternoteRepository.createGallery(input.payload)
                        is CreateAfternoteInput.Memorial -> afternoteRepository.createMemorial(input.payload)
                    }
                }

                is SaveAfternoteCommand.Update -> {
                    afternoteRepository.update(command.id, command.payload)
                }
            }
    }
