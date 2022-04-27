package com.dds.core.socket;

import static com.dds.core.socket.MyWebSocketServer.DEFAULT_PORT;
import static com.dds.core.util.AppRTCUtils.IP_PATTERN;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dds.skywebrtc.Logger;

import com.dds.App;
import com.dds.core.util.AppRTCUtils;
import com.dds.core.voip.Utils;
import com.dds.core.voip.VoipReceiver;
import com.dds.skywebrtc.CallSession;
import com.dds.skywebrtc.EnumType;
import com.dds.skywebrtc.SkyEngineKit;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.List;
import java.util.regex.Matcher;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/**
 * Created by dds on 2019/7/26.
 * ddssignsong@163.com
 */
public class SocketManager implements IEvent {
    private final static String TAG = "dds_SocketManager";
    private RecWebSocket webSocket;
    private int userState;
    private String myId;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean local;

    private SocketManager() {

    }

    private static class Holder {
        private static final SocketManager socketManager = new SocketManager();
    }

    public static SocketManager getInstance() {
        return Holder.socketManager;
    }

    public void connectLocal(String url, String userId, int device) {
        if (webSocket == null || !webSocket.isOpen()) {
            String substring = url.replace("http://", "");
            Matcher matcher = IP_PATTERN.matcher(substring);
            this.myId = "/" + userId + "/" + device;
            Logger.d(TAG,Log.getStackTraceString(new Throwable("connectLocal myId = "+myId)));
            URI uri;
            try {
                String urls = url + myId;
                uri = new URI(urls);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return;
            }

            if (matcher.matches()) {
                local = true;
                String ip = matcher.group(1);
                String portStr = matcher.group(matcher.groupCount());
                int port;

                if (portStr != null) {
                    try {
                        port = Integer.parseInt(portStr);
                    } catch (NumberFormatException e) {
                        //reportError("Invalid port number: " + portStr);
                        return;
                    }
                } else {
                    port = DEFAULT_PORT;
                }

                if (ip != null && ip.contains(AppRTCUtils.getIpAddress())) {
                    InetAddress address;
                    try {
                        address = InetAddress.getByName(ip);
                    } catch (UnknownHostException e) {
                        //reportError("Invalid IP address.");
                        return;
                    }
                    webSocket = new MyWebSocketServer(address, port, myId, this);
                } else {
                    webSocket = new MyWebSocket(uri, this);
                }
            }
        }
        // 开始connect
        webSocket.connect();
    }

    public void connect(String url, String userId, int device) {
        if (webSocket == null || !webSocket.isOpen()) {
            local = false;
            String substring = url.replace("http://", "");
            Matcher matcher = IP_PATTERN.matcher(substring);
            this.myId = "/" + userId + "/" + device;
            Logger.d(TAG,Log.getStackTraceString(new Throwable("connect myId = "+myId)));
            URI uri;
            try {
                String urls = url + myId;
                uri = new URI(urls);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return;
            }

            webSocket = new MyWebSocket(uri, this);
            // 设置wss
            if (url.startsWith("wss")) {
                try {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    if (sslContext != null) {
                        sslContext.init(null, new TrustManager[]{new MyWebSocket.TrustManagerTest()}, new SecureRandom());
                    }

                    SSLSocketFactory factory = null;
                    if (sslContext != null) {
                        factory = sslContext.getSocketFactory();
                    }

                    if (factory != null) {
                        webSocket.setSocketFactory(factory);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 开始connect
            webSocket.connect();
        }
    }

    public void unConnect() {
        if (webSocket != null) {
            webSocket.setConnectFlag(false);
            webSocket.close();
            webSocket = null;
        }

    }

    @Override
    public void onOpen() {
        Logger.i(TAG, "socket is open!");
        userState = 1;
        if (iUserState != null && iUserState.get() != null) {
            if (local)
                iUserState.get().localUserLogin(myId,webSocket instanceof MyWebSocketServer);
            else
                iUserState.get().userLogin(myId);
        }
    }

    @Override
    public void loginSuccess(String userId, String avatar) {
        Logger.i(TAG, Log.getStackTraceString(new Throwable("loginSuccess: userId " + userId)));
        myId = userId;
        userState = 1;
        if (iUserState != null && iUserState.get() != null) {
            iUserState.get().userLogin(userId);
        }
    }


    // ======================================================================================
    public void createRoom(String room, int roomSize) {
        if (webSocket != null) {
            webSocket.createRoom(room, roomSize, myId);
        }

    }

    public void sendInvite(String room, List<String> users, boolean audioOnly) {
        if (webSocket != null) {
            webSocket.sendInvite(room, myId, users, audioOnly);
        }
    }

    public void sendLeave(String room, String userId) {
        if (webSocket != null) {
            webSocket.sendLeave(myId, room, userId);
        }
    }

    public void sendRingBack(String targetId, String room) {
        if (webSocket != null) {
            webSocket.sendRing(myId, targetId, room);
        }
    }

    public void sendRefuse(String room, String inviteId, int refuseType) {
        if (webSocket != null) {
            webSocket.sendRefuse(room, inviteId, myId, refuseType);
        }
    }

    public void sendCancel(String mRoomId, List<String> userIds) {
        if (webSocket != null) {
            webSocket.sendCancel(mRoomId, myId, userIds);
        }
    }

    public void sendJoin(String room) {
        Logger.d(TAG," sendJoin myId = "+myId);
        if (webSocket != null) {
            webSocket.sendJoin(room, myId);
        }
    }

    public void sendMeetingInvite(String userList) {

    }

    public void sendOffer(String userId, String sdp) {
        if (webSocket != null) {
            webSocket.sendOffer(myId, userId, sdp);
        }
    }

    public void sendAnswer(String userId, String sdp) {
        if (webSocket != null) {
            webSocket.sendAnswer(myId, userId, sdp);
        }
    }

    public void sendIceCandidate(String userId, String id, int label, String candidate) {
        if (webSocket != null) {
            webSocket.sendIceCandidate(myId, userId, id, label, candidate);
        }
    }

    public void sendTransAudio(String userId) {
        if (webSocket != null) {
            webSocket.sendTransAudio(myId, userId);
        }
    }

    public void sendDisconnect(String room, String userId) {
        if (webSocket != null) {
            webSocket.sendDisconnect(room, myId, userId);
        }
    }


    // ========================================================================================
    @Override
    public void onInvite(String room, boolean audioOnly, String inviteId, String userList) {
        Intent intent = new Intent();
        intent.putExtra("room", room);
        intent.putExtra("audioOnly", audioOnly);
        intent.putExtra("inviteId", inviteId);
        intent.putExtra("userList", userList);
        intent.setAction(Utils.ACTION_VOIP_RECEIVER);
        intent.setComponent(new ComponentName(App.getInstance().getPackageName(), VoipReceiver.class.getName()));
        // 发送广播
        App.getInstance().sendBroadcast(intent);

    }

    @Override
    public void onCancel(String inviteId) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onCancel(inviteId);
            }
        });

    }

    @Override
    public void onRing(String fromId) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onRingBack(fromId);
            }
        });


    }

    @Override  // 加入房间
    public void onPeers(String myId, String connections, int roomSize) {
        handler.post(() -> {
            //自己进入了房间，然后开始发送offer
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            Logger.e(TAG, "onPeers,currentSession" + currentSession);
            if (currentSession != null) {
                currentSession.onJoinRome(myId, connections, roomSize);
            }
        });

    }

    @Override
    public void onNewPeer(String userId) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.newPeer(userId);
            }
        });

    }

    @Override
    public void onReject(String userId, int type) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onRefuse(userId, type);
            }
        });

    }

    @Override
    public void onOffer(String userId, String sdp) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onReceiveOffer(userId, sdp);
            }
        });


    }

    @Override
    public void onAnswer(String userId, String sdp) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onReceiverAnswer(userId, sdp);
            }
        });

    }

    @Override
    public void onIceCandidate(String userId, String id, int label, String candidate) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onRemoteIceCandidate(userId, id, label, candidate);
            }
        });

    }

    @Override
    public void onLeave(String userId) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onLeave(userId);
            }
        });
    }

    @Override
    public void logout(String str) {
        Logger.i(TAG, "logout:" + str);
        userState = 0;
        if (iUserState != null && iUserState.get() != null) {
            iUserState.get().userLogout();
        }
    }

    @Override
    public void onTransAudio(String userId) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onTransAudio(userId);
            }
        });
    }

    @Override
    public void onDisConnect(String userId) {
        handler.post(() -> {
            CallSession currentSession = SkyEngineKit.Instance().getCurrentSession();
            if (currentSession != null) {
                currentSession.onDisConnect(userId, EnumType.CallEndReason.RemoteSignalError);
            }
        });
    }

    @Override
    public void reConnect() {
        handler.post(() -> {
            webSocket.reconnect();
        });
    }
    //===========================================================================================


    public int getUserState() {
        return userState;
    }

    private WeakReference<IUserState> iUserState;

    public void addUserStateCallback(IUserState userState) {
        iUserState = new WeakReference<>(userState);
    }

}
