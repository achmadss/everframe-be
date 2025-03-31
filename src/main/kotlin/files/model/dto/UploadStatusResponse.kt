package dev.achmad.files.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UploadStatusResponse(
    val sessionId: String,
    val fileName: String,
    val fileSize: Long,
    val totalChunks: Int,
    val uploadedChunks: Int,
    val status: String,
    val progress: Double
)