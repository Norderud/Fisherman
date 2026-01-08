# Fisherman

**An undetectable, external World of Warcraft fishing automation tool.**

## ? Overview

Fisherman is a Java-based bot designed with a "Passive Observation, Natural Interaction" philosophy. Unlike traditional bots, it **does not** read or write to the game's memory, nor does it inject code. Instead, it relies on:

*   **Audio Analysis:** Listens to system audio to detect the splashing sound of a catch.
*   **Hardware Input Emulation:** Uses JNA and specific Scan Codes to mimic hardware keyboard drivers for input.
*   **Optical Bridge:** Reads a strict 1x1 pixel area to communicate safe/unsafe states (e.g., Inventory Full) from a companion Lua addon.

## ? Features

*   **100% External:** Zero interaction with the WoW process memory.
*   **Human-like Behavior:** All delays and reaction times follow a Gaussian (Normal) distribution. No static `Thread.sleep(1000)` calls.
*   **Audio Trigger:** Detects fish by monitoring specific amplitude spikes (RMS) in audio output.
*   **Safety Checks:** Automatically stops when inventory is full (requires Lua addon).

## ? Prerequisites

*   **Java:** JDK 17 or higher.
*   **Build System:** Maven.
*   **OS:** Windows (due to dependence on `User32.dll` for input).

## ?? Game Configuration

For the bot to function correctly, your World of Warcraft client must be configured as follows:

1.  **Interact Key:** Bound to `F10`.
2.  **Audio:**
    *   Master Volume: 100%
    *   Sound Effects: 100%
    *   Music/Ambience: 0% (to reduce noise)
3.  **Console Variable:** Run `/console SoftTargetInteractRange 30` to maximize interaction range.
4.  **Fishing Key:** Bound to `F6`.

## ? Lua Addon Setup

Install the Fisherman addon to facilitate communication between the game and the bot:

## ?? Build & Run

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/Fisherman.git
    cd Fisherman
    ```

2.  **Build with Maven:**
    ```bash
    mvn clean package
    ```

3.  **Run:**
    *   Ensure WoW is running and your character is near water with a fishing pole equipped.
    *   Run the compiled JAR or direct from IDE (e.g., `FishermanMain.java`).

## ?? Disclaimer

This software is for **educational purposes only**. Automating gameplay may violate the Terms of Service of your game provider. The author accepts no responsibility for bans or account suspensions resulting from the use of this tool. Use at your own risk.
g