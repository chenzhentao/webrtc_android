package com.dds.core.socket;

/**
 * Created by dds on 2019/8/2.
 * android_shuai@163.com
 */
public interface IUserState {


    void userLogin(String userId);
    void localUserLogin(String userId, boolean out);

    void userLogout();


}
