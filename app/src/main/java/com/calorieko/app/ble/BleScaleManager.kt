package com.calorieko.app.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

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

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false

    // ─── Scan ───────────────────────────────────────────────

    /**
     * Start scanning for the ESP32 scale filtered by SERVICE_UUID.
     */
    fun startScan() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = BleConnectionState.Failed("Bluetooth is off or unavailable")
            return
        }

        // Build a scan filter so we only see our scale
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        _connectionState.value = BleConnectionState.Scanning
        isScanning = true
        scanner.startScan(listOf(filter), settings, scanCallback)
        Log.d(TAG, "Scan started – looking for SERVICE_UUID")
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
            Log.d(TAG, "Device found: ${result.device.name ?: "unnamed"} @ ${result.device.address}")
            stopScan()
            connectToDevice(result.device)
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
        Log.d(TAG, "Connecting to ${device.address}…")

        // TRANSPORT_LE is critical — without it Android may try BR/EDR and fail
        bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when {
                newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS -> {
                    Log.d(TAG, "Connected! Discovering services…")
                    gatt.discoverServices()
                }
                newState == BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.w(TAG, "Disconnected (status=$status)")
                    gatt.close()
                    bluetoothGatt = null
                    _connectionState.value = BleConnectionState.Failed("Disconnected (status $status)")
                }
                else -> {
                    Log.e(TAG, "Connection failed (status=$status, newState=$newState)")
                    gatt.close()
                    bluetoothGatt = null
                    _connectionState.value = BleConnectionState.Failed("Connection error (status $status)")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed (status=$status)")
                _connectionState.value = BleConnectionState.Failed("Service discovery failed")
                return
            }

            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "SERVICE_UUID not found on connected device!")
                _connectionState.value = BleConnectionState.Failed("Scale service not found")
                return
            }

            // Validate that both characteristics exist
            val weightChar = service.getCharacteristic(WEIGHT_CHAR_UUID)
            val commandChar = service.getCharacteristic(COMMAND_CHAR_UUID)

            if (weightChar == null || commandChar == null) {
                Log.e(TAG, "Expected characteristics missing (weight=$weightChar, command=$commandChar)")
                _connectionState.value = BleConnectionState.Failed("Scale characteristics missing")
                return
            }

            Log.d(TAG, "✅ All services and characteristics validated — connection complete!")
            _connectionState.value = BleConnectionState.Connected
        }
    }

    // ─── Cleanup ────────────────────────────────────────────

    /**
     * Release all BLE resources. Call from screen disposal.
     */
    fun close() {
        stopScan()
        bluetoothGatt?.let { gatt ->
            gatt.disconnect()
            gatt.close()
        }
        bluetoothGatt = null
        _connectionState.value = BleConnectionState.Idle
        Log.d(TAG, "BleScaleManager closed")
    }
}
