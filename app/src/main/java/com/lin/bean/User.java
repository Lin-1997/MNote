package com.lin.bean;

public class User
{
	private static User user;

	private String account;
	private String name;

	private User (String account, String name)
	{
		this.account = account;
		this.name = name;
	}

	public static User getUser ()
	{
		if (user == null)
			user = new User ("", "");
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
