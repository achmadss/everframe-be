package dev.achmad

import dev.achmad.common.model.BaseResponse
import io.ktor.http.HttpStatusCode

data class FileUploadStatusCodes(
    val code: Int, val message: String
) {
    companion object {
        val InvalidSessionID = FileUploadStatusCodes(1, "Invalid session ID")
        val InvalidChunkIndex = FileUploadStatusCodes(2, "Invalid chunk index")
        val UploadSessionNotFound = FileUploadStatusCodes(3, "Upload session not found")
        val UploadSessionNotFoundOrCancelled = FileUploadStatusCodes(4, "Upload session not found or cancelled")
        val FileAlreadyUploaded = FileUploadStatusCodes(5, "File already uploaded")
        val ChunkAlreadyUploaded = FileUploadStatusCodes(6, "Chunk already uploaded")
        val NoFilePart = FileUploadStatusCodes(7, "No file part found in the request")
        val UploadSessionNotFoundOrIncomplete = FileUploadStatusCodes(8, "Upload session not found or incomplete")
        val InvalidUploadDirectory = FileUploadStatusCodes(9, "Invalid upload directory")
    }
}

fun FileUploadStatusCodes.toResponse(
    httpStatusCode: HttpStatusCode,
) = BaseResponse(
    statusCode = httpStatusCode.value,
    errorCode = code,
    message = message,
    data = null
)