package com.lin.mnote;

import android.app.Dialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lin.bean.User;
import com.lin.utils.Density;
import com.lin.utils.DialogToast;
import com.lin.utils.EditTextClear;
import com.lin.utils.FileHelper;
import com.lin.utils.NetworkDetector;
import com.lin.utils.RequestServes;
import com.lin.utils.RetrofitHelper;
import com.lin.utils.SQLiteHelper;
import com.lin.utils.Values;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UserCenterActivity extends AppCompatActivity
{
	private SQLiteHelper helper;
	private User user = User.getUser ();
	private Dialog dialog;
	private Dialog dialogProgressBar;

	//系统自带相机的存储地址
	Uri imageUri = Uri.fromFile (new File (Environment.getExternalStoragePublicDirectory
			(Environment.DIRECTORY_DCIM), "avatar.jpg"));

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_user_center);

		Toolbar toolbar = findViewById (R.id.toolbar);
		toolbar.setBackgroundResource (Values.COLOR);
		setSupportActionBar (toolbar);
		getSupportActionBar ().setDisplayHomeAsUpEnabled (true);

		//账号部分隐藏
		String string = "账号：" + user.getAccount ().substring (0, 3)
				+ "****" + user.getAccount ().substring (7);
		((TextView) findViewById (R.id.textViewAccount)).setText (string);

		loadView ();
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		switch (item.getItemId ())
		{
			case android.R.id.home:
				finish ();
				break;
		}
		return true;
	}

	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case Values.REQ_ACTION_IMAGE_CAPTURE: //拍照
				if (resultCode == RESULT_OK)
					startPhotoZoom (imageUri);
				break;
			case Values.REQ_ACTION_PICK: //相册
				if (resultCode == RESULT_OK)
					startPhotoZoom (data.getData ());
				break;
			case Values.REQ_ACTION_CROP: //裁剪
				if (resultCode == RESULT_OK && imageUri != null)
				{
					try
					{
						Bitmap bitmap = BitmapFactory.decodeStream
								(getContentResolver ().openInputStream (imageUri));
						setPicToView (bitmap);
					}
					catch (FileNotFoundException e)
					{
						e.printStackTrace ();
					}
				}
				break;
		}
		super.onActivityResult (requestCode, resultCode, data);
	}

	private void loadView ()
	{
		if (user.getAvatar () != null)
			((ImageView) findViewById (R.id.imageViewAvatar)).setImageBitmap (user.getAvatar ());
		View view = findViewById (R.id.line1);
		view.setBackgroundResource (Values.COLOR);
		view = findViewById (R.id.line2);
		view.setBackgroundResource (Values.COLOR);
		RelativeLayout layout = findViewById (R.id.changeColor);
		layout.setBackgroundResource (Values.SELECTOR);
		layout = findViewById (R.id.changeName);
		layout.setBackgroundResource (Values.SELECTOR);
		layout = findViewById (R.id.changePassword);
		layout.setBackgroundResource (Values.SELECTOR);
		findViewById (R.id.buttonSignOut).setBackgroundResource (Values.BACKGROUND);
	}

	public void changeAvatar (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			DialogToast.showDialogToast (this, "网络开小差了");
			return;
		}

		dialog = new Dialog (this, R.style.BottomDialog);
		View contentView = LayoutInflater.from (this)
				.inflate (R.layout.dialog_content_avatar, null);

		TextView textView = contentView.findViewById (R.id.avatarCamera);
		textView.setBackgroundResource (Values.SELECTOR);
		textView = contentView.findViewById (R.id.avatarGallery);
		textView.setBackgroundResource (Values.SELECTOR);

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

	public void changeAvatarWith (View view)
	{
		dialog.cancel ();
		switch (view.getId ())
		{
			case R.id.avatarCamera: //拍照
				avatarCamera ();
				break;
			case R.id.avatarGallery: //相册
				avatarGallery ();
				break;
		}
	}

	private void avatarCamera ()
	{
		Intent intent = new Intent (MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra (MediaStore.EXTRA_OUTPUT, imageUri);
		startActivityForResult (intent, Values.REQ_ACTION_IMAGE_CAPTURE);
	}

	private void avatarGallery ()
	{
		Intent intent = new Intent (Intent.ACTION_PICK, null);
		intent.setDataAndType (MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				"image/*");
		startActivityForResult (intent, Values.REQ_ACTION_PICK);
	}

	private void startPhotoZoom (Uri uri)
	{
		Intent intent = new Intent ("com.android.camera.action.CROP");
		intent.setDataAndType (uri, "image/*");

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) //安卓7.0以后配置
			intent.addFlags (Intent.FLAG_GRANT_READ_URI_PERMISSION);

		// crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
		intent.putExtra ("crop", "true");
		// aspectX aspectY 是宽高的比例
		intent.putExtra ("aspectX", 1);
		intent.putExtra ("aspectY", 1);
		// outputX outputY 是裁剪图片宽高
		intent.putExtra ("outputX", 1000);
		intent.putExtra ("outputY", 1000);
		intent.putExtra ("return-data", false);
		intent.putExtra ("output", imageUri);
		startActivityForResult (intent, Values.REQ_ACTION_CROP);
	}

	private void setPicToView (final Bitmap avatar)
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

		//新建一个线程去访问服务器
		new Thread (new Runnable ()
		{
			@Override public void run ()
			{
				Looper.prepare ();

				//拿到头像文件
				final File file = new File (imageUri.getPath ());
				RequestBody fileBody = RequestBody.create
						(MediaType.parse ("multipart/form-data"), file);
				MultipartBody.Part part = MultipartBody.Part.createFormData
						("avatar", file.getName (), fileBody);
				RequestBody accountBody = RequestBody.create (
						MediaType.parse ("multipart/form-data"), user.getAccount ());
				HashMap<String, RequestBody> map = new HashMap<> ();
				map.put ("account", accountBody);

				Retrofit retrofit = RetrofitHelper.getRetrofit ();
				RequestServes requestServes = retrofit.create (RequestServes.class);

				Call<String> call = requestServes.changeAvatar (map, part);
				call.enqueue (new Callback<String> ()
				{
					@Override public void onResponse (Call<String> call, Response<String> response)
					{
						switch (response.body ())
						{
							case ":-1":
								dialogProgressBar.cancel ();
								DialogToast.showDialogToast (UserCenterActivity.this,
										"数据被外星人带走了");
								break;
							case ":1":
								//把file转到getExternalFilesDir (Environment.DIRECTORY_DCIM)目录
								File fileTarget = new File (getExternalFilesDir (Environment.DIRECTORY_DCIM),
										"avatar.jpg");
								FileHelper.moveFile (file, fileTarget);

								writeAvatarToSQLiteAndMemory (avatar);
								Values.CHANGE_AVATAR = true;
								setResult (Values.RES_CHANGE_SOMETHING);
								dialogProgressBar.cancel ();
								DialogToast.showDialogToast (UserCenterActivity.this,
										"改好了");
						}
						call.cancel ();
					}

					@Override public void onFailure (Call<String> call, Throwable t)
					{
						DialogToast.showDialogToast (UserCenterActivity.this,
								"服务器在维护啦");
						dialogProgressBar.cancel ();
						call.cancel ();
					}
				});
				Looper.loop ();
			}
		}).start ();
	}

	public void changeColor (View view)
	{
		dialog = new Dialog (this, R.style.BottomDialog);
		View contentView = LayoutInflater.from (this)
				.inflate (R.layout.dialog_content_color, null);

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

	public void changeColorWith (View view)
	{
		dialog.cancel ();
		switch (view.getId ())
		{
			case R.id.colorBlue:
				writeThemeToSQLite (1);
				break;
			case R.id.colorGray:
				writeThemeToSQLite (2);
				break;
			case R.id.colorRed:
				writeThemeToSQLite (3);
				break;
			case R.id.colorCyan:
				writeThemeToSQLite (4);
				break;
			case R.id.colorGreen:
				writeThemeToSQLite (5);
				break;
			case R.id.colorOrange:
				writeThemeToSQLite (6);
				break;
			case R.id.colorYellow:
				writeThemeToSQLite (7);
				break;
			case R.id.colorPink:
				writeThemeToSQLite (8);
				break;
			case R.id.colorPurple:
				writeThemeToSQLite (9);
				break;
			default:
				return;
		}
		setResult (Values.RES_CHANGE_THEME);
		finish ();
	}

	public void changeName (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			DialogToast.showDialogToast (this, "网络开小差了");
			return;
		}

		dialog = new Dialog (this, R.style.BottomDialog);
		View contentView = LayoutInflater.from (this)
				.inflate (R.layout.dialog_content_name, null);

		EditText editText = contentView.findViewById (R.id.editTextName);
		ImageView imageView = contentView.findViewById (R.id.imageViewNameClear);
		EditTextClear.addClearListener (editText, imageView);
		contentView.findViewById (R.id.nameSave).setBackgroundResource (Values.SELECTOR);

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

	public void changeNameWith (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			DialogToast.showDialogToast (this, "网络开小差了");
			return;
		}

		if (view.getId () == R.id.nameSave)
		{
			EditText editText = ((ViewGroup) view.getParent ())
					.findViewById (R.id.editTextName);
			final String name = String.valueOf (editText.getText ());

			if (TextUtils.isEmpty (name))
				DialogToast.showDialogToast (this, "没有昵称可不行");
			else if (name.length () > 10)
				DialogToast.showDialogToast (this, "长度10位以内哦");
			else if (name.contains ("  "))
				DialogToast.showDialogToast (this, "昵称可不能有连续空格");
			else
			{
				if (dialogProgressBar == null)
				{
					dialogProgressBar = new Dialog (UserCenterActivity.this,
							R.style.BottomDialog);
					View buttonContentView = LayoutInflater.from (UserCenterActivity.this)
							.inflate (R.layout.dialog_progress_bar, null);

					ProgressBar progressBar = buttonContentView.findViewById (R.id.progressBar);
					progressBar.setIndeterminateDrawable
							(getResources ().getDrawable (Values.PROGRESS));

					dialogProgressBar.setContentView (buttonContentView);
					ViewGroup.MarginLayoutParams buttonParams = (ViewGroup.MarginLayoutParams)
							buttonContentView.getLayoutParams ();
					buttonParams.width = getResources ().getDisplayMetrics ().widthPixels
							- Density.dp2px (UserCenterActivity.this, 16f);
					buttonParams.bottomMargin = Density.dp2px
							(UserCenterActivity.this, 8f);
					buttonContentView.setLayoutParams (buttonParams);
					dialogProgressBar.getWindow ().setGravity (Gravity.CENTER);
					dialogProgressBar.getWindow ().setWindowAnimations
							(R.style.BottomDialog_Animation);
					dialogProgressBar.setCancelable (false);
				}
				dialogProgressBar.show ();

				//新建一个线程去访问服务器
				new Thread (new Runnable ()
				{
					@Override public void run ()
					{
						Looper.prepare ();
						Retrofit retrofit = RetrofitHelper.getRetrofit ();
						RequestServes requestServes = retrofit.create (RequestServes.class);
						Call<String> call = requestServes.changeName (user.getAccount (), name);
						call.enqueue (new Callback<String> ()
						{
							@Override public void onResponse (Call<String> call,
									Response<String> response)
							{
								switch (response.body ())
								{
									case ":-1":
										dialogProgressBar.cancel ();
										dialog.cancel ();
										DialogToast.showDialogToast (UserCenterActivity.this,
												"数据被外星人带走了");
										break;
									case ":1":
										writeNameToSQLiteAndMemory (name);
										Values.CHANGE_NAME = true;
										setResult (Values.RES_CHANGE_SOMETHING);
										dialogProgressBar.cancel ();
										dialog.cancel ();
										DialogToast.showDialogToast (UserCenterActivity.this,
												"改好了");
								}
								call.cancel ();
							}

							@Override public void onFailure (Call<String> call, Throwable t)
							{
								dialogProgressBar.cancel ();
								dialog.cancel ();
								DialogToast.showDialogToast (UserCenterActivity.this,
										"服务器在维护啦");
								call.cancel ();
							}
						});
						Looper.loop ();
					}
				}).start ();
			}
		}
		else
			dialog.cancel ();
	}

	public void changePassword (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			DialogToast.showDialogToast (this, "网络开小差了");
			return;
		}

		dialog = new Dialog (this, R.style.BottomDialog);
		View contentView = LayoutInflater.from (this).inflate
				(R.layout.dialog_content_password, null);

		EditText editTextOld = contentView.findViewById (R.id.editTextPasswordOld);
		ImageView imageView = contentView.findViewById (R.id.imageViewPasswordOldClear);
		EditTextClear.addClearListener (editTextOld, imageView);
		EditText editTextNew = contentView.findViewById (R.id.editTextPasswordNew);
		imageView = contentView.findViewById (R.id.imageViewPasswordNewClear);
		EditTextClear.addClearListener (editTextNew, imageView);
		contentView.findViewById (R.id.passwordSave).setBackgroundResource (Values.SELECTOR);

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

	public void changePasswordWith (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			DialogToast.showDialogToast (this, "网络开小差了");
			return;
		}

		if (view.getId () == R.id.passwordSave)
		{
			ViewGroup viewGroup = (ViewGroup) view.getParent ();
			EditText editText = viewGroup.findViewById (R.id.editTextPasswordOld);
			final String passwordOld = String.valueOf (editText.getText ());
			editText = viewGroup.findViewById (R.id.editTextPasswordNew);
			final String passwordNew = String.valueOf (editText.getText ());

			if (TextUtils.isEmpty (passwordOld) || !Pattern.matches
					(Values.passwordRegex, passwordOld))
				DialogToast.showDialogToast (UserCenterActivity.this,
						"这显然不是对的原密码");
			else if (TextUtils.isEmpty (passwordNew) || !Pattern.matches
					(Values.passwordRegex, passwordNew))
				DialogToast.showDialogToast (UserCenterActivity.this,
						"这显然不是好的新密码");
			else
			{
				if (dialogProgressBar == null)
				{
					dialogProgressBar = new Dialog (UserCenterActivity.this,
							R.style.BottomDialog);
					View buttonContentView = LayoutInflater.from (UserCenterActivity.this).inflate
							(R.layout.dialog_progress_bar, null);

					ProgressBar progressBar = buttonContentView.findViewById (R.id.progressBar);
					progressBar.setIndeterminateDrawable
							(getResources ().getDrawable (Values.PROGRESS));

					dialogProgressBar.setContentView (buttonContentView);
					ViewGroup.MarginLayoutParams buttonParams = (ViewGroup.MarginLayoutParams)
							buttonContentView.getLayoutParams ();
					buttonParams.width = getResources ().getDisplayMetrics ().widthPixels
							- Density.dp2px (UserCenterActivity.this, 16f);
					buttonParams.bottomMargin = Density.dp2px
							(UserCenterActivity.this, 8f);
					buttonContentView.setLayoutParams (buttonParams);
					dialogProgressBar.getWindow ().setGravity (Gravity.CENTER);
					dialogProgressBar.getWindow ().setWindowAnimations (R.style.BottomDialog_Animation);
					dialogProgressBar.setCancelable (false);
				}
				dialogProgressBar.show ();

				//新建一个线程去访问服务器
				new Thread (new Runnable ()
				{
					@Override public void run ()
					{
						Looper.prepare ();
						Retrofit retrofit = RetrofitHelper.getRetrofit ();
						RequestServes requestServes = retrofit.create (RequestServes.class);
						Call<String> call = requestServes.changePassword
								(user.getAccount (), passwordOld, passwordNew);
						call.enqueue (new Callback<String> ()
						{
							@Override public void onResponse (Call<String> call,
									Response<String> response)
							{
								switch (response.body ())
								{
									case ":-1":
										dialogProgressBar.cancel ();
										dialog.cancel ();
										DialogToast.showDialogToast (UserCenterActivity.this,
												"数据被外星人带走了");
										break;
									case ":0":
										dialogProgressBar.cancel ();
										dialog.cancel ();
										DialogToast.showDialogToast (UserCenterActivity.this,
												"原密码错了");
										break;
									case ":1":
										setResult (Values.RES_CHANGE_PASSWORD);
										dialogProgressBar.cancel ();
										dialog.cancel ();
										DialogToast.showDialogToast (UserCenterActivity.this,
												"改好了");
										finish ();
								}
								call.cancel ();
							}

							@Override public void onFailure (Call<String> call, Throwable t)
							{
								dialogProgressBar.cancel ();
								dialog.cancel ();
								DialogToast.showDialogToast (UserCenterActivity.this,
										"服务器在维护啦");
								call.cancel ();
							}
						});
						Looper.loop ();
					}
				}).start ();
			}
		}
		else
			dialog.cancel ();
	}

	public void signOut (View view)
	{
		dialog = new Dialog (this, R.style.BottomDialog);
		View contentView = LayoutInflater.from (this)
				.inflate (R.layout.dialog_content_sign_out, null);

		contentView.findViewById (R.id.signOut).setBackgroundResource (Values.SELECTOR);

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

	public void signOutWith (View view)
	{
		dialog.cancel ();
		if (view.getId () == R.id.signOut)
		{
			setResult (Values.RES_SIGN_OUT);
			finish ();
		}
	}

	/**
	 * 写入内存头像
	 */
	private void writeAvatarToSQLiteAndMemory (Bitmap avatar)
	{
		user.setAvatar (avatar);
		user.setHasAvatar (true);
		ImageView imageView = findViewById (R.id.imageViewAvatar);
		imageView.setImageBitmap (avatar);
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		db.execSQL ("update user set avatar=\"1\"");
		db.close ();
	}

	/**
	 * 写入SQLite主题颜色
	 */
	private void writeThemeToSQLite (int color)
	{
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		SQLiteStatement sqLiteStatement = db.compileStatement
				("update setting set color = ?");
		sqLiteStatement.bindString (1, color + "");
		sqLiteStatement.executeUpdateDelete ();
		db.close ();
	}

	/**
	 * 写入SQLite，内存昵称
	 */
	private void writeNameToSQLiteAndMemory (String name)
	{
		user.setName (name);
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		SQLiteStatement sqLiteStatement = db.compileStatement
				("update user set name = ?");
		sqLiteStatement.bindString (1, name);
		sqLiteStatement.executeUpdateDelete ();
		db.close ();
	}
}
