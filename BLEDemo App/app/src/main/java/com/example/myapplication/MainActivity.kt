package com.example.myapplication

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.SeekBar
import java.util.*

import android.widget.SeekBar.OnSeekBarChangeListener
import com.google.android.material.button.MaterialButton
import android.os.Bundle

import android.content.Intent

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import com.example.myapplication.R


const val PERMISSION_REQUEST_COARSE_LOCATION = 1
const val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914c"
const val CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a9"

class MainActivity : AppCompatActivity() {

    lateinit var sk: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setEnableBtnDisconnect(false);
        setEnableBtnConnect(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                setEnableBtnDisconnect(false);
                setEnableBtnConnect(false);

                val builder = AlertDialog.Builder(this)
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access - otherwise we cannot find the BLE host!");
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setOnDismissListener(DialogInterface.OnDismissListener { dialog ->
                    Log.v("BLEDemo", "Requesting needed permissions")
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_COARSE_LOCATION)
                })
                builder.show()
            }
        }

        sk = findViewById<SeekBar>(R.id.seekBarRed)
        sk.isEnabled = false
        sk.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                runOnUiThread {
                    this@MainActivity.onSliderRedChanged(seekBar.progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            }
        })

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun setEnableBtnDisconnect(value: Boolean) {
        findViewById<MaterialButton>(R.id.btnDisconnect).isEnabled = value;
    }

    private fun setEnableBtnConnect(value: Boolean) {
        findViewById<MaterialButton>(R.id.btnConnect).isEnabled = value;
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_COARSE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.v("BLEDemo", "Needed permission was granted");
                    setEnableBtnDisconnect(false);
                    setEnableBtnConnect(true);
                } else {
                    Log.v("BLEDemo", "Permission was not granted");
                }
                return
            }

            else -> {
            }
        }
    }


    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanning = false
    private val SCAN_PERIOD: Long = 20000

    private fun scanLeDevice() {
        if (!scanning) {

            Handler(Looper.getMainLooper()).postDelayed({
                Log.v("BLEDemo", "Stop scanning");
                scanning = false
                bluetoothLeScanner?.stopScan(leScanCallback)
            }, SCAN_PERIOD)

            val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SERVICE_UUID)).build()

            val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

            scanning = true
            bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, leScanCallback)
            Log.v("BLEDemo", "Started scan process");

        } else {
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }


    var ourBluetoothGatt: BluetoothGatt? = null
    var ourBluetoothGattService: BluetoothGattService? = null
    var ourBluetoothGattCharacteristic: BluetoothGattCharacteristic? = null

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {


        override fun onScanResult(callbackType: Int, result: ScanResult) {

            scanLeDevice() // will stop further scans

            Log.v("BLEDemo", "found something - " + result.device + " " + result.device.name);
            Log.v("BLEDemo", "trying to connect")
            var bluetoothGatt: BluetoothGatt? = result.device.connectGatt(this@MainActivity, false, object : BluetoothGattCallback() {

                override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        // successfully connected to the GATT Server
                        Log.v("BLEDemo", "Connected to device")
                        gatt?.discoverServices()

                        ourBluetoothGatt = gatt

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        // disconnected from the GATT Server
                        Log.v("BLEDemo", "Disconnected from device")
                        ourBluetoothGatt = null
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    //super.onServicesDiscovered(gatt, status)
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i("BLEDemo", "Found # services: " + gatt?.services?.size)
                        gatt?.services?.forEach {
                            Log.i("BLEDemo", "" + it.uuid)
                            if (SERVICE_UUID.equals(it.uuid.toString())) {
                                Log.i("BLEDemo", "we found our service!")

                                var service = gatt?.getService(it.uuid)
                                if (service == null) {
                                    Log.e("BLEDemo", "Did not get our service")
                                }
                                ourBluetoothGattService = service

                                if (service != null) {
                                    var chara = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID))
                                    if (chara != null) {
                                        ourBluetoothGattCharacteristic = chara
                                        Log.i("BLEDemo", "UUID " + chara.uuid)
                                        gatt?.readCharacteristic(chara)
                                    } else {
                                        Log.e("BLEDemo", "Did not find our characteristic")
                                    }
                                } else {
                                    Log.e("BLEDemo", "Did not find our service")
                                }
                            }
                        }
                    } else {
                        Log.e("BLEDemo", "Failure while scan for services")
                    }
                }

                override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i("BLEDemo", "CharacteristicRead " + characteristic.uuid)
                        var str = characteristic.getStringValue(0)
                        Log.i("BLEDemo", "Value=$str")

                        runOnUiThread {
                            // Stuff that updates the UI
                            sk.progress = str.toInt()
                            sk.isEnabled = true

                            setEnableBtnDisconnect(true);
                            setEnableBtnConnect(false);
                        }
                    }
                }
            })

        }
    }


    fun btnDisconnectClicked(view: View) {
        ourBluetoothGatt?.disconnect()

        setEnableBtnDisconnect(false);
        setEnableBtnConnect(true);
        sk.progress = 0
        sk.isEnabled = false
    }

    fun btnConnectClicked(view: View) {
        Log.v("BLEDemo", "enableBt invoked");
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.v("BLEDemo", "We have no support for BT");
        } else {
            if (bluetoothAdapter?.isEnabled == false) {
                Log.v("BLEDemo", "Requesting enabling BT");
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                val REQUEST_ENABLE_BT = 1
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }

            Log.v("BLEDemo", "Start Scanning");
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

            scanLeDevice()
        }
    }


    public fun onSliderRedChanged(value: Int) {

        Log.v("BLEDemo", "Setting new value for <red> to $value")
        ourBluetoothGattCharacteristic?.setValue("" + value)
        ourBluetoothGatt?.writeCharacteristic(ourBluetoothGattCharacteristic)

        sk.progressDrawable.setColorFilter(Color.rgb(64, 64, value), PorterDuff.Mode.SRC_IN);

    }
}