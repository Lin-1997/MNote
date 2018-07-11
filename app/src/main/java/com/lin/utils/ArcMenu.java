package com.lin.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import com.lin.mnote.R;

public class ArcMenu extends ViewGroup
{
	//主菜单按钮旋转动画
	private Animation rotate;
	//子菜单按钮弹出动画
	private Animation[] translateOpen = new Animation[3];
	//子菜单按钮关闭动画
	private Animation[] translateClose = new Animation[3];
	//子菜单按钮渐变放大动画
	private AnimationSet scaleBig;
	//子菜单按钮渐变缩小动画
	private AnimationSet scaleSmall;
	//半径
	private int mRadius;
	//主菜单按钮开关状态
	private boolean opened = false;
	//外部使用的主菜单点击监听器
	private onMenuClickListener mMenuClickListener;
	//外部使用的子菜单点击监听器
	private onMenuItemClickListener mMenuItemClickListener;
	//主菜单按钮的位置偏移，关乎到子菜单按钮能不能完全藏在主菜单后面
	int offset;

	public ArcMenu (Context context)
	{
		this (context, null);
	}

	public ArcMenu (Context context, AttributeSet attrs)
	{
		this (context, attrs, 0);
	}

	public ArcMenu (Context context, AttributeSet attrs, int defStyleAttr)
	{
		super (context, attrs, defStyleAttr);

		//获取自定义属性的值
		TypedArray typedArray = context.getTheme ().obtainStyledAttributes
				(attrs, R.styleable.ArcMenu, defStyleAttr, 0);
		//从xml获取半径，没有就用默认值
		mRadius = (int) typedArray.getDimension
				(R.styleable.ArcMenu_radius, Density.dp2px (context, 100));
		//从xml获取偏移，没有就用默认值
		offset = (int) typedArray.getDimension
				(R.styleable.ArcMenu_offset, Density.dp2px (context, 16));
		//回收
		typedArray.recycle ();
	}

	@Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
	{
		int count = getChildCount ();
		//测量child
		for (int i = 0; i < count; ++i)
			measureChild (getChildAt (i), widthMeasureSpec, heightMeasureSpec);
		super.onMeasure (widthMeasureSpec, heightMeasureSpec);
	}

	@Override protected void onLayout (boolean changed, int l, int t, int r, int b)
	{
		if (changed)
		{
			int count = getChildCount ();

			//子类view都一样
			int viewWidth = getChildAt (0).getMeasuredWidth ();
			int viewHeight = getChildAt (0).getMeasuredHeight ();
			//父类view
			int parentWidth = getMeasuredWidth ();
			int parentHeight = getMeasuredHeight ();

			for (int i = 0; i < count; ++i)
			{
				View view = getChildAt (i);
				view.setVisibility (INVISIBLE);
				view.setClickable (false);
				view.setFocusable (false);

				//子类view右边界距离父类view右边界的长度
				int right = (int) (mRadius * Math.sin (Math.PI / 2 / (count - 1) * (count - 1 - i)));
				//子类view下边界距离父类view下边界的长度
				int bottom = (int) (mRadius * Math.cos (Math.PI / 2 / (count - 1) * (count - 1 - i)));

				view.layout (parentWidth - viewWidth - right - offset,
						parentHeight - viewHeight - bottom - offset,
						parentWidth - right - offset,
						parentHeight - bottom - offset);
			}
		}
	}

	public boolean isOpened ()
	{
		return opened;
	}

	//外部使用的主菜单点击监听器
	public interface onMenuClickListener
	{
		void onClick (View view);
	}

	public void setOnMenuClickListener (onMenuClickListener mMenuClickListener)
	{
		this.mMenuClickListener = mMenuClickListener;
	}

	//外部使用的子菜单点击监听器
	public interface onMenuItemClickListener
	{
		void onClick (View view);
	}

	public void setOnMenuItemClickListener (onMenuItemClickListener mMenuItemClickListener)
	{
		this.mMenuItemClickListener = mMenuItemClickListener;
	}

	//设置主菜单按钮
	public void setMenu (View mButton)
	{
		mButton.setOnClickListener (new OnClickListener ()
		{
			@Override public void onClick (View v)
			{
				//主菜单按钮旋转动画
				rotateButton (v);
				//展开或收回子菜单按钮
				toggleMenu ();
				opened = !opened;

				//执行外部的点击事件
				if (mMenuClickListener != null)
					mMenuClickListener.onClick (v);
			}
		});
	}

	private void rotateButton (View v)
	{
		if (rotate == null)
		{
			rotate = new RotateAnimation (0f, 360f,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			rotate.setDuration (Values.duration * 5 / 3);
			rotate.setInterpolator (new AccelerateDecelerateInterpolator ());
			rotate.setFillAfter (false);
		}
		v.startAnimation (rotate);
	}

	public void toggleMenu ()
	{
		int count = getChildCount ();
		final View[] view = new View[count];

		for (int i = 0; i < count; ++i)
		{
			view[i] = getChildAt (i);

			int cl = (int) (mRadius * Math.sin (Math.PI / 2 / (count - 1) * (count - 1 - i)));
			int ct = (int) (mRadius * Math.cos (Math.PI / 2 / (count - 1) * (count - 1 - i)));

			final int index = i;
			if (!opened)
			{
				if (translateOpen[i] == null)
				{
					translateOpen[i] = new TranslateAnimation (cl, 0, ct, 0);
					translateOpen[i].setDuration (Values.duration);
					translateOpen[i].setStartOffset ((count - 1 - i) * Values.duration / count);
					translateOpen[i].setFillAfter (false);
					translateOpen[i].setInterpolator (new AccelerateDecelerateInterpolator ());
					translateOpen[i].setAnimationListener (new Animation.AnimationListener ()
					{
						@Override public void onAnimationStart (Animation animation) { }

						@Override public void onAnimationEnd (Animation animation)
						{
							view[index].setVisibility (VISIBLE);
							view[index].setClickable (true);
							view[index].setFocusable (true);
						}

						@Override public void onAnimationRepeat (Animation animation) { }
					});
				}
				view[i].startAnimation (translateOpen[i]);
			}
			else
			{
				if (translateClose[i] == null)
				{
					translateClose[i] = new TranslateAnimation (0, cl, 0, ct);
					translateClose[i].setDuration (Values.duration);
					translateClose[i].setStartOffset ((count - 1 - i) * Values.duration / count);
					translateClose[i].setFillAfter (false);
					translateClose[i].setInterpolator (new AccelerateDecelerateInterpolator ());
					translateClose[i].setAnimationListener (new Animation.AnimationListener ()
					{
						@Override public void onAnimationStart (Animation animation)
						{
							view[index].setVisibility (INVISIBLE);
							view[index].setClickable (false);
							view[index].setFocusable (false);
						}

						@Override public void onAnimationEnd (Animation animation) { }

						@Override public void onAnimationRepeat (Animation animation) { }
					});
				}
				view[i].startAnimation (translateClose[i]);
			}

			if (!view[i].hasOnClickListeners ())
			{
				view[i].setOnClickListener (new OnClickListener ()
				{
					@Override public void onClick (View v)
					{
						menuItemAnim (index);
						opened = !opened;

						//执行外部的点击事件
						if (mMenuItemClickListener != null)
							mMenuItemClickListener.onClick (v);
					}
				});
			}
		}
	}

	private void menuItemAnim (int position)
	{
		for (int i = 0; i < getChildCount (); ++i)
		{
			View view = getChildAt (i);
			//中间的按钮是页面跳转，播放动画体验不好
			if (position != 1)
			{
				if (i == position)
					view.startAnimation (scaleBigAnim ());
				else
					view.startAnimation (scaleSmallAnim ());
			}
			view.setClickable (false);
			view.setFocusable (false);
			view.setVisibility (INVISIBLE);
		}
	}

	private Animation scaleBigAnim ()
	{
		if (scaleBig == null)
		{
			scaleBig = new AnimationSet (true);
			//放大
			ScaleAnimation scaleAnimation = new ScaleAnimation
					(1f, 1.5f, 1f, 1.5f,
							Animation.RELATIVE_TO_SELF, 0.5f,
							Animation.RELATIVE_TO_SELF, 0.5f);
			//渐变透明
			AlphaAnimation alphaAnimation = new AlphaAnimation (1f, 0f);
			scaleBig.addAnimation (scaleAnimation);
			scaleBig.addAnimation (alphaAnimation);
			scaleBig.setDuration (Values.duration);
			scaleBig.setFillAfter (false);
		}
		return scaleBig;
	}

	private Animation scaleSmallAnim ()
	{
		if (scaleSmall == null)
		{
			scaleSmall = new AnimationSet (true);
			//缩小
			ScaleAnimation scaleAnimation = new ScaleAnimation
					(1f, 0f, 1f, 0f,
							Animation.RELATIVE_TO_SELF, 0.5f,
							Animation.RELATIVE_TO_SELF, 0.5f);
			//渐变透明
			AlphaAnimation alphaAnimation = new AlphaAnimation (1f, 0f);
			scaleSmall.addAnimation (scaleAnimation);
			scaleSmall.addAnimation (alphaAnimation);
			scaleSmall.setDuration (Values.duration);
			scaleSmall.setFillAfter (false);
		}
		return scaleSmall;
	}
}