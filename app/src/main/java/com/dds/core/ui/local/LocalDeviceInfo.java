package com.dds.core.ui.local;

import android.text.TextUtils;

import java.util.Objects;

/**
 * Created by dds on 2020/5/1.
 * ddssingsong@163.com
 */
public class LocalDeviceInfo {

    private String roomId;
    private String userId;

    public LocalDeviceInfo(String userId, String roomId) {
        this.userId = userId;
        this.roomId = roomId;
    }
    public LocalDeviceInfo() {
    }


    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public boolean loadEnd() {
        return TextUtils.isEmpty(userId)&&TextUtils.isEmpty(roomId);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

      @Override
    public String toString() {
        return "RoomInfo{" +
                "roomId='" + roomId + '\'' +
                ", userId='" + userId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalDeviceInfo)) return false;
        LocalDeviceInfo roomInfo = (LocalDeviceInfo) o;
        return Objects.equals(roomId, roomInfo.roomId) && Objects.equals(userId, roomInfo.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, userId);
    }
}
