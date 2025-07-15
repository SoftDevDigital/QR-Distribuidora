package com.example.qrscannerapp

data class Pedido(
    val remito: String,
    val cantidadBolsas: String,
    val responsable: String,
    val observaciones: String,
    val cantidadCajas: String = "",
    val cantidadBolsones: String = "",
    val responsableArmado: String = "",
    val responsableRevision: String = "",
    val transporte: String = "",
    val fecha: String = "",
    val cliente: String = "",
    val fotoPath: String? = null
)
