# Everframe - File Upload and Management Service

Everframe is a backend service built with Ktor, Kotlin, and Exposed that provides robust file upload, management, and retrieval capabilities. It's designed to handle large file uploads efficiently using a chunking mechanism and offers features for managing uploaded files.

## Features

*   **Chunked File Uploads:** Supports uploading large files by splitting them into smaller chunks, allowing for efficient and resumable uploads.
*   **Upload Session Management:** Creates and manages upload sessions, tracking the progress of each file upload.
*   **File Assembly:** Assembles uploaded chunks into a complete file once all chunks are received.
*   **File Status Tracking:** Provides real-time status updates on file uploads, including progress and completion status.
*   **File Cancellation:** Allows users to cancel in-progress file uploads.
*   **File Download:** Enables downloading of successfully assembled files or entire directories.
*   **Database Integration:** Uses PostgreSQL for persistent storage of file metadata and upload session information.
*   **RESTful API:** Exposes a RESTful API for interacting with the service.
*   **CORS Support:** Configured with Cross-Origin Resource Sharing (CORS) to allow requests from different domains.
*   **Error Handling:** Implements comprehensive error handling and returns appropriate HTTP status codes.
*   **Dockerized:** Ready to be deployed using Docker.

## Technologies Used

*   **Ktor:** A Kotlin framework for building asynchronous servers and clients.
*   **Kotlin:** A modern, statically typed programming language.
*   **Exposed:** A Kotlin SQL library for database access.
*   **PostgreSQL:** A powerful, open-source relational database.
*   **Gradle:** A build automation system.
*   **Netty:** A non-blocking I/O client-server framework.
*   **Gson:** A Java library to convert Java Objects into their JSON representation.
*   **Docker:** A platform for developing, shipping, and running applications in containers.

## Project Structure

*   `src/main/kotlin/`: Contains the main Kotlin source code.
    *   `Application.kt`: The main entry point for the Ktor application.
    *   `files/`: Contains the file upload and download logic.
        *   `controller/`: Defines the API endpoints.
        *   `model/`: Defines the data models and database schema.
        *   `service/`: Contains the business logic for file uploads and management.
*   `build.gradle.kts`: The Gradle build file.
*   `Dockerfile`: The Dockerfile for building a Docker image.
*   `gradle/wrapper/`: contains gradle wrapper.
*   `gradlew`: gradle wrapper executable.
*   `gradlew.bat`: gradle wrapper executable for windows.

## Getting Started

### Prerequisites

*   Java Development Kit (JDK) 17 or higher
*   Gradle
*   Docker (optional, for containerized deployment)
*   PostgreSQL (optional, for local development)

### Building and Running Locally

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/achmadss/everframe-be.git
    cd everframe/backend
    ```
2.  **Configure Database:**

    *   Ensure you have a PostgreSQL instance running.
    *   Update the database connection details in `Application.kt` if needed (or set environment variables `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`).
    *   The default values are:
        *   `POSTGRES_HOST`: localhost
        *   `POSTGRES_PORT`: 5432
        *   `POSTGRES_DB`: everframe_db
        *   `POSTGRES_USER`: everframe
        *   `POSTGRES_PASSWORD`: everframe

3.  **Build the project:**
    ```bash
    ./gradlew build
    ```
4.  **Run the application:**

    ```bash
    ./gradlew run
    ```
    or build a fat JAR and run it:

    ```bash
    ./gradlew buildFatJar
    java -jar build/libs/everframe.jar
    ```
5.  **Access the API:**

    The server will be running at `http://localhost:8080`.

### API Endpoints

#### File Uploads

*   **POST `/api/v1/uploads/session`:** Create a new upload session.
    *   Request Body: `UploadSessionRequest` (JSON):
    ```json
    {
        "directory": "my-uploads",
        "fileName": "my-file.txt",
        "fileSize": 102400,
        "mimeType": "text/plain",
        "totalChunks": 10
    }
    ```

    *   Response:
    ```json
    {
        "statusCode": 201,
        "data": {
            "sessionId": "a1b2c3d4-e5f6-7890-1234-567890abcdef"
        }
    }
    ```

    *   Response Codes:
        *   `201 Created`: Session created successfully.
        *   `400 Bad Request`: Invalid request body.
        *   `500 Internal Server Error`: An unexpected error occurred.


*   **POST `/api/v1/uploads/chunk/{directory}/{sessionId}/{chunkIndex}`:** Upload a chunk of a file.
    *   Request: `multipart/form-data` (file part named `file`)
    *   Path Parameters:
        *   `directory`: The directory where the file is being uploaded.
        *   `sessionId`: The ID of the upload session.
        *   `chunkIndex`: The index of the chunk being uploaded (starting from 0).
    *   Response:
        ```json
        {
            "statusCode": 200,
            "data": {
                "sessionId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
                "chunkIndex": 0,
                "uploadedChunks": 1,
                "completed": false
            }
        }
        ```

    *   Response Codes:
        *   `200 OK`: Chunk uploaded successfully.
        *   `400 Bad Request`: Invalid directory, session ID, chunk index, or no file part.
        *   `404 Not Found`: Upload session not found or cancelled.
        * `400 Bad Request`: Chunk already uploaded.
        * `400 Bad Request`: File already uploaded.
        *   `500 Internal Server Error`: An unexpected error occurred.


*   **GET `/api/v1/uploads/session/{sessionId}`:** Get the status of an upload session.
    *   Path Parameters:
        *   `sessionId`: The ID of the upload session.
    *   Response:
        ```json
            {
                "statusCode": 200,
                "data": {
                "sessionId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
                "fileName": "my-file.txt",
                "fileSize": 102400,
                "totalChunks": 10,
                "uploadedChunks": 5,
                "status": "IN_PROGRESS",
                "progress": 0.5
            }
        }
        ```

    *   Response Codes:
        *   `200 OK`: Session status retrieved successfully.
        *   `400 Bad Request`: Invalid session ID.
        *   `404 Not Found`: Upload session not found.
        *   `500 Internal Server Error`: An unexpected error occurred.


*   **DELETE `/api/v1/uploads/session/{directory}/{sessionId}`:** Cancel an upload session.
    *   Path Parameters:
        *   `directory`: The directory where the file is being uploaded.
        *   `sessionId`: The ID of the upload session.
    *   Response:
        ```json
        {
            "statusCode": 200,
            "message": "Upload cancelled",
            "data": null
        }
        ```

    *   Response Codes:
        *   `200 OK`: Upload cancelled successfully.
        *   `400 Bad Request`: Invalid directory or session ID.
        *   `404 Not Found`: Upload session not found.
        *   `500 Internal Server Error`: An unexpected error occurred.


*   **POST `/api/v1/uploads/assemble/{sessionId}`:** Assemble the file.
    *   Path Parameters:
        *   `sessionId`: The ID of the upload session.
    *   Response:
        ```json
        {
            "statusCode": 200,
            "message": "File assembled successfully",
            "data": null
        }
        ```

    *   Response Codes:
        *   `200 OK`: File assembled successfully.
        *   `400 Bad Request`: Invalid session ID.
        *   `404 Not Found`: Upload session not found or incomplete.
        *   `500 Internal Server Error`: An unexpected error occurred.

#### File Downloads

*   **GET `/api/v1/downloads/{directory}`:** Download a zip file of the directory.
    *   Path Parameters:
        *   `directory`: The directory to download.
    *   Response:
        *   A zip file containing the contents of the specified directory.
    *   Response Codes:
        *   `200 OK`: File downloaded successfully.
        *   `400 Bad Request`: Invalid directory.
        *   `404 Not Found`: Directory not found.
        *   `500 Internal Server Error`: An unexpected error occurred.

## Contributing

Contributions are welcome! Please feel free to open issues or submit pull requests.