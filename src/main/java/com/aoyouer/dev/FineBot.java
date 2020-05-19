package com.aoyouer.dev;

import com.aoyouer.dev.utils.PermissionController;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.console.command.JCommandManager;
import net.mamoe.mirai.console.events.EventListener;
import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.console.plugins.ConfigSection;
import net.mamoe.mirai.console.plugins.ConfigSectionFactory;
import net.mamoe.mirai.console.plugins.PluginBase;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.message.GroupMessageEvent;

class FineBot extends PluginBase {
    private Config setting;
    public void onLoad() {
        super.onLoad();
        this.setting = this.loadConfig("setting.yml");
        //this.setting.setIfAbsent("WelcomeMap", ConfigSectionFactory.create());
        this.setting.setIfAbsent("Version","1.0");
        ConfigSection cs =  this.setting.getConfigSection("WelcomeMap");
        getLogger().info("FineGroupBot loaded!");
    }

    public void onEnable() {
        getLogger().info("FineGroupBot enabled~~~~");
        EventListener eventListener = this.getEventListener();
        //注册群管理模块
        GroupManager groupManager = new GroupManager(this.setting.getConfigSection("WelcomeMap"),eventListener);
        //注册命令监听
        MCManager mcManager = new MCManager(setting,this);
        eventListener.subscribeAlways(GroupMessageEvent.class,(GroupMessageEvent event) -> {
            String message = event.getMessage().contentToString();
            if (message.contains("加群测试")){
                if(!PermissionController.check(event.getSender(), MemberPermission.ADMINISTRATOR)){
                    event.getSubject().sendMessage("您无权限执行该命令");
                    return;
                }
                groupManager.testMemberJoinWelcome(event);
            }
            else if (message.contains("服务器 在线")){
                getLogger().info("查询服务器 在线情况");
                mcManager.getMcPingInfo(event);
            }
            else if (message.contains("游戏消息同步开启")){
                getLogger().info("消息同步开启");
                if(!PermissionController.check(event.getSender(), MemberPermission.ADMINISTRATOR)){
                    event.getSubject().sendMessage("您无权限执行该命令");
                    return;
                }
                event.getGroup().sendMessage("已开启游戏消息同步");
                //开启消息同步并且把当前的群传过去
                setting.set("GameToGroup",true);
                setting.save();
                mcManager.wsConnect(event.getGroup());
            }
            else if (message.contains("游戏消息同步关闭")){
                if(!PermissionController.check(event.getSender(), MemberPermission.ADMINISTRATOR)){
                    event.getSubject().sendMessage("您无权限执行该命令");
                    return;
                }
                setting.set("GameToGroup",false);
                setting.save();
                event.getGroup().sendMessage("已关闭游戏消息同步");
            }
        });
    }
}