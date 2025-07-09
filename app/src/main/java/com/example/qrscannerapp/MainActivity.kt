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


class MainActivity : ComponentActivity() {
    private var qrResult by mutableStateOf<String?>(null)

    private var photoFile: File? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null) {
            Toast.makeText(this, "Foto guardada en: ${photoFile!!.absolutePath}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun capturarFoto(remito: String) {
        val imageFileName = "foto_$remito.jpg"
        photoFile = File(getExternalFilesDir(null), imageFileName)
        val photoUri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            photoFile!!
        )
        takePictureLauncher.launch(photoUri)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startQrScanner()
        }
    }

    private val barcodeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val contents = result.data?.getStringExtra("SCAN_RESULT")
        if (contents != null) {
            qrResult = contents // Actualiza el estado con el resultado
            println("Código QR escaneado: $contents")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkCameraPermission()

        setContent {
            QRScannerAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (qrResult != null) {
                        FormularioDespacho(
                            qrData = qrResult!!,
                            onGuardar = { cantidad, responsable, observaciones ->
                                val pedido = Pedido(
                                    remito = qrResult!!,
                                    cantidadBolsas = cantidad,
                                    responsable = responsable,
                                    observaciones = observaciones
                                )

                                guardarPedido(pedido)
                                capturarFoto(pedido.remito)

                                qrResult = null
                                checkCameraPermission()
                            }
                        )
                    } else {
                        QRScannerScreen(
                            modifier = Modifier.padding(innerPadding),
                            onScanClick = {},
                            qrResult = null
                        )
                    }
                }
            }
        }
    } // ← ESTA llave es la que te falta


        private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                startQrScanner()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                // Mostrar un diálogo explicativo
                Toast.makeText(this, "Se necesita permiso de cámara para escanear QR", Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    private fun guardarPedido(pedido: Pedido) {
        val gson = Gson()
        val json = gson.toJson(pedido)

        val fileName = "pedido_${System.currentTimeMillis()}.json"
        val file = File(getExternalFilesDir(null), fileName)
//effeef
        file.writeText(json)

        println("Archivo guardado en: ${file.absolutePath}")
    }


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
}

@Composable
fun QRScannerScreen(modifier: Modifier = Modifier, onScanClick: () -> Unit, qrResult: String? = null) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onScanClick,
            shape = RoundedCornerShape(50), // Más redondeado
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1A73E8), // Azul institucional fuerte
                contentColor = Color.White
            ),
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 8.dp)
                .height(50.dp)
                .width(200.dp) // Fijamos ancho para que se vea más profesional
        ) {
            Text(
                text = "Escanear QR",
                fontSize = 16.sp // ← así es como se debe usar cuando importás `sp`
            )
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
        QRScannerScreen(onScanClick = {})
    }
}