# Deskflow on Android: Android Client for Mouse and Keyboard Sharing

**Input Leaf** is an open-source Android client for the software KVM workflow used by [Deskflow](https://deskflow.org/) and [Input Leap](https://github.com/input-leap/input-leap). It allows a computer running Deskflow to share its keyboard and mouse with an Android phone or tablet over your local network.

## Official Deskflow Links

- **Official Website:** [deskflow.org](https://deskflow.org/)
- **Official Repository:** [github.com/deskflow/deskflow](https://github.com/deskflow/deskflow)
- **Releases & Downloads (Windows, macOS, Linux):** [Deskflow Releases](https://github.com/deskflow/deskflow/releases)

---

## Use Case

If you want to **use your computer mouse and keyboard on an Android phone or tablet**, a software KVM switch is much faster and more flexible than swapping Bluetooth peripherals or using physical KVM hardware. Input Leaf acts as the Android screen in your multi-device desktop layout.

```text
┌─────────────────────────────────┐
│        Computer + Deskflow      │
│          (Server Host)          │
└────────────────┬────────────────┘
                 │
                 │  Wi-Fi / Ethernet LAN (TCP Port 24800)
                 │  Seamless mouse crossover & keystrokes
                 ▼
┌─────────────────────────────────┐
│       Android + Input Leaf      │
│          (Client Screen)        │
└─────────────────────────────────┘
```

---

## Quick Setup

1. **Install Deskflow on PC:** Download the latest build from the [Deskflow Releases page](https://github.com/deskflow/deskflow/releases).
2. **Configure Deskflow Server:**
   - Launch Deskflow on your desktop and set it to **Server mode**.
   - Open **Configure Server** and drag a screen to represent your Android device onto the grid (for example, placed to the right of your primary monitor).
   - Set the **Screen name** to match the client name configured in Input Leaf **Settings -> Screen name** (defaults to your sanitized device model, such as `pixel-8` or `android-phone`).
   - Click **Start** to begin listening on port 24800.
3. **Install Input Leaf on Android:** Download and install the latest APK from [Input Leaf GitHub Releases](https://github.com/anasvhora284/input-leaf/releases/latest).
4. **Choose Input Injection Method:**
   - **Shizuku (Recommended):** Set up [Shizuku](https://shizuku.rikka.app/) for lowest latency (<5ms) and native system shortcut support (`Alt+Tab`, Home, Back).
   - **Accessibility Mode:** Enable Input Leaf in Android **Settings -> Accessibility** for zero-setup operation.
5. **Connect:** Ensure both devices are on the same Wi-Fi/LAN, select your Deskflow server in Input Leaf, and tap **Connect**.
6. **Glide Across Screens:** Move your PC cursor past the configured display border to begin controlling your Android device.

---

## Input Method Options

| Feature | Shizuku (Recommended) | Accessibility Mode |
| :--- | :---: | :---: |
| **Root Required** | No | No |
| **Latency** | **Lowest (< 5ms)** | Low (~15–25ms) |
| **System Hotkeys** | Full (`Alt+Tab`, `Meta`, `Esc`, `Ctrl+C/V`) | Basic text input only |
| **Cursor Emulation** | Native system pointer | Touch gesture emulation |
| **Setup Tool** | Wireless Debugging via Shizuku app | Stock Android Settings |

---

## Troubleshooting

- **Server not discovered:** Verify LAN connectivity, disable router AP/client isolation, and ensure firewall allows inbound traffic on **TCP Port 24800**.
- **Keyboard shortcuts missing:** Switch to **Shizuku mode**; stock Accessibility APIs restrict hardware shortcut dispatch.
- **Background disconnects:** Review Android battery optimization for Input Leaf and set to **Unrestricted**.

---

## Downloads & Links

- **Download Input Leaf APK:** [Latest Releases](https://github.com/anasvhora284/input-leaf/releases/latest)
- **Input Leaf Website:** [inputleaf.anasvhora.tech](https://inputleaf.anasvhora.tech/)
- **Main Project README:** [Input Leaf on GitHub](https://github.com/anasvhora284/input-leaf)
- **Deskflow Official:** [deskflow.org](https://deskflow.org/)
