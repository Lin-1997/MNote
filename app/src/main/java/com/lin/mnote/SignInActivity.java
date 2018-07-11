package com.lin.mnote;

import android.app.Dialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SignInActivity extends AppCompatActivity
{
	private User user;
	private Dialog dialog;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_sign_in);

		Toolbar toolbar = findViewById (R.id.toolbar);
		toolbar.setBackgroundResource (Values.COLOR);
		setSupportActionBar (toolbar);
		getSupportActionBar ().setDisplayHomeAsUpEnabled (true);

		EditText editText = findViewById (R.id.editTextPassword);
		ImageView imageView = findViewById (R.id.imageViewPasswordClear);
		EditTextClear.addClearListener (editText, imageView);

		editText = findViewById (R.id.editTextAccount);
		imageView = findViewById (R.id.imageViewAccountClear);
		EditTextClear.addClearListener (editText, imageView);

		//设置为上次登录的账号
		editText.setText (getIntent ().getStringExtra ("preAccount"));

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
			case Values.REQ_SIGN_UP:
				switch (resultCode)
				{
					case Values.RES_SIGN_UP:
						//注册成功就等同于登录成功？？
						//String account = data.getExtras ().getString ("account");
						//writeUserToSQLite (account, account, 0);
						//writeUserToMemory (account, account);
						//setResult (Values.RES_SIGN_IN);
						//finish ();
						//break;
					case Values.RES_ACCOUNT_OCCUPIED:
						//填入账号，方便登录
						((EditText) findViewById (R.id.editTextAccount))
								.setText (data.getStringExtra ("account"));
						break;
				}
				break;
			case Values.REQ_FORGET_PASSWORD:
				switch (resultCode)
				{
					case Values.RES_FORGET_PASSWORD:
						//填入账号，方便登录
						((EditText) findViewById (R.id.editTextAccount))
								.setText (data.getStringExtra ("account"));
						break;
				}
				break;
		}
		super.onActivityResult (requestCode, resultCode, data);
	}

	private void loadView ()
	{
		findViewById (R.id.buttonSignIn).setBackgroundResource (Values.BACKGROUND);
		TextView textView = findViewById (R.id.textViewNewRegister);
		textView.setTextColor (getResources ().getColor (Values.COLOR));
		textView = findViewById (R.id.textViewForgetPassword);
		textView.setTextColor (getResources ().getColor (Values.COLOR));
	}

	public void buttonSignIn (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			DialogToast.showDialogToast (this, "网络开小差了");
			return;
		}

		EditText editTextAccount = findViewById (R.id.editTextAccount);
		EditText editTextPassword = findViewById (R.id.editTextPassword);
		final String account = editTextAccount.getText ().toString ();
		final String password = editTextPassword.getText ().toString ();


		if (TextUtils.isEmpty (account) || !Pattern.matches (Values.accountRegex, account))
		{
			DialogToast.showDialogToast (this, "这显然是个假账号");
			return;
		}
		if (TextUtils.isEmpty (password) || !Pattern.matches (Values.passwordRegex, password))
		{
			DialogToast.showDialogToast (this, "这显然是个假密码");
			return;
		}

		if (dialog == null)
		{
			dialog = new Dialog (this, R.style.BottomDialog);
			View contentView = LayoutInflater.from (this).inflate
					(R.layout.dialog_progress_bar, null);

			ProgressBar progressBar = contentView.findViewById (R.id.progressBar);
			progressBar.setIndeterminateDrawable (getResources ().getDrawable (Values.PROGRESS));

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
		}
		dialog.show ();

		new Thread (new Runnable ()
		{
			@Override public void run ()
			{
				Looper.prepare ();
				Retrofit retrofit = RetrofitHelper.getRetrofit ();
				RequestServes requestServes = retrofit.create (RequestServes.class);
				Call<String> call = requestServes.signIn (account, password);
				call.enqueue (new Callback<String> ()
				{
					@Override public void onResponse (Call<String> call, Response<String> response)
					{
						switch (response.body ())
						{
							case ":-1":
								DialogToast.showDialogToast (SignInActivity.this,
										"数据被外星人带走了");
								break;
							case ":0":
								DialogToast.showDialogToast (SignInActivity.this,
										"账号密码或许错了");
								break;
							default:
								String name = "", avatar = "";
								try
								{
									JSONObject jsonObject = new JSONObject (response.body ());
									name = jsonObject.getString ("name");
									avatar = jsonObject.getString ("avatar");
								}
								catch (JSONException e)
								{
									e.printStackTrace ();
								}

								//把拿到的base64转为Bitmap
								Bitmap bitmap = FileHelper.String2Bitmap (avatar);
								if (bitmap != null) //有头像
								{
									writeUserToSQLite (account, name, 1);
									writeUserToMemory (account, name);
									writeAvatarToMemory (bitmap);
									writeAvatarToFile (bitmap);
								}
								else //没头像
								{
									writeUserToSQLite (account, name, 0);
									writeUserToMemory (account, name);
								}

								setResult (Values.RES_SIGN_IN);
								finish ();
						}
						dialog.cancel ();
						call.cancel ();
					}

					//超时未回应也会进入这个函数
					@Override public void onFailure (Call<String> call, Throwable t)
					{
						DialogToast.showDialogToast (SignInActivity.this,
								"服务器在维护啦");
						dialog.cancel ();
						call.cancel ();
					}
				});
				Looper.loop ();
			}
		}).start ();
	}

	public void textViewNewRegister (View view)
	{
		Intent intent = new Intent (this, SignUpActivity.class);
		startActivityForResult (intent, Values.REQ_SIGN_UP);
	}

	public void textViewForgetPassword (View view)
	{
		Intent intent = new Intent (this, ForgetPasswordActivity.class);
		intent.putExtra ("account", ((EditText) findViewById (R.id.editTextAccount))
				.getText ().toString ());
		startActivityForResult (intent, Values.REQ_FORGET_PASSWORD);
	}

	/**
	 * 写入SQLite用户
	 */
	private void writeUserToSQLite (String account, String name, int avatar)
	{
		SQLiteHelper helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		db.execSQL ("begin");
		db.execSQL ("delete from user");
		SQLiteStatement sqLiteStatement = db.compileStatement
				("insert into user values (?,?,?)");
		String[] params = {account, name, avatar + ""};
		sqLiteStatement.bindAllArgsAsStrings (params);
		sqLiteStatement.executeInsert ();
		db.execSQL ("commit");
		db.close ();
	}

	/**
	 * 写入内存用户
	 */
	private void writeUserToMemory (String account, String name)
	{
		user = User.getUser ();
		user.signIn (account, name);
	}

	/**
	 * 写入内存头像
	 */
	private void writeAvatarToMemory (Bitmap avatar)
	{
		user.setAvatar (avatar);
		user.setHasAvatar (true);
	}

	/**
	 * 写入文件头像
	 */
	private void writeAvatarToFile (Bitmap bitmap)
	{
		FileHelper.Bitmap2File (bitmap, getExternalFilesDir (Environment.DIRECTORY_DCIM)
				.getPath (), "avatar.jpg");
	}
}