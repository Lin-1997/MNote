package com.lin.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
	@Override
	public void onCreate (SQLiteDatabase db)
	{
		String content1 = "# MNote\n\nMarkdown是一种轻量级的「标记语言」，" +
				"通常为程序员群体所用，目前它已是全球最大的技术分享网站 GitHub " +
				"和技术问答网站 StackOverFlow 的御用书写格式。\n\n" +
				"您可以使用MNote：\n\n***优雅地沉浸式记录，专注内容而不是纠结排版，" +
				"达到「心中无尘，码字入神」的境界。***\n" +
				"![MNote-logo](http://120.78.155.180:3000/help/logo)\n\n" +
				"**请保留此份 MNote 的欢迎稿兼使用说明，以便查询不熟悉的标记符号。**\n\n" +
				"## 什么是 Markdown\n\nMarkdown 是一种方便记忆、书写的纯文本标记语言，" +
				"用户可以使用这些标记符号以最小的输入代价生成极富表现力的文档：" +
				"譬如您正在阅读的这份文档。它使用简单的符号标记不同的标题，" +
				"分割不同的段落，**粗体** 或者 *斜体* 或者 ~~删除线~~ 或者\n" +
				"> 引用，当然引用需要在新行，而且引用完记得换行哦\n\n" +
				"虽然这几个功能已经很实用了，但是高级用法必不可少。\n\n" +
				"## 这才是 Markdown\n\n## 1. 好多级标题\n# 一级\n## 二级\n### 三级\n" +
				"###### 六级\n####### 七级，不好意思没有七级\n\n这也是一个一级标题\n" +
				"===\n这也是一个二级标题\n-----------\n这样写标题还不如用#，| ･ ω ･ ｀ )\n\n" +
				"## 2. 无序列表\n- 你看\n- 无序的吧\n\n## 3. 有序列表\n1. 这个列表\n" +
				"1. 似乎很机智\n1. 什么\n1. 没发现？？\n100. 这下发现了吧\n\n" +
				"## 4. 高亮一段代码\n```JavaScript\nvar a = 1;\nvar b = 1;\n" +
				"console.log (a + b);\n```\n```java\nint base = 2;\nint power = 4;\n" +
				"for (int i = 1; i < power; ++i)\n{\n    a *= a\n    System.out.println (a);\n}\n```" +
				"\n\n## 5. 绘制表格\n| 标题1 | 标题2 | 标题3 |\n|--------|-----:|:----:|\n" +
				"| 这是常规内容 | 冒号在右边会靠右 | 两边都有冒号 |\n| 写多了真不方便看 " +
				"| 冒号在左边会靠左 | 会怎么样 |\n| 唉 | 本来也靠左  | 我想你也看出来了 |\n\n" +
				"## 6. 制作一份待办事宜\n- [] 支持以 PDF 格式导出文稿\n" +
				"- [x] 支持以 MD 格式导出文档\n- [x] 支持 TaskList 列表功能\n" +
				"- [] 增加个好用的菜单栏\n- [x] 输入法顶起界面\n- [] 导入 MD 格式文档\n" +
				"- [] 自定义样式\n\n## ~~***7. 这不是内容***~~\n~~***正如上面所述，" +
				"还没有个好用的菜单栏，心塞( ´  - ω ก ` )，" +
				"不过markdown本意就是不希望眼睛的焦点脱离输入框，" +
				"没有菜单栏才是初衷，语法不记得的就多看看文档，" +
				"凑合一下等更新吧***~~";

		String content2 = "- 首页点击上面的头像或文字就可以注册登录了\n" +
				"- 首页点击右下角的小浮标就可以新建便签了，" +
				"还可以进行云备份和还原，当然你得登录帐号\n" +
				"- 首页长按可以删除便签\n" +
				"- 编辑界面右上角的小浮标可以导出md文档和预览" +
				"，编辑好了直接返回就会保存，不需要手动保存\n" +
				"- 个人中心头像下拉可以展开放大\n" +
				"- markdown语法简介在另一条便签中";

		db.execSQL ("create table if not exists setting(" +
				"color char(1) not null)");
		db.execSQL ("insert into setting values(\"1\")");
		db.execSQL ("create table if not exists user(" +
				"account char(11) primary key," +
				"name varchar(30) not null," +
				"avatar char(1) not null)");
		db.execSQL ("create table if not exists note(" +
				"id integer primary key autoincrement," +
				"updateDate date not null," +
				"updateTime time not null," +
				"content longtext not null," +
				"status integer not null)");
		db.execSQL ("insert into note values(\"1\",\"2018-01-01\",\"00:00:01\",\"" +
				content1 + "\",\"-1\")");
		db.execSQL ("insert into note values(\"2\",\"2018-01-01\",\"00:00:02\",\"" +
				content2 + "\",\"-1\")");
		db.execSQL ("create table if not exists deleteLog(" +
				"id integer not null)");
	}

	/**
	 * 当数据库版本更新时的回调函数
	 *
	 * @param db 数据库对象
	 * @param oldVersion 数据库旧版本
	 * @param newVersion 数据库新版本
	 */
	@Override
	public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) { }

	/**
	 * 当数据库打开时的回调函数
	 *
	 * @param db 数据库对象
	 */
	@Override
	public void onOpen (SQLiteDatabase db)
	{
		super.onOpen (db);
	}
}
