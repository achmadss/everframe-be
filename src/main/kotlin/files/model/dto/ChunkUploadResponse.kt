package files.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChunkUploadResponse(
    val sessionId: String,
    val chunkIndex: Int,
    val uploadedChunks: Int,
    val completed: Boolean
)