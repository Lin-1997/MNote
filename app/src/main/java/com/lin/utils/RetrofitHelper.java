package com.lin.utils;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitHelper
{
	private static Retrofit retrofit;

	public static Retrofit getRetrofit ()
	{
		if (retrofit == null)
			retrofit = new Retrofit.Builder ().
					baseUrl ("http://120.78.155.180:3000")
					//增加返回值为String的支持
					.addConverterFactory (ScalarsConverterFactory.create ())
					//增加返回值为JSON的支持(以实体类返回)
					.addConverterFactory (GsonConverterFactory.create ())
					//增加返回值为Observable<T>的支持
					.addCallAdapterFactory (RxJavaCallAdapterFactory.create ())
					.build ();
		return retrofit;
	}
}
