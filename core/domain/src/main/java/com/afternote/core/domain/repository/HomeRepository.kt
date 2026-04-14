package com.afternote.core.domain.repository

import com.afternote.core.model.HomeSummary

interface HomeRepository {
    suspend fun getHomeSummary(): Result<HomeSummary>
}
