package com.rescuenet.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuenet.app.ai.EmergencyClassifier
import com.rescuenet.app.data.LocalEmergencyStore
import com.rescuenet.app.location.GpsLocationManager
import com.rescuenet.app.mesh.BluetoothMeshManager
import com.rescuenet.app.model.EmergencyPacket
import com.rescuenet.app.model.EmergencyType
import com.rescuenet.app.model.MeshPeer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RescueNetApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun RescueNetApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { LocalEmergencyStore(context) }
    val mesh = remember { BluetoothMeshManager(context) }
    val gps = remember { GpsLocationManager(context) }
    val classifier = remember { EmergencyClassifier() }

    var permissionsGranted by remember { mutableStateOf(false) }
    var peers by remember { mutableStateOf<List<MeshPeer>>(emptyList()) }
    var emergencies by remember { mutableStateOf(store.getAll()) }
    var locationText by remember { mutableStateOf("Waiting for GPS…") }
    var selectedType by remember { mutableStateOf(EmergencyType.TRAPPED) }
    var people by remember { mutableStateOf(1) }
    var injured by remember { mutableStateOf(false) }
    var roleRescue by remember { mutableStateOf(false) }
    var aiResult by remember { mutableStateOf("Ready") }

    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.toTypedArray()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> permissionsGranted = result.values.all { it } }

    LaunchedEffect(Unit) { permissionLauncher.launch(permissions) }
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            gps.requestLocation { loc ->
                locationText = if (loc == null) "GPS unavailable" else "%.5f, %.5f".format(loc.latitude, loc.longitude)
            }
            mesh.startAdvertising()
            mesh.startDiscovery { peers = it }
        }
    }

    MaterialTheme(colorScheme = MaterialTheme.colorScheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("RescueNet", fontWeight = FontWeight.Black)
                            Text("OFFLINE DISASTER NETWORK", fontSize = 10.sp, letterSpacing = 1.4.sp)
                        }
                    },
                    actions = {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFE8F7EE), modifier = Modifier.padding(end = 12.dp)) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF16A34A)))
                                Spacer(Modifier.width(6.dp))
                                Text(if (permissionsGranted) "LOCAL READY" else "SETUP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B1220), titleContentColor = Color.White, actionIconContentColor = Color.White)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA)).padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)), shape = RoundedCornerShape(24.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text(if (roleRescue) "RESCUE CENTER MODE" else "EMERGENCY MODE", color = Color(0xFF9CA3AF), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                            Text(if (roleRescue) "Command dashboard" else "Your phone is a network node", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusPill("GPS", locationText != "GPS unavailable" && locationText != "Waiting for GPS…")
                                StatusPill("Bluetooth", permissionsGranted)
                                StatusPill("Internet", false)
                            }
                        }
                    }
                }

                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Color(0xFF2563EB))
                                Spacer(Modifier.width(8.dp))
                                Column { Text("YOUR LOCATION", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold); Text(locationText, fontWeight = FontWeight.SemiBold) }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Emergency type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(EmergencyType.TRAPPED, EmergencyType.FIRE, EmergencyType.FLOOD, EmergencyType.MEDICAL).forEach { type ->
                                    FilterChip(selected = selectedType == type, onClick = { selectedType = type }, label = { Text("${type.emoji} ${type.label}", fontSize = 11.sp) })
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("People: $people", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(12.dp))
                                OutlinedButton(onClick = { people = (people - 1).coerceAtLeast(1) }, modifier = Modifier.size(44.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("−") }
                                Spacer(Modifier.width(6.dp))
                                OutlinedButton(onClick = { people = (people + 1).coerceAtMost(20) }, modifier = Modifier.size(44.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("+") }
                                Spacer(Modifier.width(12.dp))
                                FilterChip(selected = injured, onClick = { injured = !injured }, label = { Text("🩹 Injured") })
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            val ai = classifier.classify("${selectedType.label} emergency", injured, people)
                            aiResult = "AI: ${ai.type.emoji} ${ai.type.label} • ${ai.priority.label} • ${ai.confidence}%"
                            val packet = EmergencyPacket(senderNodeId = "LOCAL-NODE", type = selectedType, people = people, injured = injured, latitude = null, longitude = null, priority = ai.priority)
                            store.save(packet)
                            emergencies = store.getAll()
                        },
                        modifier = Modifier.fillMaxWidth().height(92.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Warning, null, modifier = Modifier.size(28.dp))
                            Text("SEND SOS", fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("Saved locally • ready for mesh relay", fontSize = 11.sp)
                        }
                    }
                }

                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖", fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column { Text("Offline intelligence", fontWeight = FontWeight.Bold); Text(aiResult, fontSize = 12.sp, color = Color(0xFF92400E)) }
                        }
                    }
                }

                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bluetooth, null, tint = Color(0xFF2563EB))
                                Spacer(Modifier.width(8.dp))
                                Text("Nearby RescueNet nodes", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text("${peers.size}", fontWeight = FontWeight.Black, color = Color(0xFF2563EB))
                            }
                            Spacer(Modifier.height(10.dp))
                            if (peers.isEmpty()) Text("No relay nodes discovered yet. Put another RescueNet phone nearby and enable its node mode.", fontSize = 12.sp, color = Color.Gray)
                            peers.take(4).forEach { peer ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                                    Spacer(Modifier.width(10.dp)); Column { Text(peer.name, fontWeight = FontWeight.SemiBold); Text(peer.address, fontSize = 10.sp, color = Color.Gray) }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { roleRescue = !roleRescue }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Text(if (roleRescue) "Victim mode" else "Rescue Center") }
                        OutlinedButton(onClick = { mesh.startDiscovery { peers = it } }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Text("Scan again") }
                    }
                }

                item {
                    Text("Local emergency queue", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                if (emergencies.isEmpty()) {
                    item { Text("No SOS packets stored on this phone.", color = Color.Gray, fontSize = 13.sp) }
                } else {
                    items(emergencies.take(5), key = { it.id }) { packet ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                            Column(Modifier.padding(15.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${packet.type.emoji} ${packet.type.label}", fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.weight(1f)); Text(packet.priority.label, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFB91C1C))
                                }
                                Text("${packet.people} people • ${if (packet.injured) "injury reported" else "no injury reported"}", fontSize = 12.sp, color = Color.Gray)
                                Divider(Modifier.padding(vertical = 8.dp))
                                Text("ID ${packet.id.take(8)} • hops ${packet.hops} • TTL ${packet.ttl}", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                item {
                    Text("Prototype only — do not rely on RescueNet as a certified emergency service. For real emergencies, use official emergency channels when available.", modifier = Modifier.padding(vertical = 8.dp), fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun StatusPill(label: String, active: Boolean) {
    Surface(shape = RoundedCornerShape(30.dp), color = if (active) Color(0xFF163C2A) else Color(0xFF263244)) {
        Text("● $label", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 10.sp, color = if (active) Color(0xFF86EFAC) else Color(0xFFD1D5DB), fontWeight = FontWeight.Bold)
    }
}
