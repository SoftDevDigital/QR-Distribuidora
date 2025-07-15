package com.example.qrscannerapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File
import com.example.qrscannerapp.Pedido

@Composable
fun PedidosListScreen(
    pedidos: List<PedidoConFecha>,
    onVolverClick: () -> Unit,
    onCompartirClick: (File) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onVolverClick,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("← Volver al escáner")
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
                        Text("📅 Fecha: $fecha")
                        Text("Remito: ${pedido.remito}")
                        Text("Bolsas: ${pedido.cantidadBolsas}")
                        Text("Responsable: ${pedido.responsable}")
                        Text("Observaciones: ${pedido.observaciones}")

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
                            File(nombreJson) // fallback para evitar crash si no hay fotos
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
