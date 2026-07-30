package com.fxzmusic.app.service
import com.fxzmusic.app.*
import com.fxzmusic.app.data.*
import com.fxzmusic.app.util.*

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.sqrt

class ShakeDetector(
    private val onShake: () -> Unit,
    private val thresholdG: Float = 2.2f,
    private val cooldownMs: Long  = 1200L
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var vibrator: Vibrator? = null
    private var lastShakeMs = 0L
    private var isRegistered = false

    fun start(context: Context) {
        if (isRegistered) return

        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        val registered = manager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        if (!registered) return

        sensorManager = manager
        isRegistered = true

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        vibrator = null
        isRegistered = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (gForce > thresholdG) {
            val now = System.currentTimeMillis()
            if (now - lastShakeMs > cooldownMs) {
                lastShakeMs = now
                buzz()
                onShake()
            }
        }
    }

    @Suppress("MissingPermission")
    private fun buzz() {
        try {
            val vib = vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
