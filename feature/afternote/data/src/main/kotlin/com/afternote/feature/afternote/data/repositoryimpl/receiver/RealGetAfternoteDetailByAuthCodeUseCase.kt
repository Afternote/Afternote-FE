package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.feature.afternote.data.repositoryimpl.stub.StubGetAfternoteDetailByAuthCodeUseCase
import com.afternote.feature.afternote.domain.usecase.receiver.GetAfternoteDetailByAuthCodeUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealGetAfternoteDetailByAuthCodeUseCase
    @Inject
    constructor(
        stub: StubGetAfternoteDetailByAuthCodeUseCase,
    ) : GetAfternoteDetailByAuthCodeUseCase by stub
