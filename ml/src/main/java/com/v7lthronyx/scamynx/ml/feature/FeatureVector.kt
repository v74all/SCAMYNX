package com.v7lthronyx.scamynx.ml.feature

import com.v7lthronyx.scamynx.domain.model.FeatureWeight

data class FeatureVector(
    val values: FloatArray,
    val features: List<FeatureWeight>,
)
