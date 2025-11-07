package com.zteam.zvision.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

fun getDevicePhysicalOrientation(context: Context, callback: (isLandscape: Boolean) -> Unit) {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]

            val isLandscape = kotlin.math.abs(x) > kotlin.math.abs(y)
            callback(isLandscape)

            // Clean up
            sensorManager.unregisterListener(this)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
}

@Composable
fun rememberDevicePhysicalOrientation(lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current): State<Boolean> {
    val context = LocalContext.current
    val isLandscape = remember { mutableStateOf(false) }

    DisposableEffect (lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]

                val newLandscape = kotlin.math.abs(x) > kotlin.math.abs(y)
                if (newLandscape != isLandscape.value) {
                    isLandscape.value = newLandscape
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        // Clean up when composable leaves composition
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return isLandscape
}

