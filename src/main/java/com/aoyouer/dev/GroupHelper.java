package com.aoyouer.dev;

import net.mamoe.mirai.console.events.EventListener;
import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.console.plugins.ConfigSection;
import net.mamoe.mirai.message.GroupMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//群帮手
public class GroupHelper {
    private EventListener eventListener;
    private Config setting;
    final Logger logger = LoggerFactory.getLogger(getClass());
    public GroupHelper(EventListener eventListener, Config setting) {
        this.eventListener = eventListener;
        this.setting = setting;

        eventListener.subscribeAlways(GroupMessageEvent.class,(GroupMessageEvent event)->{
            String msg = event.getMessage().contentToString();
            //char firstChar = msg.charAt(0);
            if (setting.getList("HelperGroupId").contains(String.valueOf(event.getGroup().getId()))){
                //暂时不要求？开头
                ConfigSection questionAliasSec = this.setting.getConfigSection("QuestionAlias");
                ConfigSection questionSec = this.setting.getConfigSection("QuestionMap");
                if (questionAliasSec.containsKey(msg)){
                    String key = questionAliasSec.getString(msg);
                    event.getSubject().sendMessage(questionSec.getString(key));
                }
            }
        });
    }
}
