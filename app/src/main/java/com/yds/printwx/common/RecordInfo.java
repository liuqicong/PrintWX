package com.yds.printwx.common;

import com.yds.printwx.common.utils.Utils;

import java.util.ArrayList;

/**
 * Created by Administrator on 2016/2/17.
 */
public class RecordInfo {

    public String recordTime;
    public String recordContent;
    public ArrayList<String> recordImgList;

    public RecordInfo(){
        recordTime="";
        recordContent="";
        recordImgList=new ArrayList<>();
    }

    public void setRecordTime(String recordTime){
        this.recordTime=recordTime;
    }

    public void setRecordContent(String recordContent){
        this.recordContent=recordContent;
    }

    public void addRecordImg(String imgPath){
        recordImgList.add(imgPath);
    }

    public String getRecordTime(){
        return recordTime;
    }

    public String getRecordContent(){
        return recordContent;
    }

    public ArrayList<String> getRecordImgList(){
        return recordImgList;
    }

    public String getFlag(){
        String content=recordTime+recordContent+recordImgList.toString();
        return Utils.getMD5(content);
    }

    @Override
    public String toString() {
        if(recordImgList.size()>0){
            return recordTime+" "+recordContent+" "+recordImgList.toString();
        }else{
            return recordTime+" "+recordContent;
        }

    }
}
