package com.dds.core.ui.local;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.alibaba.fastjson.JSON;
import com.dds.core.consts.Urls;
import com.dds.core.ui.room.RoomInfo;
import com.dds.core.util.DeviceListener;
import com.dds.core.util.UdpSocketUtils;
import com.dds.net.HttpRequestPresenter;
import com.dds.net.ICallback;
import com.dds.skywebrtc.Logger;

import java.util.List;

public class LocalViewModel extends ViewModel implements DeviceListener {

    private MutableLiveData<LocalDeviceInfo> mList;


    public LocalViewModel() {
    }

    public MutableLiveData<LocalDeviceInfo> getRoomList() {
        if (mList == null) {
            mList = new MutableLiveData<>();
            loadRooms();
        }
        return mList;
    }

    public void loadRooms() {
        UdpSocketUtils.get().setFindDevice(true);
        UdpSocketUtils.get().init(this);
    }

    @Override
    public void onDeviceFind(LocalDeviceInfo info) {
        mList.postValue(info);
    }
}