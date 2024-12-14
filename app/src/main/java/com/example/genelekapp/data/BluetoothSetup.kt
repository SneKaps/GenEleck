package com.example.genelekapp.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun BluetoothSetup(){
    val context = LocalContext.current
    val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    val requiredPermissions = listOf(
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.BLUETOOTH_CONNECT
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(context, "Bluetooth permissions are required.", Toast.LENGTH_SHORT).show()
        }
    }



    val missingPermissions = requiredPermissions.filter {
        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }

    if(missingPermissions.isNotEmpty()){
        LaunchedEffect(missingPermissions) {
            permissionLauncher.launch(missingPermissions.toTypedArray())

        }
    }
    if(bluetoothAdapter == null){
        Text(text = "Bluetooth not available")
    }

    if(!bluetoothAdapter?.isEnabled!!){
        LaunchedEffect(Unit){
            try {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                context.startActivity(enableBtIntent)
            } catch (e: SecurityException) {
                Toast.makeText(context, "Permission denied for enabling Bluetooth.", Toast.LENGTH_SHORT).show()
            }
                //val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                //context.startActivity(enableBtIntent)
        }
    }
    else {
            Text("Bluetooth is enabled")
    }
}