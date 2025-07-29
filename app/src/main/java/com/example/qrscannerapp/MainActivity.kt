package com.example.qrscannerapp

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.runtime.LaunchedEffect
import com.example.qrscannerapp.ui.theme.QRScannerAppTheme
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.URL
import java.net.HttpURLConnection

private var mostrarListaPedidos by mutableStateOf(false)
private val photoFiles = mutableListOf<File>()
private var numeroFotoActual = 1
data class PedidoConFecha(val pedido: Pedido, val fechaCreacion: Long)

class MainActivity : ComponentActivity() {
    private var qrResult by mutableStateOf<String?>(null)
    private var photoFile: File? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null && qrResult != null) {
            savePhotoToGalleryAndBackup(qrResult!!, photoFile!!)
            Toast.makeText(this, "✅ Foto $numeroFotoActual guardada en galería y respaldo", Toast.LENGTH_SHORT).show()
            numeroFotoActual++
        } else {
            Toast.makeText(this, "❌ No se tomó la foto o hubo un error", Toast.LENGTH_SHORT).show()
        }
    }

    private val barcodeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val contents = result.data?.getStringExtra("SCAN_RESULT")
        if (contents != null) {
            qrResult = contents
            println("Código QR escaneado: $contents")
        }
    }

    private fun savePhotoToGalleryAndBackup(remito: String, photoFile: File) {
        val photoName = "foto_${remito}_${numeroFotoActual}.jpg"
        val timestamp = System.currentTimeMillis()

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, photoName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, timestamp)
            put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRScannerApp")
            } else {
                put(MediaStore.Images.Media.DATA, photoFile.absolutePath)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                File(photoFile.absolutePath).inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        val backupDir = File(getExternalFilesDir(null), "Fotos")
        if (!backupDir.exists()) backupDir.mkdirs()

        val backupFile = File(backupDir, photoName)
        try {
            File(photoFile.absolutePath).copyTo(backupFile, overwrite = true)
            photoFiles.remove(photoFile)
            photoFiles.add(backupFile)
            photoFile?.delete()
        } catch (e: IOException) {
            Log.e("SAVE_PHOTO", "❌ Error al guardar copia de respaldo: ${e.message}")
        }
    }

    private fun capturarFoto(remito: String) {
        if (numeroFotoActual > 3) {
            Toast.makeText(this, "📷 Ya se tomaron las 3 fotos", Toast.LENGTH_SHORT).show()
            return
        }

        val nombreArchivo = "temp_foto_${remito}_${numeroFotoActual}.jpg"
        val archivo = File(getExternalFilesDir(null), nombreArchivo)
        photoFile = archivo
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", archivo)
        takePictureLauncher.launch(uri)
    }

    private fun startQrScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Escanea el código QR")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity::class.java)
        barcodeLauncher.launch(options.createScanIntent(this))
    }

    private fun cargarPedidosGuardados(): List<PedidoConFecha> {
        val pedidos = mutableListOf<PedidoConFecha>()
        val filesDir = getExternalFilesDir(null)
        val gson = Gson()

        filesDir?.listFiles()?.forEach { file ->
            if (file.name.startsWith("pedido_") && file.extension == "json") {
                try {
                    val timestamp = file.name.removePrefix("pedido_").removeSuffix(".json").toLong()
                    val contenido = file.readText()
                    val pedido = gson.fromJson(contenido, Pedido::class.java)
                    pedidos.add(PedidoConFecha(pedido, timestamp))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return pedidos.sortedByDescending { it.fechaCreacion }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startQrScanner()
        } else {
            Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startQrScanner()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                Toast.makeText(
                    this,
                    "Se necesita permiso de cámara para escanear QR",
                    Toast.LENGTH_SHORT
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun enviarPedidoAGoogleSheets(pedido: Pedido, sheetUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(sheetUrl)
                val json = Gson().toJson(pedido)

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                connection.outputStream.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("GOOGLE_SHEETS", "✅ Enviado correctamente")
                } else {
                    Log.e("GOOGLE_SHEETS", "❌ Error al enviar: Código $responseCode")
                }
            } catch (e: Exception) {
                Log.e("GOOGLE_SHEETS", "❌ Excepción al enviar: ${e.message}")
            }
        }
    }

    private fun enviarPedidoAGoogleSheetsConFecha(pedido: Pedido, fechaCreacion: Long, sheetUrl: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(sheetUrl)
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fechaCreacion))

                val jsonMap = mutableMapOf<String, Any>(
                    "remito" to pedido.remito,
                    "fecha" to dateStr,
                    "cliente" to pedido.cliente,
                    "cajas" to pedido.cajas,
                    "bolsas" to pedido.cantidadBolsas,
                    "bolsones" to pedido.bolsones,
                    "armado" to pedido.armado,
                    "revisa" to pedido.revisa,
                    "transporte" to pedido.transporte,
                    "observaciones" to pedido.observaciones
                )
                Log.d("GOOGLE_SHEETS", "Enviando JSON: ${Gson().toJson(jsonMap)}")

                val gson = Gson()
                val json = gson.toJson(jsonMap)

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { os -> os.write(json.toByteArray(Charsets.UTF_8)) }

                val responseCode = connection.responseCode
                Log.d("GOOGLE_SHEETS", "Respuesta: Código $responseCode")
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("GOOGLE_SHEETS", "✅ Enviado con fecha original correctamente")
                } else {
                    Log.e("GOOGLE_SHEETS", "❌ Error al enviar con fecha: Código $responseCode")
                }
            } catch (e: Exception) {
                Log.e("GOOGLE_SHEETS", "❌ Excepción al enviar con fecha: ${e.message}", e)
            }
        }
    }

    private fun sincronizarPedidosPendientes(context: Context, sheetUrl: String) {
        val pedidos = cargarPedidosGuardados()

        CoroutineScope(Dispatchers.IO).launch {
            for (pedidoConFecha in pedidos) {
                val pedido = pedidoConFecha.pedido
                val jsonFile = File(getExternalFilesDir(null), "pedido_${pedidoConFecha.fechaCreacion}.json")
                val sentMarker = File(getExternalFilesDir(null), "pedido_${pedidoConFecha.fechaCreacion}.sent")

                if (!sentMarker.exists() && jsonFile.exists()) {
                    enviarPedidoAGoogleSheetsConFecha(pedido, pedidoConFecha.fechaCreacion, sheetUrl)

                    try {
                        DriveUploader.uploadFile(
                            context,
                            jsonFile.absolutePath,
                            jsonFile.name,
                            "1rofvNaKGrqnw163RNw2YoxnKTgDqqrlY",
                            "application/json"
                        )
                    } catch (e: Exception) {
                        Log.e("SYNC_JSON", "❌ Error al subir JSON: ${e.message}")
                    }

                    pedido.fotosPath.forEachIndexed { index, photoPath ->
                        val file = File(photoPath)
                        if (file.exists()) {
                            val nombreFoto = "foto_${pedido.remito}_${index + 1}.jpg"
                            try {
                                DriveUploader.uploadFile(
                                    context,
                                    photoPath,
                                    nombreFoto,
                                    "1rofvNaKGrqnw163RNw2YoxnKTgDqqrlY",
                                    "image/jpeg"
                                )
                            } catch (e: Exception) {
                                Log.e("SYNC_PHOTO", "❌ Error al subir foto: ${e.message}")
                            }
                        }
                    }

                    sentMarker.writeText("enviado")
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "✅ Pedidos sincronizados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    override fun onResume() {
        super.onResume()
        setContent {
            QRScannerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when {
                        qrResult != null -> {
                            numeroFotoActual = 1
                            photoFiles.clear()

                            FormularioDespacho(
                                qrData = qrResult!!,
                                onGuardar = { fecha: String, cliente: String, cajas: String, bolsas: String, bolsones: String, armado: String, revisa: String, transporte: String, observaciones: String ->
                                    val folderId = "1rofvNaKGrqnw163RNw2YoxnKTgDqqrlY"
                                    val uploadedUrls = mutableListOf<String>()

                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            photoFiles.forEachIndexed { index, photoFile ->
                                                val photoFileName = "foto_${qrResult}_${index + 1}.jpg"
                                                val uploadedPhotoId = DriveUploader.uploadFileAndGetId(
                                                    context = this@MainActivity,
                                                    localFilePath = photoFile.absolutePath,
                                                    fileName = photoFileName,
                                                    folderId = folderId,
                                                    mimeType = "image/jpeg"
                                                )
                                                uploadedUrls.add("https://drive.google.com/file/d/$uploadedPhotoId/view")
                                            }

                                            val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
                                            val sheetUrl = sharedPreferences.getString("dispatch_sheet_url", "") ?: ""

                                            if (sheetUrl.isEmpty()) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(this@MainActivity, "❌ URL de Google Sheets no configurada", Toast.LENGTH_LONG).show()
                                                }
                                                return@launch
                                            }

                                            val pedido = Pedido(
                                                remito = qrResult!!,
                                                fecha = fecha,
                                                cliente = cliente,
                                                cajas = cajas,
                                                cantidadBolsas = bolsas,
                                                bolsones = bolsones,
                                                armado = armado,
                                                revisa = revisa,
                                                transporte = transporte,
                                                observaciones = observaciones,
                                                fotosDriveUrls = uploadedUrls,
                                                fotosPath = photoFiles.map { it.absolutePath }
                                            )

                                            guardarPedido(pedido, sheetUrl)

                                        } catch (e: Exception) {
                                            Log.e("GUARDAR", "❌ Error al guardar pedido: ${e.message}")
                                        }

                                        withContext(Dispatchers.Main) {
                                            qrResult = null
                                            photoFiles.clear()
                                            numeroFotoActual = 1
                                        }
                                    }
                                },
                                onTomarFoto = {
                                    capturarFoto(qrResult!!)
                                }
                            )
                        }

                        mostrarListaPedidos -> {
                            val pedidos = cargarPedidosGuardados()
                            val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
                            val sheetUrl = sharedPreferences.getString("dispatch_sheet_url", "") ?: ""
                            PedidosListScreen(
                                pedidos = pedidos,
                                onVolverClick = { mostrarListaPedidos = false },
                                onCompartirClick = { file -> compartirArchivo(file) },
                                onBorrarTodosClick = { borrarTodosLosPedidos() },
                                onSincronizarClick = { sincronizarPedidosPendientes(this, sheetUrl) }
                            )
                        }

                        else -> {
                            MainScreen(
                                modifier = Modifier.padding(innerPadding),
                                onDispatchClick = { checkCameraPermission() },
                                onContinuousScanClick = {
                                    val intent = Intent(this, ContinuousScanActivity::class.java)
                                    startActivity(intent)
                                },
                                onSettingsClick = { formType ->
                                    val intent = Intent(this, SettingsActivity::class.java)
                                    intent.putExtra("FORM_TYPE", formType)
                                    startActivity(intent)
                                },
                                onVerPedidosClick = { mostrarListaPedidos = true }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun compartirArchivo(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension == "json") "application/json" else "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir con..."))
    }

    private fun guardarPedido(pedido: Pedido, sheetUrl: String) {
        val gson = Gson()
        val timestamp = System.currentTimeMillis()
        val json = gson.toJson(pedido)

        val fileName = "pedido_${timestamp}.json"
        val file = File(getExternalFilesDir(null), fileName)
        file.writeText(json)

        enviarPedidoAGoogleSheetsConFecha(pedido, timestamp, sheetUrl)

        val folderId = "1rofvNaKGrqnw163RNw2YoxnKTgDqqrlY"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DriveUploader.uploadFile(
                    context = this@MainActivity,
                    localFilePath = file.absolutePath,
                    fileName = file.name,
                    folderId = folderId,
                    mimeType = "application/json"
                )

                pedido.fotosPath.forEachIndexed { index, photoPath ->
                    val photoFile = File(photoPath)
                    val photoName = "foto_${pedido.remito}_${index + 1}.jpg"

                    DriveUploader.uploadFile(
                        context = this@MainActivity,
                        localFilePath = photoFile.absolutePath,
                        fileName = photoName,
                        folderId = folderId,
                        mimeType = "image/jpeg"
                    )
                }

                val sentMarker = File(getExternalFilesDir(null), "pedido_${timestamp}.sent")
                sentMarker.writeText("enviado")
            } catch (e: Exception) {
                Log.e("UPLOAD", "❌ Error subiendo archivos: ${e.message}")
            }
        }
    }

    private fun borrarTodosLosPedidos() {
        val dir = getExternalFilesDir(null)
        dir?.listFiles()?.forEach { file ->
            if (
                (file.name.startsWith("pedido_") && file.extension == "json") ||
                (file.name.startsWith("foto_") && file.extension == "jpg")
            ) {
                file.delete()
            }
        }
        runOnUiThread {
            Toast.makeText(this, "🗑️ Todos los pedidos fueron borrados", Toast.LENGTH_SHORT).show()
            mostrarListaPedidos = false
        }
    }

    @Composable
    fun MainScreen(
        modifier: Modifier = Modifier,
        onDispatchClick: () -> Unit,
        onContinuousScanClick: () -> Unit,
        onSettingsClick: (String) -> Unit,
        onVerPedidosClick: () -> Unit
    ) {
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        // Estado reactivo para las URLs
        val (dispatchSheetUrl, setDispatchSheetUrl) = remember { mutableStateOf(sharedPreferences.getString("dispatch_sheet_url", "") ?: "") }
        val (continuousSheetUrl, setContinuousSheetUrl) = remember { mutableStateOf(sharedPreferences.getString("continuous_sheet_url", "") ?: "") }

        // Recargar las URLs al detectar un cambio en la actividad
        LaunchedEffect(Unit) {
            setDispatchSheetUrl(sharedPreferences.getString("dispatch_sheet_url", "") ?: "")
            setContinuousSheetUrl(sharedPreferences.getString("continuous_sheet_url", "") ?: "")
        }

        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDispatchClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A73E8),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .height(50.dp)
                ) {
                    Text(text = "Formulario Despacho", fontSize = 16.sp)
                }
                IconButton(
                    onClick = { onSettingsClick("DISPATCH") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurar URL Despacho",
                        tint = Color(0xFF1A73E8)
                    )
                }
            }

            Text(
                text = if (dispatchSheetUrl.isEmpty()) "URL no configurada" else "URL: $dispatchSheetUrl",
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onContinuousScanClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34A853),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .height(50.dp)
                ) {
                    Text(text = "Escaneo Continuo", fontSize = 16.sp)
                }
                IconButton(
                    onClick = { onSettingsClick("CONTINUOUS") },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurar URL Escaneo Continuo",
                        tint = Color(0xFF34A853)
                    )
                }
            }

            Text(
                text = if (continuousSheetUrl.isEmpty()) "URL no configurada" else "URL: $continuousSheetUrl",
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = onVerPedidosClick,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF34A853),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .height(50.dp)
                    .width(200.dp)
            ) {
                Text(text = "Ver pedidos", fontSize = 16.sp)
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun MainScreenPreview() {
        QRScannerAppTheme {
            MainScreen(
                onDispatchClick = {},
                onContinuousScanClick = {},
                onSettingsClick = {},
                onVerPedidosClick = {}
            )
        }
    }
}
