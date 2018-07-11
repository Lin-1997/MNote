package com.lin.mnote;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lin.bean.Note;
import com.lin.bean.User;
import com.lin.utils.ArcMenu;
import com.lin.utils.Density;
import com.lin.utils.DialogToast;
import com.lin.utils.FileHelper;
import com.lin.utils.MyAdapter;
import com.lin.utils.MyGuideline;
import com.lin.utils.NetworkDetector;
import com.lin.utils.RequestServes;
import com.lin.utils.RetrofitHelper;
import com.lin.utils.SQLiteHelper;
import com.lin.utils.Values;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity
{
	private SQLiteHelper helper;
	private User user;
	private boolean hasUser = false;
	private boolean hasNote;
	private Dialog dialog;
	private Dialog dialogProgressBar;

	private RecyclerView recyclerView;
	private MyAdapter myAdapter;
	private ArcMenu arcMenu;

	private boolean longClick = false;
	private boolean runningAnimation = false;
	private TextView textViewDay;
	private ImageView imageViewLine;
	private TextView textViewMonth;
	private TextView textViewDescription;
	private MyGuideline GLMid;
	private MyGuideline GLRight;
	private FloatingActionButton fabClear;

	//左移动画
	private Animation leftShift;
	//右移动画
	private Animation rightShift;
	//渐变放大动画
	private AnimationSet scaleBig;
	//渐变缩小动画
	private AnimationSet scaleSmall;
	//撤回时的渐变放大动画
	private AnimationSet retractScaleBig;
	//撤回时的渐变缩小动画
	private AnimationSet retractScaleSmall;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		checkSdcard ();

		//设置主题要在setContentView之前
		loadSettingFromSQLite ();
		setTheme (Values.THEME);

		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_main);

		//初始化各种布局，控件
		init ();
		loadUserFromSQLite ();
		loadNoteFromSQLite ();
		//如果发现有用户，这个用户是有头像的，文件又没有头像就去服务器看一看
		//说不定是缓存被清空了
		if (hasUser && user.isHasAvatar () && !loadAvatarFromFile ())
			new Thread (new Runnable ()
			{
				@Override public void run ()
				{
					//会去UI主线程排队刷新头像显示
					loadAvatarFromServer ();
				}
			}).start ();

		onNoteChange ();

		//这东西不能在XML上设置click，高版本安卓会报错
		CircleImageView circleImageView = findViewById (R.id.imageViewAvatar);
		circleImageView.setOnClickListener (new View.OnClickListener ()
		{
			@Override public void onClick (View v)
			{
				imageViewAvatar (v);
			}
		});
	}

	@Override
	protected void onResume ()
	{
		super.onResume ();
		if (longClick)
		{
			longClick = false;
			textViewDay.setVisibility (View.VISIBLE);
			imageViewLine.setVisibility (View.VISIBLE);
			textViewMonth.setVisibility (View.VISIBLE);
			GLMid.setGuidelinePercent (0.2f);
			GLRight.setGuidelinePercent (1f);
			fabClear.setVisibility (View.INVISIBLE);
		}
	}

	//6.0以上貌似要用户授权读写权限
	@Override
	public void onRequestPermissionsResult (int requestCode,
			@NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult (requestCode, permissions, grantResults);
		//授权了权限
		if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
			FileHelper.createDir (this);
			//没有授权权限
		else if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED)
		{
			//自定义风格对话框
			Dialog dialog = new Dialog (this, R.style.BottomDialog);
			//获取对话框将要装载的内容
			View contentView = LayoutInflater.from (this).inflate
					(R.layout.dialog_no_sdcard_or_no_permission, null);

			//设置主题
			TextView textView = contentView.findViewById (R.id.exit);
			textView.setBackgroundResource (Values.SELECTOR);

			//设置点击事件
			textView.setOnClickListener (new View.OnClickListener ()
			{
				//没有读写权限就不能用了
				@Override public void onClick (View v)
				{
					finish ();
				}
			});

			//装载内容
			dialog.setContentView (contentView);
			//调节大小
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
					contentView.getLayoutParams ();
			params.width = getResources ().getDisplayMetrics ().widthPixels
					- Density.dp2px (this, 16f);
			params.bottomMargin = Density.dp2px (this, 8f);
			contentView.setLayoutParams (params);
			dialog.getWindow ().setGravity (Gravity.CENTER);
			dialog.getWindow ().setWindowAnimations (R.style.BottomDialog_Animation);
			//禁止外部点击关闭
			dialog.setCancelable (false);
			dialog.show ();
		}
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case Values.REQ_SIGN_IN:
				switch (resultCode)
				{
					case Values.RES_SIGN_IN:
						//数据已经持久化保存，只需要刷新显示
						user = User.getUser ();
						hasUser = true;
						if (user.getAvatar () != null)
							((ImageView) findViewById (R.id.imageViewAvatar))
									.setImageBitmap (user.getAvatar ());
						((TextView) findViewById (R.id.textViewName)).setText (user.getName ());
						Intent intent = new Intent (this, UserCenterActivity.class);
						startActivityForResult (intent, Values.REQ_USER_CENTER);
						break;
				}
				break;
			case Values.REQ_USER_CENTER:
				switch (resultCode)
				{
					//这里包括修改头像，昵称
					case Values.RES_CHANGE_SOMETHING:
						//数据已经持久化保存，只需要刷新显示
						if (Values.CHANGE_AVATAR)
						{
							((ImageView) findViewById (R.id.imageViewAvatar))
									.setImageBitmap (user.getAvatar ());
							Values.CHANGE_AVATAR = false;
						}
						if (Values.CHANGE_NAME)
						{
							((TextView) findViewById (R.id.textViewName))
									.setText (user.getName ());
							Values.CHANGE_NAME = false;
						}
						break;
					case Values.RES_CHANGE_THEME:
						//重新加载Activity
						recreate ();
						break;
					case Values.RES_CHANGE_PASSWORD:
						//修改密码，转到登录界面
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
			case Values.REQ_EDIT:
				switch (resultCode)
				{
					case Values.RES_EDIT_SAVE:
						//旧的位置
						int position = data.getIntExtra ("position", -1);
						//如果是新建的id暂时为-1，会在writeNoteToSQLite (id, status)中修改
						int id = myAdapter.getId (position);
						//status设置为-1，代表新建操作未上传，设置为1，代表更新操作未上传
						int status = myAdapter.getStatus (position) == -1 ? -1 : 1;
						if (id != -1) //不是新建，先删除旧的
							myAdapter.del (position);
						myAdapter.add (id, data.getStringExtra ("content"), status);
						recyclerView.scrollToPosition (0);
						writeNoteToSQLite (id, status);
						if (!hasNote)
						{
							hasNote = true;
							onNoteChange ();
						}
						break;
				}
				break;
		}
		super.onActivityResult (requestCode, resultCode, data);
	}

	private void checkSdcard ()
	{
		if (FileHelper.hasSdcard ()) //6.0以上貌似要用户动态授权读写权限
			FileHelper.verifyStoragePermissions (this);
		else
		{
			//没有SD卡
			Dialog dialog = new Dialog (this, R.style.BottomDialog);
			View contentView = LayoutInflater.from (this).inflate
					(R.layout.dialog_no_sdcard_or_no_permission, null);

			TextView textView = contentView.findViewById (R.id.exit);
			textView.setBackgroundResource (Values.SELECTOR);

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
			dialog.setCancelable (false);
			dialog.show ();
		}
	}

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
		switch (cursor.getString (0))
		{
			case "1":
				Values.THEME = R.style.MyBlueTheme;
				Values.COLOR = R.color.colorBlue;
				Values.SELECTOR = R.drawable.selector_blue;
				Values.BACKGROUND = R.drawable.button_blue;
				Values.PROGRESS = R.drawable.progress_bar_blue;
				break;
			case "2":
				Values.THEME = R.style.MyGrayTheme;
				Values.COLOR = R.color.colorGray;
				Values.SELECTOR = R.drawable.selector_gray;
				Values.BACKGROUND = R.drawable.button_gray;
				Values.PROGRESS = R.drawable.progress_bar_gray;
				break;
			case "3":
				Values.THEME = R.style.MyRedTheme;
				Values.COLOR = R.color.colorRed;
				Values.SELECTOR = R.drawable.selector_red;
				Values.BACKGROUND = R.drawable.button_red;
				Values.PROGRESS = R.drawable.progress_bar_red;
				break;
			case "4":
				Values.THEME = R.style.MyCyanTheme;
				Values.COLOR = R.color.colorCyan;
				Values.SELECTOR = R.drawable.selector_cyan;
				Values.BACKGROUND = R.drawable.button_cyan;
				Values.PROGRESS = R.drawable.progress_bar_cyan;
				break;
			case "5":
				Values.THEME = R.style.MyGreenTheme;
				Values.COLOR = R.color.colorGreen;
				Values.SELECTOR = R.drawable.selector_green;
				Values.BACKGROUND = R.drawable.button_green;
				Values.PROGRESS = R.drawable.progress_bar_green;
				break;
			case "6":
				Values.THEME = R.style.MyOrangeTheme;
				Values.COLOR = R.color.colorOrange;
				Values.SELECTOR = R.drawable.selector_orange;
				Values.BACKGROUND = R.drawable.button_orange;
				Values.PROGRESS = R.drawable.progress_bar_orange;
				break;
			case "7":
				Values.THEME = R.style.MyYellowTheme;
				Values.COLOR = R.color.colorYellow;
				Values.SELECTOR = R.drawable.selector_yellow;
				Values.BACKGROUND = R.drawable.button_yellow;
				Values.PROGRESS = R.drawable.progress_bar_yellow;
				break;
			case "8":
				Values.THEME = R.style.MyPinkTheme;
				Values.COLOR = R.color.colorPink;
				Values.SELECTOR = R.drawable.selector_pink;
				Values.BACKGROUND = R.drawable.button_pink;
				Values.PROGRESS = R.drawable.progress_bar_pink;
				break;
			case "9":
				Values.THEME = R.style.MyPurpleTheme;
				Values.COLOR = R.color.colorPurple;
				Values.SELECTOR = R.drawable.selector_purple;
				Values.BACKGROUND = R.drawable.button_purple;
				Values.PROGRESS = R.drawable.progress_bar_purple;
				break;
		}

		cursor.close ();
		db.close ();
	}

	private void loadUserFromSQLite ()
	{
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		Cursor cursor = db.rawQuery ("select * from user where account!=\"0\"",
				null);

		if (cursor.getCount () == 1)
		{
			//cursor默认在第一个之前的位置
			cursor.moveToNext ();
			writeUserToMemory (cursor.getString (0),
					cursor.getString (1), cursor.getString (2));
		}
		cursor.close ();
		db.close ();
	}

	private void writeUserToMemory (String account, String name, String avatar)
	{
		user = User.getUser ();
		user.signIn (account, name);
		hasUser = true;
		user.setHasAvatar (avatar.equals ("1"));
		((TextView) findViewById (R.id.textViewName)).setText (user.getName ());
	}

	private boolean loadAvatarFromFile ()
	{
		File file = new File (getExternalFilesDir (Environment.DIRECTORY_DCIM),
				"avatar.jpg");
		if (file.exists ())
		{
			Bitmap bitmap = BitmapFactory.decodeFile (file.getPath ());
			writeAvatarToMemory (bitmap);
			((ImageView) findViewById (R.id.imageViewAvatar)).setImageBitmap (bitmap);
			return true;
		}
		return false;
	}

	private void clearUserInSQLite ()
	{
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		db.execSQL ("begin");
		db.execSQL ("delete from user");
		//记录上次的登录账号在name列
		SQLiteStatement sqLiteStatement = db.compileStatement
				("insert into user values (\"0\",?,\"0\")");
		sqLiteStatement.bindString (1, user.getAccount ());
		sqLiteStatement.executeInsert ();
		db.execSQL ("commit");
		db.close ();
	}

	private void clearUserInMemory ()
	{
		User.signOut ();
		hasUser = false;
		((TextView) findViewById (R.id.textViewName)).setText ("登录开启新世界");
		((ImageView) findViewById (R.id.imageViewAvatar))
				.setImageResource (R.drawable.ic_avatar);
	}

	private void writeAvatarToMemory (Bitmap avatar)
	{
		user.setAvatar (avatar);
	}

	private void clearAvatarInFile ()
	{
		File file = new File (getExternalFilesDir (Environment.DIRECTORY_DCIM),
				"avatar.jpg");
		FileHelper.deleteFile (file);
	}

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
						DialogToast.showDialogToast (MainActivity.this, "头像被外星人带走了");
						break;
					default:
						String avatar = response.body ();

						//把拿到的base64转为Bitmap
						final Bitmap bitmap = FileHelper.String2Bitmap (avatar);
						if (bitmap != null)
						{
							//加进去UI主线程排队，刷新头像显示
							runOnUiThread (new Runnable ()
							{
								@Override
								public void run ()
								{
									((ImageView) findViewById (R.id.imageViewAvatar))
											.setImageBitmap (bitmap);
								}
							});

							writeAvatarToMemory (bitmap);
							writeAvatarToFile (bitmap);
						}
				}
				call.cancel ();
			}

			//超时未回应也会进入这个函数
			@Override public void onFailure (Call<String> call, Throwable t)
			{
				DialogToast.showDialogToast (MainActivity.this, "服务器在维护啦");
				call.cancel ();
			}
		});
	}

	private void writeAvatarToFile (Bitmap bitmap)
	{
		FileHelper.Bitmap2File (bitmap, getExternalFilesDir (Environment.DIRECTORY_DCIM)
				.getPath (), "avatar.jpg");
	}

	private void loadNoteFromSQLite ()
	{
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		Cursor cursor = db.rawQuery ("select * from note order by " +
				"date(updateDate),time(updateTime) desc", null);
		if (cursor.getCount () == 0)
		{
			hasNote = false;
			cursor.close ();
			db.close ();
		}
		else
		{
			Note note;
			while (cursor.moveToNext ())
			{
				note = new Note (
						cursor.getInt (0), cursor.getString (1),
						cursor.getString (2), cursor.getString (3),
						cursor.getInt (4));
				myAdapter.add (note);
			}
			recyclerView.scrollToPosition (0);
			hasNote = true;
			onNoteChange ();
		}
		cursor.close ();
		db.close ();
	}

	private void writeNoteToSQLite (int id, int status)
	{
		//拿到下标为0的note
		Note note = myAdapter.getNote (0);
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		db.execSQL ("begin");
		//新建的
		if (id == -1)
		{
			SQLiteStatement sqLiteStatement = db.compileStatement
					("insert into note(updateDate,updateTime,content,status) " +
							"values(?,?,?,\"-1\")");
			String[] params = {note.getUpdateDate (), note.getUpdateTime (),
					note.getContent ()};
			sqLiteStatement.bindAllArgsAsStrings (params);
			sqLiteStatement.executeInsert ();
			Cursor cursor = db.rawQuery ("select last_insert_rowid()", null);
			cursor.moveToNext ();
			int newId = cursor.getInt (0);
			//为下标为0的note更新id
			note.setId (newId);
			cursor.close ();
		}
		//更新的
		else
		{
			//防SQL注入
			SQLiteStatement sqLiteStatement = db.compileStatement
					("update note set updateDate=?,updateTime=?,content=?,status=? "
							+ "where id=?");
			String[] params = {note.getUpdateDate (), note.getUpdateTime (),
					note.getContent (), status + "", id + ""};
			sqLiteStatement.bindAllArgsAsStrings (params);
			sqLiteStatement.executeUpdateDelete ();
		}
		db.execSQL ("commit");
		db.close ();
	}

	private void downloadNoteToSQLite ()
	{
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		db.execSQL ("begin");
		//删除旧的
		db.execSQL ("delete from note");
		//重置autoincrement的值
		db.execSQL ("update sqlite_sequence set seq=1 where name=\"note\"");
		//删除未上传的记录
		db.execSQL ("delete from deleteLog");
		StringBuilder stringBuilder = new StringBuilder ("insert into note values");
		List<Note> notes = myAdapter.getDownloadNote ();
		int size = notes.size ();
		String[] params = new String[size];
		for (int i = 0; i < size; ++i)
		{
			stringBuilder.append ("(\"").append (notes.get (i).getId ()).append ("\",\"")
					.append (notes.get (i).getUpdateDate ()).append ("\",\"")
					.append (notes.get (i).getUpdateTime ()).append ("\",?,\"0\"),");
			params[i] = notes.get (i).getContent ();
		}
		String sql = stringBuilder.substring (0, stringBuilder.length () - 1);
		sql += ";";
		SQLiteStatement sqLiteStatement = db.compileStatement (sql);
		sqLiteStatement.bindAllArgsAsStrings (params);
		sqLiteStatement.executeInsert ();
		db.execSQL ("commit");
		myAdapter.downloadDone ();
		db.close ();
	}

	private void deleteNoteFromSQLite (int id, int status)
	{
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		db.execSQL ("begin");
		SQLiteStatement sqLiteStatement = db.compileStatement
				("delete from note where id=?");
		sqLiteStatement.bindString (1, id + "");
		sqLiteStatement.executeUpdateDelete ();
		//只要不是新建操作的未上传的，服务器就有一个对应的note
		//没登录就不用在意了
		if (status != -1 && hasUser)
		{
			myAdapter.setDeleteNote (true);
			sqLiteStatement = db.compileStatement ("insert into deleteLog values(?)");
			sqLiteStatement.bindString (1, id + "");
			sqLiteStatement.executeInsert ();
		}
		db.execSQL ("commit");
		db.close ();
	}

	private void init ()
	{
		initArcMenu ();
		initMyAdapter ();
		intiRecyclerView ();
	}

	private void initArcMenu ()
	{
		arcMenu = findViewById (R.id.arcMenu);
		//设置主菜单
		arcMenu.setMenu (findViewById (R.id.fabMenu));
		//设置主菜单点击事件
		arcMenu.setOnMenuClickListener (new ArcMenu.onMenuClickListener ()
		{
			@Override public void onClick (View view)
			{
				if (longClick)
					retractLongClick ();
			}
		});
		//设置子菜单点击事件，所有子菜单共用一个点击事件，要手动区分子菜单
		arcMenu.setOnMenuItemClickListener (new ArcMenu.onMenuItemClickListener ()
		{
			@Override public void onClick (View view)
			{
				switch (view.getId ())
				{
					case R.id.fabUpload:
						fabUpload ();
						break;
					case R.id.fabCreate:
						fabCreate (view);
						break;
					case R.id.fabDownload:
						fabDownload ();
						break;
				}
			}
		});
	}

	private void initMyAdapter ()
	{
		myAdapter = new MyAdapter (this);
		myAdapter.setOnItemClickListener (new MyAdapter.OnItemClickListener ()
		{
			@Override public void onItemClick (View view, int position)
			{
				if (runningAnimation)
					return;
				if (longClick)
					retractLongClick ();
				else
				{
					Intent intent = new Intent (MainActivity.this, ContentActivity.class);
					intent.putExtra ("content", myAdapter.getContent (position));
					intent.putExtra ("date", myAdapter.getUpdateDate (position));
					intent.putExtra ("time", myAdapter.getUpdateTime (position));
					intent.putExtra ("position", position);
					startActivityForResult (intent, Values.REQ_EDIT);
				}
			}

			@Override public void onItemLongClick (View view, final int position)
			{
				if (runningAnimation)
					return;
				if (arcMenu.isOpened ())
					findViewById (R.id.fabMenu).performClick ();
				if (longClick)
					retractAndNewLongClick (view, position);
				else
					longClick (view, position);
			}
		});
	}

	@SuppressLint ("ClickableViewAccessibility")
	private void intiRecyclerView ()
	{
		recyclerView = findViewById (R.id.recyclerView);
		//点击空白处撤回长按
		recyclerView.setOnTouchListener (new View.OnTouchListener ()
		{
			@Override public boolean onTouch (View v, MotionEvent event)
			{
				if (!runningAnimation && longClick)
					retractLongClick ();
				return false;
			}
		});

		//设置数据适配器
		recyclerView.setAdapter (myAdapter);
		//设置布局管理
		recyclerView.setLayoutManager (new LinearLayoutManager (this));
		//设置Item之间的分割线
		recyclerView.addItemDecoration (new DividerItemDecoration
				(this, DividerItemDecoration.VERTICAL));
		//设置添加和删除的动画
		recyclerView.setItemAnimator (new DefaultItemAnimator ());
		//设置滚动监听
		recyclerView.addOnScrollListener (new RecyclerView.OnScrollListener ()
		{
			@Override public void onScrollStateChanged (RecyclerView recyclerView, int newState)
			{
				super.onScrollStateChanged (recyclerView, newState);
				if (newState == RecyclerView.SCROLL_STATE_DRAGGING)
				{
					if (arcMenu.isOpened ())
						findViewById (R.id.fabMenu).performClick ();
					if (longClick)
						retractLongClick ();
				}
			}

			@Override public void onScrolled (RecyclerView recyclerView, int dx, int dy)
			{
				super.onScrolled (recyclerView, dx, dy);
			}
		});
	}

	private void longClick (View view, final int position)
	{
		longClick = true;

		textViewDay = view.findViewById (R.id.textViewDay);
		imageViewLine = view.findViewById (R.id.imageViewLine);
		textViewMonth = view.findViewById (R.id.textViewMonth);
		textViewDescription = view.findViewById (R.id.textViewDescription);
		GLMid = view.findViewById (R.id.GLMid);
		GLRight = view.findViewById (R.id.GLRight);
		fabClear = view.findViewById (R.id.fabClear);

		if (scaleSmall == null)
		{
			scaleSmall = (AnimationSet) AnimationUtils.loadAnimation
					(this, R.anim.scale_small);
			scaleSmall.setFillAfter (false);
		}
		scaleSmall.setAnimationListener (new Animation.AnimationListener ()
		{
			@Override public void onAnimationStart (Animation animation)
			{
				runningAnimation = true;
				textViewDay.setVisibility (View.INVISIBLE);
				imageViewLine.setVisibility (View.INVISIBLE);
				textViewMonth.setVisibility (View.INVISIBLE);
			}

			@Override public void onAnimationEnd (Animation animation)
			{
				runningAnimation = false;
			}

			@Override public void onAnimationRepeat (Animation animation) { }
		});

		if (leftShift == null)
		{
			int distance = findViewById (R.id.constraintLayout).getWidth ();
			distance = (int) -(distance * 0.15);
			leftShift = new TranslateAnimation (0, distance,
					0, 0);
			leftShift.setFillAfter (false);
			leftShift.setDuration (Values.duration);
		}
		leftShift.setAnimationListener (new Animation.AnimationListener ()
		{
			@Override public void onAnimationStart (Animation animation)
			{
				runningAnimation = true;
			}

			@Override public void onAnimationEnd (Animation animation)
			{
				GLMid.setGuidelinePercent (0.05f);
				GLRight.setGuidelinePercent (0.85f);
				runningAnimation = false;
				//消除结束动画后的闪烁
				textViewDescription.clearAnimation ();
			}

			@Override public void onAnimationRepeat (Animation animation) { }
		});

		if (scaleBig == null)
		{
			scaleBig = (AnimationSet) AnimationUtils.loadAnimation
					(this, R.anim.scale_big);
			scaleBig.setFillAfter (false);
		}
		scaleBig.setAnimationListener (new Animation.AnimationListener ()
		{
			@Override public void onAnimationStart (Animation animation)
			{
				runningAnimation = true;
			}

			@Override public void onAnimationEnd (Animation animation)
			{
				fabClear.setVisibility (View.VISIBLE);
				fabClear.setOnClickListener (new View.OnClickListener ()
				{
					@Override public void onClick (View v)
					{
						//不用动画了
						longClick = false;
						textViewDay.setVisibility (View.VISIBLE);
						imageViewLine.setVisibility (View.VISIBLE);
						textViewMonth.setVisibility (View.VISIBLE);
						GLMid.setGuidelinePercent (0.2f);
						GLRight.setGuidelinePercent (1f);
						fabClear.setVisibility (View.INVISIBLE);
						int id = myAdapter.getId (position);
						int status = myAdapter.getStatus (position);
						myAdapter.del (position);
						deleteNoteFromSQLite (id, status);
						if (myAdapter.getItemCount () == 0)
						{
							hasNote = false;
							onNoteChange ();
						}
					}
				});
				runningAnimation = false;
			}

			@Override public void onAnimationRepeat (Animation animation) { }
		});

		textViewDay.startAnimation (scaleSmall);
		imageViewLine.startAnimation (scaleSmall);
		textViewMonth.startAnimation (scaleSmall);
		textViewDescription.startAnimation (leftShift);
		fabClear.startAnimation (scaleBig);
	}

	private void retractLongClick ()
	{
		longClick = false;

		scaleBig.setAnimationListener (new Animation.AnimationListener ()
		{
			@Override public void onAnimationStart (Animation animation)
			{
				runningAnimation = true;
			}

			@Override public void onAnimationEnd (Animation animation)
			{
				textViewDay.setVisibility (View.VISIBLE);
				imageViewLine.setVisibility (View.VISIBLE);
				textViewMonth.setVisibility (View.VISIBLE);
				runningAnimation = false;
			}

			@Override public void onAnimationRepeat (Animation animation) { }
		});

		if (rightShift == null)
		{
			int distance = findViewById (R.id.constraintLayout).getWidth ();
			distance = (int) (distance * 0.15);
			rightShift = new TranslateAnimation (0, distance,
					0, 0);
			rightShift.setFillAfter (false);
			rightShift.setDuration (Values.duration);
		}
		rightShift.setAnimationListener (new Animation.AnimationListener ()
		{
			@Override public void onAnimationStart (Animation animation)
			{
				runningAnimation = true;
			}

			@Override public void onAnimationEnd (Animation animation)
			{
				GLMid.setGuidelinePercent (0.2f);
				GLRight.setGuidelinePercent (1f);
				runningAnimation = false;
				//消除结束动画后的闪烁
				textViewDescription.clearAnimation ();
			}

			@Override public void onAnimationRepeat (Animation animation) { }
		});

		scaleSmall.setAnimationListener (new Animation.AnimationListener ()
		{
			@Override public void onAnimationStart (Animation animation)
			{
				runningAnimation = true;
				fabClear.setVisibility (View.INVISIBLE);
			}

			@Override public void onAnimationEnd (Animation animation)
			{
				runningAnimation = false;
			}

			@Override public void onAnimationRepeat (Animation animation) { }
		});

		textViewDay.startAnimation (scaleBig);
		imageViewLine.startAnimation (scaleBig);
		textViewMonth.startAnimation (scaleBig);
		textViewDescription.startAnimation (rightShift);
		fabClear.startAnimation (scaleSmall);
	}

	private void retractAndNewLongClick (View view, int position)
	{
		//获取上一次长按的View
		final TextView day = textViewDay;

		//如果是在同一个View上长按，直接撤回
		if (view.findViewById (R.id.textViewDay) == day)
		{
			retractLongClick ();
			return;
		}

		final ImageView line = imageViewLine;
		final TextView month = textViewMonth;
		final TextView description = textViewDescription;
		final FloatingActionButton clear = fabClear;
		final MyGuideline mid = GLMid;
		final MyGuideline right = GLRight;

		if (retractScaleBig == null)
		{
			retractScaleBig = (AnimationSet) AnimationUtils.loadAnimation
					(this, R.anim.scale_big);
			retractScaleBig.setFillAfter (false);
		}
		retractScaleBig.setAnimationListener (new Animation.AnimationListener ()
		{
			@Override public void onAnimationStart (Animation animation)
			{
				runningAnimation = true;
			}

			@Override public void onAnimationEnd (Animation animation)
			{
				day.setVisibility (View.VISIBLE);
				line.setVisibility (View.VISIBLE);
				month.setVisibility (View.VISIBLE);
				runningAnimation = false;
			}

			@Override public void onAnimationRepeat (Animation animation) { }
		});

		if (rightShift == null)
		{
			int distance = findViewById (R.id.constraintLayout).getWidth ();
			distance = (int) (distance * 0.15);
			rightShift = new TranslateAnimation (0, distance,
					0, 0);
			rightShift.setFillAfter (false);
			rightShift.setDuration (Values.duration);
		}
		rightShift.setAnimationListener (new Animation.AnimationListener ()
		{
			@Override public void onAnimationStart (Animation animation)
			{
				runningAnimation = true;
			}

			@Override public void onAnimationEnd (Animation animation)
			{
				mid.setGuidelinePercent (0.2f);
				right.setGuidelinePercent (1f);
				runningAnimation = false;
				//消除结束动画后的闪烁
				description.clearAnimation ();
			}

			@Override public void onAnimationRepeat (Animation animation) { }
		});

		if (retractScaleSmall == null)
		{
			retractScaleSmall = (AnimationSet) AnimationUtils.loadAnimation
					(this, R.anim.scale_small);
			retractScaleSmall.setFillAfter (false);
		}
		retractScaleSmall.setAnimationListener (new Animation.AnimationListener ()
		{
			@Override public void onAnimationStart (Animation animation)
			{
				runningAnimation = true;
				clear.setVisibility (View.INVISIBLE);
			}

			@Override public void onAnimationEnd (Animation animation)
			{
				runningAnimation = false;
			}

			@Override public void onAnimationRepeat (Animation animation) { }
		});

		day.startAnimation (retractScaleBig);
		line.startAnimation (retractScaleBig);
		month.startAnimation (retractScaleBig);
		description.startAnimation (rightShift);
		clear.startAnimation (retractScaleSmall);

		longClick (view, position);
	}

	private void onNoteChange ()
	{
		//如果没有任何笔记就加载欢迎界面
		if (!hasNote)
		{
			TextView textView = findViewById (R.id.textView);
			textView.setTextColor (getResources ().getColor (Values.COLOR));
			textView.setVisibility (View.VISIBLE);
			recyclerView.setVisibility (View.GONE);
		}
		else
		{
			findViewById (R.id.textView).setVisibility (View.GONE);
			recyclerView.setVisibility (View.VISIBLE);
		}
	}

	private void fabUpload ()
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			DialogToast.showDialogToast (MainActivity.this, "网络开小差了");
			return;
		}
		if (!hasUser)
		{
			DialogToast.showDialogToast (MainActivity.this, "登录就能解锁这个功能了");
			return;
		}

		dialog = new Dialog (this, R.style.BottomDialog);
		View contentView = LayoutInflater.from (this)
				.inflate (R.layout.dialog_content_upload, null);

		TextView textView = contentView.findViewById (R.id.upload);
		textView.setBackgroundResource (Values.SELECTOR);
		textView.setOnClickListener (new View.OnClickListener ()
		{
			@Override public void onClick (View v)
			{
				upload ();
				dialog.cancel ();
			}
		});
		textView = contentView.findViewById (R.id.cancel);
		textView.setOnClickListener (new View.OnClickListener ()
		{
			@Override public void onClick (View v)
			{
				dialog.cancel ();
			}
		});

		dialog.setContentView (contentView);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
				contentView.getLayoutParams ();
		params.width = getResources ().getDisplayMetrics ().widthPixels
				- Density.dp2px (this, 16f);
		params.bottomMargin = Density.dp2px (this, 8f);
		contentView.setLayoutParams (params);
		dialog.getWindow ().setGravity (Gravity.BOTTOM);
		dialog.getWindow ().setWindowAnimations (R.style.BottomDialog_Animation);
		dialog.show ();
	}

	private void upload ()
	{
		final List<Note> noteList = myAdapter.getChangedNote ();
		//没有任何操作
		if (noteList.size () == 0 && !myAdapter.isDeleteNote ())
		{
			DialogToast.showDialogToast (MainActivity.this, "没有任何改动");
			dialog.cancel ();
			return;
		}

		if (dialogProgressBar == null)
		{
			dialogProgressBar = new Dialog (this, R.style.BottomDialog);
			View contentView = LayoutInflater.from (this).inflate
					(R.layout.dialog_progress_bar, null);

			ProgressBar progressBar = contentView.findViewById (R.id.progressBar);
			progressBar.setIndeterminateDrawable (getResources ().getDrawable (Values.PROGRESS));

			dialogProgressBar.setContentView (contentView);
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
					contentView.getLayoutParams ();
			params.width = getResources ().getDisplayMetrics ().widthPixels
					- Density.dp2px (this, 16f);
			params.bottomMargin = Density.dp2px (this, 8f);
			contentView.setLayoutParams (params);
			dialogProgressBar.getWindow ().setGravity (Gravity.CENTER);
			dialogProgressBar.getWindow ().setWindowAnimations (R.style.BottomDialog_Animation);
			dialogProgressBar.setCancelable (false);
		}
		dialogProgressBar.show ();

		helper = SQLiteHelper.getHelper (this);
		final Gson gson = new Gson ();
		final JsonObject jsonObject = new JsonObject ();
		jsonObject.addProperty ("account", user.getAccount ());
		jsonObject.addProperty ("noteSize", noteList.size ());
		for (int i = 0; i < noteList.size (); ++i)
		{
			jsonObject.addProperty ("id" + i, noteList.get (i).getId ());
			jsonObject.addProperty ("updateDate" + i, noteList.get (i).getUpdateDate ());
			jsonObject.addProperty ("updateTime" + i, noteList.get (i).getUpdateTime ());
			jsonObject.addProperty ("content" + i, noteList.get (i).getContent ());
			jsonObject.addProperty ("status" + i, noteList.get (i).getStatus ());
		}

		new Thread (new Runnable ()
		{
			@Override public void run ()
			{
				final SQLiteDatabase db = helper.getWritableDatabase ();
				Cursor cursor = db.rawQuery ("select * from deleteLog", null);
				if (cursor.getCount () != 0)
				{
					int i = 0;
					jsonObject.addProperty ("deleteLogSize", cursor.getCount ());
					while (cursor.moveToNext ())
					{
						jsonObject.addProperty ("deleteLog" + i,
								cursor.getInt (0));
						++i;
					}
				}
				else
					jsonObject.addProperty ("deleteLogSize", 0);
				cursor.close ();

				RequestBody body = RequestBody.create (MediaType
						.parse ("application/json; charset=utf-8"), gson.toJson (jsonObject));
				Retrofit retrofit = RetrofitHelper.getRetrofit ();
				RequestServes requestServes = retrofit.create (RequestServes.class);
				Call<String> call = requestServes.upload (body);
				call.enqueue (new Callback<String> ()
				{
					@Override public void onResponse (Call<String> call, Response<String> response)
					{
						switch (response.body ())
						{
							case ":-1":
								db.close ();
								dialogProgressBar.cancel ();
								DialogToast.showDialogToast (MainActivity.this, "数据被外星人带走了");
								break;
							case ":0":
								db.execSQL ("begin");
								db.execSQL ("delete from deleteLog");
								db.execSQL ("update note set status =\"0\"");
								db.execSQL ("commit");
								db.close ();
								myAdapter.uploadDone ();
								dialogProgressBar.cancel ();
								DialogToast.showDialogToast (MainActivity.this, "上传好了");
						}
						call.cancel ();
					}

					@Override public void onFailure (Call<String> call, Throwable t)
					{
						db.close ();
						dialogProgressBar.cancel ();
						DialogToast.showDialogToast (MainActivity.this, "服务器在维护啦");
						call.cancel ();
					}
				});
			}
		}).start ();
	}

	public void fabCreate (View view)
	{
		Intent intent = new Intent (this, ContentActivity.class);
		intent.putExtra ("content", "");
		intent.putExtra ("date", "");
		intent.putExtra ("time", "");
		startActivityForResult (intent, Values.REQ_EDIT);
	}

	private void fabDownload ()
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			DialogToast.showDialogToast (MainActivity.this, "网络开小差了");
			return;
		}
		if (!hasUser)
		{
			DialogToast.showDialogToast (MainActivity.this, "登录就能解锁这个功能了");
			return;
		}

		dialog = new Dialog (this, R.style.BottomDialog);
		View contentView = LayoutInflater.from (this)
				.inflate (R.layout.dialog_content_download, null);

		TextView textView = contentView.findViewById (R.id.download);
		textView.setBackgroundResource (Values.SELECTOR);
		textView.setOnClickListener (new View.OnClickListener ()
		{
			@Override public void onClick (View v)
			{
				download ();
				dialog.cancel ();
			}
		});
		textView = contentView.findViewById (R.id.cancel);
		textView.setOnClickListener (new View.OnClickListener ()
		{
			@Override public void onClick (View v)
			{
				dialog.cancel ();
			}
		});

		dialog.setContentView (contentView);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
				contentView.getLayoutParams ();
		params.width = getResources ().getDisplayMetrics ().widthPixels
				- Density.dp2px (this, 16f);
		params.bottomMargin = Density.dp2px (this, 8f);
		contentView.setLayoutParams (params);
		dialog.getWindow ().setGravity (Gravity.BOTTOM);
		dialog.getWindow ().setWindowAnimations (R.style.BottomDialog_Animation);
		dialog.show ();
	}

	public void download ()
	{
		if (dialogProgressBar == null)
		{
			dialogProgressBar = new Dialog (this, R.style.BottomDialog);
			View contentView = LayoutInflater.from (this).inflate
					(R.layout.dialog_progress_bar, null);

			ProgressBar progressBar = contentView.findViewById (R.id.progressBar);
			progressBar.setIndeterminateDrawable (getResources ().getDrawable (Values.PROGRESS));

			dialogProgressBar.setContentView (contentView);
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
					contentView.getLayoutParams ();
			params.width = getResources ().getDisplayMetrics ().widthPixels
					- Density.dp2px (this, 16f);
			params.bottomMargin = Density.dp2px (this, 8f);
			contentView.setLayoutParams (params);
			dialogProgressBar.getWindow ().setGravity (Gravity.CENTER);
			dialogProgressBar.getWindow ().setWindowAnimations (R.style.BottomDialog_Animation);
			dialogProgressBar.setCancelable (false);
		}
		dialogProgressBar.show ();

		new Thread (new Runnable ()
		{
			@Override public void run ()
			{
				Retrofit retrofit = RetrofitHelper.getRetrofit ();
				RequestServes requestServes = retrofit.create (RequestServes.class);
				Call<String> call = requestServes.download (user.getAccount ());
				call.enqueue (new Callback<String> ()
				{
					@Override public void onResponse (Call<String> call, Response<String> response)
					{
						switch (response.body ())
						{
							case ":-1":
								dialogProgressBar.cancel ();
								DialogToast.showDialogToast (MainActivity.this, "数据被外星人带走了");
								break;
							case ":0":
								dialogProgressBar.cancel ();
								DialogToast.showDialogToast (MainActivity.this, "服务器数据为空");
								break;
							default:
								int id;
								String updateDate = "", updateTime = "", content = "";
								try
								{
									myAdapter.downloadPrepare ();
									JSONObject jsonObject = new JSONObject (response.body ());
									int size = jsonObject.getInt ("size");
									for (int i = 0; i < size; ++i)
									{
										id = jsonObject.getInt ("id" + i);
										updateDate = jsonObject.getString ("updateDate" + i);
										updateTime = jsonObject.getString ("updateTime" + i);
										content = jsonObject.getString ("content" + i);
										myAdapter.add (new Note
												(id, updateDate, updateTime, content, 0));
									}
									hasNote = size != 0;
									onNoteChange ();
									downloadNoteToSQLite ();
									dialogProgressBar.cancel ();
									DialogToast.showDialogToast (MainActivity.this, "下载好了");
								}
								catch (JSONException e)
								{
									e.printStackTrace ();
								}
								break;
						}
					}

					@Override public void onFailure (Call<String> call, Throwable t)
					{
						dialogProgressBar.cancel ();
						DialogToast.showDialogToast (MainActivity.this, "服务器在维护啦");
						call.cancel ();
					}
				});
			}
		}).start ();
	}

	public void imageViewAvatar (View view)
	{

		//有用户进入个人中心
		if (hasUser)
		{
			Intent intent = new Intent (this, UserCenterActivity.class);
			startActivityForResult (intent, Values.REQ_USER_CENTER);
		}

		//没有用户进入登录界面
		else
		{
			Intent intent = new Intent (this, SignInActivity.class);

			//读取SQLite里面的记录，如果曾经登录过就加载以前的账号，方便登录
			helper = SQLiteHelper.getHelper (this);
			SQLiteDatabase db = helper.getWritableDatabase ();
			Cursor cursor = db.rawQuery ("select * from user where account = \"0\"",
					null);
			if (cursor.getCount () == 1)
			{
				//cursor默认在第一个之前的位置
				cursor.moveToNext ();
				intent.putExtra ("preAccount", cursor.getString (1));
			}
			else
				intent.putExtra ("preAccount", "");
			cursor.close ();
			db.close ();

			startActivityForResult (intent, Values.REQ_SIGN_IN);
		}
	}
}