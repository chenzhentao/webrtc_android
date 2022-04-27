package com.dds.core.socket;

import android.annotation.SuppressLint;
import android.util.Log;

import com.dds.skywebrtc.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dds.core.util.StringUtil;
import com.dds.driect.Data;
import com.google.gson.Gson;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Created by dds on 2019/7/26.
 * android_shuai@163.com
 */
public class MyWebSocketServer extends WebSocketServer implements RecWebSocket {
    private final static String TAG = "dds_WebSocketServer";
    private final IEvent iEvent;
    private boolean connectFlag = false;

    private String userId;
    private int  roomSize;
    private static ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocket>> rooms = new ConcurrentHashMap<>();

    private static ConcurrentHashMap<String, String> roomList = new ConcurrentHashMap<>();

    private static CopyOnWriteArrayList<WebSocket> sockets = new CopyOnWriteArrayList<>();
    public static final int DEFAULT_PORT = 8888;

    public MyWebSocketServer(InetAddress hostName, int port, String userId, IEvent event) {
        super(new InetSocketAddress(hostName, port));
        this.iEvent = event;
        this.userId = userId;
    }

    public MyWebSocketServer(InetSocketAddress myHost, String userId, IEvent event) {
        super(myHost);
        this.userId = userId;
        this.iEvent = event;
    }

    @Override
    public void connect() {
        super.start();
    }

    @Override
    public void setSocketFactory(SSLSocketFactory factory) {
        //服务不需要
    }

    @Override
    public boolean isOpen() {
        return connectFlag;
    }
    @Override
    public void onOpen(WebSocket session, ClientHandshake handshake) {
//       Logger.e(TAG, "onOpen" + session + "  session.getResourceDescriptor()  " + session.getResourceDescriptor() + "  handshake.getResourceDescriptor()  " + handshake.getResourceDescriptor());
        addSocket(session);
        this.iEvent.onOpen();
    }

    private void addSocket(WebSocket session) {
        if (!sockets.contains(session)) {
            sockets.add(session);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Logger.e("dds_error", "onClose:" + reason + "remote:" + remote);
        if (connectFlag) {

            String room = roomList.get(userId);
            CopyOnWriteArrayList<WebSocket> curRoom;
            if (room != null) {
                curRoom = rooms.get(room);//获取对应房间的列表
                for (WebSocket aCurRoom : curRoom) {

                    Data send = new Data();
                    send.setEventName("_remove_peer");
                    Map<String, Object> map = new HashMap<>();
                    map.put("socketId", userId);
                    send.setData(map);
                    broadcast(new Gson().toJson(send).toString(), Collections.singletonList(aCurRoom));
                    removeSocket(aCurRoom);
                }
            }
        } else {
            this.iEvent.logout("onClose");
        }
        connectFlag = false;
    }

    private void removeSocket(WebSocket session) {
        if (session != null) {
            String resourceDescriptor = session.getResourceDescriptor();
            String room = roomList.get(resourceDescriptor);
            //删除session
            sockets.remove(session);
            //从房间列表删除
            if (room != null) {
                CopyOnWriteArrayList<WebSocket> webSockets = rooms.get(room);
                if (webSockets != null) {
                    webSockets.remove(session);
                }
            }
            //从sessionid 对应房间的列表删除
            roomList.remove(resourceDescriptor);
        }

    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Logger.d(TAG, message);
        handleMessage(conn, message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Logger.e("dds_error", Log.getStackTraceString(new Throwable("onError:" + ex.toString())));
        this.iEvent.logout("onError");
        connectFlag = false;
    }

    @Override
    public void onStart() {
        connectFlag = true;
        Logger.d(TAG, "onStart()，WebSocket服务端启动成功");
    }


    public void setConnectFlag(boolean flag) {
        connectFlag = flag;
    }

    @Override
    public void close() {

    }

    // ---------------------------------------处理接收消息-------------------------------------

    private void handleMessage(WebSocket session, String message) {
        Map map = JSON.parseObject(message, Map.class);
        String eventName = (String) map.get("eventName");
        if (eventName == null) return;
        switch (eventName) {
            // 登录成功
            case "__login_success":
                handleLogin(map);
                return;

            // 被邀请
            case "__invite":
                handleInvite(map);
                return;

            // 取消拨出
            case "__cancel":
                handleCancel(map);
                return;

            // 响铃
            case "__ring":
                handleRing(map);
                return;

            // 进入房间
            case "__peers":
                handlePeers(map);
                return;

            // 新人入房间
            case "__new_peer":
                handleNewPeer(map);
                return;

            // 拒绝接听
            case "__reject":
                handleReject(map);
                return;

            // 离开房间
            case "__leave":
                handleLeave(map);
                break;

            // 切换到语音
            case "__audio":
                handleTransAudio(map);
                break;

            // 意外断开
            case "__disconnect":
                handleDisConnect(map);
                break;

            case "__join":
                join(map/*.getData()*/, session);
                break;
            // ice-candidate
            case "__ice_candidate":
                handleIceCandidate(map);
                //转发给第三方
                // iceCandidate(map/*.getData()*/, session);

                break;
            // offer
            case "__offer":
                handleOffer(map);
                //转发给第三方
                //offer(map/*.getData()*/, session);
                break;
            // answer
            case "__answer":
                handleAnswer(map);
                //转发给第三方
                //answer(map/*.getData()*/, session);
                break;

        }
    }

    private void join(Map<String, Object> data, WebSocket socket) {
        //获得房间号
        String room = data.get("room") == null ? "__default" : data.get("room").toString();
        CopyOnWriteArrayList<String> ids = new CopyOnWriteArrayList<>();//存储sessionId
        CopyOnWriteArrayList<WebSocket> curRoom = rooms.get(room);//获取对应房间的列表
        if (curRoom == null) {
            curRoom = new CopyOnWriteArrayList<>();
        }
        ids.add(userId);
        WebSocket curSocket;
        //当前房间是否有加入的socket
        for (int i = 0; i < curRoom.size(); i++) {
            curSocket = curRoom.get(i);
            if (curSocket != null)
                ids.add(curSocket.getResourceDescriptor());
            else ids.add(userId);
            Data send = new Data();
            send.setEventName("_new_peer");
            Map<String, Object> map = new HashMap<>();
            if (socket == null)
                map.put("socketId", userId);
            else
                map.put("socketId", socket.getResourceDescriptor());
            send.setData(map);
            broadcast(new Gson().toJson(send), Collections.singletonList(curSocket));
        }

        curRoom.add(socket);
        roomList.put(userId, room);
        rooms.put(room, curRoom);


        if (socket != null) {
            //当前服务器直接执行
            this.iEvent.onNewPeer(socket.getResourceDescriptor());
            Data send = new Data();
            send.setEventName("__peers");
            Map<String, Object> map = new HashMap<>();
            String[] connections = new String[ids.size()];
            ids.toArray(connections);
            map.put("connections", Arrays.toString(connections));
            map.put("roomSize", roomSize);
            map.put("you", socket.getResourceDescriptor());
            send.setData(map);
            broadcast(new Gson().toJson(send), Collections.singletonList(socket));
        }


        Logger.d(TAG, "新用户" + socket + "加入房间" + room);
    }

    private void handleDisConnect(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromId = (String) data.get("fromID");
            this.iEvent.onDisConnect(fromId);
        }
    }

    private void handleTransAudio(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromId = (String) data.get("fromID");
            this.iEvent.onTransAudio(fromId);
        }
    }

    private void handleLogin(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String userID = (String) data.get("userID");
            String avatar = (String) data.get("avatar");
            this.iEvent.loginSuccess(userID, avatar);
        }


    }

    private void handleIceCandidate(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String userID = (String) data.get("fromID");
            String id = (String) data.get("id");
            int label = (int) data.get("label");
            String candidate = (String) data.get("candidate");
            this.iEvent.onIceCandidate(userID, id, label, candidate);
        }
    }

    private void iceCandidate(Map<String, Object> data, WebSocket socket) {
        //soc=this
        WebSocket session = getSocket(data.get("socketId").toString());
        if (session == null) {
            return;
        }

        Data send = new Data();
        send.setEventName("_ice_candidate");
        Map<String, Object> map = data;
        map.put("id", data.get("id"));
        map.put("label", data.get("label"));
        map.put("candidate", data.get("candidate"));
        if (socket != null)
            map.put("socketId", socket.getResourceDescriptor());
        send.setData(map);
        broadcast(new Gson().toJson(send), Collections.singletonList(session));
        if (socket != null)
            Logger.d(TAG, "接收到来自" + socket.getResourceDescriptor() + "的ICE Candidate");
    }

    private WebSocket getSocket(String socketId) {
        if (sockets.size() > 0) {
            for (int i = 0; i < sockets.size(); i++) {
                WebSocket webSocket = sockets.get(i);
                if (webSocket != null)
                    if (socketId.equals(webSocket.getResourceDescriptor())) {
                        return webSocket;
                    }
            }
        }
        return null;
    }

    private void handleAnswer(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String sdp = (String) data.get("sdp");
            String userID = (String) data.get("fromID");
            this.iEvent.onAnswer(userID, sdp);
        }
    }

    private void answer(Map<String, Object> data, WebSocket socket) {
        WebSocket session = getSocket(data.get("socketId").toString());
        if (session == null) {
            return;
        }

        Data send = new Data();
        send.setEventName("_answer");

        Map<String, Object> map = data;
        map.put("sdp", data.get("sdp"));
        if (socket != null)
            map.put("socketId", socket.getResourceDescriptor());
        send.setData(map);
        broadcast(new Gson().toJson(send), Collections.singletonList(session));
        if (socket != null)
            Logger.d(TAG, "接收到来自" + socket.getResourceDescriptor() + "的Answer");

    }

    private void handleOffer(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String sdp = (String) data.get("sdp");
            String userID = (String) data.get("fromID");
            this.iEvent.onOffer(userID, sdp);
        }
    }

    private void offer(Map<String, Object> data, WebSocket socket) {
        WebSocket session = getSocket(data.get("socketId").toString());
        if (session == null) {
            return;
        }
        Data send = new Data();
        send.setEventName("_offer");

        Map<String, Object> map = data;
        map.put("sdp", data.get("sdp"));
        if (socket != null)
            map.put("socketId", socket.getResourceDescriptor());
        send.setData(map);
        broadcast(new Gson().toJson(send), Collections.singletonList(session));
        if (socket != null)
            Logger.d(TAG, "接收到来自" + socket.getResourceDescriptor() + "的Offer");

    }

    private void handleReject(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromID = (String) data.get("fromID");
            int rejectType = Integer.parseInt(String.valueOf(data.get("refuseType")));
            this.iEvent.onReject(fromID, rejectType);
        }
    }

    private void handlePeers(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String you = (String) data.get("you");
            String connections = (String) data.get("connections");
            int roomSize = (int) data.get("roomSize");
            if (connections != null) {
                connections = connections.replace("[", "").replace("]", "");
            }
            this.iEvent.onPeers(you, connections, roomSize);
        }
    }

    private void handleNewPeer(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String userID = (String) data.get("userID");
            this.iEvent.onNewPeer(userID);
        }
    }

    private void handleRing(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromId = (String) data.get("fromID");
            this.iEvent.onRing(fromId);
        }
    }

    private void handleCancel(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String inviteID = (String) data.get("inviteID");
            String userList = (String) data.get("userList");
            this.iEvent.onCancel(inviteID);
        }
    }

    private void handleInvite(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String room = (String) data.get("room");
            boolean audioOnly = (boolean) data.get("audioOnly");
            String inviteID = (String) data.get("inviteID");
            String userList = (String) data.get("userList");
            this.iEvent.onInvite(room, audioOnly, inviteID, userList);
        }
    }

    private void handleLeave(Map map) {
        Map data = (Map) map.get("data");
        if (data != null) {
            String fromID = (String) data.get("fromID");
            this.iEvent.onLeave(fromID);
        }
    }

    /**
     * ------------------------------发送消息----------------------------------------
     */
    @Override
    public void createRoom(String room, int roomSize, String myId) {
        this.roomSize = roomSize;
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__create");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("roomSize", roomSize);
        childMap.put("userID", myId);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Logger.d(TAG, "send-->" + jsonString);
        String[] connections = {myId};
        map.put("connections", Arrays.toString(connections));
        map.put("you", myId);
        handlePeers(map);
    }

    // 发送邀请
    @Override
    public void sendInvite(String room, String myId, List<String> users, boolean audioOnly) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__invite");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("audioOnly", audioOnly);
        childMap.put("inviteID", myId);

        String join = StringUtil.listToString(users);
        childMap.put("userList", join);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Logger.d(TAG, "send-->" + jsonString);
        broadcast(jsonString);
    }

    // 取消邀请
    @Override
    public void sendCancel(String mRoomId, String useId, List<String> users) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__cancel");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("inviteID", useId);
        childMap.put("room", mRoomId);

        String join = StringUtil.listToString(users);
        childMap.put("userList", join);


        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Logger.d(TAG, "send-->" + jsonString);
        broadcast(jsonString);
    }

    // 发送响铃通知
    @Override
    public void sendRing(String myId, String toId, String room) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__ring");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("fromID", myId);
        childMap.put("toID", toId);
        childMap.put("room", room);


        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Logger.d(TAG, "send-->" + jsonString);
        broadcast(jsonString);
    }

    //加入房间
    @Override
    public void sendJoin(String room, String myId) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__join");

        Map<String, String> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("userID", myId);


        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Logger.d(TAG, "send-->" + jsonString);
        broadcast(jsonString);
    }

    // 拒接接听
    @Override
    public void sendRefuse(String room, String inviteID, String myId, int refuseType) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__reject");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("toID", inviteID);
        childMap.put("fromID", myId);
        childMap.put("refuseType", String.valueOf(refuseType));

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Logger.d(TAG, "send-->" + jsonString);
        broadcast(jsonString);
    }

    // 离开房间
    @Override
    public void sendLeave(String myId, String room, String userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__leave");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("fromID", myId);
        childMap.put("userID", userId);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Logger.d(TAG, "send-->" + jsonString);
        if (connectFlag) {
            broadcast(jsonString);
        }
    }

    // send offer
    @Override
    public void sendOffer(String myId, String userId, String sdp) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("sdp", sdp);
        childMap.put("userID", userId);
        childMap.put("fromID", myId);
        map.put("data", childMap);
        map.put("eventName", "__offer");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Logger.d(TAG, "send-->" + jsonString);
        broadcast(jsonString);
    }

    // send answer
    @Override
    public void sendAnswer(String myId, String userId, String sdp) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("sdp", sdp);
        childMap.put("fromID", myId);
        childMap.put("userID", userId);
        map.put("data", childMap);
        map.put("eventName", "__answer");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Logger.d(TAG, "send-->" + jsonString);
        broadcast(jsonString);
    }

    // send ice-candidate
    @Override
    public void sendIceCandidate(String myId, String userId, String id, int label, String candidate) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__ice_candidate");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("userID", userId);
        childMap.put("fromID", myId);
        childMap.put("id", id);
        childMap.put("label", label);
        childMap.put("candidate", candidate);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Logger.d(TAG, "send-->" + jsonString);
        if (connectFlag) {
            broadcast(jsonString);
        }
    }

    // 切换到语音
    @Override
    public void sendTransAudio(String myId, String userId) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("fromID", myId);
        childMap.put("userID", userId);
        map.put("data", childMap);
        map.put("eventName", "__audio");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Logger.d(TAG, "send-->" + jsonString);
        broadcast(jsonString);
    }

    // 断开重连
    @Override
    public void sendDisconnect(String room, String myId, String userId) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("fromID", myId);
        childMap.put("userID", userId);
        childMap.put("room", room);
        map.put("data", childMap);
        map.put("eventName", "__disconnect");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Logger.d(TAG, "send-->" + jsonString);
        broadcast(jsonString);
    }

    @Override
    public void reconnect() {

    }



    // 忽略证书

    public static class TrustManagerTest implements X509TrustManager {

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

}
