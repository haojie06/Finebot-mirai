package com.aoyouer.dev;

import net.mamoe.mirai.console.events.EventListener;
import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.console.plugins.ConfigSection;
import net.mamoe.mirai.console.plugins.ConfigSectionFactory;
import net.mamoe.mirai.console.plugins.PluginBase;
import net.mamoe.mirai.message.GroupMessageEvent;

class FineBot extends PluginBase {
    private Config setting;
    public void onLoad() {
        super.onLoad();
        this.setting = this.loadConfig("setting.yml");
        //this.setting.setIfAbsent("WelcomeMap", ConfigSectionFactory.create());
        this.setting.setIfAbsent("version","1.0");
        ConfigSection cs =  this.setting.getConfigSection("WelcomeMap");
        getLogger().info("FineGroupBot loaded!");
    }

    public void onEnable() {
        getLogger().info("FineGroupBot enabled!");
        EventListener eventListener = this.getEventListener();
        eventListener.subscribeAlways(GroupMessageEvent.class,(GroupMessageEvent event) -> {
            System.out.println("有人发言！");
        });
        //注册群管理模块
        GroupManager groupManager = new GroupManager(this.setting.getConfigSection("WelcomeMap"),eventListener);
    }

}