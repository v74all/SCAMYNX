package com.v7lthronyx.scamynx.ml.analyzer

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import org.tensorflow.lite.Interpreter

private const val MODEL_ASSET_PATH = "models/urlsafety.tflite"

@Singleton
class TfLiteInterpreterProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @Volatile
    private var interpreter: Interpreter? = null

    fun getInterpreter(): Interpreter? {
        val cached = interpreter
        if (cached != null) return cached
        return synchronized(this) {
            interpreter ?: loadInterpreter().also { interpreter = it }
        }
    }

    private fun loadInterpreter(): Interpreter? {
        return try {
            val mappedModel = loadModelFile()
            val options = Interpreter.Options().apply {
                numThreads = 4
            }
            Interpreter(mappedModel, options)
        } catch (ioException: IOException) {
            null
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_ASSET_PATH)
        FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->
            val channel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }
}
