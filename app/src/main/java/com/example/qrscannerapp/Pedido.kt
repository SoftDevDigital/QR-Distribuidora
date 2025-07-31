// File: Pedido.kt (podés crearlo en el mismo paquete com.example.qrscannerapp)
package com.example.qrscannerapp

data class Pedido(
    val remito: String,
    val cantidadBolsas: String,
    val responsable: String,
    val observaciones: String,
    val tester1: String,
    val fotoDriveUrl: String = "",
    val fotosDriveUrls: List<String> = emptyList(),
    val fotosPath: List<String> = emptyList()
)
