package no.kess.fisherman.input;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.W32APIOptions;
import no.kess.fisherman.util.Humanizer;

public class NativeKeyboard {

    public static final int SCANCODE_ESC = 0x01;
    public static final int SCANCODE_SPACE = 0x39;

    public static final int SCANCODE_F10 = 0x44;
    private static final int KEYEVENTF_KEYUP = 0x0002;
    private static final int KEYEVENTF_SCANCODE = 0x0008;

    public static int getScanCode(int virtualKey) {
        return User32Ext.INSTANCE.MapVirtualKey(virtualKey, 0); // MAPVK_VK_TO_VSC = 0
    }

    public static boolean isKeyPressed(int virtualKey) {
        return (User32Ext.INSTANCE.GetAsyncKeyState(virtualKey) & 0x8000) != 0;
    }

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

    public interface User32Ext extends User32 {
        User32Ext INSTANCE = Native.load("user32", User32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

        int MapVirtualKey(int uCode, int uMapType);

        short GetAsyncKeyState(int vKey);
    }
}

