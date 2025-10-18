package com.v7lthronyx.scamynx.data.preferences

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class SettingsPreferencesSerializer(
    private val json: Json,
) : Serializer<SettingsPreferences> {

    override val defaultValue: SettingsPreferences = SettingsPreferences()

    override suspend fun readFrom(input: InputStream): SettingsPreferences {
        return try {
            json.decodeFromString(
                deserializer = SettingsPreferences.serializer(),
                string = input.readBytes().decodeToString(),
            )
        } catch (error: SerializationException) {
            throw CorruptionException("Unable to deserialize settings", error)
        }
    }

    override suspend fun writeTo(t: SettingsPreferences, output: OutputStream) {
        val payload = json.encodeToString(SettingsPreferences.serializer(), t)
        output.write(payload.encodeToByteArray())
    }
}
