package com.lin.bean;

public class Note
{
	private String createDate;
	private String createTime;
	private StringBuilder content;

	public Note (String createDate, String createTime, StringBuilder content)
	{
		this.createDate = createDate;
		this.createTime = createTime;
		this.content = content;
	}

	public String getCreateDate ()
	{
		return createDate;
	}

	public void setCreateDate (String createDate)
	{
		this.createDate = createDate;
	}

	public String getCreateTime ()
	{
		return createTime;
	}

	public void setCreateTime (String createTime)
	{
		this.createTime = createTime;
	}

	public StringBuilder getContent ()
	{
		return content;
	}

	public void setContent (StringBuilder content)
	{
		this.content = content;
	}
}
