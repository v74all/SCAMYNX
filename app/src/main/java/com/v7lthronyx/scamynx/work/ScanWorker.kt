package com.v7lthronyx.scamynx.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.v7lthronyx.scamynx.domain.model.ScanRequest
import com.v7lthronyx.scamynx.domain.model.ScanState
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.domain.repository.ScanRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONException
import org.json.JSONObject

@HiltWorker
class ScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scanRepository: ScanRepository,
) : CoroutineWorker(context, params) {

    @dagger.assisted.AssistedFactory
    interface Factory {
        fun create(context: Context, params: WorkerParameters): ScanWorker
    }

    override suspend fun doWork(): Result {
        val targetTypeName = inputData.getString(KEY_TARGET_TYPE) ?: return Result.failure()
        val rawInput = inputData.getString(KEY_RAW_INPUT) ?: return Result.failure()
        val metadataJson = inputData.getString(KEY_METADATA_JSON)
        val metadata = metadataJson?.let(::decodeMetadata).orEmpty()
        val targetType = runCatching { ScanTargetType.valueOf(targetTypeName) }.getOrNull() ?: return Result.failure()
        val request = ScanRequest(
            targetType = targetType,
            rawInput = rawInput,
            metadata = metadata,
        )

        val result = withTimeoutOrNull(MAX_EXECUTION_MILLIS) {
            scanRepository.analyze(request).firstOrNull { state ->
                state is ScanState.Success || state is ScanState.Failure
            }
        } ?: return Result.retry()

        return when (result) {
            is ScanState.Success -> Result.success()
            is ScanState.Failure -> Result.retry()
            else -> Result.success()
        }
    }

    companion object {
        private const val KEY_TARGET_TYPE = "target_type"
        private const val KEY_RAW_INPUT = "raw_input"
        private const val KEY_METADATA_JSON = "metadata_json"
        private const val MAX_EXECUTION_MILLIS = 120_000L

        fun buildInputData(request: ScanRequest) = workDataOf(
            KEY_TARGET_TYPE to request.targetType.name,
            KEY_RAW_INPUT to request.rawInput,
            KEY_METADATA_JSON to encodeMetadata(request.metadata),
        )

        private fun encodeMetadata(metadata: Map<String, String>): String? {
            if (metadata.isEmpty()) return null
            val json = JSONObject()
            metadata.forEach { (key, value) ->
                json.put(key, value)
            }
            return json.toString()
        }

        private fun decodeMetadata(json: String): Map<String, String> = try {
            val obj = JSONObject(json)
            obj.keys().asSequence().associateWith { key ->
                obj.optString(key, "")
            }
        } catch (error: JSONException) {
            emptyMap()
        }
    }
}
