#!/usr/bin/python
# -*- coding: UTF-8 -*-

import sql_tools

tables = 'test'
sql_obj = sql_tools.SQLObj()

def create_insert_sql(pulse_times, log = '-'):
	insert_data_sql = 'INSERT INTO ' + \
										tables + \
												' (pulse_times,log) VALUES (%i, "%s")' % (pulse_times, log)
	return insert_data_sql

def insert_data(pulse_times, log):
	f type(pulse_times) is int:
		insert_sql = create_insert_sql(pulse_times, str(log))
		sql_obj.execute_sql(insert_sql)

def get_all_data(sql):
	return sql_obj.fetchall_sql(sql)

def get_one_data(sql):
	return sql_obj.fetchone_sql(sql)

def get_time_data():
	sql = sql_obj.sql_from_table('select date_time from ', tables)
	return get_all_data(sql)
		
def get_one_time_data():
	sql = sql_obj.sql_from_table('select date_time from ', tables)
	return get_one_data(sql)

def get_pulse_time_data():
	sql = sql_obj.sql_from_table('select pulse_times from ', tables)
	return get_all_data(sql)

def get_day_data(date_time):
	sql = "select date_format(date_time,'%Y%m%d%H%i%s'),pulse_times from " + str(tables) + ' where date_time > ' + str(date_time)
	print sql
	return get_all_data(sql)

def delect_all_data():
	sql = "TRUNCATE TABLE " + str(tables)
	print sql
	sql_obj.execute_sql(sql)

def get_log_data():
	sql = sql_obj.sql_from_table('select log from ', tables)
	return get_all_data(sql)

def finish():
	sql_obj.sql_finish()


