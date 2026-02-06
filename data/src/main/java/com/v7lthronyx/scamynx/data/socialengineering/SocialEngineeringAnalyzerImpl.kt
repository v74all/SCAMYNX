package com.v7lthronyx.scamynx.data.socialengineering

import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.domain.model.SocialEngineeringReport
import com.v7lthronyx.scamynx.domain.service.SocialEngineeringAnalyzer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialEngineeringAnalyzerImpl @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : SocialEngineeringAnalyzer {

    override suspend fun analyze(message: String): SocialEngineeringReport = withContext(dispatcher) {
        buildReport(message)
    }
}
