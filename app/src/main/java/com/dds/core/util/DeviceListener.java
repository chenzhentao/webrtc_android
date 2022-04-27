package com.dds.core.util;

import com.dds.core.ui.local.LocalDeviceInfo;
import com.dds.core.ui.room.RoomInfo;

public interface DeviceListener {
   void onDeviceFind(LocalDeviceInfo info);
}
