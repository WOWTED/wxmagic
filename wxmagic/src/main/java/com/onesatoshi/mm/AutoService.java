package com.onesatoshi.mm;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * note:APP获取到辅助功能权限后，一旦APP进程被强杀就会清除该权限，再次进入APP又需要重新申请，正常退出则不会
 * 2.确保手机的微信消息能在通知栏显示
 */
public class AutoService extends AccessibilityService {

    public static final String TAG = AutoService.class.getName();

    //记录已打招呼的人数
    private int sum = 0;

    //记录附近的人列表页码,初始页码为1
    private int page = 0;

    private int nodeNumInPage = 0;
    private int indexInPage = 0;

    boolean hasNotify = false;

    //记录页面跳转来源
    private PrePageEnum prepage = PrePageEnum.UNKNOWN;

    // text2Speech
    private TextToSpeech mTts;

    private List<String> nameList = new CopyOnWriteArrayList<>();

    private Handler handler = new Handler();

    private String name;
    private String scontent;
    private int MSG_TYPE = 0;

    private List<AccessibilityNodeInfo> listNodeResult = new ArrayList<>();

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {

        int eventType = event.getEventType();

//        Log.d(TAG, "eventType:" + eventType);
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && MainActivity.canShowWindow(this)) {

//            TasksWindow.show(this,event.getPackageName() + "\n" + event.getClassName());
            TasksWindow.changeMsg(event.getPackageName() + "\n" + event.getClassName());

        }

        if (eventType != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
                && eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        //通知栏、Toast会触发该类型事件
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {

            //通知栏事件
            if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {

                Notification notification = (Notification) event.getParcelableData();

                // 事件附加信息
                String tickerText = String.valueOf(notification.tickerText);
                Log.v(TAG, "tickerText:" + tickerText + "");

                // 自动回复开启
                if (Config.isOpenAutoReply) {

                    String content = notification.tickerText.toString();

                    String[] cc = content.split(":");

                    name = cc[0].trim();
                    scontent = cc[1].trim();

                    Log.v(TAG, "name:" + name + "\tscontent:" + scontent);

                    if (scontent.startsWith("[图片]")) {
                        MSG_TYPE = 1;
                        Log.v(TAG, "receive 图片");
                        Config.toProcessEvent = true;

                    } else if (scontent.startsWith("[链接]")) {
                        MSG_TYPE = 2;
                        Log.v(TAG, "receive 链接");
                        Config.toProcessEvent = true;

                    } else if (scontent.startsWith("[视频]")) {
                        MSG_TYPE = 3;
                        Log.v(TAG, "receive 视频");
                        Config.toProcessEvent = true;

                    } else if (scontent.startsWith("[微信红包]")) {
                        // 不处理

                        MSG_TYPE = 4;
                        Log.v(TAG, "receive 微信红包");

                    } else if (scontent.startsWith(name + "向你推荐了")) {
                        MSG_TYPE = 5;
                        Log.v(TAG, "receive 公众号");
                        Config.toProcessEvent = true;

                    } else {
                        MSG_TYPE = 0;
                    }

                    if (MSG_TYPE == 1 || MSG_TYPE == 2 || MSG_TYPE == 3 | MSG_TYPE == 5) {
                        notifyWechat(event);
                    }
                }

                if (tickerText.contains(": [微信红包]")) {
                    // 调起来对应的应用
                    openNotification(event);
                }

            }
        }

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            //抢红包
            if (Config.isOpenAutoOpenLuckyMoney) {
                processLuckMoneyEvent(event);
            }  //自动加人
            else if (Config.isOpenAutoNearBy) {
                processNearbyEvent(event);
            } // 自动点赞
            else if (Config.isOpenAutoPrize) {
                processAutoPrize(event);
            } // 接受好友，加入群聊
            else if (Config.isOpenAddFriendFromGroup) {
                processAddFriendFromGroup(event);
            } // 发朋友圈
            else if (Config.isIsOpenAutoSns && !Config.HAS_POST_SNS) {
                processPostSns(event);
            } // 自动回复
            else if (Config.isOpenAutoReply) {
                processAutoRepay(event);
            }
        }

    }

    private void processLuckMoneyEvent(AccessibilityEvent event) {

//        Log.v(TAG, "EventType: " + event.getEventType() + "\taction:" + event.getAction() + "\tpackage:" + event.getPackageName() + "\tClass:" + event.getClassName() + "\t");

        // 1.当前在聊天界面，查找并点击领取红包
        if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {

            //等待加载完成
            doSleep(2000);
            findAndOpenLuckyEnvelope();

        } else if (UI.LUCK_MONEY_DETAIL_UI.equals(event.getClassName())) {// 3.抢到红包后的详情页面

            //拆完红包后看详细纪录的界面
            //clickButtonByText("查看我的红包记录");

            Log.v(TAG, "抢到红包了");

            // 返回
            doSleep(1000);
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

        } else {
            // 2.当前在红包待开页面，点击打开红包
            for (int j = 0; j < Config.luckyActivityNames.size(); j++) {
                if (Config.luckyActivityNames.get(j).equals(event.getClassName())) {

                    doSleep(1000);
                    clickOpenBtn(event);
                    break;
                }
            }

        }


    }


    private void processNearbyEvent(AccessibilityEvent event) {

        Log.v(TAG, "EventType: " + event.getEventType() + "\taction:" + event.getAction() + "\tpackage:" + event.getPackageName() + "\tClass:" + event.getClassName() + "\t");


        // 微信主页面，依次点击：发现->附近的人
        if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {

            openNearBy();

        } else if ("com.tencent.mm.plugin.nearby.ui.NearbyFriendsUI".equals(event.getClassName())) {
            // 附近的人列表页面

            prepage = PrePageEnum.NEARBY;

            //当前在附近的人界面就点选人打招呼
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

            if (page == 0) {

                selectAllUserFromList(nodeInfo);

                nodeNumInPage = listNodeResult.size();
                indexInPage = 0;
                page = 1;
            }

            if (indexInPage == nodeNumInPage - 1) {
                // 当页最后一个用户，翻页

                sum += nodeNumInPage;

                scrollUserList(nodeInfo);

                nodeNumInPage = listNodeResult.size();

                page++;
                indexInPage = 0;

                Log.d(TAG, "new listNodeResult, 列表人数: " + listNodeResult.size());

            }


            Log.d(TAG, "附近的人, sum=" + sum + ", page=" + page + ", listNodeResult.size(): " + listNodeResult.size() + ", nodeNumInPage=" + nodeNumInPage + ", indexInPage=" + indexInPage);

            doSleep(1000);

            // 处理当页用户

            Log.e(TAG, "点击当页第" + (indexInPage + 1) + "个用户");
            AccessibilityNodeInfo node = listNodeResult.get(indexInPage);


            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);

            if (node.getParent() != null) {
                node.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            } else {
                Log.e(TAG, "node parent is null");
            }


        } else if ("com.tencent.mm.plugin.profile.ui.ContactInfoUI".equals(event.getClassName())) {

            //从打招呼界面跳转来的，则点击返回到附近的人页面
            if (prepage == PrePageEnum.SAYHI) {

                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

                Log.d(TAG, "从打招呼页面跳转到，ContactInfoUI，返回附近的人");

            } else if (prepage == PrePageEnum.NEARBY) {
                //从附近的人跳转来的，则点击打招呼按钮
                indexInPage++;

                AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                if (nodeInfo == null) {
                    Log.d(TAG, "rootWindow为空");
                    return;
                }

                doSleep(1000);

                // 找到打招呼按钮
                List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("打招呼");

                if (list != null && list.size() > 0) {

                    boolean sayHiClick = false;

                    for (AccessibilityNodeInfo sayHi : list) {

                        if (!sayHiClick && sayHi.isClickable()) {

                            doSleep(1000);

                            Log.d(TAG, "点击打招呼按钮");

                            sayHi.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                            if (sayHi.getParent() != null) {
                                sayHi.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            } else {
                                Log.e(TAG, "打招呼parent为null");
                            }

                            sayHiClick = true;
                            break;
                        }
                    }

                } else {

                    //如果遇到已加为好友的则界面的“打招呼”变为“发消息"，所以直接返回上一个界面并记录打招呼人数+1
                    performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

                    Log.d(TAG, "你们已经是好友了");

                }
            }
        } else if ("com.tencent.mm.ui.contact.SayHiEditUI".equals(event.getClassName())) {
            //当前在打招呼页面

            prepage = PrePageEnum.SAYHI;

            //输入打招呼的内容并发送
            autoInputHelloMsg(Config.HELLO_MSG);

            Log.d(TAG, "输入打招呼内容，prepage=" + PrePageEnum.SAYHI.getName());

            doSleep(1000);

            clickSendHelloMsg("发送");


        } else if ("com.tencent.mm".equals(event.getClassName().subSequence(1, 14))) {

            openNearBy();

        }
    }

    /**
     * 自动点赞----start
     *
     * @param event
     */

    //自动点赞
    private void processAutoPrize(AccessibilityEvent event) {

        Log.v(TAG, "event.getClassName():" + event.getClassName());

        // 微信主页面，依次点击：发现->朋友圈
        if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {

            openCircleOfFriends();

        } else if ("com.tencent.mm.plugin.sns.ui.SnsTimeLineUI".equals(event.getClassName())) {
            int zanNum = 0, MAX_ZAN = 1;

            AccessibilityNodeInfo firstListNode = getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/doq").get(0);
            // 下拉动作
            firstListNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            // 等待加载更新
            doSleep(5000);

            while (zanNum < MAX_ZAN) {

                AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

                if (nodeInfo != null) {
                    // 朋友圈列表  com.tencent.mm:id/ddn   android.widget.ListView
                    List<AccessibilityNodeInfo> listViews = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/doq");

                    Log.v(TAG, "listViews:" + listViews.size());

                    if (listViews != null && listViews.size() > 0) {

                        AccessibilityNodeInfo listView = listViews.get(0);

                        // 赞的button列表  com.tencent.mm:id/dao   android.widget.ImageView
                        List<AccessibilityNodeInfo> zanNodes = listView.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/dlj");
                        Log.v(TAG, "zanNodes:" + zanNodes.size());

                        for (AccessibilityNodeInfo zan : zanNodes) {

                            zan.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                            doSleep(1000);

                            // 赞  按钮  com.tencent.mm:id/d_m  android.widget.LinearLayout
                            List<AccessibilityNodeInfo> zsNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/dkm");

                            if (zsNodes != null && zsNodes.size() > 0) {
                                if (zsNodes.get(0).findAccessibilityNodeInfosByText("赞").size() > 0) {
                                    Log.v(TAG, "zsNodes:" + zsNodes.size());

                                    zsNodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    zanNum++;
                                }
                            }

                        }

                        // 向前翻页
                        listView.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);

                        doSleep(1000);

                    } else {
                        Log.v(TAG, "listViews is empty");
                        break;
                    }
                } else {
                    Log.v(TAG, "nodeInfo is empty");
                    break;
                }
            }
        } else if ("com.tencent.mm".equals(event.getClassName().subSequence(1, 14))) {
            openCircleOfFriends();
        }

    }


    //从群里面添加好友
    private void processAddFriendFromGroup(AccessibilityEvent event) {

        if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
            if (nameList != null && nameList.size() > 0) {
                // 打开群聊搜索页面
                openGroup();
            } else {
                // 打开好友申请
                openNewFriend();
            }

        } else if ("com.tencent.mm.plugin.subapp.ui.friend.FMessageConversationUI".equals(event.getClassName())) {
            // 接受好友申请
            addFriends();

        } else if ("com.tencent.mm.plugin.profile.ui.SayHiWithSnsPermissionUI".equals(event.getClassName())) {
            // 完成按钮
            verifyFriend();

        } else if ("com.tencent.mm.plugin.profile.ui.ContactInfoUI".equals(event.getClassName())) {
            // 好友详细资料页
            contactInfo();

        } else if ("com.tencent.mm.ui.contact.ChatroomContactUI".equals(event.getClassName())) {

            if (nameList.size() > 0) {
                // 搜索群聊，并打开
                searchGroup();
            } else {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            }

        } else if ("com.tencent.mm.ui.chatting.ChattingUI".equals(event.getClassName())) {
            // 打开群聊设置页面
            openGroupSetting();

        } else if ("com.tencent.mm.chatroom.ui.ChatroomInfoUI".equals(event.getClassName())) {

            Log.v(TAG, "nameList.size():" + nameList.size());

            if (nameList.size() > 0) {
                //添加用户到群聊里
                addToGroup();
            } else {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            }

        } else if ("com.tencent.mm.ui.base.i".equals(event.getClassName())) {
            // 对话框的处理
            dialogClick();
        }

    }


    /**
     * 发朋友圈
     *
     * @param event
     */
    private void processPostSns(AccessibilityEvent event) {

        //第一次启动app
        if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
            //进入朋友圈页面
            openCircleOfFriends();

        } else if ("com.tencent.mm.plugin.sns.ui.SnsTimeLineUI".equals(event.getClassName())) {
            //点击发朋友圈按钮，并且选择照片
            clickCircleOfFriendsBtn();
        } else if ("com.tencent.mm.plugin.sns.ui.SnsUploadUI".equals(event.getClassName())) {
            //写入要发送的朋友圈内容
            inputContentFinish(Config.SNS_CONTENT);
        } else if ("com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI".equals(event.getClassName())) {
            // 选择图片
            choosePicture(Config.ALBUM_INDEX, Config.ALBUM_COUNT);

        } else if ("com.tencent.mm".equals(event.getClassName().subSequence(1, 14))) {
            openCircleOfFriends();
        }

    }

    /***
     * 抢红包----start
     */


    // 聊天页面遍历“领取红包”，点击红包，没有找到则隐藏微信
    public void findAndOpenLuckyEnvelope() {

        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {

            // 领取红包
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ai5");

//            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("领取红包");

            if (list != null && list.size() > 0) {
                //选择聊天记录中最新的红包，倒数去查找
                for (int i = list.size() - 1; i >= 0; i--) {

                    if ("领取红包".equals(list.get(i).getText())) {
                        AccessibilityNodeInfo parent = list.get(i).getParent();

                        Log.d(TAG, "-->领取红包:" + parent);

                        if (parent != null) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                    }
                }
            }
        } else {
            Log.e(TAG, "nodeInfo is null");
        }

        // 返回，微信后台运行
//        doSleep(1000);
//        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }


    //点击打开红包
    private void clickOpenBtn(AccessibilityEvent event) {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            List<AccessibilityWindowInfo> nodeInfos = getWindows();

            for (AccessibilityWindowInfo window : nodeInfos) {
                AccessibilityNodeInfo nodeInfo = window.getRoot();
                if (nodeInfo == null) {
                    Log.e(TAG, "nodeInfo为空");

                    break;
                }

                Log.v(TAG, "getWindows()不为空");
                List<AccessibilityNodeInfo> list = null;
                for (String id : Config.openIds) {
                    list = nodeInfo.findAccessibilityNodeInfosByViewId(id);

                    if (list != null && list.size() > 0) {
                        list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);

                        Log.v(TAG, "开红包，clickBy getWindows()");
                        return;
                    }
                }
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {

            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();  //获得整个窗口对象

            if (nodeInfo == null) {
                Log.e(TAG, "rootWindow为空");
                return;
            }

            //bi3是本人写代码时微信拆红包的button的id,该id可能会在更新微信版本后发生变更,可通过Android Device Monitor查看获取
            //可创建一个hashMap,在微信发生版本变更时储存对应微信版本号与id值，用于适配多个微信版本
            List<AccessibilityNodeInfo> list = null;
            for (String id : Config.openIds) {

                list = nodeInfo.findAccessibilityNodeInfosByViewId(id);

                if (list != null && list.size() > 0) {
                    list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);

                    Log.v(TAG, "开红包，clickBy getRootInActiveWindow()");

                    return;
                }
            }

            //如果没找到拆红包的button，则将界面上所有子节点都点击一次
            for (int i = nodeInfo.getChildCount() - 1; i >= 0; i--) {
                if (("android.widget.Button").equals(nodeInfo.getChild(i).getClassName())) {
                    nodeInfo.getChild(i).performAction(AccessibilityNodeInfo.ACTION_CLICK);

                    Log.e(TAG, "开红包，clickBy android.widget.Button");

                    return;
                }
            }

            Log.e(TAG, "开红包，未找到开红包按钮");

        }
    }


    /***
     * 接受好友，加入群聊--start
     */

    //接受好友申请
    private void addFriends() {

        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo != null) {
            // 待通过好友申请
            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("接受");
            if (list != null && list.size() > 0) {

                list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);

                // 昵称 android.widget.TextView
                List<AccessibilityNodeInfo> nameText = list.get(0).getParent().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ber");

                // 新用户昵称列表
                nameList.add(nameText.get(0).getText().toString());

                Log.v(TAG, "nameText.get(0).getText().toString():" + nameText.get(0).getText().toString());

            } else {
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

            }
        } else {
            Log.e(TAG, "nodeinfo is null");
        }
    }

    //接受好友申请的完成按钮
    private void verifyFriend() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo != null) {
            // 完成按钮 android.widget.TextView
            AccessibilityNodeInfo finishNode = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/iv").get(0);
            Log.v(TAG, "click 添加完成");
            doSleep(1000);
            finishNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }


    //好友详细资料页
    private void contactInfo() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo != null) {

            doSleep(1000);

            // 详情页用户昵称
            AccessibilityNodeInfo nameNode = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/sm").get(0);

            Log.i(TAG, "nameNode.toString():" + nameNode.toString());

            if (nameList.contains(nameNode.getText().toString().trim())) {

                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            }
        }
    }


    //搜索群聊，并打开
    private void searchGroup() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        doSleep(1000);

        if (nodeInfo != null) {
            // 群聊列表，android.widget.TextView
            List<AccessibilityNodeInfo> nodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ad9");
            for (AccessibilityNodeInfo info : nodes) {

                if (Config.GroupName.equals(info.getText().toString())) {
                    Log.v(TAG, "click 搜索群聊并打开");
                    info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    break;
                }
            }
        }
    }


    //打开群聊设置页面
    private void openGroupSetting() {
        if (nameList.size() > 0) {
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

            if (nodeInfo != null) {
                // 群聊设置按钮 android.widget.ImageButton
                nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/iw").get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.v(TAG, "click 设置");
            } else {
                Log.e(TAG, "nodeInfo is null");
            }
        }
    }


    //添加用户到群聊里
    private void addToGroup() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo == null) {

            Log.e(TAG, "nodeInfo is null");
            return;
        }

        // android.widget.ListView
        List<AccessibilityNodeInfo> listNodes = nodeInfo.findAccessibilityNodeInfosByViewId("android:id/list");
        if (listNodes == null || listNodes.size() == 0) {

            Log.e(TAG, "listNodes is null");
            return;
        }

        AccessibilityNodeInfo listNode = listNodes.get(0);
        // +号按钮，android.widget.ImageView
        List<AccessibilityNodeInfo> plusNodes = listNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/d92");
        while (plusNodes == null || plusNodes.size() == 0) {
            listNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            doSleep(1000);
            plusNodes = listNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/d92");
        }

        for (AccessibilityNodeInfo info : plusNodes) {
            if ("添加成员".equals(info.getContentDescription().toString())) {
                info.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.v(TAG, "click ➕");
                break;
            }
        }

        doSleep(2000);

        AccessibilityNodeInfo searchNode = getRootInActiveWindow();

        // 搜索框，android.widget.EditText
        List<AccessibilityNodeInfo> editNodes = searchNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/cmy");
        if (editNodes != null && editNodes.size() > 0) {

            AccessibilityNodeInfo editNode = editNodes.get(0);

            Log.v(TAG, "搜索：" + nameList.get(0));

            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, nameList.get(0));
            editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            nameList.remove(0);
        } else {
            Log.e(TAG, "editNodes is null");
            return;
        }

        // 没有找到"雄安客栈"相关结果

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                // 选中按钮，android.widget.CheckBox
                List<AccessibilityNodeInfo> cbNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/v8");
                if (cbNodes != null) {

                    AccessibilityNodeInfo cbNode = null;

                    if (cbNodes.size() >= 1) {
                        cbNode = cbNodes.get(0);
                    }

                    if (cbNode != null) {

                        cbNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.v(TAG, "click 添加确定");

                        // 确认按钮，android.widget.TextView
                        AccessibilityNodeInfo sureNode = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/iv").get(0);
                        sureNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            }
        }, 1000L);

    }


    //对话框处理
    private void dialogClick() {

        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        AccessibilityNodeInfo inviteNode = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/all").get(0);
        inviteNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AccessibilityNodeInfo rootNodeInfo = getRootInActiveWindow();

                List<AccessibilityNodeInfo> sureNodes = rootNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/all");

                if (sureNodes != null && sureNodes.size() > 0) {

                    AccessibilityNodeInfo sureNode = sureNodes.get(0);
                    sureNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }, 1000L);
    }

    /**
     * nearby ---start
     *
     * @param delaytime
     * @param text
     */

//延迟点击按钮，打开界面
    public void openDelay(final int delaytime, final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                doSleep(delaytime);
                clickButtonByText(text);
            }
        }).start();
    }


    private void doSleep(long millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 点击匹配的nodeInfo
     *
     * @param str text关键字
     */
    public void clickButtonByText(String str) {

        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            Log.d(TAG, "查找按钮，clickButtonByText:" + str);

            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(str);

            if (list != null && list.size() > 0) {

                Log.d(TAG, "按钮个数，clickButtonByText, list.size:" + list.size());

                AccessibilityNodeInfo node = list.get(list.size() - 1);

                Log.d(TAG, "按钮上文字，nodeText:" + node.getText().toString());

                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.d(TAG, "点击按钮:" + node.getText().toString());

                AccessibilityNodeInfo nodeParent = node.getParent();

                if (nodeParent != null) {
                    Log.d(TAG, "点击按钮父节点:" + node.getText().toString());

                    nodeParent.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                } else {
                    Log.e(TAG, "node parent is null");
                }

            } else {
                Log.v(TAG, "clickButtonByText, 找不到有效的节点");
            }

        }
    }


    /**
     * 打招呼点击发送按钮
     */
    public void clickSendHelloMsg(String str) {


        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            Toast.makeText(this, "rootWindow为空", Toast.LENGTH_SHORT).show();
            return;
        }

        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(str);

        if (list != null && list.size() > 0) {

            // 测试则不发送信息
            if (Config.DEBUG) {

                Log.d(TAG, "点击三次返回键");

                doSleep(1000);

                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

                doSleep(1000);

                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

                doSleep(1000);

                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

            } else {

                Log.d(TAG, "打招呼发送按钮，clickSendHelloMsg str=" + str);
                // 点击发送按钮
                AccessibilityNodeInfo sendNode = list.get(list.size() - 1);

                sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (sendNode.getParent() != null) {
                    sendNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        } else {
            Toast.makeText(this, "clickSendHelloMsg, 找不到有效的节点", Toast.LENGTH_SHORT).show();
        }

    }


    //自动输入打招呼内容
    public void autoInputHelloMsg(String msg) {

        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        //找到当前获取焦点的view
        AccessibilityNodeInfo target = nodeInfo.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (target == null) {
            Log.d(TAG, "autoInputHelloMsg: null");
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", msg);
        clipboard.setPrimaryClip(clip);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            target.performAction(AccessibilityNodeInfo.ACTION_PASTE);
        }

    }

    private boolean scrollUserList(AccessibilityNodeInfo nodeInfo) {

        AccessibilityNodeInfo listViewNode = null;

        //本页已全部打招呼，所以下滑列表加载下一页，每次下滑的距离是一屏
        AccessibilityNodeInfo contacts = nodeInfo.getChild(0);

        final int listsize = contacts.getChildCount();
        for (int i = 0; i < listsize; i++) {
            AccessibilityNodeInfo child = contacts.getChild(i);
            if (child.getClassName().equals("android.widget.ListView")) {
                listViewNode = child;
            }

        }

        if (listViewNode != null) {

            listViewNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);

            Log.d(TAG, "翻页, 当前page = " + page);

            doSleep(1000);

            AccessibilityNodeInfo newRootNode = getRootInActiveWindow();

            List<AccessibilityNodeInfo> listNodes = newRootNode.findAccessibilityNodeInfosByText("米以内");
            List<AccessibilityNodeInfo> mileListNodes = newRootNode.findAccessibilityNodeInfosByText("公里以内");

            selectAllUserFromList(newRootNode);

        }

        return true;
    }

    private void selectAllUserFromList(AccessibilityNodeInfo userListNode) {

        listNodeResult.clear();

        // 根据"米以内"选择附近的人列表
        List<AccessibilityNodeInfo> list = userListNode.findAccessibilityNodeInfosByText("米以内");
        for (AccessibilityNodeInfo info : list) {
            listNodeResult.add(info);
        }

        List<AccessibilityNodeInfo> mileList = userListNode.findAccessibilityNodeInfosByText("公里以内");
        for (AccessibilityNodeInfo info : mileList) {
            listNodeResult.add(info);
        }

    }

    /**
     * 把通知对应的应用调起来
     */
    private void openNotification(AccessibilityEvent event) {
        if (event.getParcelableData() == null || !(event.getParcelableData() instanceof Notification)) {
            return;
        }

        // 调起来应用
        Notification notification = (Notification) event.getParcelableData();
        PendingIntent pendingIntent = notification.contentIntent;

        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

/*****
 *
 * 发送朋友圈--start
 */

    /**
     * 写入朋友圈内容
     *
     * @param contentStr
     */
    private void inputContentFinish(final String contentStr) {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
                // 文本框，android.widget.EditText
                List<AccessibilityNodeInfo> editNodeInfo = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/dp0");

                if (editNodeInfo.size() > 0) {
                    // 粘贴内容
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("text", contentStr);
                    if (clipboard == null) {
                        return;
                    }
                    clipboard.setPrimaryClip(clip);
                    editNodeInfo.get(0).performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                    editNodeInfo.get(0).performAction(AccessibilityNodeInfo.ACTION_PASTE);
                    Log.v(TAG, "写入内容");
                }
                //点击发送 android.widget.TextView
                List<AccessibilityNodeInfo> sendMsgButton = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/iv");
                if (sendMsgButton == null || sendMsgButton.size() == 0) {
                    Log.v(TAG, "发送朋友圈失败");
                    return;
                }

                sendMsgButton.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);

                Config.HAS_POST_SNS = true;
                Log.v(TAG, "发送朋友圈成功");
                Toast.makeText(getApplicationContext(), "发送朋友圈成功", Toast.LENGTH_LONG).show();

            }
        }, 1500);
    }

    /**
     * 选择图片
     *
     * @param startPicIndex 从第startPicIndex张开始选
     * @param picCount      总共选picCount张
     */
    private void choosePicture(final int startPicIndex, final int picCount) {

        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo != null) {

            // 相册，android.widget.GridView
            List<AccessibilityNodeInfo> gridNodeInfoList = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/d_t");

            if (gridNodeInfoList != null && gridNodeInfoList.size() > 0) {

                Log.v(TAG, "gridNodeInfo:" + gridNodeInfoList.size());

                AccessibilityNodeInfo gradeInfo = gridNodeInfoList.get(0);

                for (int j = startPicIndex; j < startPicIndex + picCount; j++) {

                    AccessibilityNodeInfo childNodeInfo = gradeInfo.getChild(j);

                    if (childNodeInfo != null) {
                        // 选中按钮，android.widget.CheckBox
                        List<AccessibilityNodeInfo> childNodeInfoList = childNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ba2");
                        if (childNodeInfoList != null && childNodeInfoList.size() > 0) {

                            Log.v(TAG, "childNodeInfoList:" + childNodeInfoList.size() + "," + childNodeInfoList.get(0).toString() + "," + childNodeInfoList.get(0).getClassName());

                            childNodeInfoList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
//                            childNodeInfoList.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION);

                            doSleep(1000);
                        } else {
                            Log.v(TAG, "childNodeInfoList is null");

                        }
                    } else {
                        Log.v(TAG, "childNodeInfo is null");

                    }
                }
            } else {
                Log.v(TAG, "gridNodeInfo is null");
            }

            doSleep(1000);

            // 点击确定，android.widget.TextView
            List<AccessibilityNodeInfo> finishList = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/iv");
            if (finishList != null && finishList.size() != 0) {

                finishList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.v(TAG, "完成图片选择!");
            }
        } else {
            Log.v(TAG, "nodeInfo is null");
        }

    }

    /**
     * 点击发朋友圈按钮
     */
    private void clickCircleOfFriendsBtn() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo == null) {
            return;
        }

        doSleep(1000);

        // 发朋友圈按钮，android.widget.ImageButton
        List<AccessibilityNodeInfo> makeBtn = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/iw");
        if (makeBtn != null && makeBtn.size() != 0 && makeBtn.get(0) != null) {
            makeBtn.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

        doSleep(1000);

        // 重新抓取视图
        nodeInfo = getRootInActiveWindow();

        // 相册 android.widget.TextView
        List<AccessibilityNodeInfo> albumBtn = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ci");
        if (albumBtn != null && albumBtn.size() > 1) {

            Log.v(TAG, "albumBtn:" + albumBtn.size());

            // 第二个从相册获取
            albumBtn.get(1).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.v(TAG, "打开相册!");

        } else {
            Log.v(TAG, "albumBtn is null");

        }
    }

/**
 * 自动回复---start
 */

    /**
     * @param event
     */
    public void processAutoRepay(final AccessibilityEvent event) {

        if ("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {

            boolean isFillEditText = false;
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();

            if (Config.toProcessEvent) {
                getSnsMsg(rootNode);
            }

//            if (rootNode != null) {
//                isFillEditText = findEditText(rootNode, "Auto Reply");
//            }
//            if (isFillEditText) {
//                clickSendBtn();
//            }

        }

    }

    /**
     * 寻找窗体中的“发送”按钮，并且点击。
     */
    private void clickSendBtn() {

        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo
                    .findAccessibilityNodeInfosByText("发送");
            if (list != null && list.size() > 0) {
                for (AccessibilityNodeInfo n : list) {
                    if (n.getClassName().equals("android.widget.Button") && n.isEnabled()) {
                        Log.v(TAG, "click sendMsg button");
                        if (!Config.DEBUG) {
                            n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }

            } else {
                List<AccessibilityNodeInfo> liste = nodeInfo
                        .findAccessibilityNodeInfosByText("Send");

                if (liste != null && liste.size() > 0) {

                    for (AccessibilityNodeInfo n : liste) {
                        if (n.getClassName().equals("android.widget.Button") && n.isEnabled()) {
                            Log.v(TAG, "click sendMsg button");
                            if (!Config.DEBUG) {
                                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            }
                        }
                    }
                }
            }

            Log.v(TAG, "click backButton");
            doSleep(1000);
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

            // 测试环境，多退出一次
            if (Config.DEBUG) {
                doSleep(1000);
                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            }

        } else {
            Log.e(TAG, "nodeInfo is null");
        }
    }

    /**
     * 拉起微信界面
     *
     * @param event event
     */
    private void notifyWechat(AccessibilityEvent event) {
        if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {

            Notification notification = (Notification) event.getParcelableData();

            PendingIntent pendingIntent = notification.contentIntent;
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }


    private boolean findEditText(AccessibilityNodeInfo rootNode, String content) {
        int count = rootNode.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
            if (nodeInfo == null) {
                continue;
            }
//            if (nodeInfo.getContentDescription() != null) {
//                int nindex = nodeInfo.getContentDescription().toString().indexOf(name);
//                int cindex = nodeInfo.getContentDescription().toString().indexOf(scontent);
//                if (nindex != -1) {
//                    itemNodeinfo = nodeInfo;
//                }
//            }
            if ("android.widget.EditText".equals(nodeInfo.getClassName())) {
                Bundle arguments = new Bundle();

                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);

                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, true);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);

                ClipData clip = ClipData.newPlainText("label", content);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);

                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);

                return true;
            }
            if (findEditText(nodeInfo, content)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析消息内容
     *
     * @param rootNode
     */
    private void getSnsMsg(AccessibilityNodeInfo rootNode) {

        List<AccessibilityNodeInfo> msgNodeList = rootNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a4");

        if (msgNodeList == null || msgNodeList.size() == 0) {
            Log.e(TAG, "msgNodeList is null");
            return;
        }
        Log.e(TAG, "msgNodeList.size():" + msgNodeList.size());

        for (int i = msgNodeList.size() - 1; i >= 0; i--) {
            AccessibilityNodeInfo msgNode = msgNodeList.get(i);

            List<AccessibilityNodeInfo> timeNodeList = msgNode.getParent().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a_");

            if (timeNodeList != null & timeNodeList.size() > 0) {
                String timeStr = timeNodeList.get(0).getText().toString();
                Log.v(TAG, "timeStr:" + timeStr);
            }

            List<AccessibilityNodeInfo> nickNameNodeList = msgNode.getParent().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ly");
            if (nickNameNodeList != null && nickNameNodeList.size() > 0) {
                String nickName = nickNameNodeList.get(0).getText().toString();
                Log.v(TAG, "nickName:" + nickName);
            }

            if (MSG_TYPE == 1) {
                final List<AccessibilityNodeInfo> imgNodeList = msgNode.getParent().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ah3");
                if (imgNodeList != null && imgNodeList.size() > 0) {
                    Log.v(TAG, "imgNodeList.size():" + imgNodeList.size());

                    AccessibilityNodeInfo imgNode = imgNodeList.get(0);

                    if (imgNode.isClickable()) {
                        Log.v(TAG, "click image");
                        imgNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                        doSleep(1000);


                        AccessibilityNodeInfo imgFileNodeInfo = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> imgFileNodeList = imgFileNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bxb");
                        if (imgFileNodeList != null && imgFileNodeList.size() > 0) {
                            Log.v(TAG, "click 下载按钮");

                            imgFileNodeList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            doSleep(2000);

                            Config.toProcessEvent = false;
                        }
                    } else {
                        Log.e(TAG, "imgNode not isClickable");
                    }

                }

                break;

            } else if (MSG_TYPE == 2) {
                // 文章分享，可以点击\

                final List<AccessibilityNodeInfo> articleNodeList = msgNode.getParent().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ah3");

                if (articleNodeList != null && articleNodeList.size() > 0) {
                    Log.v(TAG, "articleNodeList.size():" + articleNodeList.size());

                    AccessibilityNodeInfo articleNode = articleNodeList.get(0);

                    List<AccessibilityNodeInfo> titleNodeList = articleNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ahi");
                    if (titleNodeList != null && titleNodeList.size() > 0) {
                        String title = titleNodeList.get(0).getText().toString();
                        Log.v(TAG, "title:" + title);
                    }

                    List<AccessibilityNodeInfo> abstractNodeList = articleNode.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ahl");
                    if (abstractNodeList != null && abstractNodeList.size() > 0) {
                        String abst = abstractNodeList.get(0).getText().toString();
                        Log.v(TAG, "abstract:" + abst);
                    }


                    if (articleNodeList.get(0).isClickable()) {
                        Log.v(TAG, "click article");
                        articleNodeList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);

                        doSleep(15000);

                        AccessibilityNodeInfo webNodeInfo = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> webNodeList = webNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/iw");
                        if (webNodeList != null && webNodeList.size() > 0) {
                            Log.v(TAG, "click 文章详情");

                            webNodeList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            doSleep(2000);

                            AccessibilityNodeInfo linkNodeInfo = getRootInActiveWindow();
                            List<AccessibilityNodeInfo> linkNodeList = linkNodeInfo.findAccessibilityNodeInfosByText("复制链接");
                            if (linkNodeList != null && linkNodeList.size() > 0 && linkNodeList.get(0).getParent() != null && linkNodeList.get(0).getParent().isClickable()) {
                                Log.v(TAG, "click 复制链接");

                                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

                                linkNodeList.get(0).getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                doSleep(2000);

                                ClipData clip = clipboard.getPrimaryClip();

                                if (clip != null && clip.getItemCount() > 0) {
                                    for (int j = 0; j < clip.getItemCount(); j++) {
                                        String link = clip.getItemAt(j).getText().toString();
                                        Log.v(TAG, "link:" + link);
                                    }
                                }
                            } else {
                                Log.e(TAG, "linkNodeList has problems");
                            }
                        } else {
                            Log.e(TAG, "webNodeList is null");
                        }

                        Config.toProcessEvent = false;


                    } else {
                        Log.e(TAG, "articleNodeList.get(0) not isClickable");
                    }
                }

                break;

            } else if (MSG_TYPE == 3) {

                final List<AccessibilityNodeInfo> videoNodeList = msgNode.getParent().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ah3");

                if (videoNodeList != null && videoNodeList.size() > 0) {
                    Log.v(TAG, "videoNodeList.size():" + videoNodeList.size());

                    AccessibilityNodeInfo videoNode = videoNodeList.get(0);

                    if (videoNode.isClickable()) {
                        Log.v(TAG, "click videoNode");
                        videoNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                        doSleep(5000);


                        AccessibilityNodeInfo videoFileNodeInfo = getRootInActiveWindow();
                        List<AccessibilityNodeInfo> videoFileNodeList = videoFileNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/a83");
                        if (videoFileNodeList != null && videoFileNodeList.size() > 0) {
                            Log.v(TAG, "click 长按视频");

                            videoFileNodeList.get(0).performAction(AccessibilityNodeInfo.ACTION_SELECT);
                            videoFileNodeList.get(0).performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
                            doSleep(1000);

                            AccessibilityNodeInfo videoDownloadNodeInfo = getRootInActiveWindow();
                            List<AccessibilityNodeInfo> videoDownloadNodeList = videoDownloadNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/jm");
                            if (videoDownloadNodeList != null && videoDownloadNodeList.size() > 0) {
                                Log.v(TAG, "click 保存视频");

                                videoDownloadNodeList.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                doSleep(1000);
                            }

                        }

                        Config.toProcessEvent = false;
                    } else {
                        Log.e(TAG, "videoNode not isClickable");
                    }

                }

                break;
            } else if (MSG_TYPE == 5) {

                final List<AccessibilityNodeInfo> publicNodeList = msgNode.getParent().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ah3");

                if (publicNodeList != null && publicNodeList.size() > 0) {
                    Log.v(TAG, "publicNodeList.size():" + publicNodeList.size());

                    List<AccessibilityNodeInfo> List = msgNode.getParent().findAccessibilityNodeInfosByViewId("com.tencent.mm:id/aij");

                    if (List != null && List.size() > 0) {

                        String publicName = List.get(0).getText().toString();
                        Log.v(TAG, "publicName:" + publicName);

                        Config.toProcessEvent = false;
                    } else {
                        Log.e(TAG, "List is null");
                    }

                }

                break;
            }

        }


        Log.v(TAG, "click backButton");
        doSleep(1000);
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);

        // 测试环境，多退出一次
        if (Config.DEBUG) {
            doSleep(1000);
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        }


    }

    /**
     * 点开朋友圈
     */
    private void openCircleOfFriends() {

        doSleep(1000);
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo != null) {

            // 底部导航按钮，android.widget.LinearLayout
            List<AccessibilityNodeInfo> tabNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/chp");

            if (tabNodes != null && tabNodes.size() > 0) {
                for (AccessibilityNodeInfo tabNode : tabNodes) {

                    Log.v(TAG, "tabNode.getText().toString():" + tabNode.getText().toString());

                    if ("发现".equals(tabNode.getText().toString())) {

                        tabNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.v(TAG, "click 发现");

                        break;
                    }
                }
            } else {
                Log.v(TAG, "tabNodes is null");
            }
        }

        doSleep(1000);

        // 列表android.widget.ListView
        List<AccessibilityNodeInfo> nodeInfoList = nodeInfo.findAccessibilityNodeInfosByViewId("android:id/list");

        if (nodeInfoList != null && nodeInfoList.size() != 0) {

            // 获取列表
            AccessibilityNodeInfo subNodeInfo = nodeInfoList.get(0);

            if (subNodeInfo != null) {

                // 朋友圈按钮，android.widget.LinearLayout
                AccessibilityNodeInfo snsNodeInfo = subNodeInfo.getChild(1);

                if (snsNodeInfo != null) {
                    snsNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.v(TAG, "click 朋友圈");

                }
            }
        }
    }


    /**
     * 点开附近的人
     */
    private void openNearBy() {

        doSleep(1000);
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo != null) {

            // 底部导航按钮，android.widget.LinearLayout
            List<AccessibilityNodeInfo> tabNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/chp");

            if (tabNodes != null && tabNodes.size() > 0) {
                for (AccessibilityNodeInfo tabNode : tabNodes) {

                    Log.v(TAG, "tabNode.getText().toString():" + tabNode.getText().toString());

                    if ("发现".equals(tabNode.getText().toString())) {

                        tabNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        Log.v(TAG, "click 发现");
                        break;
                    }
                }
            } else {
                Log.v(TAG, "tabNodes is null");
            }
        }

        doSleep(1000);

        // 发现里面的列表，android.widget.ListView
        nodeInfo = getRootInActiveWindow();
        List<AccessibilityNodeInfo> nodeInfoList = nodeInfo.findAccessibilityNodeInfosByViewId("android:id/list");

        if (nodeInfoList != null && nodeInfoList.size() != 0) {

            // 获取列表
            AccessibilityNodeInfo subNodeInfo = nodeInfoList.get(0);

            if (subNodeInfo != null) {
                // 附近的人按钮，android.widget.LinearLayout
                AccessibilityNodeInfo snsNodeInfo = subNodeInfo.getChild(9);

                if (snsNodeInfo != null) {
                    snsNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                    Log.v(TAG, "click 附近的人");
                }
            }
        }
    }

    /**
     * 点开通讯录-->新朋友
     */
    private void openNewFriend() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo != null) {

            // 底部导航按钮，android.widget.LinearLayout
            List<AccessibilityNodeInfo> tabNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/chp");

            for (AccessibilityNodeInfo tabNode : tabNodes) {

                if (tabNode.getText() != null && "通讯录".equals(tabNode.getText().toString())) {

                    tabNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.v(TAG, "click 通讯录");

                    break;
                }
            }


            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    AccessibilityNodeInfo newNodeInfo = getRootInActiveWindow();

                    if (newNodeInfo != null) {

                        // 按钮，android.widget.LinearLayout
                        List<AccessibilityNodeInfo> tagNodes = newNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bec");

                        boolean found = false;
                        for (AccessibilityNodeInfo tagNode : tagNodes) {

                            for (int i = 0; i < tagNode.getChildCount(); i++) {
                                List<AccessibilityNodeInfo> allFriendNodeList = new ArrayList<>();
                                // 以前申请的
                                List<AccessibilityNodeInfo> oldFriendNodeList = tagNode.getChild(i).findAccessibilityNodeInfosByText("新的朋友");
                                allFriendNodeList.addAll(oldFriendNodeList);

                                // 新申请的
                                List<AccessibilityNodeInfo> newFriendNodeList = tagNode.getChild(i).findAccessibilityNodeInfosByText("对方请求添加你为朋友");
                                allFriendNodeList.addAll(newFriendNodeList);

                                // 红色按钮
                                List<AccessibilityNodeInfo> redNode = newNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bee");
                                allFriendNodeList.addAll(redNode);

                                Log.v(TAG, "allFriendNodeList.size():" + allFriendNodeList.size());

                                if (allFriendNodeList != null && allFriendNodeList.size() > 0) {

                                    AccessibilityNodeInfo clickableNode = allFriendNodeList.get(0).getParent();
                                    while (!clickableNode.isClickable()) {
                                        clickableNode = clickableNode.getParent();
                                    }

                                    clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    Log.v(TAG, "click 新的朋友");
                                    found = true;
                                    break;
                                }

                            }
                            if (found) {
                                break;
                            }
                        }
                    } else {
                        Log.e(TAG, "newNodeInfo is null");
                    }
                }
            }, 5000);
        }
    }

    //打开群聊搜索页面
    private void openGroup() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo != null) {

            // 底部导航按钮，android.widget.LinearLayout
            List<AccessibilityNodeInfo> tabNodes = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/chp");

            for (AccessibilityNodeInfo tabNode : tabNodes) {

                if (tabNode.getText() != null && "通讯录".equals(tabNode.getText().toString())) {

                    tabNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.v(TAG, "click 通讯录");


                    break;
                }
            }

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    AccessibilityNodeInfo newNodeInfo = getRootInActiveWindow();

                    if (newNodeInfo != null) {

                        // 群聊按钮，android.widget.LinearLayout
                        List<AccessibilityNodeInfo> tagNodes = newNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/lh");

                        for (AccessibilityNodeInfo tagNode : tagNodes) {

                            if (tagNode.getText() != null && "群聊".equals(tagNode.getText().toString())) {

                                AccessibilityNodeInfo clickableNode = tagNode.getParent();
                                while (!clickableNode.isClickable()) {
                                    clickableNode = clickableNode.getParent();
                                }

                                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                Log.v(TAG, "click 群聊");
                                break;
                            }
                        }
                    } else {
                        Log.e(TAG, "newNodeInfo is null");
                    }
                }
            }, 5000);
        }
    }


    @Override
    public void onInterrupt() {

        Log.e(TAG, "服务已中断");
        Toast.makeText(this, "服务已中断", Toast.LENGTH_SHORT).show();

        mTts.shutdown();

    }

    @Override
    protected void onServiceConnected() {

        super.onServiceConnected();

        Log.e(TAG, "服务已开启");

        Toast.makeText(this, "服务已开启", Toast.LENGTH_SHORT).show();

        mTts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {

                    mTts.setLanguage(Locale.CHINESE);
                }
            }
        });

    }

    @Override
    public boolean onUnbind(Intent intent) {

        TasksWindow.dismiss();

        return super.onUnbind(intent);

    }

}
