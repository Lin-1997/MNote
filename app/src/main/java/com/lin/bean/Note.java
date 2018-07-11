package com.lin.bean;

public class Note
{
	private int id;
	private String updateDate;
	private String updateTime;
	private String content;
	private int status; //0所有操作已经上传，-1新建操作未上传，1更新未上传，-2下载待删除

	public Note (int id, String updateDate, String updateTime, String content, int status)
	{
		this.id = id;
		this.updateDate = updateDate;
		this.updateTime = updateTime;
		this.content = content;
		this.status = status;
	}

	public int getId ()
	{
		return id;
	}

	public void setId (int id)
	{
		this.id = id;
	}

	public String getUpdateDate ()
	{
		return updateDate;
	}

	public String getUpdateTime ()
	{
		return updateTime;
	}

	public String getContent ()
	{
		return content;
	}

	public int getStatus ()
	{
		return status;
	}

	public void setStatus (int status)
	{
		this.status = status;
	}
}