package com.example.ble_phone_sample

import android.content.Context
import android.os.PowerManager
import android.util.Log
import java.util.concurrent.Executor


class ScreenUtil(private var context: Context) : Executor {


    private lateinit var wakeLockTurnOn: PowerManager.WakeLock
    private lateinit var wakeLockCpuOn: PowerManager.WakeLock
    private lateinit var powerManager: PowerManager

    fun init() {

        powerManager =
            context.getSystemService(Context.POWER_SERVICE) as PowerManager

        wakeLockCpuOn =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WAKELOCK:InHandPlusCpuOn")

        wakeLockTurnOn = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "WAKELOCK:InHandPlus"
        )

    }

    fun wakeUp() {
        try {
            Log.d(TAG, "wakeUp: ")
            wakeLockTurnOn.acquire(10 * 60 * 1000L /*10 minutes*/)
            wakeLockTurnOn.release() // WakeLock 해제
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "wakeUp: eeee ", e)
        }
    }

    fun cpuOn(callback: () -> Unit) {
        try {
            Log.d(TAG, "wakeUp: cpuOn")
            wakeLockCpuOn.acquire()
//            wakeLockCpuOn.acquire(6 * 10 * 60 * 1000L)/*1 시간*/
            callback()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "wakeUp: eeee ", e)
        }
    }

    fun cpuOff(callback: () -> Unit) {
        try {
            Log.d(TAG, "wakeUp: cpuOff : ${wakeLockCpuOn.isHeld}")
            if (wakeLockCpuOn.isHeld) {
                wakeLockCpuOn.release()
                callback()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "wakeUp: eeee ", e)
        }

    }

    fun cpuCheck(callback: (Boolean) -> Unit) {
        callback(wakeLockCpuOn.isHeld)
    }

    companion object {
        private const val TAG = "ScreenController"
    }


    override fun execute(command: Runnable?) {
        command?.run { }
    }

}