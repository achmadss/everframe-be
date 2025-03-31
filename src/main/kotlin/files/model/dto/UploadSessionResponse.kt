package dev.achmad.files.model.dto

data class UploadSessionResponse(
    val sessionId: String,
    val fileName: String,
    val fileSize: Long,
    val totalChunks: Int,
    val uploadedChunks: List<Int>,
    val status: String
)
