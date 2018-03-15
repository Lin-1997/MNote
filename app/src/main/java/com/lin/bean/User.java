package com.lin.bean;

import android.graphics.Bitmap;

public class User
{
	private static User user;

	private String account;
	private String name;
	private Bitmap avatar;

	private User (String account, String name, Bitmap avatar)
	{
		this.account = account;
		this.name = name;
		this.avatar = avatar;
	}

	public static User getUser ()
	{
		if (user == null)
			user = new User ("", "", null);
		return user;
	}

	public String getAccount ()
	{
		return account;
	}

	public String getName ()
	{
		return name;
	}

	public void setName (String name)
	{
		this.name = name;
	}

	public Bitmap getAvatar ()
	{
		return avatar;
	}

	public void setAvatar (Bitmap avatar)
	{
		this.avatar = avatar;
	}

	public void signIn (String account, String name, Bitmap avatar)
	{
		this.account = account;
		this.name = name;
		this.avatar = avatar;
	}

	public static void signOut ()
	{
		user = null;
	}
}
