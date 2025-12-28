package com.bungamust.deepnoura.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.bungamust.deepnoura.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

class ProjectRepository(
    private val context: Context,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
)
{
    private val fileName = "project.json"

    suspend fun load(): Project = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) {
            return@withContext Project()
        }
        val content = file.readText()
        json.decodeFromString(Project.serializer(), content)
    }

    suspend fun save(project: Project) = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, fileName)
        file.writeText(json.encodeToString(Project.serializer(), project))
    }

    suspend fun importFromUri(contentResolver: ContentResolver, uri: Uri): Project = withContext(Dispatchers.IO) {
        val data = readAllBytes(contentResolver.openInputStream(uri))
        val imported = json.decodeFromString(Project.serializer(), data)
        save(imported.copy(updatedAt = System.currentTimeMillis()))
        imported
    }

    suspend fun exportToShareIntent(project: Project): Intent = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val exportFile = File(cacheDir, "deepnoura_project.json")
        exportFile.writeText(json.encodeToString(Project.serializer(), project))
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", exportFile)
        Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun readAllBytes(stream: InputStream?): String {
        if (stream == null) error("No stream available")
        return stream.bufferedReader().use { it.readText() }
    }
}
