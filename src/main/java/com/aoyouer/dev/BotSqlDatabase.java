package com.aoyouer.dev;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jdk.internal.jline.internal.Nullable;
import net.mamoe.mirai.console.plugins.Config;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.events.MemberLeaveEvent;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

//机器人数据库操作相关类
public class BotSqlDatabase {
    private Connection connection;
    private Statement statement;
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Config setting;
    private WebSocketClient webSocketClient;
    private Group group;
    public BotSqlDatabase(Config setting) {
        this.setting = setting;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:finebot.db");
            logger.info("建立数据库连接");
            statement = connection.createStatement();
            //创建加群记录的数据表
            Statement statement = connection.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS member_join"
                    + "(id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "member_id INT NOT NULL,"
                    + "join_time TEXT NOT NULL,"
                    + "join_count INT NOT NULL,"
                    + "join_history TEXT NOT NULL);";
            statement.executeUpdate(sql);
            //创建白名单和群名片绑定的数据表
            sql = "CREATE TABLE IF NOT EXISTS group_whitelist"
                    + "(id integer primary key autoincrement,"
                    + "member_id int not null,"
                    + "game_id text not null,"
                    + "add_time text not null,"
                    + "add_count int not null,"
                    + "history text not null);";
            statement.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        }


        try{
            webSocketClient = new  WebSocketClient(new URI("ws://192.168.0.200:255/fine")){
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    logger.info("白名单WS建立连接");
                }
                @Override
                public void onMessage(String message) {
                    JsonObject msgObj = JsonParser.parseString(message).getAsJsonObject();

                    if (msgObj.get("operate").getAsString().equals("runcmd")) {
                        String text = msgObj.get("text").getAsString();
                        logger.info("命令返回:" + text);
                        if (group != null){
                            //group.sendMessage("命令返回" + text);
                            if (text.equals("Player removed from whitelist")){
                                group.sendMessage("删除白名单");
                            }
                            else if (text.equals("Player added to whitelist")){
                                group.sendMessage("添加白名单");
                            }
                        }
                        //close();
                    }

                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    logger.info("关闭临时ws连接");
                    if (group != null){
                        group.sendMessage("WS连接已经断开，请不要再添加白名单");
                    }
                }

                @Override
                public void onError(Exception ex) {

                }
            };
        }catch (Exception e){e.printStackTrace();}

    }

    //加群记录
    public String joinGroup(Member member) {
        String result = "";
        try {
            //从加群数据表中搜索该成员
            //ResultSet rs = statement.executeQuery( String.format("SELECT * FROM MEMBERJOIN WHERE MEMBER_ID=%s;", String.valueOf(member.getId())));
            String sql = String.format("SELECT * FROM member_join WHERE member_id=%d;", member.getId());
            ResultSet rs = statement.executeQuery(sql);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");//设置日期格式
            String joinTime = df.format(new Date());
            if (!rs.next()) {
                logger.info("群员记录不存在");
                //创建记录
                sql = String.format("INSERT INTO member_join (member_id,join_time,join_count,join_history) VALUES (%d,'%s',%d,'')", member.getId(), joinTime, 1);
                statement.executeUpdate(sql);
                result = "\n这是您首次加入本群，玩得愉快~";
            } else {
                logger.info("群员记录存在" + rs.getFetchSize());
                long memberId = rs.getLong("member_id");
                String join_time = rs.getString("join_time");
                int count = rs.getInt("join_count") + 1;
                String joinHistory = rs.getString("join_history");
                //更新为本次加群
                sql = String.format("UPDATE member_join set join_time='%s',join_count=%d where member_id=%d", join_time, count, memberId);
                statement.executeUpdate(sql);
                //查找白名单记录
                sql = String.format("SELECT * FROM group_whitelist WHERE member_id=%d;", member.getId());
                rs = statement.executeQuery(sql);
                result = "\n这是您第" + count + "次加入本群\n历史加群记录:\n" + joinHistory;
                if (rs.next()){
                    result = result + "\n历史白名单记录:\n" + rs.getString("history");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    //退出QQ群
    public String leavGroup(Member member,Group group) {
        String result = "";
        try {
            String sql = String.format("SELECT * FROM member_join WHERE member_id=%d;", member.getId());
            ResultSet rs = statement.executeQuery(sql);
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");//设置日期格式
            String leaveTime = df.format(new Date());
            //考虑到已经在群里的人是没有记录的
            if (!rs.next()){
                //还没有记录的人则创建记录
                result = "xxxx-xx-xx~" + leaveTime + "\n加群次数:1";
                sql = String.format("INSERT INTO member_join (member_id,join_time,join_count,join_history) VALUES (%d,'%s',%d,'%s')", member.getId(), "", 1, "xxxx-xx-xx~" + leaveTime);
                statement.executeUpdate(sql);
            }
            else {
                //有了插件之后才加入的群员退出群
                String joinTime =  rs.getString("join_time");
                String joinHistory = rs.getString("join_history");
                int count = rs.getInt("join_count");
                joinHistory = joinHistory + joinTime + "~" + leaveTime + "\n";
                sql = String.format("UPDATE member_join set join_history='%s' where member_id=%d", joinHistory, member.getId());
                statement.executeUpdate(sql);
                //executeWSCmd(String.format("whitelist remove "));
                result = joinHistory + "\n加群次数:" + count;
            }
            //退群删除白名单，首先查找 QQ对应的白名单
            sql = String.format("SELECT * FROM group_whitelist WHERE member_id=%d;", member.getId());
            rs = statement.executeQuery(sql);
            if (rs.next()){
                executeWSCmd(String.format("whitelist remove \\\"%s\\\"",rs.getString("game_id")),group);
                logger.info("退群删除白名单:" + rs.getString("game_id"));
            }
        }catch (Exception e){e.printStackTrace();}
    return result;
    }

    public String addWhitelist(Long memberId,String gameId,Group group){
        String result = "";
        if (gameId.contains("\n")){
            return "非法输入";
        }
        try {
            //先查看此人是否已经添加过白名单了
            String sql = String.format("SELECT * FROM group_whitelist WHERE member_id=%d;", memberId);
            ResultSet rs = statement.executeQuery(sql);
            if (!rs.next()) {
                //添加白名单
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");//设置日期格式
                String addTime = df.format(new Date());
                sql = String.format("INSERT INTO group_whitelist (member_id,game_id,add_time,add_count,history) VALUES (%d,'%s','%s',%d,'%s')", memberId, gameId, addTime, 1, addTime + ":" + gameId + "\n");
                //需要建立ws连接，执行命令，如果没执行成功就不要往数据库里添加东西了
                if (webSocketClient.isOpen()) {
                    executeWSCmd(String.format("whitelist add \\\"%s\\\"", gameId), group);
                    statement.executeUpdate(sql);
                    result = "已经为你添加白名单:" + gameId + "\n如果添加错误请输入“服务器 修改白名单”，如果依旧无法进入请联系服主\n";
                }
                else {
                    result = "Websocket连接未建立，请稍后再添加白名单";
                }
            }
            else {
                //已经添加过白名了，请使用修改白名单指令
                result = "用户" + memberId + "已经添加过白名单:" + rs.getString("game_id") + "\n历史添加:\n" + rs.getString("history") + "\n请使用 “服务器 修改白名单” 来修改当前绑定的白名单";
            }
        }catch (Exception e){e.printStackTrace();}
        return result;
    }
    //修改白名单
    public String changeWhitelist(Long memberId,String gameId,Group group){
        String result = "";
        if (gameId.contains("\n")){
            return "非法输入";
        }
        try {
            //先查找这人是否添加过白名单
            String sql = String.format("SELECT * FROM group_whitelist WHERE member_id=%d;", memberId);
            ResultSet rs = statement.executeQuery(sql);
            if (!rs.next()){
                //还没加过白名单
                result = "你还没有添加过白名单,请输入“服务器 添加白名单”来添加白名单。";
            }
            else {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");//设置日期格式
                String editTime = df.format(new Date());
                String oldId = rs.getString("game_id");
                if (oldId.equals(gameId)){
                    return "你已经添加过" + oldId + "了,看起来你的群名片还没有改变哦";
                }
                int count = rs.getInt("add_count") + 1;
                String history = rs.getString("history") + editTime + ":" + gameId + "\n";
                sql = String.format("UPDATE group_whitelist set game_id='%s',add_time='%s',add_count=%d,history='%s' where member_id=%d", gameId, editTime, count, history, memberId );
                if (webSocketClient.isOpen()) {
                    statement.executeUpdate(sql);
                    executeWSCmd(String.format("whitelist remove \\\"%s\\\"", oldId), group);
                    executeWSCmd(String.format("whitelist add \\\"%s\\\"", gameId), group);
                    result = "已经为你将白名单从" + oldId + "修改为" + gameId + "\n历史添加:\n" + history;
                }
                else {
                    result = "Websocket连接未建立，请稍后再添加白名单";
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            result = "添加白名单出错";
        }
        return  result;
    }
    //删除白名单

    //通过Websocket来执行命令 group用来发送反馈
    private void executeWSCmd(String cmd,@Nullable Group group){
        this.group = group;
        String tempJson = "{\"operate\":\"runcmd\",\"passwd\":\"{password}\",\"cmd\":\"{content}\"}";
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");//设置日期格式
        String password = setting.getString("WSPassword") + df.format(new Date());
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            byte[] digest = md.digest();
            String mdpasswd = DatatypeConverter.printHexBinary(digest).toUpperCase();
            String sendData = tempJson.replace("{content}", cmd).replace("{password}", mdpasswd);
            logger.info("输出指令\n" + sendData);
            webSocketClient.send(sendData);
        }catch (NoSuchAlgorithmException e){
            logger.error("MD5加密出错");
        } catch (Exception e){
            logger.error("其他类型错误\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void wsConnect(){
        webSocketClient.connect();
    }

     public static void main(String[] args) {
        //BotSqlDatabase botSqlDatabase = new BotSqlDatabase();
     }
}
