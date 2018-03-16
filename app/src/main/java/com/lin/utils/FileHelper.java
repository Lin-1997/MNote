package com.lin.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import java.io.File;

public class FileHelper
{
	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static String[] PERMISSIONS_STORAGE = {
			"android.permission.READ_EXTERNAL_STORAGE",
			"android.permission.WRITE_EXTERNAL_STORAGE"};

	public static boolean hasSdcard ()
	{
		return Environment.getExternalStorageState ().equals
				(Environment.MEDIA_MOUNTED);
	}

	public static void verifyStoragePermissions (Activity activity)
	{
		int permission = ActivityCompat.checkSelfPermission (activity,
				"android.permission.WRITE_EXTERNAL_STORAGE");
		if (permission != PackageManager.PERMISSION_GRANTED)
		{
			ActivityCompat.requestPermissions (activity, PERMISSIONS_STORAGE,
					REQUEST_EXTERNAL_STORAGE);
		}
	}

	public static void createDir (Context context)
	{
		String path = context.getExternalFilesDir (Environment.DIRECTORY_DCIM).getPath ();
		File dir = new File (path);
		if (!dir.exists ())
			dir.mkdirs ();
	}
}
