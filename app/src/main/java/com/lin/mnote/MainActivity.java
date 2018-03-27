package com.lin.mnote;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lin.bean.User;
import com.lin.utils.Density;
import com.lin.utils.FileHelper;
import com.lin.utils.RequestServes;
import com.lin.utils.RetrofitHelper;
import com.lin.utils.SQLiteHelper;
import com.lin.utils.Values;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity
{
	private SQLiteHelper helper;
	private User user;
	private boolean hasUser = false;
	private boolean hasNote = false;
	private Dialog dialog;
	//uiHandler在主线程中创建，所以自动绑定主线程
	private Handler uiHandler = new Handler ();

	//	final SimpleDateFormat dateFormat1 = new SimpleDateFormat ("yyyy-MM-dd");
	//	final SimpleDateFormat dateFormat2 = new SimpleDateFormat ("HH:mm:ss");
	//	final Date date = new Date (System.currentTimeMillis ());

	//头像
	//getExternalFilesDir (Environment.DIRECTORY_DCIM);
	//图片
	//getExternalFilesDir (Environment.DIRECTORY_PICTURES);

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		//设置主题要在setContentView之前
		loadSettingFromSQLite ();
		setTheme (Values.getTheme ());

		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_main);

		if (FileHelper.hasSdcard ()) //6.0以上貌似要用户动态授权读写权限
			FileHelper.verifyStoragePermissions (this);
		else
		{
			//没有SD卡
			Dialog dialog = new Dialog (this, R.style.BottomDialog);
			View contentView = LayoutInflater.from (this).inflate
					(R.layout.dialog_no_sdcard_or_no_permission, null);
			TextView textView = contentView.findViewById (R.id.exit);
			textView.setBackgroundResource (Values.getSelector ());
			//没有SD卡就不能用了
			textView.setOnClickListener (new View.OnClickListener ()
			{
				@Override public void onClick (View v)
				{
					finish ();
				}
			});
			dialog.setContentView (contentView);
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
					contentView.getLayoutParams ();
			params.width = getResources ().getDisplayMetrics ().widthPixels
					- Density.dp2px (this, 16f);
			params.bottomMargin = Density.dp2px (this, 8f);
			contentView.setLayoutParams (params);
			dialog.getWindow ().setGravity (Gravity.CENTER);
			dialog.getWindow ().setWindowAnimations (R.style.BottomDialog_Animation);
			dialog.setCancelable (false);//禁止外部点击关闭
			dialog.show ();
		}

		dialog = new Dialog (this, R.style.BottomDialog);
		View contentView = LayoutInflater.from (this).inflate
				(R.layout.dialog_progress_bar, null);
		ProgressBar progressBar = contentView.findViewById (R.id.progressBar);
		progressBar.setIndeterminateDrawable (getResources ().getDrawable (Values.getProgress ()));

		dialog.setContentView (contentView);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
				contentView.getLayoutParams ();
		params.width = getResources ().getDisplayMetrics ().widthPixels
				- Density.dp2px (this, 16f);
		params.bottomMargin = Density.dp2px (this, 8f);
		contentView.setLayoutParams (params);
		dialog.getWindow ().setGravity (Gravity.CENTER);
		dialog.getWindow ().setWindowAnimations (R.style.BottomDialog_Animation);
		dialog.show ();

		//读取各种数据
		loadUserFromSQLite ();
		loadNoteFromSQLite ();
		//如果发现有用户，这个用户是有头像的，文件又没有头像就去服务器看一看
		//说不定是缓存被清空了
		if (hasUser && user.isHasAvatar () && !loadAvatarFromFile ())
			new Thread (new Runnable ()
			{
				@Override public void run ()
				{
					loadAvatarFromServer (); //会去UI主线程排队刷新头像显示
				}
			}).start ();
		else
			dialog.cancel ();
	}

	//6.0以上貌似要用户授权读写权限
	@Override
	public void onRequestPermissionsResult (int requestCode,
			@NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult (requestCode, permissions, grantResults);
		if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
			FileHelper.createDir (this);
		else if (requestCode == 1 && grantResults[0] ==PackageManager.PERMISSION_DENIED)
		{
			Dialog dialog = new Dialog (this, R.style.BottomDialog);
			View contentView = LayoutInflater.from (this).inflate
					(R.layout.dialog_no_sdcard_or_no_permission, null);
			TextView textView = contentView.findViewById (R.id.exit);
			textView.setBackgroundResource (Values.getSelector ());
			//没有读写权限就不能用了
			textView.setOnClickListener (new View.OnClickListener ()
			{
				@Override public void onClick (View v)
				{
					finish ();
				}
			});
			dialog.setContentView (contentView);
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
					contentView.getLayoutParams ();
			params.width = getResources ().getDisplayMetrics ().widthPixels
					- Density.dp2px (this, 16f);
			params.bottomMargin = Density.dp2px (this, 8f);
			contentView.setLayoutParams (params);
			dialog.getWindow ().setGravity (Gravity.CENTER);
			dialog.getWindow ().setWindowAnimations (R.style.BottomDialog_Animation);
			dialog.setCancelable (false);//禁止外部点击关闭
			dialog.show ();
		}
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
						//数据已经持久化保存，只需要刷新显示
						user = User.getUser ();
						if (user.getAvatar () != null)
							((ImageView) findViewById (R.id.imageViewAvatar))
									.setImageBitmap (user.getAvatar ());
						((TextView) findViewById (R.id.textViewName)).setText (user.getName ());
						findViewById (R.id.fabSync).setVisibility (View.VISIBLE);
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
							((ImageView) findViewById (R.id.imageViewAvatar))
									.setImageBitmap (user.getAvatar ());
							Values.setChangeAvatar (false);
						}
						if (Values.isChangeName ())
						{
							((TextView) findViewById (R.id.textViewName))
									.setText (user.getName ());
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
						clearAvatarInFile ();
						Intent intent = new Intent (this, SignInActivity.class);
						intent.putExtra ("preAccount", preAccount);
						startActivityForResult (intent, Values.REQ_SIGN_IN);
						break;
					case Values.RES_SIGN_OUT:
						//退出
						clearUserInSQLite ();
						clearUserInMemory ();
						clearAvatarInFile ();
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
	private void loadSettingFromSQLite ()
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
	private void loadUserFromSQLite ()
	{
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		Cursor cursor = db.rawQuery ("select * from user where account != \"0\"",
				null);

		if (cursor.getCount () == 1)
		{
			//cursor默认在第一个之前的位置
			cursor.moveToNext ();
			writeUserToMemory (cursor.getString (cursor.getColumnIndex ("account")),
					cursor.getString (cursor.getColumnIndex ("name")),
					cursor.getString (cursor.getColumnIndex ("avatar")));
		}
		cursor.close ();
		db.close ();
	}

	/**
	 * 写入内存用户
	 */
	private void writeUserToMemory (String account, String name, String avatar)
	{
		user = User.getUser ();
		user.signIn (account, name);
		hasUser = true;
		user.setHasAvatar (avatar.equals ("1"));
		((TextView) findViewById (R.id.textViewName)).setText (user.getName ());
		findViewById (R.id.fabSync).setVisibility (View.VISIBLE);
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
		db.execSQL ("insert into user values (\"0\",\"" + user.getAccount () + "\",\"0\")");
		db.close ();
	}

	/**
	 * 清空内存用户
	 */
	private void clearUserInMemory ()
	{
		User.signOut ();
		hasUser = false;
		((TextView) findViewById (R.id.textViewName)).setText ("登录了可以上传云哦");
		findViewById (R.id.fabSync).setVisibility (View.INVISIBLE);
	}

	/**
	 * 读取文件头像
	 */
	private boolean loadAvatarFromFile ()
	{
		File file = new File (getExternalFilesDir (Environment.DIRECTORY_DCIM),
				"avatar.jpg");
		if (file.exists ())
		{
			writeAvatarToMemory (BitmapFactory.decodeFile (file.getPath ()));
			((ImageView) findViewById (R.id.imageViewAvatar))
					.setImageBitmap (user.getAvatar ());
			return true;
		}
		return false;
	}

	/**
	 * 写入内存头像
	 */
	private void writeAvatarToMemory (Bitmap avatar)
	{
		user.setAvatar (avatar);
	}

	/**
	 * 清空文件头像
	 */
	private void clearAvatarInFile ()
	{
		File file = new File (getExternalFilesDir (Environment.DIRECTORY_DCIM),
				"avatar.jpg");
		FileHelper.deleteFile (file);
	}

	/**
	 * 读取服务器头像
	 */
	private void loadAvatarFromServer ()
	{
		Retrofit retrofit = RetrofitHelper.getRetrofit ();
		RequestServes requestServes = retrofit.create (RequestServes.class);
		Call<String> call = requestServes.getAvatar (user.getAccount ());
		call.enqueue (new Callback<String> ()
		{
			@Override public void onResponse (Call<String> call,
					Response<String> response)
			{
				switch (response.body ())
				{
					case ":-1":
						Log.d ("获取头像", "失败");
						Toast.makeText (MainActivity.this,
								"头像被外星人带走了", Toast.LENGTH_SHORT).show ();
						break;
					default:
						Log.d ("获取头像", "成功");
						String avatar = response.body ();

						//把拿到的base64转为Bitmap
						Bitmap bitmap = FileHelper.String2Bitmap (avatar);
						if (bitmap != null)
						{
							//加进去UI主线程排队，刷新头像显示
							uiHandler.post (new Runnable ()
							{
								@Override public void run ()
								{
									((ImageView) findViewById (R.id.imageViewAvatar))
											.setImageBitmap (user.getAvatar ());
								}
							});
							writeAvatarToMemory (bitmap);
							writeAvatarToFile (bitmap);
						}
				}
				dialog.cancel ();
				call.cancel ();
			}

			//超时未回应也会进入这个函数
			@Override public void onFailure (Call<String> call, Throwable t)
			{
				Log.d ("获取头像", t.toString ());
				Toast.makeText (MainActivity.this,
						"服务器在维护啦", Toast.LENGTH_SHORT).show ();
				dialog.cancel ();
				call.cancel ();
			}
		});
	}

	/**
	 * 写入文件头像
	 */
	private void writeAvatarToFile (Bitmap bitmap)
	{
		FileHelper.Bitmap2File (bitmap, getExternalFilesDir (Environment.DIRECTORY_DCIM)
				.getPath (), "avatar.jpg");
	}

	/**
	 * 读取SQLite笔记
	 */
	private void loadNoteFromSQLite ()
	{

	}
}
