package com.example.genelekapp.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BluetoothViewModelFactory(
    private val bluetoothSetup1: BluetoothSetup1
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
                return BluetoothViewModel(bluetoothSetup1) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
