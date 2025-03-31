package dev.achmad.file_upload.controller

import io.ktor.server.application.Application
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

object FileDownloadController {

    fun configure(application: Application) {
        with(application) {
            routing {
                route("/api/v1/downloads") {
                    get("/{directory}") {

                    }
                }
            }
        }
    }

}