package no.kess.fisherman;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

public class NativeKeyboard {

    public static final int SCANCODE_F6 = 0x40;
    public static final int SCANCODE_F10 = 0x44;

    private static final int KEYEVENTF_KEYUP = 0x0002;
    private static final int KEYEVENTF_SCANCODE = 0x0008;

    public static void sendKeyDown(int scanCode) {
        sendInput(scanCode, KEYEVENTF_SCANCODE);
    }

    public static void sendKeyUp(int scanCode) {
        sendInput(scanCode, KEYEVENTF_SCANCODE | KEYEVENTF_KEYUP);
    }

    private static void sendInput(int scanCode, int flags) {
        WinUser.INPUT input = new WinUser.INPUT();
        input.type = new WinDef.DWORD(WinUser.INPUT.INPUT_KEYBOARD);
        input.input.setType("ki");
        input.input.ki.wScan = new WinDef.WORD(scanCode);
        input.input.ki.time = new WinDef.DWORD(0);
        input.input.ki.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
        input.input.ki.wVk = new WinDef.WORD(0);
        input.input.ki.dwFlags = new WinDef.DWORD(flags);

        User32.INSTANCE.SendInput(new WinDef.DWORD(1), (WinUser.INPUT[]) input.toArray(1), input.size());
    }

    public static void sendKey(int scanCode) {
        sendKeyDown(scanCode);
        // Dwell time: Every key press must be split into KeyDown -> Sleep(Gaussian) -> KeyUp
        Humanizer.sleep(85, 20);
        sendKeyUp(scanCode);
    }
}

