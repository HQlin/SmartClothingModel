package com.lin.dbhelper;

import java.util.ArrayList;
import java.util.List;
 
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
 
public class DBManager {
    private DBHelper helper;
    private SQLiteDatabase db;
     
    public DBManager(Context context) {
        helper = new DBHelper(context);
        //因为getWritableDatabase内部调用了mContext.openOrCreateDatabase(mName, 0, mFactory);
        //所以要确保context已初始化,我们可以把实例化DBManager的步骤放在Activity的onCreate里
        db = helper.getWritableDatabase();
    }
     
    /**
     * add pulses
     * @param pulses
     */
    public void add(List<Pulse> pulses) {
        db.beginTransaction();    //开始事务
        try {
            for (Pulse pulse : pulses) {
                db.execSQL("INSERT INTO pulse VALUES(null, ?, ?)", new Object[]{pulse.getValue(), pulse.getTime()});
            }
            db.setTransactionSuccessful();    //设置事务成功完成
        } finally {
            db.endTransaction();    //结束事务
        }
    }
     
     
    /**
     * query all pulses, return list
     * @return List<Pulse>
     */
    public List<Pulse> query() {
        ArrayList<Pulse> pulses = new ArrayList<Pulse>();
        Cursor c = queryTheCursor();
        while (c.moveToNext()) {
        	Pulse pulse = new Pulse();
        	pulse.set_id(c.getString(c.getColumnIndex("_id")));
        	pulse.setValue(c.getString(c.getColumnIndex("value")));
        	pulse.setTime(c.getString(c.getColumnIndex("time")));
        	pulses.add(pulse);
        }
        c.close();
        return pulses;
    }
    
    public List<Pulse> query(String date1, String date2){
    	ArrayList<Pulse> pulses = new ArrayList<Pulse>();
        Cursor c = db.rawQuery("SELECT * FROM pulse where time >= ? and time < ?",new String[]{date1, date2});
        while (c.moveToNext()) {
        	Pulse pulse = new Pulse();
        	pulse.set_id(c.getString(c.getColumnIndex("_id")));
        	pulse.setValue(c.getString(c.getColumnIndex("value")));
        	pulse.setTime(c.getString(c.getColumnIndex("time")));
        	pulses.add(pulse);
        }
        c.close();
        return pulses;
    }
     
    /**
     * query all pulses, return cursor
     * @return    Cursor
     */
    public Cursor queryTheCursor() {
        Cursor c = db.rawQuery("SELECT * FROM pulse", null);
        return c;
    }
     
    /**
     * close database
     */
    public void closeDB() {
        db.close();
    }
}
