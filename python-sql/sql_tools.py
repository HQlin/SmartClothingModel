#!/usr/bin/python
# -*- coding: UTF-8 -*-

import MySQLdb
HOST = 'localhost'
USER = 'will'
PASS = 'ljm'
DB_NAME = 'py_test_db'

class SQLObj(object):
	"""docstring for SQLObj"""
	def __init__(self):
		self.db = MySQLdb.connect(HOST, USER, PASS, DB_NAME)
		self.cursor = self.db.cursor()
		print 'connect to database: %s' % DB_NAME

	def execute_sql(self, sql):
		try:
			self.cursor.execute(sql)
			self.db.commit()
		except Exception, err:
			print err
			self.db.rollback()

	def fetchall_sql(self, sql):
		try:
			self.cursor.execute(sql)
			return self.cursor.fetchall()
		except Exception, err:
			print str(err) + ' when fetchall_sql'
			self.db.rollback()

	def fetchone_sql(self, sql):
		results = self.fetchall_sql(sql)
		for row in results:
			yield row

	def sql_from_table(self, sql, tables):
		return str(sql) + str(tables) + ';'

	def sql_finish(self):
		elf.db.close()

