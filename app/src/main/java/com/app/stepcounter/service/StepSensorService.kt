package com.app.stepcounter.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.app.stepcounter.MainActivity
import com.app.stepcounter.R
import kotlinx.coroutines.*
import java.util.concurrent.Flow
import java.util.jar.Manifest


class StepSensorService : Service(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        Log.d(TAG,"Running")
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
      sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)

//        coroutineScope.launch {
//            mockStep()
//        }


        return START_STICKY
    }

//    suspend fun mockStep(){
//        for(i in 1..10){
//            delay(1000)
//            sendStepCount(i)
//        }
//    }

    private inner class StepCounterReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val count = intent?.getIntExtra(EXTRA_STEP_COUNT, 0)
            Log.d(TAG, "Received step count update in service: $count")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG,"Stopping")
        sensorManager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not implemented
    }

    private suspend fun sendStepCount(count: Int) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Sending step count update: $count")
        val intent = Intent(ACTION_STEP_COUNT).apply {
            putExtra(EXTRA_STEP_COUNT, count)
        }
        sendBroadcast(intent)

    }
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            coroutineScope.launch {
                val stepCount = event.values[0].toInt()
                Log.d("steps:","$stepCount")
                sendStepCount(stepCount)
            }
        }
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "com.example.stepcounter"
            val channelName = "Step Counter Service"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            channel.enableVibration(false)
            channel.setSound(null, null)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Step Counter")
                .setContentText("Tracking your steps")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notification_manager = NotificationManagerCompat.from(this)
            startForeground(NOTIFICATION_ID, notification.build())
            notification_manager.notify(NOTIFICATION_ID, notification.build())
        } else {
            val notification = NotificationCompat.Builder(this)
                .setContentTitle("Step Counter")
                .setContentText("Tracking your steps")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_STEP_COUNT = "com.example.stepcounter.ACTION_STEP_COUNT"
        const val EXTRA_STEP_COUNT = "com.example.stepcounter.EXTRA_STEP_COUNT"
        private const val NOTIFICATION_ID = 1001
    }
}
