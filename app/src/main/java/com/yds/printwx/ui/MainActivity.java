package com.yds.printwx.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.yds.printwx.Constants;
import com.yds.printwx.R;
import com.yds.printwx.Session;
import com.yds.printwx.common.RecordInfo;
import com.yds.printwx.common.WXUser;
import com.yds.printwx.common.utils.AppUtils;
import com.yds.printwx.common.utils.RootCmd;
import com.yds.printwx.common.utils.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends Activity implements View.OnClickListener,Observer {

    private Session mSession;

    private ImageView switchView;
    private EditText nameEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSession=Session.getInstance(this);
        mSession.addObserver(this);

        switchView=(ImageView) findViewById(R.id.main_checkbox);
        switchView.setOnClickListener(this);
        nameEdit= (EditText) findViewById(R.id.main_friend_name);
        nameEdit.setText("AA黄东方");
        findViewById(R.id.main_scanning).setOnClickListener(this);
        findViewById(R.id.main_printnow).setOnClickListener(this);
        findViewById(R.id.main_save_phonelist).setOnClickListener(this);

        setSwitch();
        RootCmd.haveRoot();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_checkbox:
            {
                Intent intent =  new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
            }
            break;

            case R.id.main_scanning:
            {
                if(!mSession.whetherOpen()){
                    Utils.showShort(this,"请先点击左上角开关开启相关服务");return;
                }
                String name=nameEdit.getText().toString().trim();
                if(TextUtils.isEmpty(name)){
                    Utils.showShort(this,"请输入好友名字");return;
                }else{
                    mSession.setFriendName(name);
                    String wxPackageName="com.tencent.mm";
                    if(AppUtils.isInstalled(this,wxPackageName)){
                        cleanImgPath();
                        mSession.setSteps(0);
                        AppUtils.launchApp(this,wxPackageName);
                    }else{
                        Utils.showShort(this,"请先安装微信6.3.13版本");return;
                    }
                }
            }
                break;

            case R.id.main_printnow:
            {
                String wxUserName=mSession.getFriendName();
                WXUser wxUser=mSession.getWXUser(wxUserName);
                if(wxUser!=null){
                    ArrayList<RecordInfo> recordInfos=wxUser.getRecordList();
                    int size=recordInfos.size();
                    for(int i=0;i<size;++i){
                        RecordInfo recordInfo=recordInfos.get(i);

                        Log.e(wxUserName,recordInfo.toString());
                    }
                }
            }
                break;

            case R.id.main_save_phonelist:
                try {
                    String sdPath=Environment.getExternalStorageDirectory().getAbsolutePath()+"/ZZZ";
                    File myFile=new File(sdPath);
                    if(!myFile.exists()){
                        myFile.mkdir();
                    }
                    File file=new File(sdPath, mSession.getSaveName());
                    if(file.exists()) file.delete();
                    if(file.createNewFile()){
                        BufferedWriter bw=new BufferedWriter(new FileWriter(file));
                        List<String> phoneList=mSession.getPhoneList();
                        int size=phoneList.size();
                        for(int i=0;i<size;++i){
                            bw.write(phoneList.get(i));
                            bw.write("\r\n");
                        }
                        bw.close();
                    }

                    Utils.showShort(this,"保存："+mSession.getPhoneList().size()+"  去重："+mSession.getRepeatNumber());
                    mSession.resetPhoneList();
                } catch(Exception e) {
                    Utils.e("---caonima---",""+e.toString());
                    Utils.show(this,"保存失败");
                }

                break;

            default:break;
        }
    }

    private void setSwitch(){
        if(mSession.whetherOpen()){
            switchView.setImageResource(R.mipmap.switch_on);
        }else{
            switchView.setImageResource(R.mipmap.switch_off);
        }
    }

    @Override
    public void update(Observable observable, Object data) {
        try{
            HashMap<String, Object> map=(HashMap<String, Object>) data;
            if(map.containsKey(Constants.NOTIFY_ONOFF)){
                setSwitch();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    private void cleanImgPath() {
        File filePath = new File("/storage/emulated/0/tencent/MicroMsg/WeiXin");
        if (filePath.exists()) {
            File files[] = filePath.listFiles();
            for (File f : files){
                f.delete();
            }
        }
        filePath = new File("/storage/sdcard1/tencent/MicroMsg/WeiXin");
        if (filePath.exists()) {
            File files[] = filePath.listFiles();
            for (File f : files){
                f.delete();
            }
        }
    }

}
