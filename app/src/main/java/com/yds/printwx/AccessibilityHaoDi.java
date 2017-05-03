package com.yds.printwx;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.yds.printwx.common.utils.Utils;

import java.io.DataOutputStream;
import java.io.OutputStream;

@SuppressLint({ "NewApi", "Wakelock" })
public class AccessibilityHaoDi extends AccessibilityService {

    private final String TAG = "Accessibility";
    private Session mSession;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
		/*通过这个函数可以接收系统发送来的AccessibilityEvent，接收来的AccessibilityEvent是经过过滤的，过滤是在配置工作时设置的。*/
        final int eventType = event.getEventType();
        final AccessibilityNodeInfo nodeInfo = event.getSource();

        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (null != nodeInfo) {
                //进入到了"通知记录"界面
                traverseNode(nodeInfo);
            }
        }
        if (eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (null != nodeInfo) {
                printHaoDiPhone(nodeInfo);
            }
        }
    }

    private synchronized void printHaoDiPhone(final AccessibilityNodeInfo node) {
        try {
            if (null == node) return;
            int count = node.getChildCount();
            for (int i = 0; i < count; ++i) {
                AccessibilityNodeInfo phoneNode = node.getChild(i).getChild(0);
                CharSequence text = phoneNode.getText();
                if (null != text && text.length() > 0) {
                    mSession.addPhone(text.toString());
                }
            }

            //自动往下滚动
            autoScroll(node);

        } catch (Exception e) {
            //打印到底部了
            Utils.show(AccessibilityHaoDi.this, "=====遍历通讯记录完毕=============");
        }
    }

    private void autoScroll(final AccessibilityNodeInfo node) {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            }
        }, 500);
    }

    private void traverseNode(final AccessibilityNodeInfo node) {
        if (null == node) return;
        logDate(node);

        int index=node.getChildCount()-1;
        final AccessibilityNodeInfo contentNode = node.getChild(index);
        String className = contentNode.getClassName().toString();
        if (className.equals("android.widget.ListView")) {
            printHaoDiPhone(contentNode);
        }
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

    private void logDate(AccessibilityNodeInfo node){
        try{
            AccessibilityNodeInfo dateNode=node.getChild(3).getChild(0);
            CharSequence text = dateNode.getText();
            if (null != text && text.length() > 0) {
                String str = text.toString();
                mSession.setDate(str);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

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