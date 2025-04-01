package dev.achmad.files.controller

import dev.achmad.FileErrorStatusCodes
import dev.achmad.files.service.FileDownloadService
import dev.achmad.toResponse
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.io.File

object FileDownloadController {

    fun configure(application: Application) {
        with(application) {
            routing {
                route("/api/v1/downloads") {
                    get("/{directory}") {
                        val directory = call.parameters["directory"]
                        
                        if (directory.isNullOrBlank()) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                FileErrorStatusCodes.InvalidDownloadDirectory.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@get
                        }
                        
                        val zipFilePath = FileDownloadService.zipDirectory(directory)
                        
                        if (zipFilePath == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                FileErrorStatusCodes.InvalidDownloadDirectory.toResponse(HttpStatusCode.BadRequest)
                            )
                            return@get
                        }
                        
                        val zipFile = File(zipFilePath)
                        val fileName = zipFile.name
                        
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Attachment.withParameter(
                                ContentDisposition.Parameters.FileName, fileName
                            ).toString()
                        )
                        
                        call.respondFile(zipFile)
                    }
                }
            }
        }
    }
}