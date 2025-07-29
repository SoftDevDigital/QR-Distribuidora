package com.example.qrscannerapp

data class Pedido(
    val remito: String,
    val fecha: String,
    val cliente: String,
    val cajas: String,
    val cantidadBolsas: String,
    val bolsones: String,
    val armado: String,
    val revisa: String,
    val transporte: String,
    val observaciones: String,
    val fotosDriveUrls: List<String> = emptyList(),
    val fotosPath: List<String> = emptyList()
)
