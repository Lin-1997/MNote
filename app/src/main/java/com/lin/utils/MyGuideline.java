package com.lin.utils;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.util.AttributeSet;

/**
 * 实际上就是把新版本ConstraintLayout有的
 * public void setGuidelinePercent (float ratio)这个函数加进来而已
 * 都是因为UnfoldAndZoomScrollView不兼容新版本ConstraintLayout
 */
public class MyGuideline extends Guideline
{
	public MyGuideline (Context context)
	{
		super (context);
	}

	public MyGuideline (Context context, AttributeSet attrs)
	{
		super (context, attrs);
	}

	public MyGuideline (Context context, AttributeSet attrs, int defStyleAttr)
	{
		super (context, attrs, defStyleAttr);
	}

	//ConstraintLayout1.0.0开始提供的函数
	public void setGuidelinePercent (float ratio)
	{
		ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) this.getLayoutParams ();
		params.guidePercent = ratio;
		this.setLayoutParams (params);
	}
}
