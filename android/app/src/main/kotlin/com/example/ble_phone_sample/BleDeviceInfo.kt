package com.example.ble_phone_sample

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic

class BleDeviceInfo {
    var connectedGatt: BluetoothGatt? = null
    var writeChar: BluetoothGattCharacteristic? = null
    var readChar: BluetoothGattCharacteristic? = null
}