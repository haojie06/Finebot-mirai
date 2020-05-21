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


import javax.xml.bind.DatatypeConverter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
        group.sendMessage("Websocket连接已建立");
    }

    @Override
    public void onMessage(String message) {
        logger.info("收到信息\n" + message);
        JsonObject msgObj = JsonParser.parseString(message).getAsJsonObject();
        String operate = msgObj.get("operate").getAsString();
        boolean gameToGroup = setting.getBoolean("GameToGroup");
        if (operate.equals("onmsg")) {
            //聊天信息同步
            String sender = msgObj.get("target").getAsString();
            String msg = msgObj.get("text").getAsString();
            if (group != null) {
                //即使关闭了游戏消息同步，但是如果聊天信息是以#开头的话，还是可以同步到群离
                if (gameToGroup) {
                    group.sendMessage("[游戏聊天]" + sender + ": " + msg);
                }
                else{
                    //获取首字符
                    String firstChar = msg.substring(0,1);
                    if (firstChar.equals("#")){
                        group.sendMessage("[游戏聊天主动发送]" + sender + ": " + msg.substring(1,msg.length()));
                    }
                }
            } else {
                //debug使用
                logger.info("聊天信息{}",message);
            }
        }
        else if (operate.equals("runcmd")){
            String text = msgObj.get("text").getAsString();
            if (!text.equals(""))
                group.sendMessage("[命令返回]\n" + text);
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

    public void groupToGame(String content,String type) {
        String jsonTemp;
        if (type.equals("chat")) {
            jsonTemp = "{\"operate\":\"runcmd\",\"passwd\":\"{password}\",\"cmd\":\"{content}\"}";
        } else {
            jsonTemp = "{\"operate\":\"runcmd\",\"passwd\":\"{password}\",\"cmd\":\"{content}\"}";
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");//设置日期格式
        String password = setting.getString("WSPassword") + df.format(new Date());
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            byte[] digest = md.digest();
            String mdpasswd = DatatypeConverter.printHexBinary(digest).toUpperCase();
            String sendData = jsonTemp.replace("{content}", content).replace("{password}", mdpasswd);
            logger.info("发送的JSON：\n" + sendData);
            send(sendData);
        }catch (NoSuchAlgorithmException e){
            logger.error("MD5加密出错");
        } catch (Exception e){
            logger.error("其他类型错误\n" + e.getMessage());
        }
    }
}
