import dev.achmad.common.model.BaseResponse
import dev.achmad.files.model.dto.UploadSessionRequest
import dev.achmad.module
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.*
import io.ktor.serialization.gson.gson
import io.ktor.server.testing.*
import java.io.File
import java.nio.file.Paths
import java.util.UUID
import kotlin.test.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue

class FileUploadDownloadIntegrationTest {

    private val testDirectory = "test_images"
    private val testFileName = "tanya.jpg"
    private val testFileSize = 273584L // 273584 bytes
    private val testMimeType = "image/jpeg"
    private val testDirectoryUpload = "achmad"
    private val testTotalChunks = 1
    private var sessionId: UUID? = null
    private lateinit var testFile: File
    private lateinit var testDir: File

    private fun setup() {

        // Get the absolute path to the test resources directory
        val testResourcesPath = Paths.get("").toAbsolutePath().resolve(testDirectory).toString()
        testDir = File(testResourcesPath)

        if (!testDir.exists()) {
            testDir.mkdirs()
        }

        testFile = File(testDir, testFileName)
        if (!testFile.exists()) {
            testFile.createNewFile()
            testFile.writeBytes(ByteArray(273584)) // 273584 bytes
        }
    }

    @Test
    fun testRoot() = testApplication {
        setup()

        val httpClient = createClient {
            install(ContentNegotiation) {
                gson {  }
            }
        }

        application {
            module()
        }

        // 1. Create a new file upload session
        val createSessionResponse = httpClient.createUploadSession(
            testDirectoryUpload,
            testFileName,
            testFileSize,
            testMimeType,
            testTotalChunks
        )
        Assertions.assertEquals(HttpStatusCode.Created, createSessionResponse.status)
        val sessionData = createSessionResponse.body<BaseResponse<Map<String, String>>>().data
        sessionId = UUID.fromString(sessionData?.get("sessionId"))
        assertTrue(sessionId != null)

        // 2. Upload the file chunk
        val uploadChunkResponse = httpClient.uploadFileChunk(
            testDirectoryUpload,
            sessionId!!,
            0,
            testFile,
            testMimeType
        )
        Assertions.assertEquals(HttpStatusCode.OK, uploadChunkResponse.status)

        // 3. Assemble the uploaded chunks
        val assembleResponse = httpClient.assembleFile(sessionId!!)
        Assertions.assertEquals(HttpStatusCode.OK, assembleResponse.status)

        // 4. Download the file as zip
        val downloadResponse = httpClient.downloadFileAsZip(testDirectoryUpload)
        Assertions.assertEquals(HttpStatusCode.OK, downloadResponse.status)

        // Verify the downloaded zip file
        val downloadedZipFile = File("downloaded.zip")
        downloadedZipFile.createNewFile()
        downloadedZipFile.appendBytes(downloadResponse.bodyAsBytes())
        assertTrue(downloadedZipFile.length() > 0)
        downloadedZipFile.delete()

        // Delete the session
        val deleteSessionResponse = httpClient.deleteSession(testDirectoryUpload, sessionId!!)
        Assertions.assertEquals(HttpStatusCode.OK, deleteSessionResponse.status)

        // delete uploads content dir
        val uploads = File("uploads")
        uploads.deleteRecursively()
    }

    private suspend fun HttpClient.createUploadSession(
        directory: String,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        totalChunks: Int
    ): HttpResponse {
        return post("/api/v1/uploads/session") {
            contentType(ContentType.Application.Json)
            setBody(
                UploadSessionRequest(
                    directory = directory,
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = mimeType,
                    totalChunks = totalChunks
                )
            )
        }
    }

    private suspend fun HttpClient.uploadFileChunk(
        directory: String,
        sessionId: UUID,
        chunkIndex: Int,
        file: File,
        mimeType: String,
    ): HttpResponse {
        val bytes = file.readBytes()
        return submitFormWithBinaryData(
            url = "/api/v1/uploads/chunk/$directory/$sessionId/$chunkIndex",
            formData = formData {
                append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentType, mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                })
            }
        )
    }

    private suspend fun HttpClient.assembleFile(sessionId: UUID): HttpResponse {
        return post("/api/v1/uploads/assemble/$sessionId")
    }

    private suspend fun HttpClient.downloadFileAsZip(directory: String): HttpResponse {
        return get("/api/v1/downloads/$directory")
    }

    private suspend fun HttpClient.deleteSession(directory: String, sessionId: UUID): HttpResponse {
        return delete("/api/v1/uploads/session/$directory/$sessionId")
    }

}
