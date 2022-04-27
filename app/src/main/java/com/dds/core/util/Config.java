package com.dds.core.util;

public class Config {

    //协议类型-- 24 代表udp协议
    public static final byte PROTOCOL_TYPE = 0x24;

    //监听server的端口
    public static final int LISTER_SERVER_PORT = 11008;

    //智慧投屏server的端口
    public static final int CAST_LISTER_SERVER_PORT = 11118;

    //智慧投屏client的端口
    public static final int CAST_LISTER_CLIENT_PORT = 11119;

    //接收数据的长度
    public static final int RECV_LENGTH = 2048;

    //线程名字，用于接收服务端的udp广播
    public static final String RECEIVE_TOUPING_THREAD_NAME = "receive_touping_server_udp";

    //线程名字，用于定时给server端发送自己的例如deviceId、deviceName等信息
    public static final String SEND_DEVICE_INFOR_THREAD_NAME = "send_device_infor";

    //线程名字，用于接收设备自发现udp广播
    public static final String RECEIVE_FIND_DEVICE_THREAD = "receive_find_device_thread";

    //线程名字，用于登录成功之后给手机端发送登录状态
    public static final String SEND_LOGIN_STATUS_THREAD = "send_login_status";

    //线程名字，用于投屏
    public static final String SEND_PING_REQUEST = "send_ping_request";

    public static final String SEND_PING_RESEND = "send_ping_resend";

    //线程名字，用于投屏，手机端使用
    public static final String THREAD_RECV_APP_DATA = "thread_recv_app_data";

    //线程名字，发送投屏服务拉取结果线程
    public static final String SEND_PULL_TOUPING_SERVER_THREAD = "send_pull_touping_server_thread";

    //目的IP
    public static final String DES_ADDRESS = "255.255.255.255";

    //目的端口
    public static final int DES_PORT = 11008;

    //上报设备信息的间隔（单位：毫秒）
    public static final int UP_INTERVAL = 800;

    //再次发送登录结果给手机的时间（单位：毫秒）
    public static final int SEND_LOGIN_STATE_INTERVAL = 10000;

    //协议头部（这里的头部=协议所占1字节+命令所占1字节+数据长度所占4字节）长度
    public static final int HEAD_LENGTH = 6;

    //协议中数据长度所占的字节数
    public static final int DATA_LENGTH = 4;


    //上报设备信息的指令
    public static final byte CMD_UP_DEVICE_INFOR = 0x11;
    //下面这两个命令用于投屏使用。
    public static final byte CMD_PING_REQUEST = 0x16;

}
