package com.example.genelekapp.data

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {

    val isConnected : StateFlow<Boolean>
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val errors : SharedFlow<String>

    fun startDiscovery()
    fun stopDiscovery()

    //device A to launch a server
    fun startBluetoothServer() :Flow<ConnectionResult>

    //device B to connect to device
    fun connectToDevice(device: BluetoothDeviceDomain) : Flow<ConnectionResult>

    suspend fun trySendMessage(message : ByteArray): BluetoothMessage?

    //suspend fun trySendMessage(message:ByteArray): BluetoothMessage?

    fun closeConnection()

}