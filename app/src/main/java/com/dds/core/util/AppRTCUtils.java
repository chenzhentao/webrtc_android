/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.dds.core.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import com.dds.skywebrtc.Logger;

import com.dds.App;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * AppRTCUtils provides helper functions for managing thread safety.
 */
public final class AppRTCUtils {
  private AppRTCUtils() {}
  // Regex pattern used for checking if room id looks like an IP.
 public static final Pattern IP_PATTERN = Pattern.compile("("
          // IPv4
          + "((\\d+\\.){3}\\d+)|"
          // IPv6
          + "\\[((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::"
          + "(([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?)\\]|"
          + "\\[(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})\\]|"
          // IPv6 without []
          + "((([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?::(([0-9a-fA-F]{1,4}:)*[0-9a-fA-F]{1,4})?)|"
          + "(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4})|"
          // Literals
          + "localhost"
          + ")"
          // Optional port number
          + "(:(\\d+))?");
  /** Helper method which throws an exception  when an assertion has failed. */
  public static void assertIsTrue(boolean condition) {
    if (!condition) {
      throw new AssertionError("Expected condition to be true");
    }
  }

  /** Helper method for building a string of thread information.*/
  public static String getThreadInfo() {
    return "@[name=" + Thread.currentThread().getName() + ", id=" + Thread.currentThread().getId()
        + "]";
  }

  /** Information about the current build, taken from system properties. */
  public static void logDeviceInfo(String tag) {
   Logger.d(tag, "Android SDK: " + Build.VERSION.SDK_INT + ", "
            + "Release: " + Build.VERSION.RELEASE + ", "
            + "Brand: " + Build.BRAND + ", "
            + "Device: " + Build.DEVICE + ", "
            + "Id: " + Build.ID + ", "
            + "Hardware: " + Build.HARDWARE + ", "
            + "Manufacturer: " + Build.MANUFACTURER + ", "
            + "Model: " + Build.MODEL + ", "
            + "Product: " + Build.PRODUCT);
  }
  /**
   * 获取自己的ip
   * @return
   */
  public static String getIpAddress() {
    App instance = App.getInstance();
    @SuppressLint("MissingPermission") NetworkInfo info = ((ConnectivityManager) instance
            .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
    if (info != null && info.isConnected()) {
      if (info.getType() == ConnectivityManager.TYPE_MOBILE || info.getType() == ConnectivityManager.TYPE_ETHERNET) {//当前使用2G/3G/4G网络/有线网络
        try {
          for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
              InetAddress inetAddress = enumIpAddr.nextElement();
              if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                return inetAddress.getHostAddress();
              }
            }
          }
        } catch (SocketException e) {
          e.printStackTrace();
        }

      } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
        WifiManager wifiManager = (WifiManager) instance.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ipAddress = int2ip(wifiInfo.getIpAddress());//得到IPV4地址
        return ipAddress;
      }
    } else {
      //当前无网络连接,请在设置中打开网络
    }
    return "";
  }

  /**
   * 将ip的整数形式转换成ip形式
   *
   * @param ipInt
   * @return
   */
  public static String int2ip(int ipInt) {
    StringBuilder sb = new StringBuilder();
    sb.append(ipInt & 0xFF).append(".");
    sb.append((ipInt >> 8) & 0xFF).append(".");
    sb.append((ipInt >> 16) & 0xFF).append(".");
    sb.append((ipInt >> 24) & 0xFF);
    return sb.toString();
  }
}
