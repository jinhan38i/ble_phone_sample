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
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import io.flutter.plugin.common.EventChannel
import java.nio.charset.StandardCharsets
import java.util.UUID


@SuppressLint("MissingPermission", "NewApi")
object BleUtil : EventChannel.StreamHandler {

    private const val TAG = "BleUtil"
    var isConnected = MutableLiveData(false)
    var connectedGatt: BluetoothGatt? = null
    var connectedChar: BluetoothGattCharacteristic? = null
    private const val sensorGattUuid = "6e400081-b5a3-f393-e0a9-e50e24dcca9e"
    private const val chattingGattUuid = "fec26ec4-6d71-4442-9f81-55bc21d658d0"
    private const val chattingCharacteristicUuid = "fec26ec4-6d71-4442-9f81-55bc21d658d1"
    private const val descriptorUuid = "00002902-0000-1000-8000-00805f9b34fb"

    // BLE Central Mode 관련 변수
    private var bluetoothAdapter: BluetoothAdapter? = null
    private const val scanLimitTime = 10000L
    var isScanning = MutableLiveData(false)
    var scanList = ArrayList<ScanResult>()
    private var bleEventSink: EventChannel.EventSink? = null


    /**
     * https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner#startScan(android.bluetooth.le.ScanCallback)
     * sleep 모드에서 scan을 하기 위해서는 scanFilter에 service를 추가해야 한다.
     */
    fun scanStart(context: Context, needTimer: Boolean) {
        scanList.clear()
        val bluetoothManager =
            context.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        isScanning.value?.let {
            if (!it) {
                isScanning.postValue(true)

                val list = listOf<ScanFilter>(
                    ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(chattingGattUuid))
                        .build(),
                    ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(sensorGattUuid))
                        .build(),
                )

//                SCAN_MODE_LOW_POWER - 저전력 모드에서 Bluetooth LE 스캔을 수행합니다. 전력을 가장 적게 소모하는 기본 스캔 모드입니다. 스캐너는 0.5초 동안 스캔하고 4.5초 동안 정지합니다. Bluetooth LE 장치는 이 모드에서 발견되려면 매우 자주(최소 100ms당 한 번) 광고해야 합니다. 그렇지 않으면 검색 간격에서 일부 또는 모든 광고 이벤트를 놓칠 수 있습니다. 이 모드는 스캐닝 응용 프로그램이 전경에 있지 않은 경우에 시행될 수 있습니다.
//                SCAN_MODE_BALANCED - 균형 잡힌 전원 모드에서 Bluetooth LE 스캔을 수행합니다. 스캔 결과는 스캔 주파수와 전력 소비 사이에 적절한 균형을 제공하는 속도로 반환됩니다. 스캐너는 2초 동안 스캔한 후 3초 동안 유휴 상태가 됩니다.
//                SCAN_MODE_LOW_LATENCY - 가장 높은 듀티 사이클을 사용하여 스캔합니다. 애플리케이션이 포그라운드에서 실행 중일 때만 이 모드를 사용하는 것이 좋습니다.
//                SCAN_MODE_OPPORTUNISTIC - 특별한 Bluetooth LE 스캔 모드입니다. 이 스캔 모드를 사용하는 애플리케이션은 BLE 스캔 자체를 시작하지 않고 수동적으로 다른 스캔 결과를 수신합니다.
                // TODO:: 스캔 텀을 백그라운드일 때랑 최초랑 다르게 가야한다.
//                val settings =
//                    ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build()
                val settings = ScanSettings.Builder()
                if (needTimer) {
                    settings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                } else {
                    settings.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                }

                bluetoothAdapter?.bluetoothLeScanner?.startScan(
                    list,
                    settings.build(),
                    leScanCallback
                )
                if (needTimer) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        stopBleScan()
                    }, scanLimitTime)
                }
            }
        }

    }

    /**
     * Ble 스캔 콜백
     */
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            Log.d(TAG, "onScanResult: result : $result")
            if (result.device.name.contains("IHP")) {
                for (scanResult in scanList) {
                    if (scanResult.device.address == result.device.address) {
                        return
                    }
                }

                scanList.add(result)

                val deviceList = ArrayList<String>()
                for (scanResult in scanList) {
                    scanResult.device.apply {
                        var deviceName = name
                        if (deviceName.isNullOrEmpty()) {
                            deviceName = ""
                        }
                        deviceList.add(
                            Gson().toJson(
                                BleDevice(
                                    name = deviceName,
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

    fun stopBleScan() {
        isScanning.postValue(false)
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
    }

    var count = 0

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
                    if (gatt != null) {
                        if (status == 133 && count == 0) {
                            count++
                            Log.d(TAG, "onConnectionStateChange: 재호출 $count")
                            gatt.device.connectGatt(
                                mainActivity,
                                true,
                                this,
                                BluetoothDevice.TRANSPORT_LE
                            )

//                            gatt.connect
//                            gatt(
//                                mainActivity, true,
//                                bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
                        }
                    }
                    // 채팅인 경우
                    if (gatt == connectedGatt) {
//                        connectedGatt = null
//                        connectedChar = null
                        // 페리퍼럴에서 앱을 종료시켜서 끊어질 경우 status 133으로 들어온다.
                        // 이 경우에는 gatt disconnect를 시켜줘야 한다.
                        if (status == 133) {
                            connectedGatt?.disconnect()
                        } else {
                            isConnected.postValue(false)
                            val response = mapOf(
                                "type" to "disconnect",
                                "data" to "success"
                            )
                            callEventSink(response)

                            // 연결이 해제되면 바로 다시 스캔 시작
                            scanStart(mainActivity, false)
                        }
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

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
        }

        /**
         * Peripheral 에서 notification 했을 때 진입
         */
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            val message = String(value, StandardCharsets.UTF_8)
            val response = mapOf("type" to "notification", "data" to message)
            callEventSink(response)
            if (message == "disconnect") {
                disconnectToDevice()
            }
        }

    }

    fun connectToDevice(context: Context, device: BluetoothDevice) {
        bluetoothAdapter?.getRemoteDevice(device.address)
            ?.connectGatt(context, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE)
    }


    fun disconnectToDevice() {
        connectedGatt?.disconnect()
        Log.d(TAG, "onCharacteristicChanged: disconnect 호출")
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