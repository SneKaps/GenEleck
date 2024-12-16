package com.example.genelekapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.FlowColumnScopeInstance.weight
//import androidx.compose.foundation.layout.ColumnScopeInstance.weight
//import androidx.compose.foundation.layout.FlowColumnScopeInstance.weight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.genelekapp.data.BluetoothDeviceDomain
import com.example.genelekapp.data.BluetoothSetup1
import com.example.genelekapp.data.BluetoothUiState
import com.example.genelekapp.data.BluetoothViewModel
import com.example.genelekapp.data.BluetoothViewModelFactory
import com.example.genelekapp.data.ChatScreen
import com.example.genelekapp.ui.theme.GenElekAppTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GenElekAppTheme {
                val context = LocalContext.current
                val bluetoothSetup1 = BluetoothSetup1(context) // Create or inject this instance
                val viewModelFactory = BluetoothViewModelFactory(bluetoothSetup1)
                val viewModel: BluetoothViewModel by viewModels { viewModelFactory }
                //val viewModel : BluetoothViewModel = viewModel()
                val state by viewModel.state.collectAsState()

                LaunchedEffect(key1 = state.errorMessage){
                    state.errorMessage?.let{ message ->
                        Toast.makeText(
                            applicationContext,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(key1 = state.isConnected) {
                    if(state.isConnected){
                        Toast.makeText(
                            applicationContext,
                            "You're connected",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    when{
                        state.isConnecting ->{
                            Column(modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center)
                            {
                                CircularProgressIndicator()
                                Text(text = "Connecting...")
                            }
                        }
                        state.isConnected ->{
                            ChatScreen(
                                state = state,
                                onDisconnect = viewModel::disconnectFromDevice,
                                onSendMessage = viewModel:: sendMessage
                            )
                        }

                        else -> {
                            HomeScreen(
                                state = state,
                                viewModel = viewModel,
                                onDeviceClick = viewModel::connectToDevice
                        )
                    }



                }

                }

            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun HomeScreen(
    state: BluetoothUiState,
    //onStartScan: () -> Unit,
    //onStopScan: () -> Unit,
    viewModel: BluetoothViewModel,
    //onStartServer : () -> Unit,
    onDeviceClick : (BluetoothDeviceDomain) -> Unit

){
    val context = LocalContext.current

    val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    val isBluetoothEnabled : Boolean = bluetoothAdapter?.isEnabled == true

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){permission->
        val enableBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permission[android.Manifest.permission.BLUETOOTH_CONNECT] == true
        } else true

        if(enableBluetooth && !isBluetoothEnabled){
            enableBluetoothLauncher.launch(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            )
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        SideEffect {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }

    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ){

        Spacer(modifier = Modifier.height(5.dp))

        BluetoothDeviceList(
            title = "Paired devices",
            devices = state.pairedDevices,
            onClick = onDeviceClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        
        Spacer(modifier = Modifier.height(5.dp))
        
        BluetoothDeviceList(
            title = "Scanned devices",
            devices = state.scannedDevices ,
            onClick = onDeviceClick,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom){

            Button(onClick = {
                viewModel.startScan()
            }) {
                Text(text = "Start Scan")
            }

            Button(onClick = {
                viewModel.stopScan()
            }) {
                Text(text = "Stop Scan")
            }

            Button(onClick = {
                viewModel.waitForIncoming()
            }) {
                Text(text = "Start Server")
            }
        }
    }
}

@Composable
fun BluetoothDeviceList(
    title : String,
    devices : List<BluetoothDeviceDomain>,
    onClick: (BluetoothDeviceDomain) -> Unit,
    modifier: Modifier = Modifier
){
    LazyColumn(
        modifier = Modifier.height(300.dp)
    ){
        item { 
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(25.dp)
            )
        }
        items(devices){ device->
            Text(
                text = device.name ?: "No Name",
                modifier
                    .fillMaxWidth()
                    .clickable { onClick(device) }
                    .padding(16.dp)
            )

        }
    }
    
}

