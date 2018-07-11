package com.lin.utils;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;

public interface RequestServes
{
	@POST ("/user/findAccount")
	@FormUrlEncoded
	Call<String> findAccount (@Field ("account") String account);

	@POST ("/user/signUp")
	@FormUrlEncoded
	Call<String> signUp (@Field ("account") String account,
			@Field ("password") String password);

	@POST ("/user/signIn")
	@FormUrlEncoded
	Call<String> signIn (@Field ("account") String account,
			@Field ("password") String password);

	@POST ("/user/getAvatar")
	@FormUrlEncoded
	Call<String> getAvatar (@Field ("account") String account);

	@Multipart
	@POST ("/user/changeAvatar")
	Call<String> changeAvatar (@PartMap () Map<String, RequestBody> account,
			@Part MultipartBody.Part avatar);

	@POST ("/user/changeName")
	@FormUrlEncoded
	Call<String> changeName (@Field ("account") String account,
			@Field ("name") String name);

	@POST ("/user/changePassword")
	@FormUrlEncoded
	Call<String> changePassword (@Field ("account") String account,
			@Field ("passwordOld") String passwordOld,
			@Field ("passwordNew") String passwordNew);

	@POST ("/user/forgetPassword")
	@FormUrlEncoded
	Call<String> forgetPassword (@Field ("account") String account,
			@Field ("password") String password);

	@POST ("/note/upload")
	Call<String> upload (@Body RequestBody jsonBody);

	@POST ("/note/download")
	@FormUrlEncoded
	Call<String> download (@Field ("account") String account);
}
