package com.lin.utils;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface RequestServes
{
	@POST ("/user/checkAccount")
	@FormUrlEncoded
	Call<String> checkAccount (@Field ("account") String account);

	@POST ("/user/register")
	@FormUrlEncoded
	Call<String> register (@Field ("account") String account,
			@Field ("password") String password);

	@POST ("/user/signIn")
	@FormUrlEncoded
	Call<String> signIn (@Field ("account") String account,
			@Field ("password") String password);

	@POST ("/user/changeName")
	@FormUrlEncoded
	Call<String> changeName (@Field ("account") String account,
			@Field ("name") String name);

	@POST ("/user/changePassword")
	@FormUrlEncoded
	Call<String> changePassword (@Field ("account") String account,
			@Field ("passwordOld") String passwordOld,
			@Field ("passwordNew") String passwordNew);
}
