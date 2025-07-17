package com.example.qrscannerapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.net.URL
import java.net.HttpURLConnection
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.qrscannerapp.ui.theme.QRScannerAppTheme
import androidx.compose.ui.tooling.preview.Preview
import com.journeyapps.barcodescanner.ScanOptions
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.FormularioDespacho
import com.google.gson.Gson
import java.io.File
import androidx.core.content.FileProvider
import com.example.qrscannerapp.PedidosListScreen
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.*
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.api.ApiException

private var mostrarListaPedidos by mutableStateOf(false)
private val photoFiles = mutableListOf<File>()
private var numeroFotoActual = 1
data class PedidoConFecha(val pedido: Pedido, val fechaCreacion: Long)

class MainActivity : ComponentActivity() {
    private val RC_SIGN_IN = 1001
    private var qrResult by mutableStateOf<String?>(null)

    private var photoFile: File? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null) {
            photoFiles.add(photoFile!!)
            Toast.makeText(this, "✅ Foto $numeroFotoActual guardada", Toast.LENGTH_SHORT).show()
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

    private fun capturarFoto(remito: String) {
        if (numeroFotoActual > 3) {
            Toast.makeText(this, "📷 Ya se tomaron las 3 fotos", Toast.LENGTH_SHORT).show()
            return
        }

        val nombreArchivo = "foto_${remito}_$numeroFotoActual.jpg"
        val archivo = File(getExternalFilesDir(null), nombreArchivo)
        photoFile = archivo
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", archivo)
        takePictureLauncher.launch(uri)
    }

    // Mueve la función fuera de onCreate
    private fun startQrScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Escanea el código QR")
        options.setCameraId(0) // Cámara trasera
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
        }
    }


    private fun enviarPedidoAGoogleSheets(pedido: Pedido) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    URL("https://script.google.com/macros/s/AKfycbwv6BS2WCAjU11HS08ZdCltqRxXCsMjz01BNQiUdj4cE9CNXNnB-zhREIvhG2SEfcd_8w/exec") // Reemplazá con tu URL
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkCameraPermission()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestServerAuthCode(getString(R.string.server_client_id), false)
            .requestIdToken(getString(R.string.server_client_id))
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)

        setContent {
            QRScannerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when {
                        qrResult != null -> {
                            numeroFotoActual = 1
                            photoFiles.clear()

                            FormularioDespacho(
                                qrData = qrResult!!,
                                onGuardar = { cantidad, responsable, observaciones ->
                                    val folderId = "1rofvNaKGrqnw163RNw2YoxnKTgDqqrlY"
                                    val uploadedUrls = mutableListOf<String>()

                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            // Subir cada foto a Drive y guardar su URL
                                            photoFiles.forEachIndexed { index, photoFile ->
                                                val photoFileName =
                                                    "foto_${qrResult}_${index + 1}.jpg"
                                                val uploadedPhotoId =
                                                    DriveUploader.uploadFileAndGetId(
                                                        context = this@MainActivity,
                                                        localFilePath = photoFile.absolutePath,
                                                        fileName = photoFileName,
                                                        folderId = folderId,
                                                        mimeType = "image/jpeg"
                                                    )
                                                uploadedUrls.add("https://drive.google.com/file/d/$uploadedPhotoId/view")
                                            }

                                            // Crear el pedido final con rutas y URLs
                                            val pedido = Pedido(
                                                remito = qrResult!!,
                                                cantidadBolsas = cantidad,
                                                responsable = responsable,
                                                observaciones = observaciones,
                                                fotosDriveUrls = uploadedUrls,
                                                fotosPath = photoFiles.map { it.absolutePath }
                                            )

                                            guardarPedido(pedido)

                                        } catch (e: Exception) {
                                            Log.e(
                                                "GUARDAR",
                                                "❌ Error al guardar pedido: ${e.message}"
                                            )
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
                                onBorrarTodosClick = { borrarTodosLosPedidos() }
                            )
                        }

                        else -> {
                            QRScannerScreen(
                                modifier = Modifier.padding(innerPadding),
                                onScanClick = { checkCameraPermission() },
                                onVerPedidosClick = { mostrarListaPedidos = true },
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
        val json = gson.toJson(pedido)

        val fileName = "pedido_${System.currentTimeMillis()}.json"
        val file = File(getExternalFilesDir(null), fileName)
        file.writeText(json)

        enviarPedidoAGoogleSheets(pedido)

        val folderId = "1rofvNaKGrqnw163RNw2YoxnKTgDqqrlY"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Subir JSON
                DriveUploader.uploadFile(
                    context = this@MainActivity,
                    localFilePath = file.absolutePath,
                    fileName = file.name,
                    folderId = folderId,
                    mimeType = "application/json"
                )

                // Subir fotos a Drive
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

            } catch (e: Exception) {
                Log.e("UPLOAD", "❌ Error subiendo archivos: ${e.message}")
            }
        }
    }

    @Composable
    fun QRScannerScreen(
        modifier: Modifier = Modifier,
        onScanClick: () -> Unit,
        onVerPedidosClick: () -> Unit,
        qrResult: String? = null
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onScanClick,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A73E8),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 8.dp)
                    .height(50.dp)
                    .width(200.dp)
            ) {
                Text(text = "Escanear QR", fontSize = 16.sp)
            }

            Button( // ← este es el nuevo botón
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

            if (qrResult != null) {
                Text(
                    text = "Resultado: $qrResult",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun QRScannerPreview() {
        QRScannerAppTheme {
            QRScannerScreen(
                onScanClick = {},
                onVerPedidosClick = {},
                qrResult = null
            )
        }
    }
}