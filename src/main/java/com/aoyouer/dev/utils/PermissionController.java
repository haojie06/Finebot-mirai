package com.aoyouer.dev.utils;

import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.User;

import java.util.List;

public class PermissionController {
    public static boolean check(Member member,MemberPermission memberPermission) {
        if (member.getPermission().compareTo(memberPermission) < 0){
            return false;
        }
        else{
            return true;
        }
    }
    public static boolean check(Config setting, User user){
        List<Long> adminList = setting.getLongList("Admin");
        if (adminList.contains(user.getId())){
            return true;
        }
        else{
            return false;
        }
    }
}
