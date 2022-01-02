# BLE-Demo
BLE Demo - Communication between Android App and ESP32 in BLE mode.

## What it does
We have an ESP32 where we want to control the brightness of the "internal LED". 
The brightness is to be controlled via an androida app.

For the communication I'm using BLE (Bluetooth Low Energie) - as this provides a modern way for the communication, although it adds a little more overhead compared to "Bluetooth classic". 
For the implementation this means the ESP32 will need to setup a BLE service with one characteristic, representing the LED brightness. The android app on the other side will need to find the service, connect to the service and write the desired LED brightness (represented as integer 0-255) to the characteristic.

## What it does not 😃
* There is not too much error checking - just a demo!
* code is not cleaned up - there is a lot of smell!

## Preconditions
These are the needed / used things:

* ESP32 D1 Mini
* MS Code with Platformio + installed support for ESP32
* Android Studio ("latest")

## Important readings
This is rather good overview of BLE in Android, with good examples, etc. \
[https://punchthrough.com/android-ble-guide/](https://punchthrough.com/android-ble-guide/)

This is more the official documentation, worth to read, but sometimes a little blurry\
[https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview)

Good, but rather technical overview of BLE for ESP32\
[https://github.com/nkolban/ESP32_BLE_Arduino](https://github.com/nkolban/ESP32_BLE_Arduino)

Examples, which I also took as base for this demo, for the ESP32 BLE\
[https://github.com/nkolban/esp32-snippets/tree/master/Documentation](https://github.com/nkolban/esp32-snippets/tree/master/Documentation)


## ESP32 part
All files for the ESP32 can be found in the ESP32_BLE_server folder of the repo. 
It is actually a Platformio based code - but it can easilly be "ported" to Arduino Studio, too. 

The code has some important parts:

* At the beginning there are two UUIDs defined ... these will later be usded to intentify the BLE service and the corresponding characteristic. 
* There is a `MyServerCallbacks` class. The important function here is `onConnect`, containing a `startAdvertising` - this is needed to keep the advertisement of the BLE service up, even if a client connected. 
* Thre is also `MyCallbacks` ... here we find the `onWrite` function, which will be invoked, once a client sends new data. Here we also change the brightness of the LED. 
Please notice that the data is sent as `string` - even if we want integer values. This is just for simplicity. 
* The `setup` function is pretty simple. It mainly initializes the `BLEDevice`, creates a `BLEService`, adds the `BLECharacteristic` ... and finally starts advertising the service.

Most of the code is taken from the Arduino ESP32 BLE examples. If you need more details look into them!

## The Android part

... is actually not too complicated, you find the Andriod Studio code in "BLEDemo App". Be aware, the code is written in kotlin!

There is only the `MainActivity`, which has two buttons an one slider (SeekBar).
* The connect button enables bluetooth and starts the scan process + connecting to the (hopefully) found ESP32.
* the second button disconnects the app from the ESP32 (if connected)
* the slider is used to enter the desired brightness

### Some functions to highlight

* `onCreate()`\
Here we check for the Android version and for some needed permissions. This is rather important!
We want to search the ESP32 via a BLE scan. For this to work you need to have "location permissions", which you cannot gain by a simple configuration, the user needs to explicitly grant these permission - so you need to ask for them⚠️ \
This is mainly done via an `AlertDialog` - where in the `DismissListener` the needed permissions are requested from the system. This will popup a system dialog, asking the user to grant the permissions ...\
\
What also happens here is that we add a `onSeekBarChangeListener` - to be able to react on changes later. 

* `onRequestPermissionResult()`\
If the user grants the needed permissions we get notified via this function. 

* `scanLeDevice()`\
This is the function handling the BLE scan process - it is mainly just triggering the scan process. To notice: There is a `Handler` used to terminate the scan process after some seconds.\
Also important to notice: We're setting up a `scanFilter` - to not find every BLE device in range, but detect only our ESP32. For that reason we're filtering for the `SERVICE_UUID`, we also defined in the ESP32 part. 

* `leScanCallback()`\
Here we get our scan results reported. More later!

* `btnConnectClicked()`\
This function searches for the `BluetoothAdapter`, if not enabled, ask to enable it and finally gets an instance of the `bluetoothLeScanner`

### Connection process

The BLE connection process works in steps.
After we've triggered the scan, the following steps happens:
1. Once the system finds a BLE device with our given `SERVICE_UUID` the function `onScanResult` is invoked.
2. As we were searching explicitly for our service, we can now directly connect to the gatt. 
3. Once we're connected (`onConnectionStateChange`), we initiate a `discoverServices` ... to get access to the services of the device.
4. After the service discovery is done, we get notified via `onServicesDisvoered`. In this function we can now iterate over all found services. Attention: Also we expose just one service on the ESP32, we find more than one! There are some default services, too. So we need to pick exactly the service we need. 
5. If we identified the object representing our service, we can request access to the needed characteristic. 
6. Then we trigger to read the characteristic ...
7. ... the read result will be provided via `onCharacteristicRead`
8. We use the data to update the UI, especially the seek bar. \
Attention: We want to change the UI, so we need to use a `runOnUiThread` here - as the event is handled in an other process!

After that we're ready to party!

### Updating values
The update procedure is rather simple.\
During the connection phase, we stored the needed instances, especially the gatt and characeristic instance in a member variable. So, if the seek bar changes, we can now simply do a
```
ourBluetoothGattCharacteristic?.setValue("" + value)
ourBluetoothGatt?.writeCharacteristic(ourBluetoothGattCharacteristic)
```

### Limation
This demo application assumes that only one client is connected to the ESP32!\
So there is no update ("notify") mechanism implemented to update changed values to the world. The Android app will simply write its current value. This value is updated only during connection phase, via an explicit read request. 
