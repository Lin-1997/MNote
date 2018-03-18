package com.lin.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteHelper extends SQLiteOpenHelper
{
	private static SQLiteHelper helper;

	/**
	 * 构造函数
	 *
	 * @param context 上下文对象
	 * @param name 数据库的名称
	 * @param factory 游标工厂
	 * @param version 数据库版本 >= 1
	 */
	private SQLiteHelper (Context context, String name,
			SQLiteDatabase.CursorFactory factory, int version)
	{
		super (context, name, factory, version);
	}

	public static SQLiteHelper getHelper (Context context)
	{
		if (helper == null)
			helper = new SQLiteHelper (context, "MNode.db",
					null, 1);
		return helper;
	}

	/**
	 * 创建数据库时的回调函数
	 *
	 * @param db 数据库对象
	 */
	@Override public void onCreate (SQLiteDatabase db)
	{
		Log.i ("onCreate", "onCreate");
		db.execSQL ("create table if not exists setting(" +
				"color char(1)," +
				"sort char(1))");
		db.execSQL ("insert into setting values(\"1\",\"0\")");
		db.execSQL ("create table if not exists user(" +
				"account char(11) primary key," +
				"name varchar(30) not null," +
				"avatar char(1) not null)");
		db.execSQL ("create table if not exists note(" +
				"id integer primary key autoincrement," +
				"createDate date not null," +
				"createTime time not null," +
				"updateDtae date," +
				"updateTime time," +
				"edited boolean default 1," +
				"content longtext not null)");
	}

	/**
	 * 当数据库版本更新时的回调函数
	 *
	 * @param db 数据库对象
	 * @param oldVersion 数据库旧版本
	 * @param newVersion 数据库新版本
	 */
	@Override public void onUpgrade (SQLiteDatabase db,
			int oldVersion, int newVersion)
	{
		Log.i ("onUpgrade", "onUpgrade");
	}

	/**
	 * 当数据库打开时的回调函数
	 *
	 * @param db 数据库对象
	 */
	@Override public void onOpen (SQLiteDatabase db)
	{
		Log.i ("onOpen", "onOpen");
		super.onOpen (db);
	}
}
