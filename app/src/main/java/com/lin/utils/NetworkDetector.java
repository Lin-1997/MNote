package com.lin.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkDetector
{
	//判断当前是否能连接网络
	public static boolean detect (Activity activity)
	{
		ConnectivityManager connectivityManager = (ConnectivityManager) activity.
				getApplicationContext ().getSystemService (Context.CONNECTIVITY_SERVICE);

		if (connectivityManager == null)
			return false;

		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo ();
		return networkInfo != null && networkInfo.isAvailable ();
	}
}
