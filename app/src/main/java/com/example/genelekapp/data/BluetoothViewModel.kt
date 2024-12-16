package com.example.genelekapp.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


//@HiltViewModel
class BluetoothViewModel /*@Inject constructor*/(
    private val bluetoothSetup1: BluetoothSetup1
):ViewModel() {

    private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothSetup1.scannedDevices,
        bluetoothSetup1.pairedDevices,
        _state
    ) { scannedDevices, pairedDevices, state->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            messages = if(state.isConnected) state.messages else emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)


    private var deviceConnectionJob : Job? = null


    init {
        bluetoothSetup1.isConnected.onEach { isConnected ->
            _state.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)

        bluetoothSetup1.errors.onEach { error ->
            _state.update { it.copy(
                errorMessage = error
            ) }
        }.launchIn(viewModelScope)
    }

    fun connectToDevice(device: BluetoothDeviceDomain){
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothSetup1
            .connectToDevice(device)
            .listen()
    }

    fun disconnectFromDevice() {
        deviceConnectionJob?.cancel()
        bluetoothSetup1.closeConnection()
        _state.update { it.copy(
            isConnecting = false,
            isConnected = false
        ) }
    }

    fun waitForIncoming(){
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothSetup1
            .startBluetoothServer()
            .listen()
    }

    fun sendMessage(message: String){
        viewModelScope.launch{
            val bluetoothMessage = bluetoothSetup1.trySendMessage(message)
            if (bluetoothMessage != null){
                _state.update { it.copy(
                    messages = it.messages + bluetoothMessage
                ) }
            }
        }
    }

    fun startScan(){
        bluetoothSetup1.startDiscovery()

    }

    fun stopScan(){
        bluetoothSetup1.stopDiscovery()
    }
    /*
    private fun logScannedDevices() {
        viewModelScope.launch {
            bluetoothSetup1.scannedDevices.collect { devices ->
                Log.d("BluetoothViewModel", "Scanned Devices: ${devices.joinToString { it.name ?: "No Name" }}")
            }
        }
    }

     */

    private fun Flow<ConnectionResult>.listen() : Job {
        return onEach { result ->
            when (result) {
                ConnectionResult.ConnectionEstablished -> {
                    _state.update {
                        it.copy(
                            isConnected = true,
                            isConnecting = false,
                            errorMessage = null
                        )
                    }
                }
                is ConnectionResult.TransferSucceeded -> {
                    _state.update { it.copy(
                        messages = it.messages +result.message
                    ) }
                }
                is ConnectionResult.Error -> {
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            errorMessage = result.message
                        )
                    }
                }

            }
        }
            .catch {  throwable ->
                bluetoothSetup1.closeConnection()
                _state.update { it.copy(
                    isConnected = false,
                    isConnecting = false,
                ) }
            }
            .launchIn(viewModelScope)

    }

    /*
    override fun onCleared() {
        super.onCleared()
        bluetoothSetup1.release()
    }

     */
}