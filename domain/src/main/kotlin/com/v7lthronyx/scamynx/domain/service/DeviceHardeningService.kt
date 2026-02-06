package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.DeviceHardeningReport
import com.v7lthronyx.scamynx.domain.model.HardeningAction
import com.v7lthronyx.scamynx.domain.model.HardeningActionResult

interface DeviceHardeningService {
    suspend fun analyzeDeviceState(): DeviceHardeningReport

    suspend fun applyHardeningAction(action: HardeningAction): HardeningActionResult

    suspend fun revertHardeningAction(actionId: String): HardeningActionResult

    suspend fun applyAllRecommended(): List<HardeningActionResult>
}
