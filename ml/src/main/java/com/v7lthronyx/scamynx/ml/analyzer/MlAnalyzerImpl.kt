package com.v7lthronyx.scamynx.ml.analyzer

import com.v7lthronyx.scamynx.domain.model.MlReport
import com.v7lthronyx.scamynx.domain.service.MlAnalyzer
import com.v7lthronyx.scamynx.ml.feature.FeatureVector
import com.v7lthronyx.scamynx.ml.feature.UrlFeatureExtractor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

@Singleton
class MlAnalyzerImpl @Inject constructor(
    private val featureExtractor: UrlFeatureExtractor,
    private val interpreterProvider: TfLiteInterpreterProvider,
) : MlAnalyzer {

    override suspend fun evaluate(url: String, htmlSnapshot: String?): MlReport = withContext(Dispatchers.Default) {
        val featureVector = featureExtractor.extract(url, htmlSnapshot)
        val interpreter = interpreterProvider.getInterpreter()
        if (interpreter == null) {
            return@withContext MlReport(probability = 0.0, topFeatures = featureVector.features)
        }
        runInference(interpreter, featureVector)
    }

    private fun runInference(
        interpreter: Interpreter,
        featureVector: FeatureVector,
    ): MlReport {
        val input = arrayOf(featureVector.values)
        val output = Array(1) { FloatArray(1) }
        return try {
            interpreter.run(input, output)
            val rawProbability = output[0].firstOrNull()?.toDouble()
            val probability = rawProbability?.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.0
            MlReport(probability = probability, topFeatures = featureVector.features)
        } catch (_: Throwable) {
            MlReport(probability = 0.0, topFeatures = featureVector.features)
        }
    }
}
