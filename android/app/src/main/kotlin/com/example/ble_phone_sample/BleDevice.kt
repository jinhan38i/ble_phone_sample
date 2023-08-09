package com.example.ble_phone_sample

data class BleDevice(
    val name: String, val address: String,  val isBonded: Boolean,
)