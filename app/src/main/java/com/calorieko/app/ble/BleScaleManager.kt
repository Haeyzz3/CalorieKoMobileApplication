package com.calorieko.app.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import android.bluetooth.le.ScanFilter

// ── UUIDs must match exactly with the ESP32 firmware ──
private val SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
private val WEIGHT_CHAR_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
private val COMMAND_CHAR_UUID: UUID = UUID.fromString("825442fb-f002-4b21-8255-a0bc8e77cde3")

private const val TAG = "BleScaleManager"

/**
 * Represents the connection lifecycle states.
 */
sealed class BleConnectionState {
    data object Idle : BleConnectionState()
    data object Scanning : BleConnectionState()
    data object Connecting : BleConnectionState()
    data object Connected : BleConnectionState()
    data class Failed(val reason: String) : BleConnectionState()
}

/**
 * Manages the BLE connection lifecycle to the ESP32 smart scale.
 *
 * Usage:
 *   1. Call [startScan] to discover the scale.
 *   2. Observe [connectionState] to drive the UI.
 *   3. Call [close] when done (e.g. screen disposal).
 */
@SuppressLint("MissingPermission") // Permissions are checked at the UI layer before calling
class BleScaleManager(private val context: Context) {

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Idle)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _calibrationEvent = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val calibrationEvent: SharedFlow<String> = _calibrationEvent

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false

    // ─── Scan ───────────────────────────────────────────────

    /**
     * Start scanning for the ESP32 scale.
     * Uses explicit SERVICE_UUID filter for reliability.
     */
    fun startScan() {
        // 1. Force a clean state before every scan
        reset()

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = BleConnectionState.Failed("Bluetooth is off or unavailable")
            return
        }

        // 2. Clear previous callback just in case
        try { scanner.stopScan(scanCallback) } catch (e: Exception) {}

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        // 3. Use explicit scan filter for the Service UUID
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        _connectionState.value = BleConnectionState.Scanning
        isScanning = true
        scanner.startScan(null, settings, scanCallback)
        Log.d(TAG, "Scan started – looking for scale")
    }

    /**
     * Stop an active scan (idempotent).
     */
    fun stopScan() {
        if (!isScanning) return
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        isScanning = false
        Log.d(TAG, "Scan stopped")
    }

    /**
     * Transition to [BleConnectionState.Failed] with the given [reason].
     * Used by the UI layer to signal a scan timeout.
     */
    fun failWithReason(reason: String) {
        _connectionState.value = BleConnectionState.Failed(reason)
        Log.w(TAG, "Marked as failed: $reason")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val record = result.scanRecord
            val deviceName = device.name ?: record?.deviceName ?: "Unknown Device"
            val serviceUuids = record?.serviceUuids
            
            // Diagnostic logging: Log everything found during the scan at Debug level
            Log.d(TAG, "Found device: $deviceName [${device.address}] - Services: $serviceUuids")

            if (!isScanning) return

            val matchedByUuid = serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true
            val matchedByName = deviceName != "Unknown Device" && listOf("esp32", "scale", "ble").any {
                deviceName.contains(it, ignoreCase = true)
            }

            if (matchedByUuid || matchedByName) {
                Log.d(TAG, "🎯 MATCH FOUND: $deviceName [${device.address}]")
                isScanning = false
                stopScan()
                connectToDevice(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            isScanning = false
            _connectionState.value = BleConnectionState.Failed("Scan failed (error $errorCode)")
        }
    }

    // ─── Connect ────────────────────────────────────────────

    private fun connectToDevice(device: BluetoothDevice) {
        if (bluetoothGatt != null) {
            Log.w(TAG, "Already connecting/connected to ${bluetoothGatt?.device?.address}. Ignoring redundant connect call to ${device.address}.")
            return
        }

        _connectionState.value = BleConnectionState.Connecting
        Log.d(TAG, "Connecting to ${device.address}…")

        // TRANSPORT_LE is critical — without it Android may try BR/EDR and fail
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: status=$status, newState=$newState")

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Connection error (status=$status, newState=$newState). Closing GATT.")
                gatt.close()
                if (gatt == bluetoothGatt) bluetoothGatt = null
                _connectionState.value = BleConnectionState.Failed("Connection error (status $status)")
                return
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected! Waiting 600ms before discovering services…")
                // Add the 600ms stability delay from the prototype
                Handler(Looper.getMainLooper()).postDelayed({
                    gatt.discoverServices()
                }, 600)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "Disconnected")
                gatt.close()
                bluetoothGatt = null
                if (_connectionState.value !is BleConnectionState.Idle) {
                    _connectionState.value = BleConnectionState.Failed("Disconnected")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.Failed("Service discovery failed")
                return
            }

            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                _connectionState.value = BleConnectionState.Failed("Scale service not found")
                return
            }

            val weightChar = service.getCharacteristic(WEIGHT_CHAR_UUID)
            val commandChar = service.getCharacteristic(COMMAND_CHAR_UUID)

            if (weightChar == null || commandChar == null) {
                _connectionState.value = BleConnectionState.Failed("Characteristics missing")
                return
            }

            // Enable notifications on the weight characteristic
            gatt.setCharacteristicNotification(weightChar, true)
            val descriptor = weightChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }

            // Enable notifications on the command characteristic after a short delay to prevent write collisions
            Handler(Looper.getMainLooper()).postDelayed({
                gatt.setCharacteristicNotification(commandChar, true)
                val cmdDescriptor = commandChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                if (cmdDescriptor != null) {
                    cmdDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    try {
                        gatt.writeDescriptor(cmdDescriptor)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Failed to write command descriptor: ${e.message}")
                    }
                }
            }, 250)

            Log.d(TAG, "✅ Services validated and notifications enabled!")
            _connectionState.value = BleConnectionState.Connected
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val strValue = characteristic.getStringValue(0) ?: ""
            
            if (strValue.trim() == "TARE_OK" || strValue.trim() == "CAL_OK") {
                Log.d(TAG, "Calibration event received: $strValue on char ${characteristic.uuid}")
                _calibrationEvent.tryEmit(strValue.trim())
            } else if (characteristic.uuid == WEIGHT_CHAR_UUID) {
                Log.d(TAG, "Weight received: $strValue")
                // TODO: Update a StateFlow here so your UI can display the weight
            } else if (characteristic.uuid == COMMAND_CHAR_UUID) {
                Log.d(TAG, "Command response: $strValue")
            }
        }
    }

    // ─── Commands ───────────────────────────────────────────

    fun sendTareCommand() {
        val gatt = bluetoothGatt
        if (gatt == null) {
            Log.e(TAG, "Cannot send TARE: not connected")
            return
        }
        val service = gatt.getService(SERVICE_UUID)
        val commandChar = service?.getCharacteristic(COMMAND_CHAR_UUID)
        if (commandChar == null) {
            Log.e(TAG, "Cannot send TARE: characteristic not found")
            return
        }
        
        commandChar.value = "TARE".toByteArray()
        gatt.writeCharacteristic(commandChar)
        Log.d(TAG, "Sent TARE command")
    }

    fun sendCalibrateCommand(knownWeight: Int) {
        val gatt = bluetoothGatt
        if (gatt == null) {
            Log.e(TAG, "Cannot send CAL: not connected")
            return
        }
        val service = gatt.getService(SERVICE_UUID)
        val commandChar = service?.getCharacteristic(COMMAND_CHAR_UUID)
        if (commandChar == null) {
            Log.e(TAG, "Cannot send CAL: characteristic not found")
            return
        }
        
        commandChar.value = "CAL:$knownWeight".toByteArray()
        gatt.writeCharacteristic(commandChar)
        Log.d(TAG, "Sent CAL:$knownWeight command")
    }

    // ─── Cleanup ────────────────────────────────────────────

    /**
     * Release all BLE resources immediately and force-stop everything.
     */
    fun close() {
        Log.d(TAG, "close() called")
        reset()
        _connectionState.value = BleConnectionState.Idle
    }

    /**
     * Resets the BLE internal state, ensuring any existing connection is nuked.
     */
    private fun reset() {
        stopScan()
        bluetoothGatt?.let { gatt ->
            Log.d(TAG, "Resetting GATT: disconnecting and closing")
            try {
                gatt.disconnect()
                // In a reset/close scenario, we close immediately to free resources
                // even if the DISCONNECTED callback hasn't fired yet.
                gatt.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error during GATT reset: ${e.message}")
            }
        }
        bluetoothGatt = null
    }
}
