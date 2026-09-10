# Input Leap on Android: Control an Android Phone with a PC Mouse and Keyboard

**Input Leaf** is an open-source Android client for [Input Leap](https://github.com/input-leap/input-leap). It lets an Android phone or tablet participate in an Input Leap software-KVM setup so your computer's mouse and keyboard can seamlessly control your Android device over the local network.

## Official Input Leap Links

- **Official Repository:** [github.com/input-leap/input-leap](https://github.com/input-leap/input-leap)
- **Releases & Downloads (Windows, macOS, Linux):** [Input Leap Releases](https://github.com/input-leap/input-leap/releases)
- **Official Documentation:** [Input Leap Wiki](https://github.com/input-leap/input-leap/wiki)

---

## What You Need

- A Windows, macOS, or Linux computer running the [Input Leap Server](https://github.com/input-leap/input-leap/releases).
- An Android phone or tablet connected to the same local network (Wi-Fi or Ethernet).
- [Input Leaf APK](https://github.com/anasvhora284/input-leaf/releases/latest) installed on your Android device.
- Either [Shizuku](https://shizuku.rikka.app/) (recommended for lowest latency) or stock Android Accessibility mode.

---

## How the Setup Works

```text
┌─────────────────────────────────┐
│           PC / Laptop           │
│        Input Leap Server        │
└────────────────┬────────────────┘
                 │
                 │  Wi-Fi / Ethernet LAN (TCP Port 24800)
                 │  Shared Mouse Coordinates & Keystrokes
                 ▼
┌─────────────────────────────────┐
│     Android Phone / Tablet      │
│        Input Leaf Client        │
└─────────────────────────────────┘
```

Input Leap runs on your PC as the server, capturing your mouse and keyboard when your cursor hits the edge of the monitor. Input Leaf runs on Android as the client, receiving the input stream and injecting it as native Android touches, pointer movements, and keystrokes.

---

## Step-by-Step Setup

1. **Install Input Leap on PC:** Download the installer for Windows, macOS, or Linux from [Input Leap Releases](https://github.com/input-leap/input-leap/releases).
2. **Configure Server Mode:**
   - Open Input Leap on your PC and select **Server (share this computer's mouse and keyboard)**.
   - Click **Configure Server...** to open the interactive screen grid.
   - Drag a new monitor icon from the top-right tray and place it next to your PC monitor (for example, to the right).
   - Double-click the newly placed screen and set the **Screen name** to match your Android device name (viewable or editable in Input Leaf **Settings -> Screen Name**).
   - Click **OK**, then click **Start** on the main Input Leap window.
3. **Install Input Leaf on Android:** Download and install the latest APK from [Input Leaf GitHub Releases](https://github.com/anasvhora284/input-leaf/releases/latest).
4. **Grant Permissions / Choose Injection Mode:**
   - **Shizuku (Recommended):** Set up [Shizuku](https://shizuku.rikka.app/) via Wireless Debugging. Grant Input Leaf Shizuku permission for native system injection and full shortcut support (`Alt+Tab`, Home, Back).
   - **Accessibility Mode:** If you prefer zero third-party tools, enable Input Leaf in Android **Settings -> Accessibility**.
5. **Connect:**
   - Ensure both devices are connected to the same Wi-Fi network.
   - Input Leaf will discover your Input Leap server automatically via mDNS. You can also tap **+** to enter the PC's IP address directly.
   - Tap **Connect**.
6. **Glide to Android:** Move your mouse cursor across the configured monitor edge. The cursor will glide directly onto your Android screen!

---

## Shizuku vs. Accessibility

| Capability | Shizuku (Recommended) | Accessibility Mode |
| :--- | :---: | :---: |
| **Root Required** | No | No |
| **Input Latency** | **Lowest (< 5ms)** | Low (~15–25ms) |
| **System Shortcuts** | Full (`Alt+Tab`, `Meta`, `Esc`, `Ctrl+C/V`) | Basic typing only |
| **Mouse Interaction** | Native system pointer | Gesture/touch emulation |
| **Setup Overhead** | One-time Wireless Debugging | Immediate in Settings |

---

## Troubleshooting

### Android cannot discover the Input Leap server
- Make sure both devices are on the same local subnet / Wi-Fi band.
- Check Windows Defender Firewall or Linux firewall (`ufw allow 24800/tcp`) to ensure port **24800** is open for inbound connections.
- Ensure the **Screen name** in Input Leap matches the client name in Input Leaf exactly.

### Mouse works, but keyboard shortcuts (like Alt+Tab) do not
- Android restricts accessibility services from dispatching hardware-level navigation shortcuts. Switch to **Shizuku mode** for full system shortcut access.

### The connection stops when the phone locks
- Disable battery optimization for Input Leaf in Android **Settings -> Apps -> Input Leaf -> Battery -> Unrestricted**.

---

## Downloads & Community

- **Download APK:** [Input Leaf Releases](https://github.com/anasvhora284/input-leaf/releases/latest)
- **Website:** [inputleaf.anasvhora.tech](https://inputleaf.anasvhora.tech/)
- **Main README:** [Input Leaf on GitHub](https://github.com/anasvhora284/input-leaf)
- **Input Leap Upstream:** [input-leap/input-leap](https://github.com/input-leap/input-leap)
