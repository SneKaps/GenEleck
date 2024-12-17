package com.example.genelekapp.data

import kotlinx.serialization.Serializable


data class BluetoothMessage(
    //val message: String,
    val message : ByteArray,
    val senderName: String,
    val isFromLocalUser: Boolean
)

/*
data class BluetoothSenderReceiver(
    val senderName : String,
    val isFromLocalUser : Boolean
    )
*/

/*
@Serializable
data class BluetoothMessageTrial (
    val message : ByteArray(1024),
    val senderName: String,
    val isFromLocalUser: Boolean
)
*/