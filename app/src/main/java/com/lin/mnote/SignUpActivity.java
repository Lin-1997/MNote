package com.lin.mnote;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lin.utils.Density;
import com.lin.utils.NetworkDetector;
import com.lin.utils.RequestServes;
import com.lin.utils.RetrofitHelper;
import com.lin.utils.Values;
import com.mob.MobSDK;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SignUpActivity extends AppCompatActivity
{
	private Dialog dialog;
	private String account, password;
	private boolean getNonce = false;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_sign_up);

		Toolbar toolbar = findViewById (R.id.toolbar);
		toolbar.setBackgroundResource (Values.getColor ());
		setSupportActionBar (toolbar);
		getSupportActionBar ().setDisplayHomeAsUpEnabled (true);

		loadView ();

		// FIXME: 2018/6/9 这东西迟早是要换的
		// FIXME: 2018/6/9 流程应该是app发送手机号码到服务器
		// FIXME: 2018/6/9 服务器发送号码到短信验证平台
		// FIXME: 2018/6/9 验证平台发送短信到手机，发送验证码到服务器
		// FIXME: 2018/6/9 app提交验证码到服务器
		// FIXME: 2018/6/9 app不和短信验证平台交互
		MobSDK.init (this);
		SMSSDK.registerEventHandler (new EventHandler ()
		{
			@Override
			public void afterEvent (int event, int result, Object data)
			{
				if (result == SMSSDK.RESULT_COMPLETE)
				{
					if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) //回调完成
					{
						dialog.cancel ();
						//提交验证码成功
						new Thread (new Runnable ()
						{
							@Override public void run ()
							{
								Looper.prepare ();
								Retrofit retrofit = RetrofitHelper.getRetrofit ();
								RequestServes requestServes = retrofit.create (RequestServes.class);
								Call<String> call = requestServes.signUp (account, password);
								call.enqueue (new Callback<String> ()
								{
									@Override public void onResponse (Call<String> call,
											Response<String> response)
									{
										switch (response.body ())
										{
											case ":-1":
												Log.d ("注册", "失败");
												Toast.makeText (SignUpActivity.this,
														"数据被外星人带走了", Toast.LENGTH_SHORT).show ();
												break;
											case ":1":
												Log.d ("注册", "成功");
												Toast.makeText (SignUpActivity.this,
														"注册成功", Toast.LENGTH_SHORT).show ();
												Intent intent = new Intent ();
												intent.putExtra ("account", account);
												setResult (Values.RES_SIGN_UP, intent);
												finish ();
												break;
										}
									}

									@Override public void onFailure (Call<String> call, Throwable t)
									{
										Log.d ("注册失败", t.toString ());
										Toast.makeText (SignUpActivity.this,
												"服务器在维护啦", Toast.LENGTH_SHORT).show ();
									}
								});
							}
						}).start ();
					}
					else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE)
					//获取验证码成功
					{
						boolean isSmart = (boolean) data;
						if (isSmart)
							Log.d ("ssss", "isSmart");
						else
							Log.d ("ssss", "notSmart");
						getNonce = true;
						runOnUiThread (new Runnable ()
						{
							@Override
							public void run ()
							{
								Toast.makeText (SignUpActivity.this,
										"验证码正在飞来", Toast.LENGTH_SHORT).show ();
							}
						});
					}
				}
				else
				{
					((Throwable) data).printStackTrace ();
					Throwable throwable = (Throwable) data;
					throwable.printStackTrace ();
					Log.d ("ssss", throwable.toString ());
					try
					{
						JSONObject obj = new JSONObject (throwable.getMessage ());
						final int status = obj.optInt ("status");
						switch (status)
						{
							case 400:
								runOnUiThread (new Runnable ()
								{
									@Override
									public void run ()
									{
										Toast.makeText (SignUpActivity.this,
												"获取验证码太多次啦",
												Toast.LENGTH_SHORT).show ();
									}
								});
							case 468:
								dialog.cancel ();
								runOnUiThread (new Runnable ()
								{
									@Override
									public void run ()
									{
										Toast.makeText (SignUpActivity.this,
												"验证码错误", Toast.LENGTH_SHORT).show ();
									}
								});
								break;
							default:
								runOnUiThread (new Runnable ()
								{
									@Override
									public void run ()
									{
										Toast.makeText (SignUpActivity.this,
												"数据被外星人带走了", Toast.LENGTH_SHORT).show ();
									}
								});
						}
					}
					catch (JSONException e)
					{
						e.printStackTrace ();
					}
				}
			}
		});
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
		((TextView) findViewById (R.id.textViewGetNonce))
				.setTextColor (getResources ().getColor (Values.getColor ()));
		findViewById (R.id.buttonSignUp)
				.setBackgroundResource (Values.getBackground ());
	}

	public void textViewGetNonce (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			Toast.makeText (this, "网络开小差了",
					Toast.LENGTH_SHORT).show ();
			return;
		}

		account = ((TextView) findViewById (R.id.editTextAccount)).getText ().toString ();
		if (TextUtils.isEmpty (account) || !Pattern.matches (Values.accountRegex, account))
		{
			Toast.makeText (this, "这显然是个假号码",
					Toast.LENGTH_SHORT).show ();
			return;
		}

		new Thread (new Runnable ()
		{
			@Override public void run ()
			{
				Looper.prepare ();
				Retrofit retrofit = RetrofitHelper.getRetrofit ();
				RequestServes requestServes = retrofit.create (RequestServes.class);
				Call<String> call = requestServes.findAccount (account);
				call.enqueue (new Callback<String> ()
				{
					@Override public void onResponse (Call<String> call,
							Response<String> response)
					{
						switch (response.body ())
						{
							case ":-1":
								Log.d ("查询账号", "失败");
								Toast.makeText (SignUpActivity.this,
										"数据被外星人带走了", Toast.LENGTH_SHORT).show ();
								break;
							case ":1":
								Log.d ("查询账号", "已注册");
								dialog = new Dialog (SignUpActivity.this, R.style.BottomDialog);
								View contentView = LayoutInflater.from (SignUpActivity.this)
										.inflate (R.layout.dialog_content_account_occupied, null);

								contentView.findViewById (R.id.gotoSignIn)
										.setBackgroundResource (Values.getSelector ());

								dialog.setContentView (contentView);
								ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
										contentView.getLayoutParams ();
								params.width = getResources ().getDisplayMetrics ().widthPixels
										- Density.dp2px (SignUpActivity.this, 16f);
								params.bottomMargin = Density.dp2px (SignUpActivity.this, 8f);
								contentView.setLayoutParams (params);
								dialog.getWindow ().setGravity (Gravity.BOTTOM);
								dialog.getWindow ().setWindowAnimations (R.style.BottomDialog_Animation);
								dialog.show ();
								break;
							case ":0":
								// FIXME: 2018/6/6 获取验证码后改一下textViewGetNonce内容
								SMSSDK.getVerificationCode ("86", account);
						}
						call.cancel ();
					}

					//超时未回应也会进入这个函数
					@Override public void onFailure (Call<String> call, Throwable t)
					{
						Log.d ("查询账号", t.toString ());
						Toast.makeText (SignUpActivity.this,
								"服务器在维护啦", Toast.LENGTH_SHORT).show ();
						call.cancel ();
					}
				});
				Looper.loop ();
			}
		}).start ();
	}

	public void buttonSignUp (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			Toast.makeText (this, "网络开小差了",
					Toast.LENGTH_SHORT).show ();
			return;
		}

		if (!getNonce || !account.equals (((TextView) findViewById
				(R.id.editTextAccount)).getText ().toString ()))
		{
			Toast.makeText (this, "请获取验证码",
					Toast.LENGTH_SHORT).show ();
			return;
		}

		String nonce = ((TextView) findViewById (R.id.editTextNonce)).getText ().toString ();
		if (TextUtils.isEmpty (nonce) || !Pattern.matches (Values.nonceRegex, nonce))
		{
			Toast.makeText (this, "这显然是个假验证码",
					Toast.LENGTH_SHORT).show ();
			return;
		}

		password = ((TextView) findViewById (R.id.editTextPassword)).getText ().toString ();
		if (TextUtils.isEmpty (password) || !Pattern.matches (Values.passwordRegex, password))
		{
			Toast.makeText (this, "这显然不是好的新密码",
					Toast.LENGTH_SHORT).show ();
			return;
		}

		Log.d ("提交验证码验证", account + "," + nonce);
		SMSSDK.submitVerificationCode ("86", account, nonce);

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
	}

	public void gotoSignIn (View view)
	{
		if (view.getId () == R.id.gotoSignIn)
		{
			Intent intent = new Intent ();
			intent.putExtra ("account", account);
			setResult (Values.RES_ACCOUNT_OCCUPIED, intent);
			dialog.cancel (); //finish前记得关闭dialog，不然会内存泄漏
			finish ();
		}
		else
			dialog.cancel ();
	}
}
