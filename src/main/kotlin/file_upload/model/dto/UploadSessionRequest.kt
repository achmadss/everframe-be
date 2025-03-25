package dev.achmad.file_upload.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UploadSessionRequest(
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val totalChunks: Int
)