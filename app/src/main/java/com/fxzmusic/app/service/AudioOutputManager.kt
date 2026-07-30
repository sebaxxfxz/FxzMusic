package com.fxzmusic.app.service

import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
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

    private var selectedDeviceId by mutableStateOf<Int?>(null)

    private val audioManager = context.applicationContext
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var lastRefreshTime = 0L
    private val REFRESH_DELAY_MS = 300L

    init {
        refresh()
    }

    fun refresh() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime < REFRESH_DELAY_MS) return
        lastRefreshTime = now

        refreshBluetooth()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                devices = outputs.mapNotNull { info ->
                    val isUsbc = isUsbCType(info.type)

                    val productName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        info.productName?.toString()?.takeIf { it.isNotBlank() }
                    } else null

                    val typeName = when {
                        isUsbc -> productName ?: "Auriculares USB-C"
                        info.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER   -> "Altavoz del teléfono"
                        info.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES  -> productName ?: "Auriculares 3.5mm"
                        info.type == AudioDeviceInfo.TYPE_WIRED_HEADSET     -> productName ?: "Auriculares 3.5mm"
                        info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP    -> productName ?: connectedBtName ?: "Dispositivo Bluetooth"
                        info.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO     -> return@mapNotNull null
                        info.type == AudioDeviceInfo.TYPE_HEARING_AID       -> productName ?: "Audífono"
                        else -> return@mapNotNull null
                    }

                    @Suppress("DEPRECATION")
                    val isWired = audioManager.isWiredHeadsetOn

                    val isCurrent = if (selectedDeviceId != null) {
                        info.id == selectedDeviceId
                    } else {
                        when {
                            isUsbc -> isWired
                            info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> audioManager.isBluetoothA2dpOn
                            info.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                                    info.type == AudioDeviceInfo.TYPE_WIRED_HEADSET -> isWired && !isUsbcConnected(outputs)
                            info.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> !audioManager.isBluetoothA2dpOn && !isWired
                            else -> false
                        }
                    }

                    AudioOutputDevice(
                        id        = info.id,
                        name      = typeName,
                        type      = info.type,
                        isCurrent = isCurrent,
                        isUsbc    = isUsbc
                    )
                }.distinctBy { it.id }
            } catch (e: Exception) {
                Log.w("AudioOutputManager", "Error al obtener dispositivos de audio", e)
                devices = listOf(
                    AudioOutputDevice(0, "Altavoz del teléfono", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, true)
                )
            }
        }
    }

    private fun isUsbCType(type: Int): Boolean =
        type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                type == AudioDeviceInfo.TYPE_USB_DEVICE  ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_USB_ACCESSORY)

    private fun isUsbcConnected(outputs: Array<AudioDeviceInfo>): Boolean =
        outputs.any { isUsbCType(it.type) }

    fun getCurrentLabel(): String {
        val current = devices.firstOrNull { it.isCurrent }
        return current?.name ?: "Altavoz del teléfono"
    }

    fun routeTo(device: AudioOutputDevice) {
        selectedDeviceId = device.id
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val targetInfo = outputs.firstOrNull { it.id == device.id }
                    ?: outputs.firstOrNull { it.type == device.type }

                val player = PlaybackService.exoPlayerInstance
                if (player != null) {
                    if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                        val speakerInfo = outputs.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                        player.setPreferredAudioDevice(speakerInfo)
                    } else {
                        player.setPreferredAudioDevice(targetInfo)
                    }
                }
            } catch (e: Exception) {
                Log.w("AudioOutputManager", "Error al cambiar preferred audio device en ExoPlayer", e)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    val speakerDevice = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speakerDevice != null) {
                        audioManager.setCommunicationDevice(speakerDevice)
                    } else {
                        audioManager.clearCommunicationDevice()
                    }
                } else {
                    val targetDevice = audioManager.availableCommunicationDevices.firstOrNull { it.id == device.id }
                        ?: audioManager.availableCommunicationDevices.firstOrNull { it.type == device.type }
                    if (targetDevice != null) {
                        audioManager.setCommunicationDevice(targetDevice)
                    } else {
                        audioManager.clearCommunicationDevice()
                    }
                }
            } else {
                if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = true
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = false
                    @Suppress("DEPRECATION")
                    audioManager.stopBluetoothSco()
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.isSpeakerphoneOn = false
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = false
                }
            }
        } catch (e: Exception) {
            Log.w("AudioOutputManager", "Error al enrutar audio a ${device.name}", e)
        }
        refresh()
    }

    private fun refreshBluetooth() {
        try {
            val bm = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter ?: return
            try { if (!adapter.isEnabled) return } catch (_: SecurityException) { return }
            adapter.getProfileProxy(context.applicationContext, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val name = try {
                        proxy.connectedDevices.firstOrNull()?.name
                    } catch (e: SecurityException) {
                        null
                    }
                    try { adapter.closeProfileProxy(profile, proxy) } catch (_: Exception) {}
                    Handler(Looper.getMainLooper()).post {
                        if (!name.isNullOrBlank()) {
                            connectedBtName = name
                        }
                    }
                }
                override fun onServiceDisconnected(profile: Int) {}
            }, BluetoothProfile.A2DP)
        } catch (e: Exception) {
            Log.w("AudioOutputManager", "Error al consultar nombre de Bluetooth", e)
        }
    }
}
