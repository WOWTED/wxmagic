

````

        String str_eventType;
        switch (eventType) {
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                Log.i(TAG, "==============Start====================");
                str_eventType = "TYPE_VIEW_CLICKED";
                AccessibilityNodeInfo noteInfo = event.getSource();
                Log.i(TAG, noteInfo.toString());
                Log.i(TAG, "=============END=====================");
                break;
            case AccessibilityEvent.TYPE_VIEW_FOCUSED:
                str_eventType = "TYPE_VIEW_FOCUSED";
                break;
            case AccessibilityEvent.TYPE_VIEW_LONG_CLICKED:
                str_eventType = "TYPE_VIEW_LONG_CLICKED";
                break;
            case AccessibilityEvent.TYPE_VIEW_SELECTED:
                str_eventType = "TYPE_VIEW_SELECTED";
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED:
                str_eventType = "TYPE_VIEW_TEXT_CHANGED";
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                str_eventType = "TYPE_WINDOW_STATE_CHANGED";
                break;
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                str_eventType = "TYPE_NOTIFICATION_STATE_CHANGED";
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END:
                str_eventType = "TYPE_TOUCH_EXPLORATION_GESTURE_END";
                break;
            case AccessibilityEvent.TYPE_ANNOUNCEMENT:
                str_eventType = "TYPE_ANNOUNCEMENT";
                break;
            case AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START:
                str_eventType = "TYPE_TOUCH_EXPLORATION_GESTURE_START";
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER:
                str_eventType = "TYPE_VIEW_HOVER_ENTER";
                break;
            case AccessibilityEvent.TYPE_VIEW_HOVER_EXIT:
                str_eventType = "TYPE_VIEW_HOVER_EXIT";
                break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                str_eventType = "TYPE_VIEW_SCROLLED";
                break;
            case AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED:
                str_eventType = "TYPE_VIEW_TEXT_SELECTION_CHANGED";
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                str_eventType = "TYPE_WINDOW_CONTENT_CHANGED";
                break;
            default:
                str_eventType = String.valueOf(eventType);
        }

        String action;

        switch (event.getAction()) {
            case AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS:
                action = "ACTION_ACCESSIBILITY_FOCUS";
                break;
            case AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS:
                action = "ACTION_CLEAR_ACCESSIBILITY_FOCUS";
                break;
            case AccessibilityNodeInfo.ACTION_CLEAR_FOCUS:
                action = "ACTION_CLEAR_FOCUS";
                break;
            case AccessibilityNodeInfo.ACTION_CLEAR_SELECTION:
                action = "ACTION_CLEAR_SELECTION";
                break;
            case AccessibilityNodeInfo.ACTION_CLICK:
                action = "ACTION_CLICK";
                break;
            case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:
                action = "ACTION_SCROLL_FORWARD";
                break;
            case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:
                action = "ACTION_SCROLL_BACKWARD";
                break;
            case AccessibilityNodeInfo.ACTION_FOCUS:
                action = "ACTION_FOCUS";
                break;
            case 0:
                //com.android.systemui 一般是系统标题栏内容发生改变
                //在微信内页但不在聊天详情页时，收到红包不会产生通知栏事件，只有系统标题栏内容发生改变
                //标题栏中已经有微信未读消息图标时：TYPE_WINDOW_CONTENT_CHANGED	package:com.android.systemui	Class:android.widget.ImageView
                //标题栏中尚未有未读消息图标时：TYPE_WINDOW_CONTENT_CHANGED	package:com.android.systemui	Class:android.widget.FrameLayout
            default:
                action = event.getAction() + "";
                break;
        }

        Log.v(TAG, "EventType: " + str_eventType + "\tAction:" + action + "\tpackage:" + event.getPackageName() + "\tClass:" + event.getClassName() + "\t");
        Log.v(TAG, "package:" + event.getPackageName() + "\tClass:" + event.getClassName() + "\t");


````