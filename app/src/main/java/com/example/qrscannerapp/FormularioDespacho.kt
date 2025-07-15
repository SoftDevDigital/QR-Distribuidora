package com.example.qrscannerapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FormularioDespacho(
    qrData: String,
    onGuardar: (cantidad: String, responsable: String, observaciones: String, cantidadCajas: String, cantidadBolsas: String, cantidadBolsones: String, responsableArmado: String, responsableRevision: String, transporte: String, fecha: String, cliente: String) -> Unit
) {
    var cantidad by remember { mutableStateOf("") }
    var responsable by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }
    var cantidadCajas by remember { mutableStateOf("") }
    var cantidadBolsas by remember { mutableStateOf("") }
    var cantidadBolsones by remember { mutableStateOf("") }
    var responsableArmado by remember { mutableStateOf("") }
    var responsableRevision by remember { mutableStateOf("") }
    var transporte by remember { mutableStateOf("") }
    var cliente by remember { mutableStateOf("") }
    val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cantidadCajas,
            onValueChange = { cantidadCajas = it },
            label = { Text("Cantidad de Cajas") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cantidadBolsas,
            onValueChange = { cantidadBolsas = it },
            label = { Text("Cantidad de Bolsas") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = cantidadBolsones,
            onValueChange = { cantidadBolsones = it },
            label = { Text("Cantidad de Bolsones") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = responsableArmado,
            onValueChange = { responsableArmado = it },
            label = { Text("Responsable Armado") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = responsableRevision,
            onValueChange = { responsableRevision = it },
            label = { Text("Responsable Revisión") },
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

        Button(
            onClick = {
                onGuardar(cantidad, responsable, observaciones, cantidadCajas, cantidadBolsas, cantidadBolsones, responsableArmado, responsableRevision, transporte, fecha, cliente)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Guardar")
        }
    }
}