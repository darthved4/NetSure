package com.example.netsure.data

data class Transaction(
    val id: Long = 0,
    val amount: String,
    val target: String,
    val referenceNumber: String,
    val timestampMs: Long
)
