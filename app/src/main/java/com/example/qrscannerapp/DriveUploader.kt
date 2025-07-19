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
        Log.d(TAG, "🚀 Iniciando uploadFileAndGetId")
        Log.d(TAG, "📥 Parámetros recibidos:")
        Log.d(TAG, "  - localFilePath: $localFilePath")
        Log.d(TAG, "  - fileName: $fileName")
        Log.d(TAG, "  - folderId: $folderId")
        Log.d(TAG, "  - mimeType: $mimeType")

        val uploadedFileId: String
        try {
            Log.d(TAG, "🔑 Abriendo archivo de credenciales service_account.json")
            val credentialsStream = try {
                context.assets.open("service_account.json")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al abrir service_account.json: ${e.message}", e)
                throw e
            }

            Log.d(TAG, "🔐 Creando credenciales de Google")
            val credential = GoogleCredential
                .fromStream(credentialsStream)
                .createScoped(listOf("https://www.googleapis.com/auth/drive.file"))
            Log.d(TAG, "📧 client_email de la cuenta de servicio: ${credential.serviceAccountId}")

            Log.d(TAG, "🌐 Inicializando servicio de Google Drive")
            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("QRScannerApp").build()

            Log.d(TAG, "📝 Creando metadatos del archivo: name=$fileName, parents=$folderId")
            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
            }

            Log.d(TAG, "📂 Preparando archivo local: $localFilePath")
            val filePath = JavaFile(localFilePath)
            Log.d(TAG, "🔍 Verificando archivo local: existe=${filePath.exists()}, tamaño=${filePath.length()} bytes")
            if (!filePath.exists()) {
                Log.e(TAG, "❌ El archivo no existe: $localFilePath")
                throw IllegalStateException("Archivo no encontrado: $localFilePath")
            }
            val mediaContent = FileContent(mimeType, filePath)

            Log.d(TAG, "📤 Subiendo archivo a Google Drive")
            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            uploadedFileId = uploadedFile.id
            Log.d(TAG, "✅ Archivo subido exitosamente con ID: $uploadedFileId")

            // Añadir permisos para que el archivo sea accesible
            Log.d(TAG, "🔓 Configurando permisos 'anyone:reader' para el archivo: $uploadedFileId")
            val permission = com.google.api.services.drive.model.Permission().apply {
                type = "anyone"
                role = "reader"
            }
            try {
                driveService.permissions().create(uploadedFileId, permission).execute()
                Log.d(TAG, "✅ Permisos configurados para el archivo: $uploadedFileId")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al configurar permisos: ${e.message}", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en uploadFileAndGetId: ${e.message}", e)
            if (e.message?.contains("invalid_grant") == true) {
                Log.e(TAG, "⚠️ Error de autenticación: Firma JWT inválida. Verifica service_account.json, permisos de la cuenta de servicio, o la sincronización del reloj del dispositivo.")
            }
            throw e
        }

        Log.d(TAG, "🔙 Retornando uploadedFileId: $uploadedFileId")
        return uploadedFileId
    }

    fun uploadFile(context: Context, localFilePath: String, fileName: String, folderId: String, mimeType: String) {
        Log.d(TAG, "🚀 Iniciando uploadFile")
        Log.d(TAG, "📥 Parámetros recibidos:")
        Log.d(TAG, "  - localFilePath: $localFilePath")
        Log.d(TAG, "  - fileName: $fileName")
        Log.d(TAG, "  - folderId: $folderId")
        Log.d(TAG, "  - mimeType: $mimeType")

        try {
            Log.d(TAG, "🔑 Abriendo archivo de credenciales service_account.json")
            val credentialsStream = try {
                context.assets.open("service_account.json")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al abrir service_account.json: ${e.message}", e)
                throw e
            }

            Log.d(TAG, "🔐 Creando credenciales de Google")
            val credential = GoogleCredential
                .fromStream(credentialsStream)
                .createScoped(listOf("https://www.googleapis.com/auth/drive.file"))
            Log.d(TAG, "📧 client_email de la cuenta de servicio: ${credential.serviceAccountId}")

            Log.d(TAG, "🌐 Inicializando servicio de Google Drive")
            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("QRScannerApp").build()

            Log.d(TAG, "📝 Creando metadatos del archivo: name=$fileName, parents=$folderId")
            val fileMetadata = File().apply {
                name = fileName
                parents = listOf(folderId)
            }

            Log.d(TAG, "📂 Preparando archivo local: $localFilePath")
            val filePath = JavaFile(localFilePath)
            Log.d(TAG, "🔍 Verificando archivo local: existe=${filePath.exists()}, tamaño=${filePath.length()} bytes")
            if (!filePath.exists()) {
                Log.e(TAG, "❌ El archivo no existe: $localFilePath")
                throw IllegalStateException("Archivo no encontrado: $localFilePath")
            }
            val mediaContent = FileContent(mimeType, filePath)

            Log.d(TAG, "📤 Subiendo archivo a Google Drive")
            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            Log.d(TAG, "✅ Archivo subido exitosamente con ID: ${uploadedFile.id}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en uploadFile: ${e.message}", e)
            if (e.message?.contains("invalid_grant") == true) {
                Log.e(TAG, "⚠️ Error de autenticación: Firma JWT inválida. Verifica service_account.json, permisos de la cuenta de servicio, o la sincronización del reloj del dispositivo.")
            }
            throw e
        }
    }
}