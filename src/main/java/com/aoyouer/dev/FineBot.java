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
    private MCWebsocketClient mcWebsocketClient;
    public void onLoad() {
        super.onLoad();
        this.setting = this.loadConfig("setting.yml");
        //this.setting.setIfAbsent("WelcomeMap", ConfigSectionFactory.create());
        this.setting.setIfAbsent("Version","1.0");
        getLogger().info("FineGroupBot loaded!");
    }

    public void onEnable() {
        getLogger().info("FineGroupBot enabled~~~~");
        EventListener eventListener = this.getEventListener();
        //注册群管理模块
        GroupManager groupManager = new GroupManager(this.setting.getConfigSection("WelcomeMap"),eventListener);
        //群帮手对象
        GroupHelper groupHelper = new GroupHelper(eventListener,setting);
        //MC服务器相关对象
        MCManager mcManager = new MCManager(setting,this,eventListener);
    }
}