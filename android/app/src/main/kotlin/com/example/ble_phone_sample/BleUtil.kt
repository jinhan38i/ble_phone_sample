package com.example.ble_phone_sample

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import io.flutter.plugin.common.EventChannel
import java.nio.charset.StandardCharsets
import java.util.UUID

@SuppressLint("MissingPermission", "NewApi")
object BleUtil : EventChannel.StreamHandler {

    interface Listener {
        fun scannedDevice(device: ScanResult)
        fun bondedDevice(bondedDevice: BluetoothDevice)
        fun stopScan()
        fun connect(address: String)
        fun disConnect(address: String)
        fun sendEvent(message: String)
        fun didConnect(bleDevice: BleDevice)
        fun didPhoneConnect(bleDevice: BleDevice)
        fun disConnectPhone()
        fun didDisconnect(address: String?)
        fun readMessage(data: ByteArray, address: String)

    }

    private const val TAG = "BleUtil"
    var listener: Listener? = null

    var isConnected = MutableLiveData(false)
    var connectedGatt: BluetoothGatt? = null
    var connectedChar: BluetoothGattCharacteristic? = null
    private const val chattingGattUuid = "fec26ec4-6d71-4442-9f81-55bc21d658d0"
    private const val chattingCharacteristicUuid = "fec26ec4-6d71-4442-9f81-55bc21d658d1"
    private const val descriptorUuid = "00002902-0000-1000-8000-00805f9b34fb"

    // BLE Central Mode 관련 변수
    private var bluetoothAdapter: BluetoothAdapter? = null
    private const val scanLimitTime = 10000L
    var isScanning = MutableLiveData(false)
    var scanList = ArrayList<ScanResult>()
    private var bleEventSink: EventChannel.EventSink? = null

    fun scanStart(context: Context) {
        scanList.clear()
        val bluetoothManager =
            context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        isScanning.value?.let {
            if (!it) {
                isScanning.postValue(true)
                bluetoothAdapter?.bluetoothLeScanner?.startScan(leScanCallback)

                Handler(Looper.getMainLooper()).postDelayed({
                    stopBleScan()
                }, scanLimitTime)
            }
        }
    }

    fun scanStop() {
        stopBleScan()
    }

    /**
     * Ble 스캔 콜백
     */
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            if (!result.device.name.isNullOrEmpty() && result.device.name.contains("IHP")) {
                listener?.scannedDevice(result)
                for (scanResult in scanList) {
                    if (scanResult.device.address == result.device.address) {
                        return
                    }
                }
                Log.d(TAG, "onScanResult: result : $result")

                scanList.add(result)

                val deviceList = ArrayList<String>()
                for (scanResult in scanList) {
                    scanResult.device.apply {
                        deviceList.add(
                            Gson().toJson(
                                BleDevice(
                                    name = name,
                                    address = address,
                                    isBonded = bondState == BluetoothDevice.BOND_BONDED,
                                ),
                            )
                        )
                    }
                }

                val response = mapOf("type" to "scan", "data" to deviceList)
                callEventSink(response)
            }
        }
    }

    /**
     * 블루투스 활성화 여부 체크
     */
    fun isBluetoothEnable(context: Context): Boolean {
        val bluetoothManager =
            context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter.isEnabled
    }

    private fun stopBleScan() {
        isScanning.postValue(false)
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
        listener?.stopScan()
    }

    /**
     * gatt 연결 상태 콜백
     */
    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        /**
         * 연결 상태 변경되면 진입
         */
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.d(
                TAG,
                "onConnectionStateChange: gatt : $gatt, status : $status, newState : $newState"
            )
            when (newState) {

                BluetoothProfile.STATE_CONNECTED -> {
                    stopBleScan()
                    gatt?.discoverServices()
                }

                // connectedGatt?.disconnect() 호출 성공하면 진입
                // 해제한 정보는 gatt에 있다.
                BluetoothProfile.STATE_DISCONNECTED -> {
                    // 채팅인 경우
                    if (gatt == connectedGatt) {
                        connectedGatt = null
                        connectedChar = null
                        isConnected.postValue(false)
                        val response = mapOf(
                            "type" to "disconnect",
                            "data" to "success"
                        )
                        callEventSink(response)
                    }
                }
            }
        }


        /**
         * gatt?.discoverServices() 호출하면 진입
         */
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, "onServicesDiscovered: gatt : $gatt, status : $status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (service in gatt!!.services) {
                    if (service.uuid.toString() == chattingGattUuid) {
                        Log.d(TAG, "onServicesDiscovered: chattingGattUuid 진입")
                        if (isConnected.value != true) {
                            for (characteristics in service.characteristics) {
                                if (characteristics.uuid.toString() == chattingCharacteristicUuid) {
                                    gatt.setCharacteristicNotification(characteristics, true)

                                    val descriptor = characteristics?.getDescriptor(
                                        UUID.fromString(descriptorUuid)
                                    )

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        gatt.writeDescriptor(
                                            descriptor!!,
                                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                        )
                                    } else {
                                        descriptor?.value =
                                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                        gatt.writeDescriptor(descriptor)
                                    }

                                    connectedGatt = gatt
                                    connectedChar = characteristics
                                    isConnected.postValue(true)

                                    val response = mapOf(
                                        "type" to "connect",
                                        "data" to connectedGatt?.device?.address
                                    )

                                    callEventSink(response)
                                    return
                                }
                            }
                        } else {
                            gatt.disconnect()
                            listener?.sendEvent("채팅이 이미 실행중입니다.")
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (gatt != null && characteristic != null) {
                onCharacteristicChanged(gatt, characteristic, characteristic.value)
            }
        }

        /**
         * Peripheral 에서 notification 했을 때 진입
         */
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Log.d(TAG, "onCharacteristicChanged: value : $value")

            val message = String(value, StandardCharsets.UTF_8)
            val response = mapOf("type" to "notification", "data" to message)
            callEventSink(response)
        }
    }

    fun connectToDevice(context: Context, device: BluetoothDevice) {
        Log.d(TAG, "connectToDevice: bluetoothAdapter : $bluetoothAdapter")
        bluetoothAdapter?.getRemoteDevice(device.address)?.let {
            it.connectGatt(context, false, bluetoothGattCallback)
        }
    }


    fun disconnectToDevice() {
        connectedGatt?.disconnect()
    }

    fun writeData(message: String) {
        Log.d(TAG, "writeData: 데이터 쓰기 : $message")
        val data = message.toByteArray(StandardCharsets.UTF_8)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            connectedGatt?.writeCharacteristic(
                connectedChar!!,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )
        } else {
            connectedChar?.value = data
            connectedGatt?.writeCharacteristic(connectedChar)
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        bleEventSink = events
    }

    override fun onCancel(arguments: Any?) {
        bleEventSink = null
    }


    private fun callEventSink(response: Map<*, *>) {
        mainActivity.runOnUiThread {
            bleEventSink?.success(response)
        }
    }


}