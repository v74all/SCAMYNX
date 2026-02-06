package com.v7lthronyx.scamynx.domain.usecase

import com.v7lthronyx.scamynx.domain.model.SocialEngineeringReport
import com.v7lthronyx.scamynx.domain.service.SocialEngineeringAnalyzer
import javax.inject.Inject

class AnalyzeSocialEngineeringUseCase @Inject constructor(
    private val analyzer: SocialEngineeringAnalyzer,
) {
    suspend operator fun invoke(message: String): SocialEngineeringReport = analyzer.analyze(message)
}
