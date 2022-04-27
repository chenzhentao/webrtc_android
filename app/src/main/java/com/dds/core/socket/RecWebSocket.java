package com.dds.core.socket;

import java.util.List;

import javax.net.ssl.SSLSocketFactory;

public interface RecWebSocket {
    void connect();

    void setSocketFactory(SSLSocketFactory factory);

    void setConnectFlag(boolean b);

    void close();

    void createRoom(String room, int roomSize, String myId);

    void sendInvite(String room, String myId, List<String> users, boolean audioOnly);

    void sendLeave(String myId, String room, String userId);

    void sendRing(String myId, String targetId, String room);

    void sendRefuse(String room, String inviteId, String myId, int refuseType);

    void sendCancel(String mRoomId, String myId, List<String> userIds);

    void sendJoin(String room, String myId);

    void sendOffer(String myId, String userId, String sdp);

    void sendAnswer(String myId, String userId, String sdp);

    void sendIceCandidate(String myId, String userId, String id, int label, String candidate);

    void sendTransAudio(String myId, String userId);

    void sendDisconnect(String room, String myId, String userId);

    void reconnect();

    boolean isOpen();
}
