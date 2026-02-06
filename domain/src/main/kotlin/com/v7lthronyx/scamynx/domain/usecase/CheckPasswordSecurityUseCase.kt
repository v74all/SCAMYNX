package com.v7lthronyx.scamynx.domain.usecase

import com.v7lthronyx.scamynx.domain.model.PasswordSecurityReport
import com.v7lthronyx.scamynx.domain.service.PasswordSecurityAnalyzer
import javax.inject.Inject

class CheckPasswordSecurityUseCase @Inject constructor(
    private val analyzer: PasswordSecurityAnalyzer,
) {
    suspend operator fun invoke(password: String): PasswordSecurityReport = analyzer.evaluate(password)
}
