package com.yds.printwx;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.yds.printwx.common.RecordInfo;
import com.yds.printwx.common.WXUser;
import com.yds.printwx.common.utils.ChineseToEnglish;
import com.yds.printwx.common.utils.Utils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.CookieManager;

/**
 * 微信6.3.13
 */
@SuppressLint({ "NewApi", "Wakelock" })
public class AccessibilityPrintWX extends AccessibilityService {

    private final String TAG = "Accessibility";
    private Session mSession;
    private String curWXUser;
    private boolean detailEnable=false;
    private boolean scrollEnable;
    private boolean saveImgEnable;
    private boolean moreImgEnable;
    private String curTime;
    private String curText;
    private int curVIndex;
    private int curHIndex;
    private int curImgIndex;

    private String lastImgPath;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
		/*通过这个函数可以接收系统发送来的AccessibilityEvent，接收来的AccessibilityEvent是经过过滤的，过滤是在配置工作时设置的。*/
        final int eventType = event.getEventType();
        AccessibilityNodeInfo nodeInfo = event.getSource();

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (null != nodeInfo) {
                int steps=mSession.getSteps();
                //Utils.e(TAG, steps+"列表=========================>>>AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED");
                if(steps==0){
                    mSession.setSteps(++steps);
                    //nodeInfo.getChild(0).getChild(1).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    execShellCmd("input tap " + (mSession.getScreenWidth() - 150) + " 50");
                    return;
                }else if(steps==1){
                    String fName= ChineseToEnglish.getPingYin(mSession.getFriendName());
                    execShellCmd("input text "+fName);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mSession.setSteps(2);
                            execShellCmd("input tap 200 200");
                        }
                    },1500);
                    /*AccessibilityNodeInfo editNode = nodeInfo.getChild(0).getChild(2);
                    if (editNode != null && editNode.getClassName().equals("android.widget.EditText")) {
                        Utils.e(TAG, "000000000000000000000000000000000000000000000000000");
                        Bundle arguments = new Bundle();
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "caonima");
                        editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    }*/
                    return;
                }else if(steps==2){
                    mSession.setSteps(++steps);
                    execShellCmd("input tap "+(mSession.getScreenWidth()-50)+" 50");
                    return;
                }else if(steps==3){
                    mSession.setSteps(++steps);
                    execShellCmd("input tap 100 200");
                    return;
                }else if(steps==4){
                    mSession.setSteps(++steps);
                    execShellCmd("input tap 200 " + (mSession.getScreenHeight()/2-100));
                    return;
                }else{
                    //进入到了好友的朋友圈界面
                    if (checkPrintUser(nodeInfo) && !TextUtils.isEmpty(curWXUser)) {
                        //Utils.e(TAG, "列表=>>>AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED");
                        nodeInfo = nodeInfo.getChild(0).getChild(1);//列表listview
                        printWXInfo(nodeInfo);
                    } else if (detailEnable) {
                        //Log.e(TAG, "详情=>>>AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED");
                        traverseNode(nodeInfo);
                    }
                }
            }
        }
        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (null != nodeInfo) {
                if (checkPrintUser(nodeInfo) && scrollEnable) {
                    //Utils.e(TAG, "=>>>AccessibilityEvent.TYPE_VIEW_SCROLLED");
                    scrollEnable = false;
                    printWXInfo(nodeInfo);
                }
            }
        }
    }

    private boolean checkPrintUser(AccessibilityNodeInfo nodeInfo) {
        try {
            AccessibilityNodeInfo parentNode = nodeInfo.getParent();
            while (null != parentNode) {
                nodeInfo = parentNode;
                parentNode = nodeInfo.getParent();
            }

            AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(0).getChild(0);
            childNodeInfo = childNodeInfo.getChild(1);
            CharSequence text = childNodeInfo.getText();
            if (null != text && text.length() > 0) {
                String str = text.toString();
                if (mSession.getFriendName().equals(str)) {
                    curWXUser = str;
                    mSession.addWxUser(curWXUser);
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    private void printWXInfo(AccessibilityNodeInfo node) {
        curTime = "";
        curText = "";

        try {
            if (null == node) return;
            int count = node.getChildCount();
            for (int i = curVIndex; i < count; ++i) {

                AccessibilityNodeInfo itemNode = node.getChild(i).getChild(0);
                String itemClassName = itemNode.getClassName().toString();

                if (itemClassName.equals("android.widget.LinearLayout")) {
                    detailEnable = true;
                    AccessibilityNodeInfo rightNode = itemNode.getChild(1);
                    //logNodeInfo(rightNode);
                    int rightSize = rightNode.getChildCount();
                    for (int j = curHIndex; j < rightSize; ++j) {
                        if (rightNode.getChild(j).isClickable()) {
                            if (curHIndex + 1 == rightSize) {
                                curHIndex = 0;
                                curVIndex = i + 1;
                            } else {
                                curHIndex = j + 1;
                            }
                            rightNode.getChild(j).performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            return;
                        }
                    }
                    if (curHIndex > 0) {
                        curHIndex = 0;
                        curVIndex = i + 1;
                        if (curVIndex >= count) {
                            autoScroll(node);
                        } else {
                            printWXInfo(node);
                        }
                    } else {
                        curHIndex = 0;
                        curVIndex = i + 1;
                        rightNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    return;
                }
            }

            //自动往下滚动
            //Utils.e(TAG, curVIndex + "--------" + count);
            if (curVIndex >= count) {
                autoScroll(node);
            }
        } catch (Exception e) {
            //打印到底部了
            Utils.show(this, "=====遍历朋友圈完毕=============");
        }
    }

    private void autoScroll(final AccessibilityNodeInfo node) {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                curVIndex = 0;
                curHIndex = 0;
                scrollEnable = true;
                node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            }
        }, 300);
    }

    private void traverseNode(final AccessibilityNodeInfo node) {
        if (null == node) return;
        //logNodeInfo(node);
        final AccessibilityNodeInfo contentNode = node.getChild(0);
        String className = contentNode.getClassName().toString();
        //Utils.e(TAG, "---------->>" + className);

        if (className.equals("android.widget.ListView")) {
            try {
                //保存图片
                final AccessibilityNodeInfo saveNode = contentNode.getChild(1);
                if (saveNode.getChild(0).getText().toString().equals("保存到手机")) {
                    saveImgEnable = true;
                    saveNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }else{
                    execShellCmd("input tap 100 100");//点击退出弹窗
                }
                return;
            } catch (Exception e) {
            }
        } else if (className.equals("android.widget.Gallery")) {
            if(saveImgEnable){
                //获取图片路径
                WXUser wxUser = mSession.getWXUser(curWXUser);
                String newImgPath=getImgPath();
                if(!TextUtils.isEmpty(newImgPath) && newImgPath.equals(lastImgPath)){
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            execShellCmd("input swipe 200 200 200 200 800");
                        }
                    }, 300);
                }else{
                    saveImgEnable=false;
                    wxUser.addImgPath(newImgPath);
                    execShellCmd("input tap 200 200");
                }

            }else{
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        execShellCmd("input swipe 200 200 200 200 800");
                    }
                }, 300);
            }
            return;
        } else if (className.equals("android.widget.FrameLayout")) {
            try {
                //第二种状况：图文详情
                if (!moreImgEnable) {
                    moreImgEnable=true;
                    execShellCmd("input tap "+(mSession.getScreenWidth()-50)+" "+(mSession.getScreenHeight()-50));
                    return;
                }else{
                    moreImgEnable=false;
                    //execShellCmd("input tap 40 40");
                    execShellCmd("input keyevent KEYCODE_BACK");
                }
            } catch (Exception e) {}
            try{
                //logNodeInfo(node);
                int count=contentNode.getChildCount();
                AccessibilityNodeInfo titleNode=contentNode.getChild(count-1).getChild(1);
                if(titleNode.getText().toString().equals("详情")){
                    AccessibilityNodeInfo itemNode = contentNode.getChild(0).getChild(0);
                    count=itemNode.getChildCount();

                    if (TextUtils.isEmpty(curTime)) {
                        AccessibilityNodeInfo timeNode = itemNode.getChild(count - 2);
                        curTime = timeNode.getText().toString();
                        AccessibilityNodeInfo textNode = itemNode.getChild(3);
                        curText = textNode.getText().toString();
                        //Log.e(TAG, curTime + "===>>>" + curText);
                        saveRecordInfo(curTime, curText);
                        curImgIndex=4;
                    }

                    if(curImgIndex<count-2){
                        AccessibilityNodeInfo imgNode=itemNode.getChild(curImgIndex);
                        if(imgNode.getClassName().toString().equals("android.view.View")){
                            imgNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            ++curImgIndex;
                            return;
                        }else{
                            //分享了链接或其他
                        }
                    }

                    detailEnable = false;
                    //execShellCmd("input tap 40 40");
                    execShellCmd("input keyevent KEYCODE_BACK");
                }else{

                }
            } catch (Exception e) {
                Utils.e(TAG,"异常了"+e.toString());
                detailEnable = false;
                moreImgEnable=false;
                //execShellCmd("input tap 40 40");
                execShellCmd("input keyevent KEYCODE_BACK");

                //logNodeInfo(node);
            }

            //=======================其他异常信息都返回==============================
            //previousStep(contentNode);
        }
    }

    private void saveRecordInfo(String timeStr, String textStr) {
        RecordInfo recordInfo = new RecordInfo();
        recordInfo.setRecordTime(timeStr);
        recordInfo.setRecordContent(textStr);
        WXUser wxUser = mSession.getWXUser(curWXUser);
        wxUser.addRecordInfo(recordInfo);
    }

    private void logNodeInfo(AccessibilityNodeInfo node) {
        int count = node.getChildCount();
        Log.e(TAG, count + "=>>>" + node.getClassName());
        if (count > 0) {
            for (int i = 0; i < count; ++i) {
                AccessibilityNodeInfo childNode = node.getChild(i);
                logNodeInfo(childNode);
            }
        } else {
            CharSequence text = node.getText();
            if (null != text && text.length() > 0) {
                String str = text.toString();
                Log.e(TAG, "===>>>" + str);
            }
        }
    }

    private String getImgPath() {
        {
            File filePath = new File("/storage/emulated/0/tencent/MicroMsg/WeiXin");
            if (filePath.exists()) {
                File files[] = filePath.listFiles();
                File file = files[files.length - 1];
                return file.getAbsolutePath();
            }
        }
        {
            File filePath = new File("/storage/sdcard1/tencent/MicroMsg/WeiXin");
            if (filePath.exists()) {
                File files[] = filePath.listFiles();
                File file = files[files.length - 1];
                return file.getAbsolutePath();
            }
        }
        return "";
    }


    @Override
    public void onInterrupt() {
    	/*系统想要中断AccessibilityService返给的响应时会调用。在整个生命周期里会被调用多次。*/
        Log.e(TAG, "=======onInterrupt============");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.e(TAG, "======onServiceConnected==========");
		/*系统会在成功连接上你的服务的时候调用这个方法，在这个方法里你可以做一下初始化工作，例如设备的声音震动管理，也可以调用setServiceInfo()进行配置工作。
		 * 也可以在我们的xml里面配置我们的Service的信息,这里我也是在xml里面配置的信息
		 * */
        mSession = Session.getInstance(this);
        mSession.openService(true);

    }

    @Override
    public boolean onUnbind(Intent intent) {
		/*在系统将要关闭这个AccessibilityService会被调用。在这个方法中进行一些释放资源的工作。*/
        Log.e(TAG, "======onUnbind==========");
        mSession.openService(false);
        return super.onUnbind(intent);
    }

    /**
     * 执行shell命令
     *
     * @param cmd
     */
    private void execShellCmd(String cmd) {
        try {
            // 申请获取root权限，这一步很重要，不然会没有作用
            Process process = Runtime.getRuntime().exec("su");
            // 获取输出流
            OutputStream outputStream = process.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            dataOutputStream.writeBytes(cmd);
            dataOutputStream.flush();
            dataOutputStream.close();
            outputStream.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}