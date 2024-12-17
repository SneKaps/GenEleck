package com.example.genelekapp.data

import java.nio.ByteBuffer

/*
fun String.toBluetoothMessage(isFromLocalUser: Boolean): BluetoothMessage {
    //val name = substringBeforeLast("#")
    //val message = substringAfter("#")
    //val message = Properties.encodeToHexString(BluetoothMessage)

    val message = ByteArray(1)
    return BluetoothMessage(
        message = message,
        senderName = "Unknown",
        isFromLocalUser = isFromLocalUser
    )
}
 */

/*
fun Int.toByteArray(): ByteArray {
    return ByteBuffer.allocate(7).putInt(this).array()
}
 */

fun ByteArray.toBluetoothMessage(isFromLocalUser : Boolean) : BluetoothMessage {

    val buffer = ByteBuffer.wrap(this)

    //first 4 bytes of the message will be read as byteArray
    val messageBytes = ByteArray(4)
    buffer.get(messageBytes)

    //remaining bytes will be read for sender name
    val nameBytes = ByteArray(this.size -4)
    buffer.get(nameBytes)

    //decoding sender name to a string
    val senderName = nameBytes.decodeToString()

    return BluetoothMessage(
        message = messageBytes,
        senderName = senderName,
        isFromLocalUser = isFromLocalUser
    )



}





fun BluetoothMessage.toByteArray() : ByteArray {
    //return "$senderName#$message".encodeToByteArray()
    return message
}

/*
fun Int.toBluetoothMessage(isFromLocalUser : Boolean) : BluetoothMessage{
    //val message = ByteBuffer.allocate(4).putInt(this).array()
    return BluetoothMessage(
        message = 7,
        senderName = "Unknown",
        isFromLocalUser = isFromLocalUser
    )
}

 */





/*
fun BluetoothMessage.toByteArray() : ByteArray{
    return ByteBuffer.allocate(4).putInt(message).array()
}

 */





/*

fun BluetoothMessage.toByteArray() : ByteArray{
    val json = Json.encodeToString(this)  // converting the object into a json string
    return json.encodeToByteArray()            // converting the json string to ByteArray
}

fun ByteArray.toBluetoothMessageTrial(): BluetoothMessage{
     val json = decodeToString()                           //when receiving the byte array convert it into a string
    return Json.decodeFromString(json)
}


 */