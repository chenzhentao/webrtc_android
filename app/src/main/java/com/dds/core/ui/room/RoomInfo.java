package com.dds.core.ui.room;

import java.util.Objects;

/**
 * Created by dds on 2020/5/1.
 * ddssingsong@163.com
 */
public class RoomInfo {

    private String roomId;
    private String userId;
    private int maxSize;
    private int currentSize;

    public RoomInfo(String userId, String roomId) {
        this.userId = userId;
        this.roomId = roomId;
    }


    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public void setCurrentSize(int currentSize) {
        this.currentSize = currentSize;
    }

    @Override
    public String toString() {
        return "RoomInfo{" +
                "roomId='" + roomId + '\'' +
                ", userId='" + userId + '\'' +
                ", maxSize=" + maxSize +
                ", currentSize=" + currentSize +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomInfo)) return false;
        RoomInfo roomInfo = (RoomInfo) o;
        return maxSize == roomInfo.maxSize && currentSize == roomInfo.currentSize && Objects.equals(roomId, roomInfo.roomId) && Objects.equals(userId, roomInfo.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, userId, maxSize, currentSize);
    }
}
