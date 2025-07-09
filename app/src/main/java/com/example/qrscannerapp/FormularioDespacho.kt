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
    onGuardar: (cantidad: String, responsable: String, observaciones: String) -> Unit
) {
    var cantidad by remember { mutableStateOf("") }
    var responsable by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Remito escaneado: $qrData", fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = cantidad,
            onValueChange = { cantidad = it },
            label = { Text("Cantidad de bolsas") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = responsable,
            onValueChange = { responsable = it },
            label = { Text("Responsable del despacho") },
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

        Button(
            onClick = {
                onGuardar(cantidad, responsable, observaciones)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Guardar")
        }
    }
}
