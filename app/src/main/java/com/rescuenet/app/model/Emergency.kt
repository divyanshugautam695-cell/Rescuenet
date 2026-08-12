package com.rescuenet.app.model

import java.util.UUID

enum class EmergencyType(val label: String, val emoji: String) {
    EARTHQUAKE("Earthquake", "🌐"),
    FLOOD("Flood", "🌊"),
    FIRE("Fire", "🔥"),
    MEDICAL("Medical", "🩺"),
    TRAPPED("Trapped", "🚨"),
    OTHER("Other", "⚠️")
}

enum class Priority(val label: String) { CRITICAL("CRITICAL"), HIGH("HIGH"), MEDIUM("MEDIUM"), LOW("LOW") }

data class EmergencyPacket(
    val id: String = UUID.randomUUID().toString(),
    val senderNodeId: String,
    val type: EmergencyType,
    val people: Int,
    val injured: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: Priority,
    val hops: Int = 0,
    val ttl: Int = 8
) {
    fun nextHop(): EmergencyPacket = copy(hops = hops + 1, ttl = ttl - 1)
}

data class MeshPeer(
    val name: String,
    val address: String,
    val rssi: Int? = null,
    val connected: Boolean = false
)
