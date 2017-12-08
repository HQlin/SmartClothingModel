package com.skyfishjy.ripplebackground.sample;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.lin.dbhelper.DBManager;
import com.lin.dbhelper.Pulse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class HistoryChatActivity extends Activity{

	public static final String TITLE = "title";
	private PulseView pulseView;
	private String title;
	private long[] addX;
	private int[] addY;
	
	private DBManager mgr;
	private int aveAddY = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		pulseView = new PulseView(this);
		setContentView(pulseView.getmView());
		
		Intent intent = getIntent();
		title = intent.getStringExtra(TITLE);
		
		pulseView.setTitleTv(title);
		title = title.replace("\n~\n", "~");
		String[] dates = title.split("~");
		mgr = new DBManager(HistoryChatActivity.this);
		List<Pulse> pulses = mgr.query(dates[0],dates[1]);
		addX = new long[pulses.size()];
		addY = new int[pulses.size()];
		for(int i = 0;i<pulses.size();i++){
			addX[i] = date2TimeStamp(pulses.get(i).getTime(), "yyyy-MM-dd HH:mm:ss");
			addY[i] = Integer.parseInt(pulses.get(i).getValue());
			aveAddY += addY[i];
		}
		
		if(pulses.size()!=0){
			aveAddY = aveAddY/pulses.size();
			pulseView.setChat(addX, addY, aveAddY);
		} else
			Toast.makeText(this, "无数据！", Toast.LENGTH_SHORT).show();
	}
	
	 /** 
     * 日期格式字符串转换成时间戳 (ms)
     * @param date 字符串日期 
     * @param format 如：yyyy-MM-dd HH:mm:ss 
     * @return 
     */  
    public static long date2TimeStamp(String date_str,String format){  
        try {  
            SimpleDateFormat sdf = new SimpleDateFormat(format);  
            return sdf.parse(date_str).getTime();  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
        return 0;  
    } 
}
