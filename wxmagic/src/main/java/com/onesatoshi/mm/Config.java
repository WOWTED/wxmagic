package com.onesatoshi.mm;

import java.util.Arrays;
import java.util.List;

/**
 * 保存全局变量
 */
public class Config {

    /**
     * 测试环境，不真正添加附近的人
     */
    public static final boolean DEBUG = true;

    /**
     * 微信包名
     */
    public final static String WX_PACKAGE_NAME = "com.tencent.mm";

    /**
     * 是否打开自动抢红包
     */
    public static boolean isOpenAutoOpenLuckyMoney = false;

    /**
     * 是否打开自动添加附近的人
     */
    public static boolean isOpenAutoNearBy = false;

    /**
     * 是否自动点赞
     */
    public static boolean isOpenAutoPrize = false;

    /**
     * 自动添加群好友
     */
    public static boolean isOpenAddFriendFromGroup = false;

    /**
     * 自动发布朋友圈
     */
    public static boolean isIsOpenAutoSns = false;

    /**
     * 是否打开自动回复
     */
    public static boolean isOpenAutoReply = false;

    /**
     * 是否打开自动抓取群信息
     */
    public static boolean isOpenAutoCrawler = false;

    /**
     * 正在运行中
     */
    public static boolean isRunning = false;

    /**
     * 群名称
     */
    public static final String GroupName = "币交所探密";

    /**
     * 打招呼内容
     */
    public static final String HELLO_MSG = "你好，可以认识下吗？";

    /**
     * 自动回复文本
     */
    public static final String AutoReplyText = "我现在没空, 稍后回复哈~";

    /**
     * 选择ID
     */
    public static int SelectId = 0;

    /**
     * 朋友圈内容
     */
    public static final String SNS_CONTENT = "我是朋友圈机器人";

    /**
     * 朋友圈相册图片索引
     */
    public static final int ALBUM_INDEX = 0;

    /**
     * 朋友圈相册图片个数
     */
    public static final int ALBUM_COUNT = 1;

    /**
     * 用于存储微信开红包按钮使用过的id，微信几乎每次版本更新都会修改此button的id
     */
    public static final List<String> openIds = Arrays.asList("bjj", "bi3", "brt");

    /**
     * 储存微信开红包activity使用过的className，最新的微信版本更新有修改过此activity的名称
     */
    public static final List<String> luckyActivityNames = Arrays.asList("com.tencent.mm.plugin.luckymoney.ui.En_fba4b94f",
            "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI");

    /**
     * 抓取新闻的网址
     */
    public static final String NEWS_URL = "http://45.77.248.237:9999/api/top10news.do";

    /**
     * 上次执行时间
     */
    public static long lastUpdate = 0;
}