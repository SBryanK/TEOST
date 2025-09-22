package com.example.teost.io

import android.content.ContentResolver
import android.net.Uri
import com.example.teost.config.TestPlan
import kotlinx.serialization.json.Json

object JsonConfigIO {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun parse(contentResolver: ContentResolver, uri: Uri): Result<TestPlan> = runCatching {
        contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "Cannot open stream" }
            val text = stream.reader().readText()
            json.decodeFromString(TestPlan.serializer(), text)
        }
    }

    fun toJson(plan: TestPlan): String = json.encodeToString(TestPlan.serializer(), plan)
}


