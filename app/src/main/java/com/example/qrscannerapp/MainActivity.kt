package com.example.qrscannerapp

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.qrscannerapp.ui.theme.QRScannerAppTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.gms.tasks.Task // Para resolver "Unresolved reference: Task"
import androidx.compose.ui.tooling.preview.Preview // Para la anotación @Preview



private var mostrarListaPedidos by mutableStateOf(false)
private val photoFiles = mutableListOf<File>()
private var numeroFotoActual = 1
data class PedidoConFecha(val pedido: Pedido, val fechaCreacion: Long)

class MainActivity : ComponentActivity() {
    private val RC_SIGN_IN = 1001
    private var qrResult by mutableStateOf<String?>(null)
    private var photoFile: File? = null
    private var multiScanMode by mutableStateOf(false)
    private var singleScanMode by mutableStateOf(false)
    private var lastScanTime by mutableStateOf(0L)
    private val SCAN_DELAY_MS = 4000L // 4 seconds delay between scans

    // SharedPreferences keys
    private val PREFS_NAME = "QRScannerPrefs"
    private val KEY_FORMULARIO_URL = "formulario_url"
    private val KEY_MULTI_SCAN_URL = "multi_scan_url"
    private val DEFAULT_FORMULARIO_URL = "https://script.google.com/macros/s/AKfycbxOGr_laO7ZELpAMe7u2xi69W-VLRJb5wl2Gspo7NURjcIq0Vp1IwuVN96I1YqVP9Gn/exec"
    private val DEFAULT_MULTI_SCAN_URL = "https://script.google.com/macros/s/AKfycbwv6BS2WCAjU11HS08ZdCltqRxXCsMjz01BNQiUdj4cE9CNXNnB-zhREIvhG2SEfcd_8w/exec"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getFormularioUrl(context: Context): String {
        return getSharedPreferences(context).getString(KEY_FORMULARIO_URL, DEFAULT_FORMULARIO_URL) ?: DEFAULT_FORMULARIO_URL
    }

    private fun getMultiScanUrl(context: Context): String {
        return getSharedPreferences(context).getString(KEY_MULTI_SCAN_URL, DEFAULT_MULTI_SCAN_URL) ?: DEFAULT_MULTI_SCAN_URL
    }

    private fun saveFormularioUrl(context: Context, url: String) {
        getSharedPreferences(context).edit().putString(KEY_FORMULARIO_URL, url).apply()
    }

    private fun saveMultiScanUrl(context: Context, url: String) {
        getSharedPreferences(context).edit().putString(KEY_MULTI_SCAN_URL, url).apply()
    }

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

    private fun savePhotoToGalleryAndBackup(remito: String, photoFile: File) {
        val photoName = "foto_${remito}_${numeroFotoActual}.jpg"
        val timestamp = System.currentTimeMillis()

        // 1. Guardar en la galería usando MediaStore
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

        // 2. Guardar en una carpeta personalizada (respaldo)
        val backupDir = File(getExternalFilesDir(null), "Fotos")
        if (!backupDir.exists()) backupDir.mkdirs()

        val backupFile = File(backupDir, photoName)
        try {
            File(photoFile.absolutePath).copyTo(backupFile, overwrite = true)
            photoFiles.remove(photoFile) // Reemplazar el archivo temporal por el de respaldo
            photoFiles.add(backupFile)
            photoFile?.delete() // Eliminar archivo temporal
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
            // Permission granted, no further action needed here as onGranted is handled in checkCameraPermission
        } else {
            Toast.makeText(this, "❌ Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enviarPedidoAGoogleSheets(pedido: Pedido) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://script.google.com/macros/library/d/1huWvzJ2BSKp1ItRxHC1UpvgY-pvyakB2MNb8-S628JCwQ420pFh7k1bG/1")
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

    private fun enviarPedidoAGoogleSheetsConFecha(pedido: Pedido, fechaCreacion: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val context = this@MainActivity
                val url = URL(getFormularioUrl(context))

                // Usar la fecha original
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fechaCreacion))

                val jsonMap = mutableMapOf<String, Any>(
                    "remito" to pedido.remito,
                    "fecha" to dateStr,
                    "cliente" to pedido.cliente,
                    "cajas" to pedido.cajas,
                    "bolsas" to pedido.bolsas,
                    "bolsones" to pedido.bolsones,
                    "armado" to pedido.armado,
                    "revisa" to pedido.revisa,
                    "transporte" to pedido.transporte,
                    "observaciones" to pedido.observaciones
                )

                // ver el log de datos
                Log.d("GOOGLE_SHEETS", "Datos a enviar: $jsonMap")

                val gson = Gson()
                val json = gson.toJson(jsonMap)

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { os -> os.write(json.toByteArray(Charsets.UTF_8)) }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("GOOGLE_SHEETS", "✅ Enviado con fecha original correctamente")
                } else {
                    Log.e("GOOGLE_SHEETS", "❌ Error al enviar con fecha: Código $responseCode")
                }
            } catch (e: Exception) {
                Log.e("GOOGLE_SHEETS", "❌ Excepción al enviar con fecha: ${e.message}")
            }
        }
    }

    private fun enviarRemitoAGoogleSheets(remito: String, fechaCreacion: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val context = this@MainActivity
                val url = URL(getMultiScanUrl(context))

                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(fechaCreacion))

                val jsonMap = mapOf(
                    "remito" to remito,
                    "fecha" to dateStr
                )

                val gson = Gson()
                val json = gson.toJson(jsonMap)

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.use { os -> os.write(json.toByteArray(Charsets.UTF_8)) }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("GOOGLE_SHEETS_MULTI", "✅ Remito $remito enviado correctamente")
                } else {
                    Log.e("GOOGLE_SHEETS_MULTI", "❌ Error al enviar remito $remito: Código $responseCode")
                }
            } catch (e: Exception) {
                Log.e("GOOGLE_SHEETS_MULTI", "❌ Excepción al enviar remito $remito: ${e.message}")
            }
        }
    }

    private fun sincronizarPedidosPendientes(context: Context) {
        val pedidos = cargarPedidosGuardados()

        CoroutineScope(Dispatchers.IO).launch {
            for (pedidoConFecha in pedidos) {
                val pedido = pedidoConFecha.pedido
                val jsonFile = File(getExternalFilesDir(null), "pedido_${pedidoConFecha.fechaCreacion}.json")
                val sentMarker = File(getExternalFilesDir(null), "pedido_${pedidoConFecha.fechaCreacion}.sent")

                if (!sentMarker.exists() && jsonFile.exists()) {
                    // 1. Enviar a Google Sheets
                    enviarPedidoAGoogleSheetsConFecha(pedido, pedidoConFecha.fechaCreacion)

                    // 2. Subir JSON
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

                    // 3. Subir fotos
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

                    // 4. Crear archivo de marca para evitar reenvío
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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestServerAuthCode(getString(R.string.server_client_id), false)
            .requestIdToken(getString(R.string.server_client_id))
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            QRScannerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when {
                        multiScanMode -> {
                            MultiScanScreen(
                                onStop = { multiScanMode = false },
                                onScan = { remito ->
                                    enviarRemitoAGoogleSheets(remito, System.currentTimeMillis())
                                    Toast.makeText(this@MainActivity, "✅ Remito $remito enviado", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        singleScanMode -> {
                            SingleScanScreen(
                                onStop = { singleScanMode = false },
                                onScan = { remito ->
                                    qrResult = remito
                                    singleScanMode = false
                                }
                            )
                        }
                        qrResult != null -> {
                            numeroFotoActual = 1
                            photoFiles.clear()

                            FormularioDespacho(
                                qrData = qrResult!!,
                                onGuardar = { remito, fecha, cliente, cajas, bolsas, bolsones, armado, revisa, transporte, observaciones ->
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

                                            val pedido = Pedido(
                                                remito = remito,
                                                fecha = fecha,
                                                cliente = cliente,
                                                cajas = cajas,
                                                bolsas = bolsas,
                                                bolsones = bolsones,
                                                armado = armado,
                                                revisa = revisa,
                                                transporte = transporte,
                                                observaciones = observaciones,
                                                fotosDriveUrls = uploadedUrls,
                                                fotosPath = photoFiles.map { it.absolutePath }
                                            )

                                            guardarPedido(pedido)
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
                            PedidosListScreen(
                                pedidos = pedidos,
                                onVolverClick = { mostrarListaPedidos = false },
                                onCompartirClick = { file -> compartirArchivo(file) },
                                onBorrarTodosClick = { borrarTodosLosPedidos() },
                                onSincronizarClick = { sincronizarPedidosPendientes(this) }
                            )
                        }
                        else -> {
                            QRScannerScreen(
                                modifier = Modifier.padding(innerPadding),
                                onFormularioDespachoClick = { checkCameraPermission { singleScanMode = true } },
                                onVerPedidosClick = { mostrarListaPedidos = true },
                                onMultiScanClick = { checkCameraPermission { multiScanMode = true } },
                                qrResult = null
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            account.account?.let { accountInfo ->
                val scope = "oauth2:${DriveScopes.DRIVE_FILE} ${DriveScopes.DRIVE}"
                val token = GoogleAuthUtil.getToken(this, accountInfo, scope)
                Log.d("ACCESS_TOKEN", "Token: $token")
            }
        } catch (e: ApiException) {
            Log.e("GOOGLE_SIGN_IN", "Sign in failed", e)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun checkCameraPermission(onGranted: () -> Unit) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                onGranted()
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

    private fun compartirArchivo(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension == "json") "application/json" else "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir con..."))
    }

    private fun guardarPedido(pedido: Pedido) {
        val gson = Gson()
        val timestamp = System.currentTimeMillis()
        val json = gson.toJson(pedido)

        val fileName = "pedido_${timestamp}.json"
        val file = File(getExternalFilesDir(null), fileName)
        file.writeText(json)

        enviarPedidoAGoogleSheetsConFecha(pedido, timestamp) // ⬅️ usa el timestamp real

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

                // Marcar como sincronizado
                val sentMarker = File(getExternalFilesDir(null), "pedido_${timestamp}.sent")
                sentMarker.writeText("enviado")
            } catch (e: Exception) {
                Log.e("UPLOAD", "❌ Error subiendo archivos: ${e.message}")
            }
        }
    }

    @Composable
    fun QRScannerScreen(
        modifier: Modifier = Modifier,
        onFormularioDespachoClick: () -> Unit,
        onVerPedidosClick: () -> Unit,
        onMultiScanClick: () -> Unit,
        qrResult: String? = null
    ) {
        val context = LocalContext.current
        var showFormularioDialog by remember { mutableStateOf(false) }
        var showMultiScanDialog by remember { mutableStateOf(false) }
        var formularioUrl by remember { mutableStateOf(TextFieldValue(getFormularioUrl(context))) }
        var multiScanUrl by remember { mutableStateOf(TextFieldValue(getMultiScanUrl(context))) }

        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onFormularioDespachoClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFF1A73E8),
                        contentColor = ComposeColor.White
                    ),
                    modifier = Modifier
                        .height(50.dp)
                        .weight(1f)
                ) {
                    Text(text = "Formulario de despacho", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showFormularioDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("⚙️", fontSize = 24.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onVerPedidosClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFF34A853),
                        contentColor = ComposeColor.White
                    ),
                    modifier = Modifier
                        .height(50.dp)
                        .weight(1f)
                ) {
                    Text(text = "Ver pedidos", fontSize = 16.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onMultiScanClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ComposeColor(0xFFEA4335),
                        contentColor = ComposeColor.White
                    ),
                    modifier = Modifier
                        .height(50.dp)
                        .weight(1f)
                ) {
                    Text(text = "Multi-scan", fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showMultiScanDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text("⚙️", fontSize = 24.sp)
                }
            }

            if (qrResult != null) {
                Text(
                    text = "Resultado: $qrResult",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Dialog for Formulario URL
            if (showFormularioDialog) {
                AlertDialog(
                    onDismissRequest = { showFormularioDialog = false },
                    title = { Text("Configurar URL de Formulario") },
                    text = {
                        OutlinedTextField(
                            value = formularioUrl,
                            onValueChange = { formularioUrl = it },
                            label = { Text("URL de Google Apps Script") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                saveFormularioUrl(context, formularioUrl.text)
                                showFormularioDialog = false
                                Toast.makeText(context, "✅ URL de Formulario guardada", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Guardar")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { showFormularioDialog = false }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Dialog for Multi-scan URL
            if (showMultiScanDialog) {
                AlertDialog(
                    onDismissRequest = { showMultiScanDialog = false },
                    title = { Text("Configurar URL de Multi-scan") },
                    text = {
                        OutlinedTextField(
                            value = multiScanUrl,
                            onValueChange = { multiScanUrl = it },
                            label = { Text("URL de Google Apps Script") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                saveMultiScanUrl(context, multiScanUrl.text)
                                showMultiScanDialog = false
                                Toast.makeText(context, "✅ URL de Multi-scan guardada", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Guardar")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { showMultiScanDialog = false }
                        ) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun MultiScanScreen(
        onStop: () -> Unit,
        onScan: (String) -> Unit
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val vibratorManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        } else {
            null
        }
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }

        val barcodeView = remember {
            DecoratedBarcodeView(context).apply {
                decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
                statusView.text = "Escanea códigos QR continuamente"
                viewFinder.setMaskColor(Color.argb(200, 0, 0, 0)) // Darker mask (increased opacity)
                viewFinder.setLaserVisibility(true) // Show laser
                decodeContinuous(object : BarcodeCallback {
                    override fun barcodeResult(result: BarcodeResult?) {
                        result?.text?.let { remito ->
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastScanTime >= SCAN_DELAY_MS) {
                                lastScanTime = currentTime
                                // Vibrate or play sound
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    if (vibrator?.hasVibrator() == true) {
                                        vibrator.vibrate(
                                            VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                                        )
                                    } else {
                                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    if (vibrator?.hasVibrator() == true) {
                                        vibrator.vibrate(200)
                                    } else {
                                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                                    }
                                }
                                coroutineScope.launch {
                                    onScan(remito)
                                }
                            }
                        }
                    }

                    override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
                })
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { barcodeView },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onStop,
                modifier = Modifier
                    .padding(16.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ComposeColor(0xFFEA4335),
                    contentColor = ComposeColor.White
                )
            ) {
                Text(text = "Detener Multi-scan", fontSize = 16.sp)
            }
            DisposableEffect(Unit) {
                barcodeView.resume()
                onDispose {
                    barcodeView.pause()
                    toneGenerator.release()
                }
            }
        }
    }

    @Composable
    fun SingleScanScreen(
        onStop: () -> Unit,
        onScan: (String) -> Unit
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val vibratorManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        } else {
            null
        }
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }

        val barcodeView = remember {
            DecoratedBarcodeView(context).apply {
                decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
                statusView.text = "Escanea el código QR"
                viewFinder.setMaskColor(Color.argb(200, 0, 0, 0)) // Darker mask (increased opacity)
                viewFinder.setLaserVisibility(true) // Show laser
                decodeSingle(object : BarcodeCallback {
                    override fun barcodeResult(result: BarcodeResult?) {
                        result?.text?.let { remito ->
                            // Vibrate or play sound
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                if (vibrator?.hasVibrator() == true) {
                                    vibrator.vibrate(
                                        VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                                    )
                                } else {
                                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                if (vibrator?.hasVibrator() == true) {
                                    vibrator.vibrate(200)
                                } else {
                                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                                }
                            }
                            coroutineScope.launch {
                                onScan(remito)
                            }
                        }
                    }

                    override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {}
                })
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { barcodeView },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onStop,
                modifier = Modifier
                    .padding(16.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ComposeColor(0xFFEA4335),
                    contentColor = ComposeColor.White
                )
            ) {
                Text(text = "Cancelar Escaneo", fontSize = 16.sp)
            }
            DisposableEffect(Unit) {
                barcodeView.resume()
                onDispose {
                    barcodeView.pause()
                    toneGenerator.release()
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun QRScannerPreview() {
        QRScannerAppTheme {
            QRScannerScreen(
                onFormularioDespachoClick = {},
                onVerPedidosClick = {},
                onMultiScanClick = {},
                qrResult = null
            )
        }
    }
}