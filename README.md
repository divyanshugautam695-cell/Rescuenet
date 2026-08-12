# RescueNet 🚨📡

RescueNet is an offline-first disaster communication prototype designed to help people exchange emergency messages when internet/mobile connectivity is unavailable.

## Core concept

- 📍 GPS/GNSS for location — GPS determines position but does not transmit messages.
- 📡 Bluetooth / Wi-Fi Direct for local device-to-device communication.
- 🔄 Store-and-forward routing so messages can travel across multiple participating devices.
- 🆘 One-tap SOS with location, emergency type, people affected and priority.
- 🗺️ Offline map support planned.
- 🤖 Optional on-device/free AI for emergency classification and prioritization.
- ☁️ Internet is optional and can later be used by a gateway device to synchronize queued messages.

## Planned architecture

```text
Victim Phone A
     │ Bluetooth / local Wi-Fi
     ▼
Relay Phone B → Relay Phone C
     │
     ▼
Rescue Center Phone / Gateway
```

## Development roadmap

1. Professional Android UI and navigation
2. GPS location and local SOS storage
3. Bluetooth discovery and emergency packet exchange
4. Store-and-forward mesh routing with TTL/message IDs
5. Rescue Center dashboard
6. Offline maps and safe-location data
7. Free/on-device AI emergency classification
8. Real-device demo and reliability testing

> RescueNet is a prototype for controlled demonstrations. It must not be treated as a certified emergency communication system or a replacement for official emergency services.
