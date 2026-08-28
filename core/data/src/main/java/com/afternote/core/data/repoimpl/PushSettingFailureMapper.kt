package com.afternote.core.data.repoimpl

import com.afternote.core.domain.error.PushSettingFailure
import com.afternote.core.network.model.ApiException
import java.io.IOException

internal inline fun <T> mapPushSettingFailure(block: () -> T): T =
    try {
        block()
    } catch (failure: ApiException) {
        throw PushSettingFailure.ServerUnavailable(failure)
    } catch (failure: IOException) {
        throw PushSettingFailure.NetworkUnavailable(failure)
    }
