package dev.achmad.files.controller

import dev.achmad.FileErrorStatusCodes
import dev.achmad.common.model.BaseResponse
import dev.achmad.files.model.FileUploadStatus
import dev.achmad.files.model.dto.UploadSessionRequest
import dev.achmad.files.service.FileUploadService
import dev.achmad.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
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

                    post("/chunk/{directory}/{sessionId}/{chunkIndex}") {
                        val directory = call.parameters["directory"]
                        if (directory == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileErrorStatusCodes.InvalidUploadDirectory.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@post
                        }

                        val sessionId = try {
                            UUID.fromString(call.parameters["sessionId"])
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileErrorStatusCodes.InvalidSessionID.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@post
                        }

                        val chunkIndex = call.parameters["chunkIndex"]?.toIntOrNull()
                        if (chunkIndex == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileErrorStatusCodes.InvalidChunkIndex.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@post
                        }

                        // First check if session exists
                        val sessionStatus = FileUploadService.getUploadStatus(sessionId)
                        if (sessionStatus == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                FileErrorStatusCodes.UploadSessionNotFound.toResponse(HttpStatusCode.NotFound)
                            )
                            return@post
                        }
                        if (sessionStatus.status == FileUploadStatus.ASSEMBLED.name) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileErrorStatusCodes.FileAlreadyUploaded.toResponse(HttpStatusCode.BadRequest)
                            )
                        }
                        if (chunkIndex < sessionStatus.uploadedChunks) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileErrorStatusCodes.ChunkAlreadyUploaded.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@post
                        }

                        val fileProcessed = FileUploadService.processMultipart(
                            data = call.receiveMultipart(),
                            sessionId = sessionId,
                            directory = directory,
                            chunkIndex = chunkIndex
                        )

                        if (fileProcessed) {
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
                                FileErrorStatusCodes.UploadSessionNotFoundOrCancelled.toResponse(HttpStatusCode.NotFound)
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileErrorStatusCodes.NoFilePart.toResponse(HttpStatusCode.BadRequest)
                            )
                        }
                    }

                    get("/session/{sessionId}") {
                        val sessionId = try {
                            UUID.fromString(call.parameters["sessionId"])
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileErrorStatusCodes.InvalidSessionID.toResponse(HttpStatusCode.BadRequest)
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
                                FileErrorStatusCodes.UploadSessionNotFound.toResponse(HttpStatusCode.NotFound)
                            )
                        }
                    }

                    delete("/session/{sessionId}") {
                        val directory = call.parameters["directory"]
                        if (directory == null) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileErrorStatusCodes.InvalidUploadDirectory.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@delete
                        }

                        val sessionId = try {
                            UUID.fromString(call.parameters["sessionId"])
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileErrorStatusCodes.InvalidSessionID.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@delete
                        }

                        if (FileUploadService.cancelUpload(directory, sessionId)) {
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
                                FileErrorStatusCodes.UploadSessionNotFound.toResponse(HttpStatusCode.NotFound)
                            )
                        }
                    }

                    post("/assemble/{sessionId}") {
                        val sessionId = try {
                            UUID.fromString(call.parameters["sessionId"])
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileErrorStatusCodes.InvalidSessionID.toResponse(HttpStatusCode.BadRequest)
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
                                FileErrorStatusCodes.UploadSessionNotFoundOrIncomplete.toResponse(HttpStatusCode.NotFound)
                            )
                        }
                    }
                }
            }
        }
    }

}