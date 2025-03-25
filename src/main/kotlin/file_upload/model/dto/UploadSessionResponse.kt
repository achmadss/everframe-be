package dev.achmad.file_upload.model.dto

data class UploadSessionResponse(
    val sessionId: String,
    val fileName: String,
    val fileSize: Long,
    val totalChunks: Int,
    val uploadedChunks: List<Int>,
    val status: String
)
