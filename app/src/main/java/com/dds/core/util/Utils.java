package com.dds.core.util;

import android.app.ActivityManager;
import android.app.Application;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.dds.App;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class Utils {
    //设置界面全屏
    public static void setFullScreenWindowLayout(Window window) {
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.
                SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.TRANSPARENT);
        }
        //设置页面全屏显示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(layoutParams);
        }
    }

    public static int getStatusBarHeight() {
        Resources resources = App.getInstance().getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        return resources.getDimensionPixelSize(resourceId);
    }


    public static boolean isAppRunningForeground() {
        ActivityManager activityManager =
                (ActivityManager) App.getInstance().getSystemService(Application.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcessInfo : runningAppProcesses) {
            if (appProcessInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                if (appProcessInfo.processName.equals(App.getInstance().getApplicationInfo().processName))
                    return true;
            }
        }
        return false;
    }
    public static byte[] protocolCmd(byte cmdType, String object) {
        byte finalData[] = null;
        String haxData = null;
        if (null != object) {
            byte[] dataBytes = strToByte(object);
            finalData = new byte[Config.HEAD_LENGTH + dataBytes.length];
            haxData = String.format("%08x", dataBytes.length);
            System.arraycopy(dataBytes, 0, finalData, Config.HEAD_LENGTH, dataBytes.length);
        } else {
            finalData = new byte[Config.HEAD_LENGTH];
            haxData = "00000000";
        }
        finalData[0] = Config.PROTOCOL_TYPE;
        finalData[1] = cmdType;
        byte[] bytesDataLength = hexToByte(haxData);
        System.arraycopy(bytesDataLength, 0, finalData, 2, bytesDataLength.length);

        return finalData;
    }
    /**
     * 从每一帧数据中取出数据部分
     *
     * @param allBuffer
     * @return
     */
    public static byte[] getRailByteData(byte[] allBuffer) {
        byte[] dataLength = new byte[Config.DATA_LENGTH];
        System.arraycopy(allBuffer, 2, dataLength, 0, dataLength.length);
        String strDataLen = byteToHex(dataLength);
        int dataLen = Integer.parseInt(strDataLen, 16);
        byte realData[] = new byte[dataLen];
        System.arraycopy(allBuffer, Config.HEAD_LENGTH, realData, 0, realData.length);
        return realData;
    }
    /**
     * byte数组转hex
     *
     * @param bytes
     * @return
     */
    public static String byteToHex(byte[] bytes) {
        String strHex = "";
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < bytes.length; n++) {
            strHex = Integer.toHexString(bytes[n] & 0xFF);
            sb.append((strHex.length() == 1) ? "0" + strHex : strHex); // 每个字节由两个字符表示，位数不够，高位补0
        }
        return sb.toString().trim();
    }
    /**
     * hex转byte数组
     *
     * @param hex
     * @return
     */
    public static byte[] hexToByte(String hex) {
        int m = 0, n = 0;
        int byteLen = hex.length() / 2; // 每两个字符描述一个字节
        byte[] ret = new byte[byteLen];
        for (int i = 0; i < byteLen; i++) {
            m = i * 2 + 1;
            n = m + 1;
            int intVal = Integer.decode("0x" + hex.substring(i * 2, m) + hex.substring(m, n));
            ret[i] = Byte.valueOf((byte) intVal);
        }
        return ret;
    }
    public static String byteToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] strToByte(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }
}
