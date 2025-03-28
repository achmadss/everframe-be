package dev.achmad.file_upload.controller

import dev.achmad.FileUploadStatusCodes
import dev.achmad.common.model.BaseResponse
import dev.achmad.file_upload.model.FileUploadStatus
import dev.achmad.file_upload.model.dto.UploadSessionRequest
import dev.achmad.file_upload.service.FileUploadService
import dev.achmad.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.io.File
import java.util.UUID

object FileUploadController {

    fun configure(application: Application) {
        with(application) {
            routing {
                route("/api/v1/uploads") {

                    post("/session") {
                        val request = call.receive<UploadSessionRequest>()
                        val sessionId = FileUploadService.createUpload(request)
                        call.respond(
                            HttpStatusCode.Created,
                            BaseResponse(
                                statusCode = HttpStatusCode.Created.value,
                                data = mapOf(
                                    "sessionId" to sessionId
                                )
                            )
                        )
                    }

                    post("/chunk/{sessionId}/{chunkIndex}") {
                        val sessionId = try {
                            UUID.fromString(call.parameters["sessionId"])
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileUploadStatusCodes.InvalidSessionID.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@post
                        }

                        val chunkIndex = call.parameters["chunkIndex"]?.toIntOrNull()
                        if (chunkIndex == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileUploadStatusCodes.InvalidChunkIndex.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@post
                        }

                        // First check if session exists
                        val sessionStatus = FileUploadService.getUploadStatus(sessionId)
                        if (sessionStatus == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                FileUploadStatusCodes.UploadSessionNotFound.toResponse(HttpStatusCode.NotFound)
                            )
                            return@post
                        }
                        if (sessionStatus.status == FileUploadStatus.ASSEMBLED.name) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileUploadStatusCodes.FileAlreadyUploaded.toResponse(HttpStatusCode.BadRequest)
                            )
                        }
                        if (chunkIndex < sessionStatus.uploadedChunks) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileUploadStatusCodes.ChunkAlreadyUploaded.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@post
                        }

                        val multipart = call.receiveMultipart()
                        var fileProcessed = false
                        var chunkFile: File? = null

                        multipart.forEachPart { part ->
                            if (part is PartData.FileItem && !fileProcessed) {
                                chunkFile = File("uploads/$sessionId/chunk_$chunkIndex")

                                part.streamProvider().use { input ->
                                    chunkFile!!.outputStream().buffered().use { output ->
                                        input.copyTo(output)
                                    }
                                }

                                fileProcessed = true
                            }

                            part.dispose()
                        }

                        if (fileProcessed && chunkFile != null) {
                            val response = FileUploadService.saveChunk(sessionId, chunkIndex)
                            if (response != null) {
                                call.respond(
                                    HttpStatusCode.OK,
                                    BaseResponse(
                                        statusCode = HttpStatusCode.OK.value,
                                        data = response
                                    )
                                )
                            } else call.respond(
                                HttpStatusCode.NotFound,
                                FileUploadStatusCodes.UploadSessionNotFoundOrCancelled.toResponse(HttpStatusCode.NotFound)
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileUploadStatusCodes.NoFilePart.toResponse(HttpStatusCode.BadRequest)
                            )
                        }
                    }

                    get("/session/{sessionId}") {
                        val sessionId = try {
                            UUID.fromString(call.parameters["sessionId"])
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileUploadStatusCodes.InvalidSessionID.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@get
                        }

                        val status = FileUploadService.getUploadStatus(sessionId)
                        if (status != null) {
                            call.respond(
                                HttpStatusCode.OK,
                                BaseResponse(
                                    statusCode = HttpStatusCode.OK.value,
                                    data = status
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                FileUploadStatusCodes.UploadSessionNotFound.toResponse(HttpStatusCode.NotFound)
                            )
                        }
                    }

                    delete("/session/{sessionId}") {
                        val sessionId = try {
                            UUID.fromString(call.parameters["sessionId"])
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileUploadStatusCodes.InvalidSessionID.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@delete
                        }

                        if (FileUploadService.cancelUpload(sessionId)) {
                            call.respond(
                                HttpStatusCode.OK,
                                BaseResponse(
                                    statusCode = HttpStatusCode.OK.value,
                                    message = "Upload cancelled",
                                    data = null
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                FileUploadStatusCodes.UploadSessionNotFound.toResponse(HttpStatusCode.NotFound)
                            )
                        }
                    }

                    post("/assemble/{sessionId}") {
                        val sessionId = try {
                            UUID.fromString(call.parameters["sessionId"])
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileUploadStatusCodes.InvalidSessionID.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@post
                        }

                        val filePath = FileUploadService.assembleFile(sessionId)

                        if (filePath != null) {
                            call.respond(
                                HttpStatusCode.OK,
                                BaseResponse(
                                    statusCode = HttpStatusCode.OK.value,
                                    message = "File assembled successfully",
                                    data = null
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                FileUploadStatusCodes.UploadSessionNotFoundOrIncomplete.toResponse(HttpStatusCode.NotFound)
                            )
                        }
                    }
                }
            }
        }
    }

}