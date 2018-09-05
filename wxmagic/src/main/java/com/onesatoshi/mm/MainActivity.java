package com.onesatoshi.mm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private CheckBox cb_assist;
    private CheckBox cb_window;

    private RadioButton cb_lucky_money;
    private RadioButton cb_people_nearby;
    private RadioButton cb_auto_prize;
    private RadioButton cb_add_friend_group;
    private RadioButton auto_sns;
    private RadioButton auto_repay;

    public static final String TAG = MainActivity.class.getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 辅助功能事件
        cb_assist = (CheckBox) findViewById(R.id.cb_assist_permission);
        if (cb_assist != null) {
            cb_assist.setOnCheckedChangeListener(this);
        }

        //
        cb_window = (CheckBox) findViewById(R.id.cb_show_window);
        if (cb_window != null) {
            cb_window.setOnCheckedChangeListener(this);
        }

        // 红包事件
        cb_lucky_money = (RadioButton) findViewById(R.id.cb_lucky_money);
        if (cb_lucky_money != null) {
            cb_lucky_money.setOnCheckedChangeListener(this);
        }

        // 附近的人事件
        cb_people_nearby = (RadioButton) findViewById(R.id.cb_people_nearby);
        if (cb_people_nearby != null) {
            cb_people_nearby.setOnCheckedChangeListener(this);
        }

        // 自动点赞
        cb_auto_prize = (RadioButton) findViewById(R.id.cb_auto_prize);
        if (cb_auto_prize != null) {
            cb_auto_prize.setOnCheckedChangeListener(this);
        }

        // 添加群好友
        cb_add_friend_group = (RadioButton) findViewById(R.id.cb_add_friend_group);
        if (cb_add_friend_group != null) {
            cb_add_friend_group.setOnCheckedChangeListener(this);
        }

        // 自动发朋友圈
        auto_sns = (RadioButton) findViewById(R.id.cb_auto_sns);
        if (auto_sns != null) {
            auto_sns.setOnCheckedChangeListener(this);
        }

        auto_repay = (RadioButton) findViewById(R.id.cb_auto_repay);
        if (auto_repay != null) {
            auto_repay.setOnCheckedChangeListener(this);
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

//        cb_assist.setChecked(isAccessibilitySettingsOn());
//        cb_window.setChecked(canShowWindow(this));

        if (canShowWindow(this)) {
            requestFloatWindowPermissionIfNeeded();
        }
    }

    /**
     * 申请辅助功能权限
     */
    private void requestAssistPermission() {
        try {

            //打开系统设置中辅助功能
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);

            startActivity(intent);

            Context ctx = this.getApplicationContext();
            PackageManager packManager = ctx.getPackageManager();
            ApplicationInfo appInfo = ctx.getApplicationInfo();
            String appName = (String) packManager.getApplicationLabel(appInfo);

            Toast.makeText(MainActivity.this, "找到" + appName + "，然后开启服务即可", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 申请悬浮窗权限
     */
    private void requestFloatWindowPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {

            new AlertDialog.Builder(this)
                    .setMessage(R.string.dialog_enable_overlay_window_msg)
                    .setPositiveButton(R.string.dialog_enable_overlay_window_positive_btn
                            , new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    dialog.dismiss();

                                    // 打开权限设置窗口
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                                    intent.setData(Uri.parse("package:" + getPackageName()));

                                    startActivity(intent);
                                }
                            })

                    .setNegativeButton(android.R.string.cancel
                            , new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    setShowWindow(MainActivity.this, false);
                                    cb_window.setChecked(false);
                                }
                            })

                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            setShowWindow(MainActivity.this, false);
                            cb_window.setChecked(false);

                        }
                    })
                    .create()
                    .show();

        }
    }

/*    private MoveTextView floatBtn1;
    private MoveTextView floatBtn2;
    private WindowManager wm;

    //创建悬浮按钮
    private void createFloatView() {
        WindowManager.LayoutParams pl = new WindowManager.LayoutParams();
        wm = (WindowManager) getSystemService(getApplication().WINDOW_SERVICE);
        pl.type = WindowManager.LayoutParams.TYPE_TOAST;//修改为此TYPE_TOAST，可以不用申请悬浮窗权限就能创建悬浮窗,但在部分手机上会崩溃
        pl.format = PixelFormat.RGBA_8888;
        pl.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        pl.gravity = Gravity.END | Gravity.BOTTOM;
        pl.x = 0;
        pl.y = 0;

        pl.width = WindowManager.LayoutParams.WRAP_CONTENT;
        pl.height = WindowManager.LayoutParams.WRAP_CONTENT;

        LayoutInflater inflater = LayoutInflater.from(this);
        floatBtn1 = (MoveTextView) inflater.inflate(R.layout.floatbtn, null);
        floatBtn1.setText("打招呼");
        floatBtn2 = (MoveTextView) inflater.inflate(R.layout.floatbtn, null);
        floatBtn2.setText("抢红包");
        wm.addView(floatBtn1, pl);
        pl.gravity = Gravity.BOTTOM | Gravity.START;
        wm.addView(floatBtn2, pl);

        floatBtn1.setOnClickListener(this);
        floatBtn2.setOnClickListener(this);
        floatBtn1.setWm(wm, pl);
        floatBtn2.setWm(wm, pl);
    }*/

    /**
     * 检测辅助功能是否开启
     */
    private boolean isAccessibilitySettingsOn() {
        int accessibilityEnabled = 0;

        String service = getPackageName() + "/" + AutoService.class.getCanonicalName();

        try {

            accessibilityEnabled = Settings.Secure.getInt(getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);

        } catch (Settings.SettingNotFoundException e) {

            Log.d(TAG, "Error finding setting, default accessibility to not found: " + e.getMessage());
        }

        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {

            String settingValue = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            if (settingValue != null) {

                mStringColonSplitter.setString(settingValue);

                while (mStringColonSplitter.hasNext()) {

                    String accessibilityService = mStringColonSplitter.next();

                    Log.d(TAG, "-------------- > accessibilityService :: " + accessibilityService + " " + service);
                    if (accessibilityService.equalsIgnoreCase(service)) {

                        Log.d(TAG, "We've found the correct setting - accessibility is switched on!");
                        return true;
                    }

                }
            }
        } else {

            Log.e(TAG, "***ACCESSIBILITY IS DISABLED***");

        }

        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        switch (buttonView.getId()) {

            /**
             * 申请权限
             */
            case R.id.cb_assist_permission:

                if (isChecked && !isAccessibilitySettingsOn()) {
                    requestAssistPermission();
                }

                break;

            /**
             * 展示悬浮框
             */
            case R.id.cb_show_window:

                setShowWindow(this, isChecked);

                if (isChecked) {
                    requestFloatWindowPermissionIfNeeded();
                }

                if (!isChecked) {
                    TasksWindow.dismiss();
                } else {
                    TasksWindow.show(this, getPackageName() + "\n" + getClass().getName());
//                    TasksWindow.changeMsg(getPackageName() + "\n" + getClass().getName());
                }

                break;

            /**
             * 抢红包
             */
            case R.id.cb_lucky_money:
                if (isChecked) {
                    if (isAccessibilitySettingsOn()) {

                        // 开启红包服务
                        Config.isOpenAutoOpenLuckyMoney = true;
                        Log.d(TAG, "Config.isOpenAutoOpenLuckyMoney=" + Config.isOpenAutoOpenLuckyMoney);

                    } else {
                        Toast.makeText(MainActivity.this, "辅助功能未开启", Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }
                } else {
                    Config.isOpenAutoOpenLuckyMoney = false;
                    Log.d(TAG, "Config.isOpenAutoOpenLuckyMoney=" + Config.isOpenAutoOpenLuckyMoney);
                }

                if (isChecked) {
//                cb_lucky_money.setChecked(false);
                    cb_people_nearby.setChecked(false);
                    cb_auto_prize.setChecked(false);
                    cb_add_friend_group.setChecked(false);
                    auto_sns.setChecked(false);
                    auto_repay.setChecked(false);
                }
                break;

            /**
             * 附近的人
             */
            case R.id.cb_people_nearby:

                if (isChecked) {

                    if (isAccessibilitySettingsOn()) {

                        Config.isOpenAutoNearBy = true;

                        Log.d(TAG, "Config.isOpenAutoNearBy=" + Config.isOpenAutoNearBy);
                    } else {
                        Toast.makeText(MainActivity.this, "辅助功能未开启", Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);

                    }

                } else {

                    Config.isOpenAutoNearBy = false;
                    Log.d(TAG, "Config.isOpenAutoNearBy=" + Config.isOpenAutoNearBy);
                }
                if (isChecked) {

                    cb_lucky_money.setChecked(false);
//                cb_people_nearby.setChecked(false);
                    cb_auto_prize.setChecked(false);
                    cb_add_friend_group.setChecked(false);
                    auto_sns.setChecked(false);
                    auto_repay.setChecked(false);
                }

                break;

            /**
             * 自动点赞
             */
            case R.id.cb_auto_prize:

                if (isChecked) {

                    if (isAccessibilitySettingsOn()) {

                        Config.isOpenAutoPrize = true;

                        Log.d(TAG, "Config.isOpenAutoPrize=" + Config.isOpenAutoPrize);
                    } else {

                        Toast.makeText(MainActivity.this, "辅助功能未开启", Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }

                } else {

                    Config.isOpenAutoPrize = false;
                    Log.d(TAG, "Config.isOpenAutoPrize=" + Config.isOpenAutoPrize);

                }

                if (isChecked) {

                    cb_lucky_money.setChecked(false);
                    cb_people_nearby.setChecked(false);
//                cb_auto_prize.setChecked(false);
                    cb_add_friend_group.setChecked(false);
                    auto_sns.setChecked(false);
                    auto_repay.setChecked(false);
                }

                break;


            /**
             * 添加群好友
             */
            case R.id.cb_add_friend_group:

                if (isChecked) {

                    if (isAccessibilitySettingsOn()) {

                        Config.isOpenAddFriendFromGroup = true;

                        Log.d(TAG, "Config.isOpenAddFriendFromGroup=" + Config.isOpenAddFriendFromGroup);
                    } else {
                        Toast.makeText(MainActivity.this, "辅助功能未开启", Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }

                } else {
                    Config.isOpenAddFriendFromGroup = false;
                    Log.d(TAG, "Config.isOpenAddFriendFromGroup=" + Config.isOpenAddFriendFromGroup);
                }

                if (isChecked) {

                    cb_lucky_money.setChecked(false);
                    cb_people_nearby.setChecked(false);
                    cb_auto_prize.setChecked(false);
//                cb_add_friend_group.setChecked(false);
                    auto_sns.setChecked(false);
                    auto_repay.setChecked(false);
                }

                break;

            /**
             * 自动发朋友圈
             */
            case R.id.cb_auto_sns:

                if (isChecked) {

                    if (isAccessibilitySettingsOn()) {

                        Config.isIsOpenAutoSns = true;

                        Log.d(TAG, "Config.isIsOpenAutoSns=" + Config.isIsOpenAutoSns);
                    } else {

                        Toast.makeText(MainActivity.this, "辅助功能未开启", Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }

                } else {
                    Config.isIsOpenAutoSns = false;
                    Log.d(TAG, "Config.isIsOpenAutoSns=" + Config.isIsOpenAutoSns);
                }

                if (isChecked) {

                    cb_lucky_money.setChecked(false);
                    cb_people_nearby.setChecked(false);
                    cb_auto_prize.setChecked(false);
                    cb_add_friend_group.setChecked(false);
//                auto_sns.setChecked(false);
                    auto_repay.setChecked(false);
                }

                break;

            /**
             * 自动回复
             */
            case R.id.cb_auto_repay:

                if (isChecked) {

                    if (isAccessibilitySettingsOn()) {

                        Config.isOpenAutoReply = true;

                        Log.d(TAG, "Config.isOpenAutoReply=" + Config.isOpenAutoReply);
                    } else {

                        Toast.makeText(MainActivity.this, "辅助功能未开启", Toast.LENGTH_SHORT).show();
                        buttonView.setChecked(false);
                    }

                } else {
                    Config.isOpenAutoReply = false;
                    Log.d(TAG, "Config.isOpenAutoReply=" + Config.isOpenAutoReply);
                }

                if (isChecked) {

                    cb_lucky_money.setChecked(false);
                    cb_people_nearby.setChecked(false);
                    cb_auto_prize.setChecked(false);
                    cb_add_friend_group.setChecked(false);
                    auto_sns.setChecked(false);
//                    auto_repay.setChecked(false);
                }

                break;
        }

    }

    public static boolean canShowWindow(Context context) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean("show_window", true);

    }

    public static void setShowWindow(Context context, boolean isShow) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean("show_window", isShow).apply();

    }
}
