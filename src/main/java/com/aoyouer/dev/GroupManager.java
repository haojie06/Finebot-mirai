package com.aoyouer.dev;

import net.mamoe.mirai.console.events.EventListener;
import net.mamoe.mirai.console.plugins.ConfigSection;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.event.events.MemberLeaveEvent;

import java.util.Map;

public class GroupManager {
    private ConfigSection notifySection;
    private EventListener eventListener;

    public GroupManager(ConfigSection notifySection, EventListener eventListener) {
        this.notifySection = notifySection;
        this.eventListener = eventListener;
        //对加群事件做出响应
        eventListener.subscribeAlways(MemberJoinEvent.class,(event)->{
            //根据群号不同提示也不同
            String groupId = String.valueOf(event.getGroup().getId());
            System.out.println("有人加群");
            if (notifySection.containsKey(groupId)){
                System.out.println("群" + groupId + "有对应的欢迎词");
                String notifyMsg = notifySection.getString(groupId);
                event.getGroup().sendMessage("欢迎" + event.getMember().getNick() + notifyMsg);
            }
            else{
                System.out.println("群" + groupId + "无对应的欢迎词" + notifySection.toString());
                event.getGroup().sendMessage("欢迎" + event.getMember().getNick() + "加入本群");
            }

        });

        eventListener.subscribeAlways(MemberLeaveEvent.class,(MemberLeaveEvent event)->{
            event.getGroup().sendMessage(event.getMember().getNameCard() + "离开了我们");
        });
    }
//对加群者进行欢迎
}
