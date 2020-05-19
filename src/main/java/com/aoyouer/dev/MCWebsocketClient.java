package com.aoyouer.dev;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.contact.Group;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class MCWebsocketClient extends WebSocketClient {
    final Logger logger = LoggerFactory.getLogger(getClass());
    private Group group;
    private Config setting;

    public MCWebsocketClient(URI serverUri, Group group, Config setting) {
        super(serverUri);
        this.group = group;
        this.setting = setting;
    }

    public MCWebsocketClient(URI serverUri) {
        super(serverUri);
    }

    public MCWebsocketClient(URI serverUri, Draft protocolDraft) {
        super(serverUri, protocolDraft);
    }

    public MCWebsocketClient(URI serverUri, Map<String, String> httpHeaders) {
        super(serverUri, httpHeaders);
    }

    public MCWebsocketClient(URI serverUri, Draft protocolDraft, Map<String, String> httpHeaders) {
        super(serverUri, protocolDraft, httpHeaders);
    }

    public MCWebsocketClient(URI serverUri, Draft protocolDraft, Map<String, String> httpHeaders, int connectTimeout) {
        super(serverUri, protocolDraft, httpHeaders, connectTimeout);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        logger.info("连接已经打开");
    }

    @Override
    public void onMessage(String message) {
        logger.info("收到信息\n" + message);
        JsonObject msgObj = JsonParser.parseString(message).getAsJsonObject();
        String operate = msgObj.get("operate").getAsString();
        String sender = msgObj.get("target").getAsString();
        String msg = msgObj.get("text").getAsString();
        boolean gameToGroup = setting.getBoolean("GameToGroup");
        if (operate.equals("onmsg") && gameToGroup) {
            //聊天信息同步
            if (group != null) {
                group.sendMessage("[游戏聊天]" + sender + ": " + msg);
            } else {
                //debug使用
                logger.info("聊天信息{}",message);
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {
        logger.info("出错了！！");
    }

    public static void main(String[] args) throws URISyntaxException {
        MCWebsocketClient websocketClient = new MCWebsocketClient(new URI("ws://192.168.0.200:255/fine"));
        websocketClient.connect();
    }
}
