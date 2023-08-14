package com.example.ble_phone_sample

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

lateinit var mainActivity: MainActivity

@SuppressLint("InlinedApi")
class MainActivity : FlutterActivity() {

    companion object {
        private const val TAG = "MainActivity"

        private const val CHANNEL_NAME = "android"
        private const val BLE_INIT = "bleInit"
        private const val BLE_SCAN = "bleScan"
        private const val BLE_SCAN_STOP = "bleScanStop"
        private const val BLE_CONNECT = "bleConnect"
        private const val BLE_DISCONNECT = "bleDisconnect"
        private const val BLE_WRITE_DATA = "bleWriteData"
        private const val BLE_EVENT_CHANNEL = "bleEventChannel"

    }

    lateinit var screenUtil: ScreenUtil

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        mainActivity = this
        screenUtil = ScreenUtil(this)
        screenUtil.init()
        setupMethodChannels(flutterEngine.dartExecutor.binaryMessenger)
    }

    @SuppressLint("NewApi")
    private fun setupMethodChannels(messenger: BinaryMessenger) {
        MethodChannel(messenger, CHANNEL_NAME).setMethodCallHandler { call, result ->
            when (call.method) {

                BLE_INIT -> {
                    try {

                        /// 블루투스 이벤트 채널 오픈
                        EventChannel(
                            messenger,
                            BLE_EVENT_CHANNEL
                        ).setStreamHandler(BleUtil)

                        result.success(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                BLE_SCAN -> BleUtil.scanStart(this)

                BLE_SCAN_STOP -> BleUtil.scanStop()

                BLE_CONNECT -> {

                    val arg = call.arguments as Map<*, *>

                    for (scanResult in BleUtil.scanList) {
                        if (scanResult.device.address == arg["address"].toString()) {
                            BleUtil.connectToDevice(activity, scanResult.device)
                            break
                        }
                    }
                }

                BLE_DISCONNECT -> BleUtil.disconnectToDevice()

                BLE_WRITE_DATA -> BleUtil.writeData(call.arguments.toString())

                "cpuOn" -> {
                    screenUtil.cpuOn {
                        result.success(true)
                    }
                }

                "cpuOff" -> {
                    screenUtil.cpuOff {
                        result.success(true)
                    }
                }

                "cpuCheck" -> {
                    screenUtil.cpuCheck {
                        result.success(it)
                    }
                }
            }
        }
    }

}
