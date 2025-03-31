package dev.achmad

import dev.achmad.common.model.BaseResponse
import dev.achmad.files.controller.FileUploadController
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import java.io.File
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    configureControllers()
    configureDatabase()
    configureUploadDirectory()
    configureSerialization()
    configureCORS()
    configureStatusPages()
}

fun Application.configureControllers() {
    FileUploadController.configure(this)
}

fun Application.configureDatabase() {
    val dbHost = System.getenv("POSTGRES_HOST") ?: "localhost"
    val dbPort = System.getenv("POSTGRES_PORT") ?: "5432"
    val dbName = System.getenv("POSTGRES_DB") ?: "everframe_db"
    val dbUser = System.getenv("POSTGRES_USER") ?: "everframe"
    val dbPassword = System.getenv("POSTGRES_PASSWORD") ?: "everframe"
    val dbUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"
    Database.connect(
        url = dbUrl,
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword,
    )
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

fun Application.configureUploadDirectory() {
    val uploadsDir = File("uploads")
    if (!uploadsDir.exists()) {
        uploadsDir.mkdir()
    }
}

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
}

fun Application.configureCORS() {
    install(CORS) {
        anyMethod()
        anyHost()
        headers.apply {
            add(HttpHeaders.Authorization)
            add(HttpHeaders.ContentType)
        }
    }
}

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                BaseResponse(
                    statusCode = HttpStatusCode.InternalServerError.value,
                    errorCode = -1,
                    message = cause.message ?: "Unknown Error",
                    data = null
                )
            )
        }
    }
}