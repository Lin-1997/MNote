package com.lin.mnote;

import android.app.Dialog;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lin.bean.User;
import com.lin.utils.Density;
import com.lin.utils.EditTextClear;
import com.lin.utils.FileHelper;
import com.lin.utils.NetworkDetector;
import com.lin.utils.RequestServes;
import com.lin.utils.RetrofitHelper;
import com.lin.utils.SQLiteHelper;
import com.lin.utils.Values;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SignInActivity extends AppCompatActivity
{
	private User user;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_sign_in);

		Toolbar toolbar = findViewById (R.id.toolbar);
		toolbar.setBackgroundResource (Values.getColor ());
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

	private void loadView ()
	{
		findViewById (R.id.buttonSignIn).setBackgroundResource (Values.getBackground ());
		TextView textView = findViewById (R.id.textViewNewRegister);
		textView.setTextColor (getResources ().getColor (Values.getColor ()));
		textView = findViewById (R.id.textViewForgetPassword);
		textView.setTextColor (getResources ().getColor (Values.getColor ()));
	}

	public void buttonSignIn (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			Toast.makeText (this, "网络开小差了",
					Toast.LENGTH_SHORT).show ();
			return;
		}

		EditText editTextAccount = findViewById (R.id.editTextAccount);
		EditText editTextPassword = findViewById (R.id.editTextPassword);
		final String account = editTextAccount.getText ().toString ();
		final String password = editTextPassword.getText ().toString ();

		if (account.equals (""))
		{
			Toast.makeText (this, "你没有账号的吗", Toast.LENGTH_SHORT).show ();
			return;
		}
		if (password.equals (""))
		{
			Toast.makeText (this, "你没有密码的吗", Toast.LENGTH_SHORT).show ();
			return;
		}

		final Dialog dialog = new Dialog (this, R.style.BottomDialog);
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
					@Override public void onResponse (Call<String> call,
							Response<String> response)
					{
						switch (response.body ())
						{
							case ":-1":
								Log.d ("登录", "失败");
								Toast.makeText (SignInActivity.this,
										"数据被外星人带走了", Toast.LENGTH_SHORT).show ();
								break;
							case ":0":
								Log.d ("登录", "失败");
								Toast.makeText (SignInActivity.this,
										"账号密码或许错了", Toast.LENGTH_SHORT).show ();
								break;
							default:
								Log.d ("登录", "成功");

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
								if (bitmap != null)
								{
									writeUserToSQLite (account, name, 1);
									writeUserToMemory (account, name);
									writeAvatarToMemory (bitmap);
									writeAvatarToFile (bitmap);
								}
								else
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
						Log.d ("登录失败", t.toString ());
						Toast.makeText (SignInActivity.this,
								"服务器在维护啦", Toast.LENGTH_SHORT).show ();
						dialog.cancel ();
						call.cancel ();
					}
				});
				Looper.loop ();
			}
		}).start ();
	}

	// FIXME: 2018/3/18 注册账号
	public void textViewNewRegister (View view)
	{

	}

	// FIXME: 2018/3/18 忘记密码
	public void textViewForgetPassword (View view)
	{

	}

	/**
	 * 写入SQLite用户
	 */
	private void writeUserToSQLite (String account, String name, int avatar)
	{
		SQLiteHelper helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		db.execSQL ("delete from user");
		db.execSQL ("insert into user values (\"" + account + "\",\"" +
				name + "\",\"" + avatar + "\")");
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