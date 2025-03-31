package dev.achmad.files.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.datetime

object FileUpload: UUIDTable() {
    val directory = text("directory")
    val fileName = text("file_name")
    val fileSize = long("file_size")
    val mimeType = varchar("mime_type", 100)
    val totalChunks = integer("total_chunks")
    val uploadedChunks = integer("uploaded_chunks").default(0)
    val status = varchar("status", 50).default(FileUploadStatus.PENDING.name)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}
