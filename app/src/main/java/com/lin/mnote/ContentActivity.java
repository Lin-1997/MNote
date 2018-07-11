package com.lin.mnote;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

import com.lin.utils.DialogToast;
import com.lin.utils.FileHelper;
import com.lin.utils.Values;

public class ContentActivity extends AppCompatActivity
{
	private EditText editText;
	private WebView webView;
	private boolean previewing = false;
	private String content;
	private int position;

	@Override
	protected void onCreate (Bundle savedInstanceState)
	{
		setTheme (Values.THEME);
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_content);

		Toolbar toolbar = findViewById (R.id.toolbar);
		toolbar.setBackgroundResource (Values.COLOR);
		setSupportActionBar (toolbar);
		getSupportActionBar ().setDisplayHomeAsUpEnabled (true);

		initWebView ();

		Intent intent = getIntent ();
		content = intent.getStringExtra ("content");
		if (content == null)
			content = "";
		position = intent.getIntExtra ("position", -1);
		if (position != -1)
		{
			String date = intent.getStringExtra ("date");
			String time = intent.getStringExtra ("time").substring (0, 5);
			toolbar.setTitle (date + "  " + time);
		}

		editText = findViewById (R.id.editText);
		editText.setText (content);

		//输入法顶起界面
		getWindow ().setSoftInputMode
				(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
						| WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu)
	{
		getMenuInflater ().inflate (R.menu.menu_content, menu);
		return super.onCreateOptionsMenu (menu);
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item)
	{
		switch (item.getItemId ())
		{
			case android.R.id.home:
				if (previewing)
					fabPreview ();
				else
					fabSave ();
				break;
			case R.id.export:
				fabExport ();
				break;
			case R.id.preview:
				fabPreview ();
				break;
		}
		return true;
	}

	@Override
	public boolean onKeyDown (int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK && previewing)
		{
			fabPreview ();
			return true;
		}
		else if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			fabSave ();
			return true;
		}
		return super.onKeyDown (keyCode, event);
	}

	private void initWebView ()
	{
		webView = findViewById (R.id.webView);
		WebSettings webSettings = webView.getSettings ();
		// 允许JS
		webSettings.setJavaScriptEnabled (true);
		// 设置允许JS弹窗
		webSettings.setJavaScriptCanOpenWindowsAutomatically (true);
		// access Assets and resources
		webSettings.setAllowFileAccess (true);
		webSettings.setAppCacheEnabled (false);
		// 提高渲染优先级
		webSettings.setRenderPriority (WebSettings.RenderPriority.HIGH);
		// 设置编码格式
		webSettings.setDefaultTextEncodingName ("utf-8");
		// 支持自动加载图片
		webSettings.setLoadsImagesAutomatically (true);
		// 关闭webView中缓存
		webSettings.setCacheMode (WebSettings.LOAD_NO_CACHE);
		// 将图片调整到适合webView的大小
		webSettings.setUseWideViewPort (true);
		// 缩放至屏幕的大小
		webSettings.setLoadWithOverviewMode (true);
		// 支持缩放，默认为true。
		webSettings.setSupportZoom (true);
		// 设置内置的缩放控件。若为false，则该WebView不可缩放
		webSettings.setBuiltInZoomControls (false);
		// 隐藏原生的缩放控件
		webSettings.setDisplayZoomControls (false);
		// 隐藏滚动条
		//webView.setScrollBarStyle (WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		// 使页面获取焦点，防止点击无响应
		webView.requestFocus ();
		// 设置WebViewClient
		webView.setWebViewClient (new WebViewClient ()
		{
			@Override
			public void onReceivedError (WebView view, WebResourceRequest request, WebResourceError error)
			{
				// 当网页加载出错时，加载本地错误文件
				webView.loadUrl ("file:///android_asset/error.html");
			}
		});
		// 加载本地HTML
		webView.loadUrl ("file:///android_asset/MNote.html");
	}

	public void fabPreview ()
	{
		if (previewing)
		{
			previewing = false;
			editText.setVisibility (View.VISIBLE);
			webView.setVisibility (View.INVISIBLE);
		}
		else
		{
			previewing = true;
			editText.setVisibility (View.INVISIBLE);
			String text = editText.getText ().toString ().replaceAll ("\\n", "\\\\n");
			String js = "javascript:compile('" + text + "')";
			webView.loadUrl (js);
			webView.setVisibility (View.VISIBLE);
		}
	}

	public void fabSave ()
	{
		String newContent = editText.getText ().toString ();
		if (content.equals (newContent) || newContent.length () == 0)
			finish ();

		Intent intent = new Intent ();
		intent.putExtra ("content", newContent);
		intent.putExtra ("position", position);
		setResult (Values.RES_EDIT_SAVE, intent);
		finish ();
	}

	public void fabExport ()
	{
		String path = Environment.getExternalStoragePublicDirectory
				(Environment.DIRECTORY_DOWNLOADS).getPath ();
		if (FileHelper.String2Markdown (path, editText.getText ().toString ()))
			DialogToast.showDialogToast (this, "导出到Download目录里面了");
		else
			DialogToast.showDialogToast (this, "导出失败了");
	}
}
