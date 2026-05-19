package com.calorieko.app.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import android.bluetooth.le.ScanFilter
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.observer.ConnectionObserver

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
 * Manages the BLE connection lifecycle to the ESP32 smart scale using Nordic BLE.
 *
 * Usage:
 *   1. Call [startScan] to discover the scale.
 *   2. Observe [connectionState] to drive the UI.
 *   3. Call [close] when done (e.g. screen disposal).
 */
@Suppress("DEPRECATION")
@SuppressLint("MissingPermission") // Permissions are checked at the UI layer before calling
class BleScaleManager(private val context: Context) : BleManager(context) {

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Idle)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _calibrationEvent = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val calibrationEvent: SharedFlow<String> = _calibrationEvent

    private val _liveWeight = MutableStateFlow(0f)
    val liveWeight: StateFlow<Float> = _liveWeight.asStateFlow()

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var weightChar: BluetoothGattCharacteristic? = null
    private var commandChar: BluetoothGattCharacteristic? = null
    private var isScanning = false

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                    Log.w(TAG, "Bluetooth stopped – forcing disconnection.")
                    handleBluetoothDisabled()
                }
            }
        }
    }

    init {
        // Register receiver for Bluetooth state changes (System-wide)
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(bluetoothStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(bluetoothStateReceiver, filter)
        }

        // Set Nordic connection observer
        setConnectionObserver(object : ConnectionObserver {
            override fun onDeviceConnecting(device: BluetoothDevice) {
                Log.d(TAG, "onDeviceConnecting: ${device.address}")
                _connectionState.value = BleConnectionState.Connecting
            }

            override fun onDeviceConnected(device: BluetoothDevice) {
                Log.d(TAG, "onDeviceConnected: ${device.address}")
                // Waiting for services discovery and ready callback
            }

            override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
                Log.w(TAG, "onDeviceFailedToConnect: reason=$reason")
                _liveWeight.value = 0f
                _connectionState.value = BleConnectionState.Failed("Connection failed (reason $reason)")
            }

            override fun onDeviceReady(device: BluetoothDevice) {
                Log.d(TAG, "onDeviceReady: ${device.address}")
                _connectionState.value = BleConnectionState.Connected
            }

            override fun onDeviceDisconnecting(device: BluetoothDevice) {
                Log.d(TAG, "onDeviceDisconnecting: ${device.address}")
            }

            override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
                Log.w(TAG, "onDeviceDisconnected: reason=$reason")
                _liveWeight.value = 0f
                if (_connectionState.value !is BleConnectionState.Idle) {
                    val errorMsg = when (reason) {
                        ConnectionObserver.REASON_SUCCESS -> "Disconnected"
                        ConnectionObserver.REASON_TERMINATE_LOCAL_HOST -> "Disconnected by user"
                        ConnectionObserver.REASON_TERMINATE_PEER_USER -> "Scale disconnected"
                        ConnectionObserver.REASON_LINK_LOSS -> "Link lost"
                        else -> "Disconnected (reason $reason)"
                    }
                    _connectionState.value = BleConnectionState.Failed(errorMsg)
                }
            }
        })
    }

    // ─── Nordic BleManager GATT Callback ─────────────────────

    override fun getGattCallback(): BleManagerGattCallback = ScaleGattCallback()

    private inner class ScaleGattCallback : BleManagerGattCallback() {
        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "Required service $SERVICE_UUID not found")
                return false
            }
            weightChar = service.getCharacteristic(WEIGHT_CHAR_UUID)
            commandChar = service.getCharacteristic(COMMAND_CHAR_UUID)

            val hasWeightChar = weightChar != null
            val hasCommandChar = commandChar != null

            if (!hasWeightChar || !hasCommandChar) {
                Log.e(TAG, "Characteristics missing - weight:$hasWeightChar, command:$hasCommandChar")
                return false
            }

            return true
        }

        override fun initialize() {
            // Set weight characteristic notification callback
            setNotificationCallback(weightChar).with { device, data ->
                val strValue = data.getStringValue(0) ?: ""
                val trimmed = strValue.trim()
                if (trimmed == "TARE_OK" || trimmed == "CAL_OK") {
                    Log.d(TAG, "Calibration event received on weight: $trimmed")
                    _calibrationEvent.tryEmit(trimmed)
                } else {
                    val weightFloat = trimmed.toFloatOrNull() ?: 0f
                    _liveWeight.value = weightFloat
                    Log.d(TAG, "Weight received: $trimmed (parsed: ${weightFloat}g)")
                }
            }

            // Set command characteristic notification callback
            setNotificationCallback(commandChar).with { device, data ->
                val strValue = data.getStringValue(0) ?: ""
                val trimmed = strValue.trim()
                if (trimmed == "TARE_OK" || trimmed == "CAL_OK") {
                    Log.d(TAG, "Calibration event received on command: $trimmed")
                    _calibrationEvent.tryEmit(trimmed)
                } else {
                    Log.d(TAG, "Command response: $trimmed")
                }
            }

            // Enqueue notification enabling operations. Nordic's BleManager internal queue 
            // completely solves overlapping write operations (descriptor writes) without manual delays!
            enableNotifications(weightChar).enqueue()
            enableNotifications(commandChar).enqueue()
            Log.d(TAG, "✅ Notification enable commands queued!")
        }

        override fun onServicesInvalidated() {
            Log.d(TAG, "GATT Services invalidated. Resetting characteristics.")
            weightChar = null
            commandChar = null
        }
    }

    // ─── Scan ───────────────────────────────────────────────

    /**
     * Start scanning for the ESP32 scale.
     * Uses explicit SERVICE_UUID filter for reliability.
     */
    fun startScan() {
        // Force a clean state before every scan
        reset()

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = BleConnectionState.Failed("Bluetooth is off or unavailable")
            return
        }

        // Clear previous callback just in case
        try { scanner.stopScan(scanCallback) } catch (e: Exception) {}

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        // Use explicit scan filter for the Service UUID
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
        _connectionState.value = BleConnectionState.Connecting
        Log.d(TAG, "Connecting to ${device.address} using Nordic BLE…")

        // connect(device) handles connection, retry, and background queuing smoothly
        connect(device)
            .retry(3, 100) // Retry up to 3 times with 100ms interval
            .useAutoConnect(false)
            .enqueue()
    }

    // ─── Commands ───────────────────────────────────────────

    fun sendTareCommand() {
        val characteristic = commandChar
        if (characteristic == null) {
            Log.e(TAG, "Cannot send TARE: not connected")
            return
        }
        val payload = "TARE".toByteArray()
        writeCharacteristic(characteristic, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            .enqueue()
        Log.d(TAG, "Queued TARE command")
    }

    fun sendCalibrateCommand(knownWeight: Float) {
        val characteristic = commandChar
        if (characteristic == null) {
            Log.e(TAG, "Cannot send CAL: not connected")
            return
        }
        val payload = String.format(java.util.Locale.US, "CAL:%.1f", knownWeight).toByteArray()
        writeCharacteristic(characteristic, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            .enqueue()
        Log.d(TAG, "Queued CAL:%.1f command".format(knownWeight))
    }

    /**
     * Resets the calibration event buffer so the UI doesn't re-handle the same event.
     */
    fun clearCalibrationEvent() {
        _calibrationEvent.tryEmit("")
    }

    // ─── Cleanup ────────────────────────────────────────────

    override fun close() {
        Log.d(TAG, "close() called")
        try {
            context.unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver: ${e.message}")
        }
        stopScan()
        disconnect().enqueue()
        _connectionState.value = BleConnectionState.Idle
        super.close()
    }

    /**
     * Handles the scenario where the system Bluetooth is turned off.
     */
    private fun handleBluetoothDisabled() {
        _liveWeight.value = 0f
        stopScan()
        disconnect().enqueue()
        _connectionState.value = BleConnectionState.Failed("Bluetooth is off")
    }

    /**
     * Resets the BLE internal state, ensuring any existing connection is nuked.
     */
    private fun reset() {
        stopScan()
        disconnect().enqueue()
    }
}
