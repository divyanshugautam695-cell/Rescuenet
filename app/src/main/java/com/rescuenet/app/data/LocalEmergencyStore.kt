package com.rescuenet.app.data

import android.content.Context
import com.rescuenet.app.model.EmergencyPacket
import org.json.JSONObject

/** Small local-first queue. It deliberately has no network dependency. */
class LocalEmergencyStore(context: Context) {
    private val prefs = context.getSharedPreferences("rescuenet_store", Context.MODE_PRIVATE)

    fun save(packet: EmergencyPacket) {
        val current = getAll().toMutableList()
        if (current.none { it.id == packet.id }) current.add(0, packet)
        prefs.edit().putString(KEY_QUEUE, current.joinToString("\n") { encode(it) }).apply()
    }

    fun getAll(): List<EmergencyPacket> = prefs.getString(KEY_QUEUE, "")
        .orEmpty()
        .lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull { runCatching { decode(it) }.getOrNull() }
        .toList()

    private fun encode(p: EmergencyPacket) = JSONObject().apply {
        put("id", p.id); put("sender", p.senderNodeId); put("type", p.type.name)
        put("people", p.people); put("injured", p.injured)
        put("lat", p.latitude); put("lon", p.longitude); put("created", p.createdAt)
        put("priority", p.priority.name); put("hops", p.hops); put("ttl", p.ttl)
    }.toString()

    private fun decode(raw: String): EmergencyPacket {
        val j = JSONObject(raw)
        return EmergencyPacket(
            id = j.getString("id"), senderNodeId = j.getString("sender"),
            type = com.rescuenet.app.model.EmergencyType.valueOf(j.getString("type")),
            people = j.getInt("people"), injured = j.getBoolean("injured"),
            latitude = if (j.isNull("lat")) null else j.getDouble("lat"),
            longitude = if (j.isNull("lon")) null else j.getDouble("lon"),
            createdAt = j.getLong("created"),
            priority = com.rescuenet.app.model.Priority.valueOf(j.getString("priority")),
            hops = j.getInt("hops"), ttl = j.getInt("ttl")
        )
    }

    private companion object { const val KEY_QUEUE = "emergency_queue" }
}
