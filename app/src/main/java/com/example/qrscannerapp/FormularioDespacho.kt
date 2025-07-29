package com.example.qrscannerapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.qrscannerapp.ui.theme.QRScannerAppTheme

@Composable
fun FormularioDespacho(
    qrData: String,
    onGuardar: (fecha: String, cliente: String, cajas: String, bolsas: String, bolsones: String, armado: String, revisa: String, transporte: String, observaciones: String) -> Unit,
    onTomarFoto: () -> Unit
) {
    var fecha by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var cliente by remember { mutableStateOf("") }
    var cajas by remember { mutableStateOf("") }
    var bolsas by remember { mutableStateOf("") }
    var bolsones by remember { mutableStateOf("") }
    var armado by remember { mutableStateOf("") }
    var revisa by remember { mutableStateOf("") }
    var transporte by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Remito escaneado: $qrData", fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = fecha,
            onValueChange = { fecha = it },
            label = { Text("Fecha") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cliente,
            onValueChange = { cliente = it },
            label = { Text("Cliente") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cajas,
            onValueChange = { cajas = it },
            label = { Text("Cajas") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = bolsas,
            onValueChange = { bolsas = it },
            label = { Text("Bolsas") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = bolsones,
            onValueChange = { bolsones = it },
            label = { Text("Bolsones") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = armado,
            onValueChange = { armado = it },
            label = { Text("Armado") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = revisa,
            onValueChange = { revisa = it },
            label = { Text("Revisa") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = transporte,
            onValueChange = { transporte = it },
            label = { Text("Transporte") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = observaciones,
            onValueChange = { observaciones = it },
            label = { Text("Observaciones") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Fotos (máx. 3)", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { onTomarFoto() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📷 Tomar Foto")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onGuardar(fecha, cliente, cajas, bolsas, bolsones, armado, revisa, transporte, observaciones)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Siguiente")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FormularioDespachoPreview() {
    QRScannerAppTheme {
        FormularioDespacho(
            qrData = "12345",
            onGuardar = { _, _, _, _, _, _, _, _, _ -> },
            onTomarFoto = {}
        )
    }
}
