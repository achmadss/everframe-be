package dev.achmad.file_upload.model

enum class FileUploadStatus {
    PENDING, ASSEMBLED, COMPLETED, UNKNOWN;
    operator fun invoke(raw: String): FileUploadStatus {
        return entries.find { it.name == raw } ?: UNKNOWN
    }
}