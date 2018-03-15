package com.lin.mnote;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.lin.bean.User;
import com.lin.utils.SQLiteHelper;
import com.lin.utils.Values;

public class MainActivity extends AppCompatActivity
{
	private SQLiteHelper helper;
	private User user;
	private boolean hasUser = false;
	private boolean hasNote = false;

//	final SimpleDateFormat dateFormat1 = new SimpleDateFormat ("yyyy-MM-dd");
//	final SimpleDateFormat dateFormat2 = new SimpleDateFormat ("HH:mm:ss");
//	final Date date = new Date (System.currentTimeMillis ());

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		//设置主题要在setContentView之前
		LoadSettingFromSQLite ();
		setTheme (Values.getTheme ());

		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_main);

		// FIXME: 2018/3/13 读取已有用户怎么拿头像
		LoadUserFromSQLite ();
		new Thread (new Runnable ()
		{
			@Override public void run ()
			{
				LoadNoteFromSQLite ();
			}
		});
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult (requestCode, resultCode, data);

		switch (requestCode)
		{
			case Values.REQ_SIGN_IN:
				switch (resultCode)
				{
					case Values.RES_SIGN_IN:
						// FIXME: 2018/3/13 头像保存在data目录
						writeUserToMemory (data.getStringExtra ("account"),
								data.getStringExtra ("name"), null);
						Intent intent = new Intent (this, UserCenterActivity.class);
						startActivityForResult (intent, Values.REQ_USER_CENTER);
						break;
				}
				break;
			case Values.REQ_USER_CENTER:
				switch (resultCode)
				{
					case Values.RES_CHANGE_SOMETHING:
						//数据已经持久化保存，只需要刷新显示
						if (Values.isChangeAvatar ())
						{
							ImageView imageView = findViewById (R.id.imageViewAvatar);
							imageView.setImageBitmap (user.getAvatar ());
							Values.setChangeAvatar (false);
						}
						if (Values.isChangeName ())
						{
							TextView textView = findViewById (R.id.textViewName);
							textView.setText (user.getName ());
							Values.setChangeName (false);
						}
						if (Values.isChangeSort ())
						{
							// FIXME: 2018/3/9 刷新笔记的显示
							Values.setChangeSort (false);
						}
						break;
					case Values.RES_CHANGE_THEME:
						//重新加载Activity
						recreate ();
						break;
					case Values.RES_CHANGE_PASSWORD:
						//退出，转到登录界面
						String preAccount = user.getAccount ();
						clearUserInSQLite ();
						clearUserInMemory ();
						Intent intent = new Intent (this, SignInActivity.class);
						intent.putExtra ("preAccount", preAccount);
						startActivityForResult (intent, Values.REQ_SIGN_IN);
						break;
					case Values.RES_SIGN_OUT:
						//退出
						clearUserInSQLite ();
						clearUserInMemory ();
						break;
				}
				break;
		}
	}

	public void imageViewAvatar (View view)
	{
		if (hasUser)
		{
			Intent intent = new Intent (this, UserCenterActivity.class);
			startActivityForResult (intent, Values.REQ_USER_CENTER);
		}

		else
		{
			Intent intent = new Intent (this, SignInActivity.class);

			helper = SQLiteHelper.getHelper (this);
			SQLiteDatabase db = helper.getWritableDatabase ();
			Cursor cursor = db.rawQuery ("select * from user where account = \"0\"",
					null);
			if (cursor.getCount () == 1)
			{
				//cursor默认在第一个之前的位置
				cursor.moveToNext ();
				intent.putExtra ("preAccount", cursor.getString (
						cursor.getColumnIndex ("name")));
			}
			else
				intent.putExtra ("preAccount", "");
			cursor.close ();
			db.close ();

			startActivityForResult (intent, Values.REQ_SIGN_IN);
		}
	}

	/**
	 * 读取SQLite偏好设置
	 */
	private void LoadSettingFromSQLite ()
	{
		helper = SQLiteHelper.getHelper (this);
		/*
		 * getReadableDatabase(), getWritableDatabase () 都是创建或打开数据库
		 * 如果不存在就创建，否者直接打开
		 * 默认情况都是可读可写
		 * 如果磁盘已满或权限问题，getReadableDatabase() 打开的是只读数据库
		 */
		SQLiteDatabase db = helper.getWritableDatabase ();
		Cursor cursor = db.rawQuery ("select * from setting", null);
		cursor.moveToNext ();

		//颜色主题
		switch (cursor.getString (cursor.getColumnIndex ("color")))
		{
			case "1":
				Values.setTheme (R.style.MyBlueTheme);
				Values.setColor (R.color.colorBlue);
				Values.setSelector (R.drawable.selector_blue);
				Values.setBackground (R.drawable.button_blue);
				Values.setProgress (R.drawable.progress_bar_blue);
				break;
			case "2":
				Values.setTheme (R.style.MyGrayTheme);
				Values.setColor (R.color.colorGray);
				Values.setSelector (R.drawable.selector_gray);
				Values.setBackground (R.drawable.button_gray);
				Values.setProgress (R.drawable.progress_bar_gray);
				break;
			case "3":
				Values.setTheme (R.style.MyRedTheme);
				Values.setColor (R.color.colorRed);
				Values.setSelector (R.drawable.selector_red);
				Values.setBackground (R.drawable.button_red);
				Values.setProgress (R.drawable.progress_bar_red);
				break;
			case "4":
				Values.setTheme (R.style.MyCyanTheme);
				Values.setColor (R.color.colorCyan);
				Values.setSelector (R.drawable.selector_cyan);
				Values.setBackground (R.drawable.button_cyan);
				Values.setProgress (R.drawable.progress_bar_cyan);
				break;
			case "5":
				Values.setTheme (R.style.MyGreenTheme);
				Values.setColor (R.color.colorGreen);
				Values.setSelector (R.drawable.selector_green);
				Values.setBackground (R.drawable.button_green);
				Values.setProgress (R.drawable.progress_bar_green);
				break;
			case "6":
				Values.setTheme (R.style.MyOrangeTheme);
				Values.setColor (R.color.colorOrange);
				Values.setSelector (R.drawable.selector_orange);
				Values.setBackground (R.drawable.button_orange);
				Values.setProgress (R.drawable.progress_bar_orange);
				break;
			case "7":
				Values.setTheme (R.style.MyYellowTheme);
				Values.setColor (R.color.colorYellow);
				Values.setSelector (R.drawable.selector_yellow);
				Values.setBackground (R.drawable.button_yellow);
				Values.setProgress (R.drawable.progress_bar_yellow);
				break;
			case "8":
				Values.setTheme (R.style.MyPinkTheme);
				Values.setColor (R.color.colorPink);
				Values.setSelector (R.drawable.selector_pink);
				Values.setBackground (R.drawable.button_pink);
				Values.setProgress (R.drawable.progress_bar_pink);
				break;
			case "9":
				Values.setTheme (R.style.MyPurpleTheme);
				Values.setColor (R.color.colorPurple);
				Values.setSelector (R.drawable.selector_purple);
				Values.setBackground (R.drawable.button_purple);
				Values.setProgress (R.drawable.progress_bar_purple);
				break;
			default:
				Values.setTheme (R.style.MyBlueTheme);
				Values.setColor (R.color.colorBlue);
				Values.setSelector (R.drawable.selector_blue);
				Values.setBackground (R.drawable.button_blue);
				Values.setProgress (R.drawable.progress_bar_blue);
		}

		//排序方式
		if (cursor.getString (cursor.getColumnIndex ("sort")).equals ("0"))
			Values.setSort (0);
		else
			Values.setSort (1);

		cursor.close ();
		db.close ();
	}

	/**
	 * 读取SQLite用户
	 */
	private void LoadUserFromSQLite ()
	{
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		Cursor cursor = db.rawQuery ("select * from user where account != \"0\"",
				null);

		Bitmap bitmap = null;

		if (cursor.getCount () == 1)
		{
			//cursor默认在第一个之前的位置
			cursor.moveToNext ();
			writeUserToMemory (cursor.getString (cursor.getColumnIndex ("account")),
					cursor.getString (cursor.getColumnIndex ("name")), bitmap);
		}
		cursor.close ();
		db.close ();
	}

	/**
	 * 写入内存用户
	 */
	private void writeUserToMemory (String account, String name, Bitmap avatar)
	{
		if (avatar == null)
			avatar = BitmapFactory.decodeResource (getResources (), R.drawable.ic_avatar);
		user = User.getUser ();
		user.signIn (account, name, avatar);
		hasUser = true;
		TextView textView = findViewById (R.id.textViewName);
		textView.setText (name);
		FloatingActionButton floatingActionButton = findViewById (R.id.fabSync);
		floatingActionButton.setVisibility (View.VISIBLE);
		ImageView imageView = findViewById (R.id.imageViewAvatar);
		imageView.setImageBitmap (avatar);
	}

	/**
	 * 清空内存用户
	 */
	private void clearUserInMemory ()
	{
		User.signOut ();
		hasUser = false;
		TextView textView = findViewById (R.id.textViewName);
		textView.setText ("登录了可以上传云哦");
		FloatingActionButton floatingActionButton = findViewById (R.id.fabSync);
		floatingActionButton.setVisibility (View.INVISIBLE);
	}

	/**
	 * 清空SQLite用户
	 */
	private void clearUserInSQLite ()
	{
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		db.execSQL ("delete from user");
		//记录上次的登录账号在name列
		db.execSQL ("insert into user values (\"0\",\"" + user.getAccount () + "\",\",null)");
		db.close ();
	}

	/**
	 * 读取SQLite笔记
	 */
	private void LoadNoteFromSQLite ()
	{

	}
}
