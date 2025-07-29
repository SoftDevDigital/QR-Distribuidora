package com.example.qrscannerapp

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.MaterialTheme // Tema por defecto si QRScannerAppTheme no está definido

@Composable
fun PedidosListScreen(
    pedidos: List<PedidoConFecha>,
    onVolverClick: () -> Unit,
    onCompartirClick: (File) -> Unit,
    onBorrarTodosClick: () -> Unit,
    onSincronizarClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onVolverClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("← Volver")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onSincronizarClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("🔄 Sincronizar")
            }

            Button(
                onClick = onBorrarTodosClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("🗑️ Borrar todos")
            }
        }

        LazyColumn {
            items(pedidos) { item ->
                val pedido = item.pedido
                val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(Date(item.fechaCreacion))

                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📅 Fecha: $fecha", fontSize = 14.sp)
                        Text("Remito: ${pedido.remito}", fontSize = 14.sp)
                        Text("Cliente: ${pedido.cliente}", fontSize = 14.sp)
                        Text("Cajas: ${pedido.cajas}", fontSize = 14.sp)
                        Text("Bolsas: ${pedido.cantidadBolsas}", fontSize = 14.sp)
                        Text("Bolsones: ${pedido.bolsones}", fontSize = 14.sp)
                        Text("Armado: ${pedido.armado}", fontSize = 14.sp)
                        Text("Revisa: ${pedido.revisa}", fontSize = 14.sp)
                        Text("Transporte: ${pedido.transporte}", fontSize = 14.sp)
                        Text("Observaciones: ${pedido.observaciones}", fontSize = 14.sp)

                        pedido.fotosPath.forEach { path ->
                            val bitmap = BitmapFactory.decodeFile(path)
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(top = 8.dp)
                                )
                            }
                        }

                        val nombreJson = "pedido_${item.fechaCreacion}.json"
                        val archivoJson = if (pedido.fotosPath.isNotEmpty()) {
                            File(File(pedido.fotosPath.first()).parentFile, nombreJson)
                        } else {
                            File(nombreJson)
                        }

                        Button(
                            onClick = { onCompartirClick(archivoJson) },
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Text("📤 Compartir JSON")
                        }

                        pedido.fotosPath.forEachIndexed { index, path ->
                            Button(
                                onClick = { onCompartirClick(File(path)) },
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth()
                            ) {
                                Text("🖼️ Compartir Foto ${index + 1}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PedidosListScreenPreview() {
    MaterialTheme {
        PedidosListScreen(
            pedidos = listOf(
                PedidoConFecha(
                    pedido = Pedido(
                        remito = "12345",
                        fecha = "01/01/2025",
                        cliente = "Cliente Ejemplo",
                        cajas = "10",
                        cantidadBolsas = "20",
                        bolsones = "5",
                        armado = "Juan",
                        revisa = "María",
                        transporte = "Camión 123",
                        observaciones = "Sin observaciones",
                        fotosPath = emptyList()
                    ),
                    fechaCreacion = System.currentTimeMillis()
                )
            ),
            onVolverClick = {},
            onCompartirClick = {},
            onBorrarTodosClick = {},
            onSincronizarClick = {}
        )
    }
}
