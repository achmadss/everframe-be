package dev.achmad

import dev.achmad.common.model.BaseResponse
import io.ktor.http.HttpStatusCode

data class FileUploadStatusCodes(
    val code: Int, val message: String
) {
    companion object {
        val InvalidSessionID = FileUploadStatusCodes(1, "Invalid session ID")
        val InvalidChunkIndex = FileUploadStatusCodes(1, "Invalid chunk index")
        val UploadSessionNotFound = FileUploadStatusCodes(1, "Upload session not found")
        val UploadSessionCancelled = FileUploadStatusCodes(2, "Upload session is already cancelled")
        val UploadSessionNotFoundOrCancelled = FileUploadStatusCodes(3, "Upload session not found or cancelled")
        val FileAlreadyUploaded = FileUploadStatusCodes(4, "File already uploaded")
        val ChunkAlreadyUploaded = FileUploadStatusCodes(5, "Chunk already uploaded")
        val NoFilePart = FileUploadStatusCodes(6, "No file part found in the request")
        val UploadCancelled = FileUploadStatusCodes(7, "Upload cancelled successfully")
        val FileAssembled = FileUploadStatusCodes(7, "File assembled successfully")
        val UploadSessionNotFoundOrIncomplete = FileUploadStatusCodes(8, "Upload session not found or incomplete")
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