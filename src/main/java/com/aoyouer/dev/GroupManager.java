package com.aoyouer.dev;

import com.aoyouer.dev.utils.PermissionController;
import net.mamoe.mirai.console.events.EventListener;
import net.mamoe.mirai.console.plugins.ConfigSection;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.event.events.MemberJoinEvent;
import net.mamoe.mirai.event.events.MemberLeaveEvent;
import net.mamoe.mirai.message.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageUtils;

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
                String notifyMsg = notifySection.getString(groupId);
                event.getGroup().sendMessage(MessageUtils.newChain(new At(event.getMember())).plus(notifyMsg));
            }
            else{
                event.getGroup().sendMessage(MessageUtils.newChain(new At(event.getMember())).plus(event.getMember().getNick() + "加入本群"));
            }

        });

        eventListener.subscribeAlways(MemberLeaveEvent.class,(MemberLeaveEvent event)->{
            event.getGroup().sendMessage(event.getMember().getNameCard() + "离开了我们");
        });
    }
    public void testMemberJoinWelcome(MessageEvent event){
        String groupId = String.valueOf(event.getSubject().getId());
        //这种方式貌似得手动添加管理员了，因为得不到发送者的permission。
        //if (PermissionController.check(event.getSubject(), MemberPermission.ADMINISTRATOR))
        if (notifySection.containsKey(groupId)){
            String notifyMsg = notifySection.getString(groupId);
            event.getSubject().sendMessage("欢迎" + event.getSender().getNick() + notifyMsg);
        }
        else{
            event.getSubject().sendMessage("欢迎" + event.getSender().getNick() + "加入本群");
        }
    }
}
