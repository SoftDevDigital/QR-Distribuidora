package com.example.qrscannerapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment

@Composable
fun FormularioDespacho(
    qrData: String,
    onGuardar: (
        remito: String,
        fecha: String,
        cliente: String,
        cajas: String,
        bolsas: String,
        bolsones: String,
        armado: String,
        revisa: String,
        transporte: String,
        observaciones: String
    ) -> Unit,
    onTomarFoto: () -> Unit
) {
    var fecha by remember { mutableStateOf("") }
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
            value = cliente,
            onValueChange = { cliente = it },
            label = { Text("Cliente") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cajas,
            onValueChange = { cajas = it },
            label = { Text("Cajas") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = bolsas,
            onValueChange = { bolsas = it },
            label = { Text("Bolsas") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = bolsones,
            onValueChange = { bolsones = it },
            label = { Text("Bolsones") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = armado,
            onValueChange = { armado = it },
            label = { Text("Armado") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = revisa,
            onValueChange = { revisa = it },
            label = { Text("Revisa") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = transporte,
            onValueChange = { transporte = it },
            label = { Text("Transporte") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = observaciones,
            onValueChange = { observaciones = it },
            label = { Text("Observaciones") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                onGuardar(
                    qrData, fecha, cliente, cajas, bolsas, bolsones,
                    armado, revisa, transporte, observaciones
                )
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Guardar")
        }
    }
}
