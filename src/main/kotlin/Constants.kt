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
        val UploadSessionCancelled = FileUploadStatusCodes(4, "Upload session is already cancelled")
        val UploadSessionNotFoundOrCancelled = FileUploadStatusCodes(5, "Upload session not found or cancelled")
        val FileAlreadyUploaded = FileUploadStatusCodes(6, "File already uploaded")
        val ChunkAlreadyUploaded = FileUploadStatusCodes(7, "Chunk already uploaded")
        val NoFilePart = FileUploadStatusCodes(8, "No file part found in the request")
        val UploadSessionNotFoundOrIncomplete = FileUploadStatusCodes(9, "Upload session not found or incomplete")
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