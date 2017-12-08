package com.lin.dbhelper;

public class Pulse {
	private String _id;
	private String value;
	private String time;
	
	public Pulse(){
		
	}
	
	public Pulse(String value,String time){
		this.value = value;
		this.time = time;
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
	
	
}
