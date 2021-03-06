package com.lin.mnote;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
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

import com.lin.utils.Density;
import com.lin.utils.DialogToast;
import com.lin.utils.EditTextClear;
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

public class ForgetPasswordActivity extends AppCompatActivity
{
	private Dialog dialog;
	private String account, password;
	private boolean getNonce = false;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_forget_password);

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
		editText.setText (getIntent ().getStringExtra ("account"));

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
								Call<String> call = requestServes.forgetPassword (account, password);
								call.enqueue (new Callback<String> ()
								{
									@Override public void onResponse (Call<String> call,
											Response<String> response)
									{
										switch (response.body ())
										{
											case ":-1":
												DialogToast.showDialogToast (ForgetPasswordActivity.this,
														"数据被外星人带走了");
												break;
											case ":1":
												DialogToast.showDialogToast (ForgetPasswordActivity.this,
														"改好了");
												Intent intent = new Intent ();
												intent.putExtra ("account", account);
												setResult (Values.RES_FORGET_PASSWORD, intent);
												finish ();
												break;
										}
									}

									@Override public void onFailure (Call<String> call, Throwable t)
									{
										DialogToast.showDialogToast (ForgetPasswordActivity.this,
												"服务器在维护啦");
									}
								});
							}
						}).start ();
					}
					else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE)
					//获取验证码成功
					{
						boolean isSmart = (boolean) data;
						getNonce = true;
						runOnUiThread (new Runnable ()
						{
							@Override
							public void run ()
							{
								DialogToast.showDialogToast (ForgetPasswordActivity.this,
										"验证码正在飞来");
							}
						});
					}
				}
				else
				{
					((Throwable) data).printStackTrace ();
					Throwable throwable = (Throwable) data;
					throwable.printStackTrace ();
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
										DialogToast.showDialogToast (ForgetPasswordActivity.this,
												"获取验证码太多次啦");
									}
								});
							case 468:
								dialog.cancel ();
								runOnUiThread (new Runnable ()
								{
									@Override
									public void run ()
									{
										DialogToast.showDialogToast (ForgetPasswordActivity.this,
												"验证码错误");
									}
								});
								break;
							default:
								runOnUiThread (new Runnable ()
								{
									@Override
									public void run ()
									{
										DialogToast.showDialogToast (ForgetPasswordActivity.this,
												"数据被外星人带走了");
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
				.setTextColor (getResources ().getColor (Values.COLOR));
		findViewById (R.id.buttonChangePassword)
				.setBackgroundResource (Values.BACKGROUND);
	}

	public void textViewGetNonce (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			DialogToast.showDialogToast (this, "网络开小差了");
			return;
		}

		account = ((TextView) findViewById (R.id.editTextAccount)).getText ().toString ();
		if (TextUtils.isEmpty (account) || !Pattern.matches (Values.accountRegex, account))
		{
			DialogToast.showDialogToast (this, "这显然是个假号码");
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
								DialogToast.showDialogToast (ForgetPasswordActivity.this,
										"数据被外星人带走了");
								break;
							case ":0":
								DialogToast.showDialogToast (ForgetPasswordActivity.this,
										"这号码还没注册过呢");
								break;
							default:
								SMSSDK.getVerificationCode ("86", account);
						}
						call.cancel ();
					}

					//超时未回应也会进入这个函数
					@Override public void onFailure (Call<String> call, Throwable t)
					{
						DialogToast.showDialogToast (ForgetPasswordActivity.this,
								"服务器在维护啦");
						call.cancel ();
					}
				});
				Looper.loop ();
			}
		}).start ();
	}

	public void buttonChangePassword (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			DialogToast.showDialogToast (this, "网络开小差了");
			return;
		}

		if (!getNonce || !account.equals (((TextView) findViewById
				(R.id.editTextAccount)).getText ().toString ()))
		{
			DialogToast.showDialogToast (this, "请获取验证码");
			return;
		}

		String nonce = ((TextView) findViewById (R.id.editTextNonce)).getText ().toString ();
		if (TextUtils.isEmpty (nonce) || !Pattern.matches (Values.nonceRegex, nonce))
		{
			DialogToast.showDialogToast (this, "这显然是个假验证码");
			return;
		}

		password = ((TextView) findViewById (R.id.editTextPassword)).getText ().toString ();
		if (TextUtils.isEmpty (password) || !Pattern.matches (Values.passwordRegex, password))
		{
			DialogToast.showDialogToast (this, "这显然不是好的新密码");
			return;
		}

		SMSSDK.submitVerificationCode ("86", account, nonce);

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
	}
}
