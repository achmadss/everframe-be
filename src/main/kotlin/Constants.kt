package dev.achmad

import dev.achmad.common.model.BaseResponse
import io.ktor.http.HttpStatusCode

data class FileErrorStatusCodes(
    val code: Int, val message: String
) {
    companion object {
        val InvalidSessionID = FileErrorStatusCodes(1, "Invalid session ID")
        val InvalidChunkIndex = FileErrorStatusCodes(2, "Invalid chunk index")
        val UploadSessionNotFound = FileErrorStatusCodes(3, "Upload session not found")
        val UploadSessionNotFoundOrCancelled = FileErrorStatusCodes(4, "Upload session not found or cancelled")
        val FileAlreadyUploaded = FileErrorStatusCodes(5, "File already uploaded")
        val ChunkAlreadyUploaded = FileErrorStatusCodes(6, "Chunk already uploaded")
        val NoFilePart = FileErrorStatusCodes(7, "No file part found in the request")
        val UploadSessionNotFoundOrIncomplete = FileErrorStatusCodes(8, "Upload session not found or incomplete")
        val InvalidUploadDirectory = FileErrorStatusCodes(9, "Invalid upload directory")
        val InvalidDownloadDirectory = FileErrorStatusCodes(10, "Invalid download directory")
    }
}

fun FileErrorStatusCodes.toResponse(
    httpStatusCode: HttpStatusCode,
) = BaseResponse(
    statusCode = httpStatusCode.value,
    errorCode = code,
    message = message,
    data = null
)