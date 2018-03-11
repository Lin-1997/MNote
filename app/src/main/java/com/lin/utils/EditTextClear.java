package com.lin.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

public class EditTextClear
{
	public static void addClearListener (final EditText editText,
			final ImageView imageView)
	{
		editText.addTextChangedListener (new TextWatcher ()
		{
			@Override public void beforeTextChanged (CharSequence s,
					int start, int count, int after)
			{

			}

			@Override public void onTextChanged (CharSequence s,
					int start, int before, int count)
			{

			}

			@Override public void afterTextChanged (Editable s)
			{
				if (s.length () > 0)
					imageView.setVisibility (View.VISIBLE);
				else
					imageView.setVisibility (View.INVISIBLE);
			}
		});

		editText.setOnFocusChangeListener (new View.OnFocusChangeListener ()
		{
			@Override public void onFocusChange (View v, boolean hasFocus)
			{
				if (hasFocus && editText.getText ().length () > 0)
					imageView.setVisibility (View.VISIBLE);
				else
					imageView.setVisibility (View.INVISIBLE);
			}
		});

		imageView.setOnClickListener (new View.OnClickListener ()
		{
			@Override public void onClick (View v)
			{
				editText.setText ("");
			}
		});
	}
}
