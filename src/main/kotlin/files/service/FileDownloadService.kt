package dev.achmad.files.service

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileDownloadService {

    /**
     * Creates a zip file containing all files from the specified directory.
     *
     * @param directory The relative directory path to zip (inside the uploads folder)
     * @return The absolute path to the created zip file, or null if directory is empty or doesn't exist
     */
    suspend fun zipDirectory(directory: String): String? {
        val directoryToZip = File("uploads/$directory")
        
        // Check if directory exists and is not empty
        if (!directoryToZip.exists() || !directoryToZip.isDirectory || directoryToZip.listFiles()?.isEmpty() == true) {
            return null
        }
        
        // Create zip file in the same parent directory
        val zipFileName = "${directory.replace("/", "_")}.zip"
        val zipFilePath = Paths.get("uploads", zipFileName).toString()
        val zipFile = File(zipFilePath)
        
        // Create parent directories if they don't exist
        zipFile.parentFile?.mkdirs()
        
        // Create the zip file
        withContext(Dispatchers.IO) {
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                zipDirectoryContent(directoryToZip, directoryToZip.name, zipOut)
            }
        }
        
        return if (zipFile.exists() && zipFile.length() > 0) zipFile.absolutePath else null
    }
    
    /**
     * Recursive function to add directory contents to the zip file
     */
    private fun zipDirectoryContent(fileToZip: File, fileName: String, zipOut: ZipOutputStream) {
        if (fileToZip.isHidden) {
            return
        }
        
        if (fileToZip.isDirectory) {
            // If it's a directory, add its content recursively
            val children = fileToZip.listFiles() ?: return
            
            for (childFile in children) {
                val childPath = if (fileName.isEmpty()) childFile.name else "$fileName/${childFile.name}"
                zipDirectoryContent(childFile, childPath, zipOut)
            }
        } else {
            // If it's a file, add it to the zip
            FileInputStream(fileToZip).use { fis ->
                val zipEntry = ZipEntry(fileName)
                zipOut.putNextEntry(zipEntry)
                
                val buffer = ByteArray(1024)
                var length: Int
                
                while (fis.read(buffer).also { length = it } >= 0) {
                    zipOut.write(buffer, 0, length)
                }
            }
        }
    }
}