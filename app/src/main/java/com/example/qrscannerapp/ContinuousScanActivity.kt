package com.example.qrscannerapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.qrscannerapp.ui.theme.QRScannerAppTheme
import com.google.gson.Gson
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContinuousScanActivity : ComponentActivity() {
    private val barcodeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val contents = result.data?.getStringExtra("SCAN_RESULT")
        if (contents != null) {
            sendRemitoToGoogleSheets(contents)
            startQrScanner() // Continúa escaneando
        } else {
            Log.w("SCAN", "Escaneo cancelado o sin resultado")
            Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
        }
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

    private fun startQrScanner() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                return
            }

            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Escanea el código QR")
            options.setCameraId(0) // Cámara trasera
            options.setBeepEnabled(true)
            options.setBarcodeImageEnabled(true)
            options.setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity::class.java)
            barcodeLauncher.launch(options.createScanIntent(this))
        } catch (e: Exception) {
            Log.e("SCAN", "Error al iniciar el escáner: ${e.message}", e)
            Toast.makeText(this, "Error al iniciar el escáner: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendRemitoToGoogleSheets(remito: String) {
        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val sheetUrl = sharedPreferences.getString("continuous_sheet_url", "") ?: ""

        if (sheetUrl.isEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "❌ URL de Google Sheets no configurada", Toast.LENGTH_LONG).show()
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(sheetUrl)
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                val jsonMap = mapOf("remito" to remito, "fecha" to dateStr)
                val json = Gson().toJson(jsonMap)

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                connection.outputStream.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("GOOGLE_SHEETS", "✅ Remito $remito enviado correctamente")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ContinuousScanActivity, "✅ Remito $remito registrado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("GOOGLE_SHEETS", "❌ Error al enviar remito $remito: Código $responseCode")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ContinuousScanActivity, "❌ Error al registrar remito $remito", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("GOOGLE_SHEETS", "❌ Excepción al enviar remito $remito: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ContinuousScanActivity, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRScannerAppTheme {
                ContinuousScanScreen(
                    onBackClick = { finish() },
                    onStartScan = { startQrScanner() }
                )
            }
        }
    }
}

@Composable
fun ContinuousScanScreen(
    onBackClick: () -> Unit,
    onStartScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Escaneo Continuo de Códigos QR",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = onStartScan,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF34A853),
                contentColor = Color.White
            ),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .height(50.dp)
                .width(200.dp)
        ) {
            Text(text = "Iniciar Escaneo", fontSize = 16.sp)
        }

        Button(
            onClick = onBackClick,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1A73E8),
                contentColor = Color.White
            ),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .height(50.dp)
                .width(200.dp)
        ) {
            Text(text = "Volver", fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContinuousScanScreenPreview() {
    QRScannerAppTheme {
        ContinuousScanScreen(
            onBackClick = {},
            onStartScan = {}
        )
    }
}
