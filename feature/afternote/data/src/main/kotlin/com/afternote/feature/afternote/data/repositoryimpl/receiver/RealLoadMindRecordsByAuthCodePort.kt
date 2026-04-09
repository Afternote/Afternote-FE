package com.afternote.feature.afternote.data.repositoryimpl.receiver

import com.afternote.feature.afternote.data.repositoryimpl.stub.StubLoadMindRecordsByAuthCodePort
import com.afternote.feature.afternote.domain.port.LoadMindRecordsByAuthCodePort
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealLoadMindRecordsByAuthCodePort
    @Inject
    constructor(
        stub: StubLoadMindRecordsByAuthCodePort,
    ) : LoadMindRecordsByAuthCodePort by stub
