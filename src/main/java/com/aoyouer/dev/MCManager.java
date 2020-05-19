package com.aoyouer.dev;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.mamoe.mirai.console.command.BlockingCommand;
import net.mamoe.mirai.console.command.CommandSender;
import net.mamoe.mirai.console.command.JCommandManager;
import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.console.plugins.PluginBase;
import net.mamoe.mirai.console.utils.Utils;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.BroadcastControllable;
import net.mamoe.mirai.event.Event;
import net.mamoe.mirai.message.GroupMessageEvent;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;


//和Minecraft服务器相关的模块
public class MCManager {
    private Config setting;
    private PluginBase pluginBase;
    private String McPingAPI = "https://api-mping.loliboy.com/ping/{address}/{port}";

    public MCManager(Config setting, PluginBase pluginBase) {
        this.setting = setting;
        this.pluginBase = pluginBase;

        JCommandManager.getInstance().register(pluginBase, new BlockingCommand("mcping", new ArrayList<>(), "设置mcping的对象", "/mcping set") {
            @Override
            public boolean onCommandBlocking(@NotNull CommandSender commandSender, @NotNull List<String> list) {
                if (list.size() < 1) {
                    //commandSender.sendMessageBlocking("/mcping set ip:port");
                    return false;
                }

                if (list.get(0).equals("set")) {
                    //修改配置文件中的server
                    //还应该先检查合法性
                    setting.set("McPing", list.get(1));
                    setting.save();
                    commandSender.sendMessageBlocking("已经修改配置文件为:" + list.get(1));
                    return true;
                } else if (list.get(0).equals("show")) {
                    commandSender.sendMessageBlocking("当前查询目标为:" + setting.getString("McPing"));
                    return true;
                } else {
                    commandSender.sendMessageBlocking("/mcping set ip:port");
                    return false;
                }

            }
        });
    }

    //获取MC服务器在线情况
    public void getMcPingInfo(GroupMessageEvent event) {
        pluginBase.getScheduler().async(() -> {
            String[] serverInfo = setting.getString("McPing").split(":");
            event.getSubject().sendMessage("正在查询服务器在线情况 目标:" + serverInfo[0] + "端口:" + serverInfo[1]);
            try {
                String response = Utils.tryNTimes(3, () ->
                        Jsoup.connect(McPingAPI.replace("{address}", serverInfo[0]).replace("{port}", serverInfo[1]))
                                .ignoreContentType(true).timeout(5000).execute().body()
                );
                JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();
                JsonObject serverObj = responseObj.get("rinfo").getAsJsonObject();
                event.getSubject().sendMessage(setting.getString("McPingTemplate")
                        .replace("{online}", responseObj.get("currentPlayers").getAsString())
                        .replace("{max}", responseObj.get("maxPlayers").getAsString())
                        .replace("{version}", responseObj.get("version").getAsString())
                );
            } catch (Exception e) {
                event.getSubject().sendMessage("查询失败");
            }
        });
    }

    public MCWebsocketClient wsConnect(Group group) {
        //pluginBase.getScheduler().async(() -> {
            //建立Websocke连接
            try {
                pluginBase.getLogger().info("尝试建立ws\n");
                MCWebsocketClient webSocketClient = new MCWebsocketClient(new URI("ws://192.168.0.200:255/fine"),group,setting);
                webSocketClient.connect();
                return webSocketClient;
            } catch (Exception e) {
                pluginBase.getLogger().error("建立ws连接出错\n" + e.getMessage());
                return null;
            }
       // });

    }

}
