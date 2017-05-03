package com.yds.printwx;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.yds.printwx.common.WXUser;
import com.yds.printwx.ui.MainActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;


@SuppressLint("SdCardPath")
@TargetApi(Build.VERSION_CODES.KITKAT)
public class Session extends Observable{

    private Context mContext;
    private static Session mInstance;
    
    private String packageName;
    private boolean whetherOpen;

    private int sWidth=-1;
    private int sHeight=-1;

	private String mFriendName;
	private int mSteps=0;
	private HashMap<String,WXUser> allUser=new HashMap<>();

    private static List<String> phoneList=new ArrayList();
    private static int repeatNumber=0;
    private String mDate;

    private Session(Context context) {
        
        synchronized (this) {
            mContext = context;
            readSettings();
        }
    }
    
    public static Session getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Session(context.getApplicationContext());
        }
        return mInstance;
    }
    
    private void readSettings(){
    	 new Thread() {
             public void run() {
                 //getApplicationInfo();
             }
         }.start();
    }


    //====================================================================================
	public void setFriendName(String friendName){
		this.mFriendName=friendName;
	}

    public String getFriendName(){
		return mFriendName;
	}

	public void setSteps(int steps){
		mSteps=steps;
	}

	public int getSteps(){
		return mSteps;
	}
    
    //=====================================================================================
    public int getScreenWidth(){
		if (sWidth <= 0) {
			DisplayMetrics dm = new DisplayMetrics();
			WindowManager windowManager=(WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
			windowManager.getDefaultDisplay().getMetrics(dm);
			sWidth = dm.widthPixels;
		}
		return sWidth;
    }
    
    public int getScreenHeight(){
		if (sHeight <= 0) {
			DisplayMetrics dm = new DisplayMetrics();
			WindowManager windowManager=(WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
			windowManager.getDefaultDisplay().getMetrics(dm);
			sHeight = dm.heightPixels;
		}
		return sHeight;
    }
    
	private void notifyData(String key,Object data){
		final HashMap<String, Object> notify=new HashMap<String, Object>(1);
		notify.put(key,data);
		super.setChanged();
	    super.notifyObservers(notify);
	}

    //==================================================================================
    public List<String> getPhoneList(){
        return phoneList;
    }
    public int getRepeatNumber(){return repeatNumber;}

    public synchronized void addPhone(String phone){
        if(phoneList.contains(phone)){
            ++repeatNumber;
            Log.e("-----"+phone, "重复：" + repeatNumber);
        }else{
            phoneList.add(phone);
            Log.e("-----"+phone, "===>>>" + phoneList.size());
        }
    }

    public void resetPhoneList(){
        phoneList.clear();
        repeatNumber=0;
    }

    public void setDate(String date){
        mDate=date;
    }

    public String getSaveName(){
        if(TextUtils.isEmpty(mDate)){
            return ""+System.currentTimeMillis()+"_"+phoneList.size()+"_"+repeatNumber+".txt";
        }
        return mDate+"_"+phoneList.size()+"_"+repeatNumber+".txt";
    }

    //==============set and get==========================================================
	public WXUser getWXUser(String userName){
		 return allUser.get(userName);
	}

	public void addWxUser(String userName){
		if(!allUser.containsKey(userName)){
			allUser.put(userName,new WXUser(userName));
		}
	}


  	public void openService(boolean wo){
  		whetherOpen=wo;
  		notifyData(Constants.NOTIFY_ONOFF, 0);
  	}
  	
  	public boolean whetherOpen(){
		return whetherOpen;
  	}
  	
  	public void backMain(boolean isInstall){
  		if(isInstall){
  			new Handler().postDelayed(new Runnable() {
				
				@Override
				public void run() {
					startApp();
				}
			}, 4000);
  			return;
  		}
  		startApp();
  	}

  	private void startApp(){
  		Intent intent=new Intent(mContext,MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(Constants.INTENT_DATA, "");
  		mContext.startActivity(intent);
  	}
  	
  	public void hasInstallApp(){
  		notifyData(Constants.NOTIFY_INSTALLED,0);
  	}
  	
}
