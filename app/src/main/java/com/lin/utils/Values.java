package com.lin.utils;

import com.lin.mnote.R;

public class Values
{
	//进入登录界面
	public static final int REQ_SIGN_IN = 1;
	public static final int RES_SIGN_IN = 1;

	//进入注册界面
	public static final int REQ_SIGN_UP = 2;
	public static final int RES_SIGN_UP = 1;
	public static final int RES_ACCOUNT_OCCUPIED = 2;

	//进入忘记密码界面
	public static final int REQ_FORGET_PASSWORD = 3;
	public static final int RES_FORGET_PASSWORD = 1;

	//进入个人中心界面
	public static final int REQ_USER_CENTER = 4;
	public static final int RES_CHANGE_SOMETHING = 1;
	public static final int RES_CHANGE_THEME = 2;
	public static final int RES_CHANGE_PASSWORD = 3;
	public static final int RES_SIGN_OUT = 4;

	//进入拍照界面
	public static final int REQ_ACTION_IMAGE_CAPTURE = 5;
	//进入相册界面
	public static final int REQ_ACTION_PICK = 6;
	//进入裁剪界面
	public static final int REQ_ACTION_CROP = 7;

	public static final int CREATE = 0;
	public static final int UPDATE = 1;

	public static String accountRegex = "^((13[0-9])|(14[57])|(15[0-35-9])|(17[035-8])|(18[0-9])|166|19[89]|(147))\\d{8}$";
	public static String passwordRegex = "^\\w{6,16}$";
	public static String nonceRegex = "^\\d{6}$";

	private static int THEME = R.style.MyBlueTheme;

	private static int COLOR = R.color.colorBlue;

	private static int SELECTOR = R.drawable.selector_blue;

	private static int BACKGROUND = R.drawable.button_blue;

	private static int PROGRESS = R.drawable.progress_bar_blue;

	private static int SORT = CREATE;

	private static boolean CHANGE_AVATAR = false;

	private static boolean CHANGE_SORT = false;

	private static boolean CHANGE_NAME = false;

	public static int getTheme ()
	{
		return THEME;
	}

	public static void setTheme (int THEME)
	{
		Values.THEME = THEME;
	}

	public static int getColor ()
	{
		return COLOR;
	}

	public static void setColor (int COLOR)
	{
		Values.COLOR = COLOR;
	}

	public static int getSelector ()
	{
		return SELECTOR;
	}

	public static void setSelector (int SELECTOR)
	{
		Values.SELECTOR = SELECTOR;
	}

	public static int getBackground ()
	{
		return BACKGROUND;
	}

	public static void setBackground (int BACKGROUND)
	{
		Values.BACKGROUND = BACKGROUND;
	}

	public static int getProgress ()
	{
		return PROGRESS;
	}

	public static void setProgress (int PROGRESS)
	{
		Values.PROGRESS = PROGRESS;
	}

	public static int getSort ()
	{
		return SORT;
	}

	public static void setSort (int SORT)
	{
		Values.SORT = SORT;
	}

	public static boolean isChangeAvatar ()
	{
		return CHANGE_AVATAR;
	}

	public static void setChangeAvatar (boolean changeAvatar)
	{
		CHANGE_AVATAR = changeAvatar;
	}

	public static boolean isChangeSort ()
	{
		return CHANGE_SORT;
	}

	public static void setChangeSort (boolean changeSort)
	{
		CHANGE_SORT = changeSort;
	}

	public static boolean isChangeName ()
	{
		return CHANGE_NAME;
	}

	public static void setChangeName (boolean changeName)
	{
		CHANGE_NAME = changeName;
	}
}
