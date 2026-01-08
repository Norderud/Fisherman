# 🎣 Fisherman

**The Ghost in the Machine: Undetectable, External WoW Fishing Automation.**

> **⚠️ DISCLAIMER:** For educational purposes only. Use at your own risk.

## 🔮 Philosophy: "Passive Observation"
Fisherman is built different. It ignores the game's memory completely.
*   **Audio Analysis:** Listens for the splash. 🔊
*   **Hardware Emulation:** Mimics physical keyboard drivers via JNA. ⌨️
*   **Optical Bridge:** Reads a 1x1 pixel for game state (e.g., Inventory Full). 👁️

## ✨ Key Features
*   **100% External:** Zero memory injection or reading.
*   **Human-like:** Randomized Gaussian distribution delays. No robotic timers.
*   **Smart Triggers:** Audio RMS spike detection for instant catches.

## 🛠️ Requirements
*   **Java 17+** & **Maven**
*   **Windows** (Required for `User32.dll` access)

## 🚀 Quick Start

### 1. Game Config
*   **Bind keys:** Fishing to `F6`, Interact with Target to `F10`.
*   **Audio:** Max SFX/Master, mute Music/Ambience.
*   **Macro:** `/console SoftTargetInteractRange 30`

### 2. Build & Deploy
```bash
git clone https://github.com/yourusername/Fisherman.git
cd Fisherman
mvn clean package
# Run the JAR or FishermanMain.java while near water!