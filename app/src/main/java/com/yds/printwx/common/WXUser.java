package com.yds.printwx.common;

import java.util.ArrayList;

/**
 * Created by Administrator on 2016/2/17.
 */
public class WXUser {

    public String userName;
    public ArrayList<RecordInfo> recordList;

    public WXUser(String userName){
        this.userName=userName;
        recordList=new ArrayList<>();
    }


    public void addRecordInfo(RecordInfo recordInfo){

        int size=recordList.size();
        String flag=recordInfo.getFlag();
        for(int i=0;i<size;++i){
            if(flag.equals(recordList.get(i).getFlag())) return;
        }
        recordList.add(recordInfo);
    }

    public ArrayList<RecordInfo> getRecordList(){
        return recordList;
    }

    public void addImgPath(String imgPath){
        int size=recordList.size();
        if(size>0){
            RecordInfo recordInfo=recordList.get(size-1);
            recordInfo.addRecordImg(imgPath);
        }else{

        }

    }

}
