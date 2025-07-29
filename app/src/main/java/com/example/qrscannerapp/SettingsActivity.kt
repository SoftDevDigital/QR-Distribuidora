package com.example.qrscannerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrscannerapp.ui.theme.QRScannerAppTheme
import android.content.Context

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val formType = intent.getStringExtra("FORM_TYPE") ?: "DISPATCH"

        setContent {
            QRScannerAppTheme {
                SettingsScreen(
                    formType = formType,
                    onSave = { url ->
                        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        val key = if (formType == "DISPATCH") "dispatch_sheet_url" else "continuous_sheet_url"
                        with(sharedPreferences.edit()) {
                            putString(key, url)
                            apply()
                        }
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    formType: String,
    onSave: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val key = if (formType == "DISPATCH") "dispatch_sheet_url" else "continuous_sheet_url"

    LaunchedEffect(Unit) {
        url = sharedPreferences.getString(key, "") ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (formType == "DISPATCH") "Configurar URL Formulario Despacho" else "Configurar URL Escaneo Continuo",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("URL de Google Sheets") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (url.isNotBlank()) {
                    onSave(url)
                } else {
                    // Mostrar mensaje de error (puedes usar un Toast o Snackbar)
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Guardar")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    QRScannerAppTheme {
        SettingsScreen(
            formType = "DISPATCH",
            onSave = {}
        )
    }
}