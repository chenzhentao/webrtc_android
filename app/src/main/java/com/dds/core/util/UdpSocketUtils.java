package com.dds.core.util;


import static com.dds.core.socket.MyWebSocketServer.DEFAULT_PORT;
import static com.dds.core.util.Utils.byteToString;
import static com.dds.core.util.Utils.getRailByteData;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.alibaba.fastjson.JSON;
import com.dds.App;
import com.dds.core.socket.SocketManager;
import com.dds.core.ui.local.LocalDeviceInfo;
import com.dds.core.ui.room.RoomInfo;
import com.dds.skywebrtc.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UdpSocketUtils {

    private String TAG = UdpSocketUtils.class.getSimpleName();
    private boolean mOpenRecvFlag = false;
    private volatile boolean isWifiOpen = false;
    private ThreadPoolExecutor executor;
    private DatagramSocket datagramSocketRecvFindDevice;
    private Context context;
    private ConnectivityManager connectionManager;
    private BroadcastReceiver broadcastReceiver;
    private volatile static UdpSocketUtils udpSocketUtils;
    private final Timer timer = new Timer();
    private volatile boolean findDevice;
    private DeviceListener mDeviceListener;
    private Runnable runnable;

    private UdpSocketUtils() {
        this.context = App.getInstance().getBaseContext();
        executor = new ThreadPoolExecutor(2, 6, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10));
        connectionManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        initBroadcast();
        initFindDeviceReviceBroadcast();
        Logger.d(TAG, "UdpSocketUtils Constructor init");
    }


    public static UdpSocketUtils get() {
        if (null == udpSocketUtils) {
            synchronized (UdpSocketUtils.class) {
                if (null == udpSocketUtils) {
                    udpSocketUtils = new UdpSocketUtils();
                }
            }
        }
        return udpSocketUtils;
    }

    private void initBroadcast() {
        //监听网络情况
        if (null == broadcastReceiver) {
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    NetworkInfo networkInfo = connectionManager.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isAvailable()) {//判断网络是否可用
                        mOpenRecvFlag = true;
                        isWifiOpen = true;
                        Logger.d(TAG, "have internet");
                    } else {
                        mOpenRecvFlag = false;
                        isWifiOpen = false;
                        Logger.d(TAG, "no internet");
                    }
                }
            };
            context.registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    /**
     * 初始化设备自发现接收广播
     */
    private void initFindDeviceReviceBroadcast() {
        executor.execute(this::receiveServerDataFindDevice);
        Logger.d(TAG, "initFindDeviceReviceBroadcast receive thread started successfully!");
    }

    /**
     * 该方法供设备自发现需求调用
     */
    public void init(DeviceListener mDeviceListener) {
        this.mDeviceListener = mDeviceListener;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Logger.d(TAG, "timer.schedule" + this);
                findDevice = false;
                mDeviceListener.onDeviceFind(new LocalDeviceInfo());
                executor.remove(runnable);
            }
        }, 1000 * 60);
        if (runnable != null) {
            executor.remove(runnable);
        }
//        executor.shutdownNow();
        runnable = () -> {
            Logger.d(TAG, "executor send---start" + this);
            while (true) {
                if (!findDevice) break;
//                Logger.d(TAG, "the device is broadcasting " + "   isWifiOpen=" + isWifiOpen + "   findDevice=" + findDevice);
                if (isWifiOpen && findDevice) {
                    //前提条件，wifi状态下并且在未登录状态下
                    sendBroadCast(Utils.protocolCmd(Config.CMD_UP_DEVICE_INFOR, App.getInstance().getUsername()),
                            Config.DES_ADDRESS, Config.DES_PORT);
                    try {
                        Thread.sleep(Config.UP_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Logger.d(TAG, "executor send---end" + this);
        };
        Logger.d(TAG, "init---start" + this);
        executor.execute(runnable);

    }

    /**
     * 发送数据到server  注：这里屏端是client  手机端是server
     *
     * @param bytes   发送给server封装好实体的字节数组
     * @param desIp   发送数据的目的的ip
     * @param desPort 发送数据的目的端口
     */
    public void sendBroadCast(byte[] bytes, String desIp, int desPort) {
        if (!InternetUtils.checkEnable(context)) {
            Logger.d(TAG, "sendBroadCast--network unreachable");
            return;
        }
        try {
            DatagramSocket socket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(bytes,
                    bytes.length,
                    InetAddress.getByName(desIp),
                    desPort);
            socket.setSendBufferSize(4 * 1024 * 1024);
            socket.setBroadcast(true);
            socket.send(packet);
            Logger.d(TAG, "already send a broadcast. opposite ip=" + desIp + " opposite port=" + desPort);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.d(TAG, "send broadcast happen a exception=" + e.getMessage());
        }
    }

    private byte[] getNoBankData(byte[] sourceData, int realLength) {
        byte bDes[] = new byte[realLength];
        System.arraycopy(sourceData, 0, bDes, 0, bDes.length);
        return bDes;
    }


    /**
     * 该方法用于监听服务端发来的数据，并进行解析
     */
    private void receiveServerDataFindDevice() {
        Logger.d(TAG, "executor receive---start" + this);
        try {
            byte[] message = new byte[Config.RECV_LENGTH];
            if (null == datagramSocketRecvFindDevice) {
                datagramSocketRecvFindDevice = new DatagramSocket(null);
                datagramSocketRecvFindDevice.setReuseAddress(true);
                datagramSocketRecvFindDevice.bind(new InetSocketAddress(Config.LISTER_SERVER_PORT));
                datagramSocketRecvFindDevice.setBroadcast(true);
            }
            DatagramPacket datagramPacket = new DatagramPacket(message, message.length);
            while (true) {
                Logger.d(TAG, "receiveServerDataFindDevice mOpenRecvFlag =" + mOpenRecvFlag);
                if (mOpenRecvFlag) {
                    datagramSocketRecvFindDevice.receive(datagramPacket);
                    if (!datagramPacket.getAddress().getHostAddress().equals(InternetUtils.getLocalIpAddress(context))) {
                        byte[] data = datagramPacket.getData();
                        byte[] bDes = getNoBankData(data, datagramPacket.getLength());
                        parseReceiveData(bDes, datagramPacket.getAddress().getHostAddress());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Logger.d(TAG, "receiveServerDataFindDevice happen a exception=" + e.getMessage());
        }
    }

    public void setFindDevice(boolean findDevice) {
        this.findDevice = findDevice;
    }

    /**
     * 解析接收到的数据
     *
     * @param buffer
     * @param ipAddress
     */
    private void parseReceiveData(byte[] buffer, String ipAddress) {

        if (null != buffer && buffer.length > 0) {
            if (buffer[1] == Config.CMD_UP_DEVICE_INFOR) {//手机端发来的登录的指令，动作：执行登录操作
                //phoneIpAddress = ipAddress;
                String phoneTokenMsg = byteToString(getRailByteData(buffer));
                if (mDeviceListener != null) {
                    mDeviceListener.onDeviceFind(new LocalDeviceInfo(phoneTokenMsg, ipAddress));
                }
                Logger.d(TAG, " save ==ee ipAddress " + ipAddress + "  " + phoneTokenMsg);
            } else if (buffer[1] == Config.CMD_PING_REQUEST) {//手机端发来的登录的指令，动作：执行登录操作
                //phoneIpAddress = ipAddress;
                String phoneTokenMsg = byteToString(getRailByteData(buffer));
                if (mDeviceListener != null) {
                    mDeviceListener.onDeviceFind(new LocalDeviceInfo(phoneTokenMsg, ipAddress));
                }
                SocketManager.getInstance().connectLocal("http://" + ipAddress + ":" + DEFAULT_PORT, App.getInstance().getUsername(), 0);
                Logger.d(TAG, " save ==ss " + phoneTokenMsg);
            }
        }
    }

    public void setFindListener(DeviceListener deviceListener) {
        this.mDeviceListener = deviceListener;
    }

    public void sendRequest(LocalDeviceInfo roomInfo) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                sendBroadCast(Utils.protocolCmd(Config.CMD_PING_REQUEST, App.getInstance().getUsername()), roomInfo.getRoomId(), Config.DES_PORT);
            }
        });
    }
}
