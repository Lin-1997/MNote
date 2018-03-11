package com.lin.utils;

import android.content.Context;
import android.util.TypedValue;

public class Density
{
	public static int dp2px (Context context, float dpVal)
	{
		return (int) TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_DIP,
				dpVal, context.getResources ().getDisplayMetrics ());
	}
}
