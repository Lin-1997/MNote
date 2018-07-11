package com.lin.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.lin.mnote.R;

public class DialogToast
{
	private static Dialog dialog;

	public static void showDialogToast (Context context, String text)
	{
		dialog = new Dialog (context, R.style.BottomDialog);
		View contentView = LayoutInflater.from (context).inflate
				(R.layout.dialog_toast, null);

		TextView textView = contentView.findViewById (R.id.textView);
		textView.setOnClickListener (new View.OnClickListener ()
		{
			@Override public void onClick (View v)
			{
				dialog.cancel ();
			}
		});

		dialog.setContentView (contentView);
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)
				contentView.getLayoutParams ();
		params.width = context.getResources ().getDisplayMetrics ().widthPixels
				- Density.dp2px (context, 16f);
		params.bottomMargin = Density.dp2px (context, 8f);
		contentView.setLayoutParams (params);
		dialog.getWindow ().setGravity (Gravity.TOP);
		dialog.getWindow ().clearFlags (WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		dialog.getWindow ().setWindowAnimations (R.style.DialogToast_Animation);
		textView.setText (text);
		dialog.show ();
	}
}
