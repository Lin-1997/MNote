package com.lin.mnote;

import android.app.Dialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lin.bean.User;
import com.lin.utils.Density;
import com.lin.utils.EditTextClear;
import com.lin.utils.NetworkDetector;
import com.lin.utils.RequestServes;
import com.lin.utils.RetrofitHelper;
import com.lin.utils.SQLiteHelper;
import com.lin.utils.Values;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UserCenterActivity extends AppCompatActivity
{
	private SQLiteHelper helper;
	private User user = User.getUser ();
	private Dialog dialog;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_user_center);

		Toolbar toolbar = findViewById (R.id.toolbar);
		toolbar.setBackgroundResource (Values.getColor ());
		setSupportActionBar (toolbar);
		getSupportActionBar ().setDisplayHomeAsUpEnabled (true);
		TextView textViewAccount = findViewById (R.id.textViewAccount);
		String string = "账号：";
		string += user.getAccount ().substring (0, 3);
		string += "****";
		string += user.getAccount ().substring (7);
		textViewAccount.setText (string);

		loadColor ();
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

	private void loadColor ()
	{
		View view = findViewById (R.id.line1);
		view.setBackgroundResource (Values.getColor ());
		view = findViewById (R.id.line2);
		view.setBackgroundResource (Values.getColor ());
		view = findViewById (R.id.line3);
		view.setBackgroundResource (Values.getColor ());
		RelativeLayout layout = findViewById (R.id.changeNoteSort);
		layout.setBackgroundResource (Values.getSelector ());
		layout = findViewById (R.id.changeColor);
		layout.setBackgroundResource (Values.getSelector ());
		layout = findViewById (R.id.changeName);
		layout.setBackgroundResource (Values.getSelector ());
		layout = findViewById (R.id.changePassword);
		layout.setBackgroundResource (Values.getSelector ());
		Button button = findViewById (R.id.buttonSignOut);
		button.setBackgroundResource (Values.getBackground ());
	}

	public void changeAvatar (View view)
	{
// FIXME: 2018/3/11 上传头像
	}

	public void changeSort (View view)
	{
		dialog = new Dialog (this, R.style.BottomDialog);
		View contentView = LayoutInflater.from (this).inflate
				(R.layout.dialog_content_sort, null);

		TextView textView = contentView.findViewById (R.id.sortCreate);
		textView.setBackgroundResource (Values.getSelector ());
		textView = contentView.findViewById (R.id.sortUpdate);
		textView.setBackgroundResource (Values.getSelector ());

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

	public void changeSortWith (View view)
	{
		switch (view.getId ())
		{
			case R.id.sortCreate:
				Values.setSort (Values.CREATE);
				Values.setChangeSort (true);
				break;
			case R.id.sortUpdate:
				Values.setSort (Values.UPDATE);
				Values.setChangeSort (true);
				break;
			default:
				dialog.cancel ();
				return;
		}
		writeSortToSQLite ();
		setResult (Values.RES_CHANGE_SOMETHING);
		dialog.cancel ();
	}

	public void changeColor (View view)
	{
		dialog = new Dialog (this, R.style.BottomDialog);
		View contentView = LayoutInflater.from (this).inflate
				(R.layout.color_dialog_content, null);
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
				dialog.cancel ();
				return;
		}
		setResult (Values.RES_CHANGE_THEME);
		dialog.cancel ();
		finish ();
	}

	public void changeName (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			Toast.makeText (this, "网络开小差了",
					Toast.LENGTH_SHORT).show ();
			return;
		}

		dialog = new Dialog (this, R.style.BottomDialog);
		View contentView = LayoutInflater.from (this).inflate
				(R.layout.dialog_content_name, null);

		final EditText editText = contentView.findViewById (R.id.editTextName);
		ImageView imageView = contentView.findViewById (R.id.imageViewNameClear);
		EditTextClear.addClearListener (editText, imageView);

		TextView textView = contentView.findViewById (R.id.nameSave);
		textView.setBackgroundResource (Values.getSelector ());
		textView.setOnClickListener (new View.OnClickListener ()
		{
			@Override public void onClick (View v)
			{
				final String name = String.valueOf (editText.getText ());
				if (name.length () > 10)
					Toast.makeText (UserCenterActivity.this,
							"长度10位以内哦", Toast.LENGTH_SHORT).show ();
				else if (name.length () < 1)
					Toast.makeText (UserCenterActivity.this,
							"没有昵称可不行", Toast.LENGTH_SHORT).show ();
				else if (name.contains ("  "))
					Toast.makeText (UserCenterActivity.this,
							"昵称可不能有连续空格", Toast.LENGTH_SHORT).show ();
				else
				{
					final Dialog bottomDialog = new Dialog (UserCenterActivity.this, R.style.BottomDialog);
					View buttonContentView = LayoutInflater.from (UserCenterActivity.this).inflate
							(R.layout.dialog_progress_bar, null);
					ProgressBar progressBar = buttonContentView.findViewById (R.id.progressBar);
					progressBar.setIndeterminateDrawable (getResources ().getDrawable (Values.getProgress ()));

					bottomDialog.setContentView (buttonContentView);
					ViewGroup.MarginLayoutParams buttonParams = (ViewGroup.MarginLayoutParams)
							buttonContentView.getLayoutParams ();
					buttonParams.width = getResources ().getDisplayMetrics ().widthPixels
							- Density.dp2px (UserCenterActivity.this, 16f);
					buttonParams.bottomMargin = Density.dp2px (UserCenterActivity.this, 8f);
					buttonContentView.setLayoutParams (buttonParams);
					bottomDialog.getWindow ().setGravity (Gravity.CENTER);
					bottomDialog.getWindow ().setWindowAnimations (R.style.BottomDialog_Animation);

					new Thread (new Runnable ()
					{
						@Override public void run ()
						{
							Looper.prepare ();
							bottomDialog.show ();
							Looper.loop ();
						}
					}).start ();

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
									Log.d ("修改昵称", "失败");
									Toast.makeText (UserCenterActivity.this,
											"数据被外星人带走了",
											Toast.LENGTH_SHORT).show ();
									break;
								case ":1":
									Log.d ("修改昵称", "成功");
									writeNameToSQLite (name);
									writeNameToMemory (name);
									Values.setChangeName (true);
									setResult (Values.RES_CHANGE_SOMETHING);
									dialog.cancel ();
									Toast.makeText (UserCenterActivity.this,
											"改好了", Toast.LENGTH_SHORT).show ();
							}
							bottomDialog.cancel ();
							call.cancel ();
						}

						@Override public void onFailure (Call<String> call, Throwable t)
						{
							Log.d ("修改昵称", t.toString ());
							Toast.makeText (UserCenterActivity.this,
									"服务器在维护啦",
									Toast.LENGTH_SHORT).show ();
							bottomDialog.cancel ();
							call.cancel ();
						}
					});
				}
			}
		});

		textView = contentView.findViewById (R.id.nameCancel);
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

	public void changePassword (View view)
	{
		boolean networkState = NetworkDetector.detect (this);
		if (!networkState)
		{
			Toast.makeText (this, "网络开小差了",
					Toast.LENGTH_SHORT).show ();
			return;
		}

		dialog = new Dialog (this, R.style.BottomDialog);
		View contentView = LayoutInflater.from (this).inflate
				(R.layout.dialog_content_password, null);

		final EditText editTextOld = contentView.findViewById (R.id.editTextPasswordOld);
		ImageView imageView = contentView.findViewById (R.id.imageViewPasswordOldClear);
		EditTextClear.addClearListener (editTextOld, imageView);
		final EditText editTextNew = contentView.findViewById (R.id.editTextPasswordNew);
		imageView = contentView.findViewById (R.id.imageViewPasswordNewClear);
		EditTextClear.addClearListener (editTextNew, imageView);
		final EditText editTextAgain = contentView.findViewById (R.id.editTextPasswordAgain);
		imageView = contentView.findViewById (R.id.imageViewPasswordAgainClear);
		EditTextClear.addClearListener (editTextAgain, imageView);

		TextView textView = contentView.findViewById (R.id.passwordSave);
		textView.setBackgroundResource (Values.getSelector ());
		textView.setOnClickListener (new View.OnClickListener ()
		{
			@Override public void onClick (View v)
			{
				final String passwordOld = String.valueOf (editTextOld.getText ());
				final String passwordNew = String.valueOf (editTextNew.getText ());
				final String passwordAgain = String.valueOf (editTextAgain.getText ());

				if (passwordOld.length () == 0)
					Toast.makeText (UserCenterActivity.this,
							"没有原密码可不行", Toast.LENGTH_SHORT).show ();
				else if (passwordOld.length () < 6 || passwordOld.length () > 16)
					Toast.makeText (UserCenterActivity.this,
							"原密码长度都明显不对了", Toast.LENGTH_SHORT).show ();
				else if (passwordNew.length () == 0)
					Toast.makeText (UserCenterActivity.this,
							"不设置新密码了吗", Toast.LENGTH_SHORT).show ();
				else if (passwordNew.length () < 6 || passwordNew.length () > 16)
					Toast.makeText (UserCenterActivity.this,
							"太长太短的密码都不行", Toast.LENGTH_SHORT).show ();
				else if (passwordAgain.length () == 0)
					Toast.makeText (UserCenterActivity.this,
							"再输入一次以防万一吧", Toast.LENGTH_SHORT).show ();
				else if (passwordAgain.length () < 6 || passwordAgain.length () > 16)
					Toast.makeText (UserCenterActivity.this,
							"太长太短的密码都不行", Toast.LENGTH_SHORT).show ();
				else if (!passwordNew.equals (passwordAgain))
					Toast.makeText (UserCenterActivity.this,
							"两次密码竟然不一样", Toast.LENGTH_SHORT).show ();
				else if (!Pattern.matches ("\\w*", passwordOld)
						|| !Pattern.matches ("\\w*", passwordNew))
					Toast.makeText (UserCenterActivity.this,
							"密码格式不对吧", Toast.LENGTH_SHORT).show ();
				else
				{
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
									Log.d ("修改密码", "失败");
									Toast.makeText (UserCenterActivity.this,
											"数据被外星人带走了",
											Toast.LENGTH_SHORT).show ();
									break;
								case ":0":
									Log.d ("修改密码", "失败");
									Toast.makeText (UserCenterActivity.this,
											"原密码错了",
											Toast.LENGTH_SHORT).show ();
									break;
								case ":1":
									Log.d ("修改密码", "成功");
									dialog.cancel ();
									setResult (Values.RES_CHANGE_PASSWORD);
									finish ();
							}
							call.cancel ();
						}

						@Override public void onFailure (Call<String> call, Throwable t)
						{
							Log.d ("修改密码", t.toString ());
							Toast.makeText (UserCenterActivity.this,
									"服务器在维护啦",
									Toast.LENGTH_SHORT).show ();
							call.cancel ();
						}
					});
				}
			}
		});

		textView = contentView.findViewById (R.id.passwordCancel);
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

	public void buttonSignOut (View view)
	{
		setResult (Values.RES_SIGN_OUT);
		finish ();
	}

	/**
	 * 写入SQLite排序方式
	 */
	private void writeSortToSQLite ()
	{
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		db.execSQL ("update setting set sort = \"" + Values.getSort () + "\"");
		db.close ();
	}

	/**
	 * 写入SQLite主题颜色
	 */
	private void writeThemeToSQLite (int color)
	{
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		db.execSQL ("update setting set color = \"" + color + "\"");
		db.close ();
	}

	/**
	 * 写入SQLite昵称
	 */
	private void writeNameToSQLite (String name)
	{
		helper = SQLiteHelper.getHelper (this);
		SQLiteDatabase db = helper.getWritableDatabase ();
		db.execSQL ("update user set name = \"" + name + "\"");
		db.close ();
	}

	/**
	 * 写入内存昵称
	 */
	private void writeNameToMemory (String name)
	{
		user.setName (name);
	}
}
