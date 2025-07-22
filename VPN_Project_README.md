# Java-Based VPN GUI Project using OpenVPN

## 📝 Abstract

This project implements a complete **Java-based GUI VPN Client** that integrates seamlessly with **OpenVPN**. Designed to support multiple platforms (Windows, Linux, macOS, Android, iOS), it enables users to select platform-specific `.ovpn` configuration files, input credentials, connect/disconnect from the VPN, and monitor connection status in a user-friendly interface. It automates OpenVPN execution and logs all activities for auditing and diagnostics.

📌 Table of Contents

Abstract

Project Goals

Key Features

System Requirements

System Architecture

Flowcharts & Diagrams

Installation and Setup

How to Use

Final Notes

Author

---

## 🎯 Project Goals

- Provide a **graphical user interface** for controlling OpenVPN connections.
- Automate the VPN connection using `.ovpn` configuration files.
- Allow **platform-based configuration selection** (Android, Windows, Linux, macOS, iOS).
- Support automatic logging, user authentication, and timestamp tracking.
- Provide real-time connection feedback and log error/status messages to the GUI and log files.

---

## 🌟 Key Features

- ✅ GUI-based VPN Client written in Java using Swing.
- 🔐 Credential entry with secure password handling.
- 🔄 Platform-aware config selection (Android, iOS, macOS, Linux, Windows).
- 📄 Reads `.ovpn` files per selected platform.
- 🧠 Saves credentials temporarily to `vpn-auth.txt` for OpenVPN execution.
- ⚙️ Executes OpenVPN using `cmd.exe` on Windows.
- 🛠 Logs output from OpenVPN in real-time to GUI, terminal, and `client.log`.
- 📆 Timestamps connection initiation and logs it to `login_history.txt`.
- ❌ Detects and displays errors including incorrect credentials, DNS leaks, and disconnections.
- 📡 Supports OpenVPN Data Channel Offload (DCO) for faster tunneling.
- 📁 Organized folder structure with logs, configs, and executables separated.

---

## 💻 System Requirements

### Software
- Java JDK 17 or higher
- OpenVPN 2.6 or later
- Windows/macOS/Linux
- ProtonVPN configuration files (`*.ovpn`)

### Hardware
- Standard PC with minimum 4 GB RAM
- Active internet connection

---

## 🏗️ System Architecture

### 🔹 Client Side
- **GUI Interface**: Built using Java Swing.
- **OpenVPN Execution**: Initiated via `ProcessBuilder`.
- **Credential Management**: Stores temporary credentials in `vpn-auth.txt`.

### 🔹 Core Components
- **NetUtils**: Logs messages and handles formatting.
- **ClientGUI**: Front-end for user input and control.
- **Client**: Core networking functionality (VPN + proxy).
- **ProxyMain**: Optional proxy forwarding.

### 🔹 VPN Subsystem
- Runs `openvpn.exe` with selected `.ovpn` file.
- Redirects stdout to GUI and logs.
- Tracks and handles all runtime errors.

---

## 🔄 Flowcharts & Diagrams

### VPN Connection Flow

[Start GUI]
      ↓
[User selects Platform]
      ↓
[User selects .ovpn file]
      ↓
[Enter Username + Password]
      ↓
[vpn-auth.txt created]
      ↓
[Execute OpenVPN via ProcessBuilder]
      ↓
[Monitor output → GUI + client.log]
      ↓
[Success?] ── Yes → [Connected]
         └── No  → [Display Error]

### Logging Workflow

[OpenVPN Output Stream]
           ↓
[BufferedReader (thread)]
           ↓
[NetUtils.logClient()]
           ↓
[client.log + GUI Console]

---

## ⚙️ Installation and Setup

1. **Install Java JDK**  
   Download and install JDK 17+ from [https://adoptopenjdk.net](https://adoptopenjdk.net)

2. **Install OpenVPN**  
   Install OpenVPN from [https://openvpn.net/community-downloads](https://openvpn.net/community-downloads)

3. **Add ProtonVPN `.ovpn` files**  
   Download from [https://account.protonvpn.com](https://account.protonvpn.com) and organize as per platform.

4. **Compile the project**  
   javac -d out -sourcepath src src/main/functionality/ClientGUI.java

5. **Run the GUI**
   java -cp out functionality.ClientGUI

---

## 🚀 How to Use

1. **Launch** `ClientGUI.java`.
2. **Select a platform** from the dropdown (e.g., `windows`, `android`, etc.).
3. **Choose a VPN config file** (`.ovpn`) from the second dropdown.
4. **Enter your credentials**:
   - Username: `5KMU58HDYulD24IR`
   - Password: `11W1kY0UHFi55nG5LG2xwsBe14YKLjPn.`
5. **Click “Connect VPN.”**
6. VPN log output is streamed live:
   - In the GUI
   - In the terminal
   - In the file: `logs/client.log`

To **disconnect**, you can currently close the OpenVPN process manually (Task Manager or Command Line). GUI Disconnect Button will be added in future releases.

---

## 📌 Final Notes

- This VPN project is intended for **educational and secure research purposes**.
- The OpenVPN process is **fully integrated** within Java GUI, giving a hybrid native-Java VPN launcher.
- This system does **mask your IP**, assuming the `.ovpn` configs are valid and authenticated.

For any issue, logs in `logs/client.log` and `login_history.txt` will help you debug.

---

## 👨‍💻 Author

**Srijato’s VPN Project**  
Java | OpenVPN | Secure Auth GUI  
2025



