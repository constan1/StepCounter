package com.app.stepcounter

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Space
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.stepcounter.service.StepSensorService
import com.app.stepcounter.ui.CustomComponent
import com.app.stepcounter.ui.theme.StepCounterTheme
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermission()
        checkPermissionsNotif()
        setContent {
            StepCounterTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    StepCounterScreen()
                }
            }
        }
    }

    @Composable
    fun StepCounterScreen() {
        val context = LocalContext.current
        val isServiceRunning = remember { mutableStateOf(false) }
        val stepCount = remember { mutableStateOf(0) }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val count = intent?.getIntExtra(StepSensorService.EXTRA_STEP_COUNT, 0)
                Log.d(TAG, "Received step count update in composable: $count")
                if (count != null) {
                    stepCount.value = count
                }
            }
        }

        DisposableEffect(Unit) {
            val intentFilter = IntentFilter().apply {
                addAction(StepSensorService.ACTION_STEP_COUNT)
            }
            context.registerReceiver(receiver, intentFilter)
            onDispose {
                context.unregisterReceiver(receiver)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StepCounterBar(stepCounter = stepCount.value)
            Text(
                text = "Step Count: ${stepCount.value}",
                style = MaterialTheme.typography.h4
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isServiceRunning.value) {
                        context.stopService(Intent(context, StepSensorService::class.java))
                        isServiceRunning.value = false
                    } else {
                        ContextCompat.startForegroundService(context,Intent(context,StepSensorService::class.java))
                        isServiceRunning.value = true
                    }
                }
            ) {
                if (isServiceRunning.value) {
                    Text(text = "Stop Service")
                } else {
                    Text(text = "Start Service")
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermission() {

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // PERMISSION GRANTED
            } else {
                // PERMISSION NOT GRANTED
            }
        }

        requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermissionsNotif(){

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // PERMISSION GRANTED
            } else {
                // PERMISSION NOT GRANTED
            }
        }
        requestPermissionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE)
    }


    @Composable
    fun StepCounterBar(stepCounter: Int){
        CustomComponent(
            indicatorValue = stepCounter
        )
    
    }

}
