package com.lin.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

	public static void moveFile (File from, File to)
	{
		try
		{
			InputStream is = new FileInputStream (from);
			OutputStream os = new FileOutputStream (to);
			byte bytes[] = new byte[1024];
			int c;
			while ((c = is.read (bytes)) > 0)
				os.write (bytes, 0, c);
			is.close ();
			os.close ();
			deleteFile (from);
		}
		catch (IOException e)
		{
			e.printStackTrace ();
		}
	}

	public static void deleteFile (File file)
	{
		if (file.exists ())
			file.delete ();
	}

	public static Bitmap String2Bitmap (String string)
	{
		if (string.equals (""))
			return null;
		byte[] bytes = Base64.decode (string, Base64.DEFAULT);
		return BitmapFactory.decodeByteArray (bytes, 0, bytes.length);
	}

	public static void Bitmap2File (Bitmap bitmap, String path, String name)
	{
		File file = new File (path, name);
		try
		{
			FileOutputStream os = new FileOutputStream (file);
			bitmap.compress (Bitmap.CompressFormat.JPEG, 100, os);
			os.flush ();
			os.close ();
		}
		catch (IOException e)
		{
			e.printStackTrace ();
		}
	}

	public static boolean String2Markdown (String path, String content)
	{
		SimpleDateFormat dateFormat1 = new SimpleDateFormat ("yyyyMMdd", Locale.CHINA);
		SimpleDateFormat dateFormat2 = new SimpleDateFormat ("HHmmss", Locale.CHINA);
		Date date = new Date (System.currentTimeMillis ());
		String curDate = dateFormat1.format (date);
		String curTime = dateFormat2.format (date);

		try
		{
			File file = new File (path, curDate + curTime + ".md");
			FileWriter fileWriter = new FileWriter (file);
			fileWriter.write (content);
			fileWriter.flush ();
			fileWriter.close ();
			return true;
		}
		catch (IOException e)
		{
			e.printStackTrace ();
			return false;
		}
	}
}
