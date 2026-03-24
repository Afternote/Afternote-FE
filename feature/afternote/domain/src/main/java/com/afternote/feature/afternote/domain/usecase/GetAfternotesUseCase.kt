package com.afternote.feature.afternote.domain.usecase

import com.afternote.feature.afternote.domain.model.ListPage
import com.afternote.feature.afternote.domain.model.input.GetListPageInput
import com.afternote.feature.afternote.domain.repository.AfternoteRepository
import javax.inject.Inject

/**
 * 애프터노트 목록 조회 UseCase.
 *
 * GET /afternotes?category=&page=&size=
 */
class GetListUseCase
    @Inject
    constructor(
        private val repository: AfternoteRepository,
    ) {
        suspend operator fun invoke(input: GetListPageInput): Result<ListPage> = repository.getListPage(input = input)
    }
