package com.lin.bean;

import android.graphics.Bitmap;

public class User
{
	private static User user;

	private String account;
	private String name;
	private Bitmap avatar;

	private boolean hasAvatar;

	private User (String account, String name, Bitmap avatar, boolean hasAvatar)
	{
		this.account = account;
		this.name = name;
		this.avatar = avatar;
		this.hasAvatar = hasAvatar;
	}

	public static User getUser ()
	{
		if (user == null)
			user = new User ("", "", null, false);
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

	public boolean isHasAvatar ()
	{
		return hasAvatar;
	}

	public void setHasAvatar (boolean hasAvatar)
	{
		this.hasAvatar = hasAvatar;
	}

	public void signIn (String account, String name)
	{
		this.account = account;
		this.name = name;
	}

	public static void signOut ()
	{
		user = null;
	}
}
