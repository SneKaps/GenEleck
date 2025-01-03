package com.example.genelekapp.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.ByteBuffer
import java.util.UUID


@SuppressLint("MissingPermission")
class BluetoothSetup1(
    private val context: Context
) : BluetoothController {


    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private var bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var dataTransferService : BluetoothDataTransferService? = null

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver{device->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if (newDevice in devices) devices else devices + newDevice
        }
    }

    private val bluetoothStateReceiver = BluetoothStateReceiver {isConnected, bluetoothDevice ->
        if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true){
            _isConnected.update { isConnected }
        } else {
            CoroutineScope(Dispatchers.IO).launch{
                _errors.emit("cant connect to a non paired device.")
            }
        }
    }

    private var currentServerSocket : BluetoothServerSocket? = null
    private var currentClientSocket : BluetoothSocket? = null

    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }


        )
    }

    //to start scanning for bluetooth devices
    override fun startDiscovery() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)){
            return
        }

        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

            updatePairedDevices()
            bluetoothAdapter?.startDiscovery()

    }

    override fun stopDiscovery() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)){
            return
        }
        bluetoothAdapter?.cancelDiscovery()
    }

    /*
    override fun startBluetoothServer() : Flow<ConnectionResult> {
        return flow<ConnectionResult> {
            if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)){

                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }

            currentServerSocket = bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(
                "data_transfer_service",
                UUID.fromString(SERVICE_UUID)
            )

            var shouldLoop = true
            while (shouldLoop){
                currentClientSocket = try {
                    currentServerSocket?.accept()
                } catch (e: IOException){
                    shouldLoop = false
                    null
                }
                if (currentClientSocket != null) {
                    emit(ConnectionResult.ConnectionEstablished)
                    currentServerSocket?.close()

                    val service = BluetoothDataTransferService(currentClientSocket!!)
                    dataTransferService = service

                    emitAll(
                        service
                            .listenFromIncomingMessages()
                            .map {
                                ConnectionResult.TransferSucceeded(it)
                            }
                    )
                }
                /*

                    emit(ConnectionResult.ConnectionEstablished)
                currentClientSocket?.let {
                    currentServerSocket?.close()
                    val service = BluetoothDataTransferService(it)
                    dataTransferService = service

                    emitAll(
                        service
                            .listenFromIncomingMessages()
                            .map {
                                ConnectionResult.TransferSucceeded(it)
                            }
                    )
                }

                 */
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)

    }

     */

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)){
                throw SecurityException("No permission to connect via bluetooth")
            }

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)


            currentClientSocket = bluetoothDevice
                ?.createInsecureRfcommSocketToServiceRecord(
                    UUID.fromString(SERVICE_UUID)
                )
            //currentClientSocket?.connect()
            stopDiscovery()

            /*
            if(bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == false){

            }
             */

            currentClientSocket.let { socket->
                try {
                    socket?.connect()
                    emit(ConnectionResult.ConnectionEstablished)

                    if (socket != null) {
                        BluetoothDataTransferService(socket).also{
                            dataTransferService = it
                            emitAll(
                                it.listenFromIncomingMessages()
                                    .map {
                                        ConnectionResult.TransferSucceeded(it)
                                    }
                            )
                        }
                    }
                } catch (e:IOException){
                    socket?.close()
                    currentClientSocket = null
                    emit(ConnectionResult.Error("Connection interrupted"))
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun trySendMessage(message: ByteArray): BluetoothMessage? {
        if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)){
            return null
        }

        if (dataTransferService == null){
            return null
        }

        //val messageByteArray = ByteBuffer.putInt()  //converting the String to ByteArray

        //val messageByteArray = ByteBuffer.allocate(7).putInt(message).array()

        val bluetoothMessage = BluetoothMessage(
            message = message,
            senderName = bluetoothAdapter?.name ?: "Unknown Name",
            isFromLocalUser = true
        )

        //dataTransferService?.sendMessage(bluetoothMessage.toByteArray())
        dataTransferService?.sendMessage(message)

        return bluetoothMessage
    }

    override fun closeConnection() {
        currentClientSocket?.close()
        //currentServerSocket?.close()
        currentClientSocket = null
        //currentServerSocket = null
    }


    @SuppressLint("MissingPermission")
    private fun updatePairedDevices(){
        if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)){
            return
        }

        bluetoothAdapter
            ?.bondedDevices
            ?.map{ it.toBluetoothDeviceDomain() }
            ?.also { devices->
                _pairedDevices.update { devices }
            }

    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val SERVICE_UUID = "09f58b08-4d60-425f-8455-f6360df50686"
    }
}