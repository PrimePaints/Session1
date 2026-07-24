package com.example.smarthome

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NestDevice(
    val id: String,
    val name: String,
    val room: String,
    val isConnected: Boolean = true,
    val volume: Int = 80
)

data class FamilyLinkChildDevice(
    val id: String,
    val childName: String,
    val deviceName: String,
    val isScreenPaused: Boolean = false,
    val dailyLimitMinutes: Int = 120,
    val remainingMinutes: Int = 45,
    val lastSyncTime: String = "Just now"
)

data class SmartLightRoutine(
    val id: String,
    val name: String,
    val roomName: String,
    val colorHex: String,
    val associatedTag: String? = null
)

data class SmartHomeLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val targetDevice: String,
    val actionType: String, // "CAST_BROADCAST", "FAMILY_LINK_PAUSE", "LIGHT_ROUTINE"
    val description: String,
    val success: Boolean = true
)

data class SmartHomeState(
    val isBroadcastingEnabled: Boolean = true,
    val selectedNestDeviceId: String = "nest_kids_room",
    val isFamilyLinkSynced: Boolean = true,
    val isSmartLightsEnabled: Boolean = true,
    val nestDevices: List<NestDevice> = listOf(
        NestDevice("nest_kids_room", "Kids Room Nest Mini", "Kids Bedroom", isConnected = true, volume = 85),
        NestDevice("nest_living_room", "Living Room Nest Hub", "Living Room", isConnected = true, volume = 75),
        NestDevice("nest_kitchen", "Kitchen Display", "Kitchen", isConnected = true, volume = 90)
    ),
    val childDevices: List<FamilyLinkChildDevice> = listOf(
        FamilyLinkChildDevice("child_leo_tab", "Leo", "Galaxy Tab A8 (Leo)", isScreenPaused = false, remainingMinutes = 35),
        FamilyLinkChildDevice("child_maya_phone", "Maya", "Pixel 7a (Maya)", isScreenPaused = false, remainingMinutes = 60)
    ),
    val lightRoutines: List<SmartLightRoutine> = listOf(
        SmartLightRoutine("routine_bedtime", "Bedtime Soft Amber", "Kids Bedroom", "#FFB74D", "bedtime"),
        SmartLightRoutine("routine_warning", "Warning Amber Pulse", "Whole House", "#FF5252", "yelling"),
        SmartLightRoutine("routine_dinner", "Dinner Time Green", "Dining Area", "#66BB6A", "dinner")
    ),
    val logs: List<SmartHomeLog> = emptyList(),
    val statusText: String = "Google Home & Family Link Synced"
)

class SmartHomeManager(private val context: Context) {

    private val _state = MutableStateFlow(SmartHomeState())
    val state: StateFlow<SmartHomeState> = _state.asStateFlow()

    fun toggleBroadcasting(enabled: Boolean) {
        _state.value = _state.value.copy(
            isBroadcastingEnabled = enabled,
            statusText = if (enabled) "Google Home Cast Broadcasting Active" else "Broadcasting Disabled"
        )
    }

    fun selectNestDevice(deviceId: String) {
        _state.value = _state.value.copy(selectedNestDeviceId = deviceId)
    }

    fun toggleFamilyLinkSync(enabled: Boolean) {
        _state.value = _state.value.copy(
            isFamilyLinkSynced = enabled,
            statusText = if (enabled) "Family Link Screen Time Rules Synced" else "Family Link Sync Paused"
        )
    }

    fun broadcastClip(padLabel: String, customDeviceId: String? = null) {
        if (!_state.value.isBroadcastingEnabled) return

        val targetId = customDeviceId ?: _state.value.selectedNestDeviceId
        val targetDevice = _state.value.nestDevices.find { it.id == targetId }
        val deviceName = targetDevice?.name ?: "All Nest Speakers"

        val log = SmartHomeLog(
            targetDevice = deviceName,
            actionType = "CAST_BROADCAST",
            description = "📢 Broadcasted audio clip \"$padLabel\" to $deviceName at ${targetDevice?.volume ?: 80}% volume"
        )

        _state.value = _state.value.copy(
            logs = listOf(log) + _state.value.logs,
            statusText = "Broadcasted \"$padLabel\" to $deviceName"
        )
    }

    fun toggleChildScreenTimePause(childDeviceId: String) {
        val updated = _state.value.childDevices.map { child ->
            if (child.id == childDeviceId) {
                val newStatus = !child.isScreenPaused
                val actionMsg = if (newStatus) "PAUSED" else "RESUMED"
                val log = SmartHomeLog(
                    targetDevice = child.deviceName,
                    actionType = "FAMILY_LINK_PAUSE",
                    description = "📱 Family Link screen access $actionMsg for ${child.childName}'s device (${child.deviceName})"
                )
                _state.value = _state.value.copy(logs = listOf(log) + _state.value.logs)
                child.copy(isScreenPaused = newStatus, lastSyncTime = "Just now")
            } else {
                child
            }
        }
        _state.value = _state.value.copy(childDevices = updated)
    }

    fun triggerLightRoutine(routineId: String) {
        val routine = _state.value.lightRoutines.find { it.id == routineId } ?: return
        val log = SmartHomeLog(
            targetDevice = routine.roomName,
            actionType = "LIGHT_ROUTINE",
            description = "💡 Activated smart lighting routine \"${routine.name}\" in ${routine.roomName}"
        )
        _state.value = _state.value.copy(
            logs = listOf(log) + _state.value.logs,
            statusText = "Triggered light routine \"${routine.name}\""
        )
    }

    fun clearLogs() {
        _state.value = _state.value.copy(logs = emptyList())
    }
}
