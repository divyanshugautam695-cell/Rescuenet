package com.rescuenet.app.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.rescuenet.app.model.MeshPeer
import java.util.UUID

class BluetoothMeshManager(context: Context) {
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("7b3b7d10-7b6a-4a7e-9b42-0f6d9a8d2026")
    }

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val scanner: BluetoothLeScanner? get() = adapter?.bluetoothLeScanner
    private val peers = linkedMapOf<String, MeshPeer>()
    private var callback: ((List<MeshPeer>) -> Unit)? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "RescueNet node"
            peers[device.address] = MeshPeer(name, device.address, result.rssi, connected = false)
            callback?.invoke(peers.values.toList())
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(onPeersChanged: (List<MeshPeer>) -> Unit) {
        callback = onPeersChanged
        peers.clear()
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() { scanner?.stopScan(scanCallback) }

    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        val advertiser = adapter?.bluetoothLeAdvertiser ?: return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        advertiser.startAdvertising(settings, data, advertiseCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() { adapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback) }

    private val advertiseCallback = object : AdvertiseCallback() {}
}
