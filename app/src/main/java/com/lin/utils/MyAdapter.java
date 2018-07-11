package com.lin.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Guideline;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.lin.bean.Note;
import com.lin.mnote.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder>
{
	private LayoutInflater layoutInflater;
	private List<Note> list;
	private OnItemClickListener onItemClickListener;
	private boolean deleteNote = false;

	public MyAdapter (Context context)
	{
		layoutInflater = LayoutInflater.from (context);
		list = new LinkedList<> ();
	}

	@NonNull @Override
	public MyViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType)
	{
		return new MyViewHolder (layoutInflater.inflate
				(R.layout.item_list_note, parent, false));
	}

	@Override
	public void onBindViewHolder (@NonNull final MyViewHolder holder, int position)
	{
		holder.textViewDay.setText (list.get (position).getUpdateDate ().substring (8, 10));
		holder.textViewMonth.setText (list.get (position).getUpdateDate ().substring (5, 7));
		//xml中做了单行限制，只要能超过一行，就会自动用省略代替
		String content = list.get (position).getContent ();
		if (content.length () > 20)
			content = content.substring (0, 20);
		holder.textViewDescription.setText (content);
		if (onItemClickListener != null)
		{
			holder.constraintLayout.setOnClickListener (new View.OnClickListener ()
			{
				@Override public void onClick (View v)
				{
					onItemClickListener.onItemClick (holder.constraintLayout, holder.getAdapterPosition ());
				}
			});
			holder.constraintLayout.setOnLongClickListener (new View.OnLongClickListener ()
			{
				@Override public boolean onLongClick (View v)
				{
					onItemClickListener.onItemLongClick (holder.constraintLayout, holder.getAdapterPosition ());
					return true;
				}
			});
		}
	}

	@Override
	public int getItemCount ()
	{
		return list.size ();
	}

	public Note getNote (int position)
	{
		return list.get (position);
	}

	public List<Note> getChangedNote ()
	{
		List<Note> noteList = new LinkedList<> ();
		for (Note note : list)
			if (note.getStatus () != 0)
				noteList.add (note);
		return noteList;
	}

	public List<Note> getDownloadNote ()
	{
		//此时原有的note的status都为-2，
		//新的都为0
		List<Note> noteList = new LinkedList<> ();
		for (Note note : list)
			if (note.getStatus () == 0)
				noteList.add (note);
		return noteList;
	}

	public void uploadDone ()
	{
		for (Note note : list)
			note.setStatus (0);
		deleteNote = false;
	}

	public void downloadPrepare ()
	{
		for (Note note : list)
			note.setStatus (-2);
	}

	public void downloadDone ()
	{
		for (int i = list.size () - 1; i >= 0; --i)
			if (list.get (i).getStatus () == -2)
			{
				list.remove (i);
				notifyItemRemoved (i);
			}
	}

	public int getId (int position)
	{
		if (position < 0 || position >= list.size ())
			return -1;
		return list.get (position).getId ();
	}

	public int getStatus (int position)
	{
		if (position < 0 || position >= list.size ())
			return -1;
		return list.get (position).getStatus ();
	}

	public String getContent (int position)
	{
		if (position < 0 || position >= list.size ())
			return "";
		return list.get (position).getContent ();
	}

	public String getUpdateDate (int position)
	{
		if (position < 0 || position >= list.size ())
			return "";
		return list.get (position).getUpdateDate ();
	}

	public String getUpdateTime (int position)
	{
		if (position < 0 || position >= list.size ())
			return "";
		return list.get (position).getUpdateTime ();
	}

	public void add (int id, String content, int status)
	{
		SimpleDateFormat dateFormat1 = new SimpleDateFormat ("yyyy-MM-dd", Locale.CHINA);
		SimpleDateFormat dateFormat2 = new SimpleDateFormat ("HH:mm:ss", Locale.CHINA);
		Date date = new Date (System.currentTimeMillis ());
		String curDate = dateFormat1.format (date);
		String curTime = dateFormat2.format (date);

		list.add (0, new Note (id, curDate, curTime, content, status));
		notifyItemInserted (0);
	}

	public void add (Note note)
	{
		list.add (note);
		notifyItemInserted (list.size () - 1);
	}

	public void del (int position)
	{
		if (position < 0 || position >= list.size ())
			return;
		list.remove (position);
		notifyItemRemoved (position);
	}

	public boolean isDeleteNote ()
	{
		return deleteNote;
	}

	public void setDeleteNote (boolean deleteNote)
	{
		this.deleteNote = deleteNote;
	}

	public interface OnItemClickListener
	{
		void onItemClick (View view, int position);

		void onItemLongClick (View view, int position);
	}

	public void setOnItemClickListener (OnItemClickListener onItemClickListener)
	{
		this.onItemClickListener = onItemClickListener;
	}

	class MyViewHolder extends RecyclerView.ViewHolder
	{
		ConstraintLayout constraintLayout;
		TextView textViewDay;
		TextView textViewMonth;
		TextView textViewDescription;
		Guideline GLMid;
		Guideline GLRight;

		MyViewHolder (View itemView)
		{
			super (itemView);
			constraintLayout = itemView.findViewById (R.id.constraintLayout);
			textViewDay = itemView.findViewById (R.id.textViewDay);
			textViewMonth = itemView.findViewById (R.id.textViewMonth);
			GLMid = itemView.findViewById (R.id.GLMid);
			GLRight = itemView.findViewById (R.id.GLRight);
			textViewDescription = itemView.findViewById (R.id.textViewDescription);
		}
	}
}
