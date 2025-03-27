package dev.achmad.file_upload.service

import dev.achmad.dbQuery
import dev.achmad.file_upload.model.FileUpload
import dev.achmad.file_upload.model.FileUploadStatus
import dev.achmad.file_upload.model.dto.UploadSessionRequest
import file_upload.model.dto.ChunkUploadResponse
import file_upload.model.dto.UploadStatusResponse
import java.io.File
import java.time.LocalDateTime
import java.util.UUID
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object FileUploadService {

    init {
        transaction {
            SchemaUtils.create(FileUpload)
        }
    }

    suspend fun createUpload(request: UploadSessionRequest): String {
        return dbQuery {
            val now = LocalDateTime.now()
            val sessionId = FileUpload.insert {
                it[fileName] = request.fileName
                it[fileSize] = request.fileSize
                it[mimeType] = request.mimeType
                it[totalChunks] = request.totalChunks
                it[uploadedChunks] = 0
                it[createdAt] = now
                it[updatedAt] = now
            } get FileUpload.id

            // Create directory for chunks
            val sessionDir = File("uploads/${sessionId}")
            if (!sessionDir.exists()) {
                sessionDir.mkdir()
            }

            sessionId.value.toString()
        }
    }

    suspend fun saveChunk(
        sessionId: UUID,
        chunkIndex: Int,
    ): ChunkUploadResponse? {
        return dbQuery {
            // Check if session exists and is not cancelled
            val session = FileUpload.selectAll()
                .where { (FileUpload.id eq sessionId) and (FileUpload.status neq FileUploadStatus.UNKNOWN.name) }
                .singleOrNull() ?: return@dbQuery null

            // Update upload session with incremented chunk count
            val newUploadedChunks = session[FileUpload.uploadedChunks] + 1
            val totalChunks = session[FileUpload.totalChunks]

            FileUpload.update({ FileUpload.id eq sessionId }) {
                it[uploadedChunks] = newUploadedChunks
                it[updatedAt] = LocalDateTime.now()

                // Update status if complete
                if (newUploadedChunks >= totalChunks) {
                    it[status] = FileUploadStatus.COMPLETED.name
                }
            }

            ChunkUploadResponse(
                sessionId = sessionId.toString(),
                chunkIndex = chunkIndex,
                uploadedChunks = newUploadedChunks,
                completed = newUploadedChunks >= totalChunks
            )
        }
    }

    suspend fun getUploadStatus(sessionId: UUID): UploadStatusResponse? {
        return dbQuery {
            val session = FileUpload.selectAll()
                .where { FileUpload.id eq sessionId and (FileUpload.status neq FileUploadStatus.UNKNOWN.name) }
                .singleOrNull() ?: return@dbQuery null

            val totalChunks = session[FileUpload.totalChunks]
            val uploadedChunks = session[FileUpload.uploadedChunks]
            val progress =
                if (totalChunks > 0) uploadedChunks.toDouble() / totalChunks
                else 0.0

            UploadStatusResponse(
                sessionId = sessionId.toString(),
                fileName = session[FileUpload.fileName],
                fileSize = session[FileUpload.fileSize],
                totalChunks = totalChunks,
                uploadedChunks = uploadedChunks,
                status = session[FileUpload.status],
                progress = progress
            )
        }
    }

    suspend fun cancelUpload(sessionId: UUID): Boolean {
        return dbQuery {
            val rows = FileUpload.deleteWhere { FileUpload.id eq sessionId }
            if (rows < 1) return@dbQuery false
            val file = File("uploads/${sessionId}")
            file.deleteRecursively()
        }
    }

    suspend fun assembleFile(sessionId: UUID): String? {
        return dbQuery {
            val session = FileUpload.selectAll()
                .where { FileUpload.id eq sessionId and (FileUpload.status eq FileUploadStatus.UNKNOWN.name) }
                .singleOrNull() ?: return@dbQuery null

            val status = session[FileUpload.status]
            if (status != FileUploadStatus.COMPLETED.name){
                return@dbQuery null
            }

            val fileName = session[FileUpload.fileName]
            val totalChunks = session[FileUpload.totalChunks]
            val uploadedChunks = session[FileUpload.uploadedChunks]

            if (uploadedChunks < totalChunks) {
                return@dbQuery null
            }

            val outputFile = File("uploads/$sessionId/$fileName")
            outputFile.createNewFile()

            for (i in 0 until totalChunks) {
                val chunkFile = File("uploads/$sessionId/chunk_$i")
                if (chunkFile.exists()) {
                    outputFile.appendBytes(chunkFile.readBytes())
                    chunkFile.delete()
                } else {
                    return@dbQuery null
                }
            }

            FileUpload.update({ FileUpload.id eq sessionId }) {
                it[this.status] = FileUploadStatus.ASSEMBLED.name
                it[updatedAt] = LocalDateTime.now()
            }

            outputFile.absolutePath
        }
    }

}