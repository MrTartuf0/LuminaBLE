# LuminaBLE - The Offline Smart Blind System 🌅

![Kotlin](https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![C++](https://img.shields.io/badge/C++-00599C?style=for-the-badge&logo=c%2B%2B&logoColor=white)
![Arduino](https://img.shields.io/badge/Arduino_Nano_33_IoT-00979D?style=for-the-badge&logo=arduino&logoColor=white)

An offline, Android-integrated IoT automation system that opens the bedroom blinds perfectly in sync with the morning alarm. 

## 📖 How It Started (The Dorm Dilemma)
So, here's the story. I live in a university dorm in Perugia, and waking up early has never been my strong suit. I figured it would be awesome to have the window blinds open automatically the exact moment my phone alarm goes off, letting the morning sun do the heavy lifting.

The easy route would have been buying a cheap Wi-Fi smart switch (like a Shelly or Sonoff). The catch? The dorm's enterprise Wi-Fi uses AP isolation and captive portals. Standard smart home devices are completely blocked from talking to each other. 

Instead of giving up, I went full DIY. I 3D-printed a custom mount, hooked a servo motor to the physical wall switch, wired it to an Arduino Nano 33 IoT, and built a native Android app from scratch. It communicates directly via Bluetooth Low Energy (BLE). No Wi-Fi needed, no cloud services, and no dorm network rules broken. Just me, my alarm, and some hardware hacking.

## 📸 Demo & Hardware

<img width="1512" height="573" alt="27815219-692E-4FC6-BECE-D3939F531E7E_1_201_a" src="https://github.com/user-attachments/assets/524ca87a-43f4-474b-9f61-e8a53d7e98fc" />


https://github.com/user-attachments/assets/b6a57758-7a99-4972-a61a-0ee88c558888






## 🏗️ The Tech Stack (4-Tier IoT Architecture)

1. **Device (Actuator):** A 5V Servo Motor physically pushing the wall switch.
2. **Gateway (Microcontroller):** An Arduino Nano 33 IoT running custom C++ firmware. It acts as a BLE Peripheral, exposing a custom GATT Service to listen for specific bytes (`0x01` to open, `0x02` to close).
3. **Edge Processing:** Right now, my Android smartphone acts as the localized Edge Gateway. 
4. **Application:** A native Android App built in Kotlin & Jetpack Compose to manage the UI and OS integration.

## 🔋 Beating Android's Battery Police
Modern Android versions (12+) are ruthless with background apps to save battery life. To make sure the blinds open precisely when the alarm rings without draining my phone overnight, I had to dive deep into Android's lifecycle constraints:
* **`AlarmManager`:** Hooking exactly into the native system alarm using `SCHEDULE_EXACT_ALARM`.
* **Broadcast Receivers:** Waking the app up from a completely killed state.
* **Foreground Services:** Temporarily bypassing Doze Mode just long enough to handle the BLE discovery, connect to the Arduino, send the payload, and shut down gracefully.

## 🚀 What's Next? (V2 & V3)

- [ ] **Dedicated Local Edge Server:** Offloading the automation scheduler from the mobile device to a local Apple Silicon server (Mac Mini) running a Python/Node.js BLE script.
- [ ] **Presence Detection:** The local server will ping my smartphone's local IP. If I'm not in the room (e.g., I went back to my hometown for the weekend), the blinds stay closed.
- [ ] **Hardware-Level Security:** Currently, the BLE payload is unencrypted. The next step is utilizing the **ATECC608A** crypto-authentication chip onboard the Arduino to compute HMAC-SHA256 hashes for a Challenge-Response handshake. No replay attacks allowed!

## 🛠️ Hardware Requirements
* Arduino Nano 33 IoT
* 5V Servo Motor (e.g., MG996R)
* 3D Printed custom switch mount
* Android Smartphone (Android 12+ recommended)

---
*Built for the fun of hardware hacking, overcoming OS limitations, and getting out of bed on time.*
