package dev.achmad.files.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UploadSessionRequest(
    val directory: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val totalChunks: Int
)