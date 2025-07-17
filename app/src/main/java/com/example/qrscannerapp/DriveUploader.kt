package com.example.qrscannerapp

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import java.io.File as JavaFile

object DriveUploader {

    private const val TAG = "DriveUploader"

    fun uploadFileAndGetId(context: Context, localFilePath: String, fileName: String, folderId: String, mimeType: String): String {
        val uploadedFileId: String
        try {
            val credentialsStream = context.assets.open("service_account.json")

            val credential = GoogleCredential
                .fromStream(credentialsStream)
                .createScoped(listOf("https://www.googleapis.com/auth/drive.file"))

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("QRScannerApp").build()

            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
            }

            val filePath = JavaFile(localFilePath)
            val mediaContent = FileContent(mimeType, filePath)

            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            uploadedFileId = uploadedFile.id // Obtener el ID del archivo

            Log.d(TAG, "✅ Archivo subido: ${uploadedFileId}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error subiendo archivo a Drive", e)
            throw e // Lanza la excepción para que el error se pueda manejar en la llamada
        }

        return uploadedFileId // Devuelve el ID del archivo
    }

    fun uploadFile(context: Context, localFilePath: String, fileName: String, folderId: String, mimeType: String) {
        try {
            val credentialsStream = context.assets.open("service_account.json")

            val credential = GoogleCredential
                .fromStream(credentialsStream)
                .createScoped(listOf("https://www.googleapis.com/auth/drive.file"))

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("QRScannerApp").build()

            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
            }

            val filePath = JavaFile(localFilePath)
            val mediaContent = FileContent(mimeType, filePath)

            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            Log.d(TAG, "✅ Archivo subido: ${uploadedFile.id}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error subiendo archivo a Drive", e)
        }
    }
}