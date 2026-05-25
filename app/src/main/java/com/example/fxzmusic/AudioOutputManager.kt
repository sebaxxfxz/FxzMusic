package com.example.fxzmusic

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class AudioOutputDevice(
    val id: Int,
    val name: String,
    val type: Int,
    val isCurrent: Boolean,
    val isUsbc: Boolean = false
)

class AudioOutputManager(private val context: Context) {

    var devices by mutableStateOf<List<AudioOutputDevice>>(emptyList())
        private set

    var connectedBtName by mutableStateOf<String?>(null)
        private set

    private val audioManager = context.applicationContext
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Debouncing to prevent excessive refresh calls
    private var lastRefreshTime = 0L
    private val REFRESH_DELAY_MS = 500L

    fun refresh() {
        // Debounce refresh calls
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime < REFRESH_DELAY_MS) return
        lastRefreshTime = now

        refreshBluetooth()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                devices = outputs.mapNotNull { info ->
                    val isUsbc = isUsbCType(info.type)
                    val typeName = when {
                        isUsbc -> "Auriculares USB-C"
                        info.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER   -> "Altavoz"
                        info.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES  -> "Auriculares 3.5mm"
                        info.type == AudioDeviceInfo.TYPE_WIRED_HEADSET     -> "Auriculares 3.5mm"
                        info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP    -> connectedBtName ?: "Bluetooth"
                        info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO     -> return@mapNotNull null
                        info.type == AudioDeviceInfo.TYPE_HEARING_AID       -> "Audifono"
                        else -> return@mapNotNull null
                    }
                    val isCurrent = when {
                        isUsbc -> audioManager.isWiredHeadsetOn
                        info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP   -> audioManager.isBluetoothA2dpOn
                        info.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                                info.type == AudioDeviceInfo.TYPE_WIRED_HEADSET    -> audioManager.isWiredHeadsetOn && !isUsbcConnected(outputs)
                        info.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER  ->
                            !audioManager.isBluetoothA2dpOn && !audioManager.isWiredHeadsetOn
                        else -> false
                    }
                    AudioOutputDevice(
                        id        = info.id,
                        name      = typeName,
                        type      = info.type,
                        isCurrent = isCurrent,
                        isUsbc    = isUsbc
                    )
                }.distinctBy { it.type }
            } catch (e: Exception) {
                Log.w("AudioOutputManager", "Failed to get audio devices", e)
                devices = listOf(
                    AudioOutputDevice(0, "Altavoz", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, true)
                )
            }
        }
    }

    private fun isUsbCType(type: Int): Boolean =
        type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                type == AudioDeviceInfo.TYPE_USB_DEVICE  ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        type == AudioDeviceInfo.TYPE_USB_ACCESSORY)

    private fun isUsbcConnected(outputs: Array<AudioDeviceInfo>): Boolean =
        outputs.any { isUsbCType(it.type) }

    fun getCurrentLabel(): String {
        val current = devices.firstOrNull { it.isCurrent }
        return when {
            current == null -> {
                val speaker = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                speaker?.name ?: "Altavoz"
            }
            current.isUsbc -> current.name.ifBlank { "USB-C" }
            current.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ->
                connectedBtName?.ifBlank { "Bluetooth" } ?: current.name.ifBlank { "Bluetooth" }
            else -> current.name.ifBlank { "Altavoz" }
        }
    }

    fun isExternalConnected(): Boolean =
        devices.any { it.isCurrent && it.type != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

    fun routeTo(device: AudioOutputDevice) {
        try {
            when {
                device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                    @Suppress("DEPRECATION") audioManager.isSpeakerphoneOn = true
                    @Suppress("DEPRECATION") audioManager.isBluetoothScoOn = false
                    @Suppress("DEPRECATION") audioManager.stopBluetoothSco()
                }
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                    @Suppress("DEPRECATION") audioManager.isBluetoothScoOn = false
                    @Suppress("DEPRECATION") audioManager.isSpeakerphoneOn = false
                }
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        device.isUsbc -> {
                    @Suppress("DEPRECATION") audioManager.isSpeakerphoneOn = false
                    @Suppress("DEPRECATION") audioManager.isBluetoothScoOn = false
                }
                else -> {}
            }
        } catch (e: Exception) {
            Log.w("AudioOutputManager", "Failed to route audio to device ${device.name}", e)
        }
        refresh()
    }

    private fun refreshBluetooth() {
        try {
            val bm      = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter ?: return
            if (!adapter.isEnabled) return
            adapter.getProfileProxy(context.applicationContext, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    connectedBtName = try {
                        proxy.connectedDevices.firstOrNull()?.name
                    } catch (e: SecurityException) {
                        Log.w("AudioOutputManager", "SecurityException getting Bluetooth device name", e)
                        null
                    }
                    try { adapter.closeProfileProxy(profile, proxy) } catch (e: Exception) {
                        Log.w("AudioOutputManager", "Failed to close Bluetooth profile proxy", e)
                    }
                }
                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.A2DP)
        } catch (e: Exception) {
            Log.w("AudioOutputManager", "Failed to refresh Bluetooth connection", e)
        }
    }
}