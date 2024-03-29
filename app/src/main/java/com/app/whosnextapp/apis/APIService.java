package com.app.whosnextapp.apis;

import com.app.whosnextapp.model.LikeUnlikePostModel;
import com.app.whosnextapp.model.ShareToModel;
import com.app.whosnextapp.model.SnippetModel;
import com.app.whosnextapp.model.UploadPictureModel;
import com.app.whosnextapp.model.UserDetailModel;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Url;

public interface APIService {

    @Headers("Content-Type:application/json")
    @POST
    Call<ResponseBody> PostRequest(@Header("AccessToken") String token, @Url String url, @Body RequestBody postData);

    @Headers("Content-Type:application/json")
    @POST
    Call<ResponseBody> PostRequest(@Header("AccessToken") String token, @Url String url);

    @Headers("Content-Type:application/json")
    @POST
    Call<ResponseBody> PostRequest(@Url String url, @Body RequestBody postData);

    @Headers("Content-Type:application/json")
    @POST
    Call<ResponseBody> PostRequest(@Url String url);


    @Headers("Content-Type:application/json")
    @GET
    Call<ResponseBody> GetRequest(@Header("AccessToken") String token, @Url String url, @Body RequestBody postData);

    @Headers("Content-Type:application/json")
    @GET
    Call<ResponseBody> GetRequest(@Header("AccessToken") String token, @Url String url);

    @Headers("Content-Type:application/json")
    @GET
    Call<ResponseBody> GetRequest(@Url String url, @Body RequestBody postData);


    @Headers("Content-Type:application/json")
    @GET
    Call<ResponseBody> GetRequest(@Url String url);

    @Multipart
    @POST
    Call<ShareToModel> GetSharePost(@Url String url,
                                    @Part MultipartBody.Part image,
                                    @Part("Model") RequestBody jsonData);

    @Multipart
    @POST
    Call<UploadPictureModel.GroupVideoList> GetPicturePost(@Url String url,
                                                           @Part MultipartBody.Part image,
                                                           @Part("Model") RequestBody jsonData, @Header("AccessToken") String token);

    @Multipart
    @POST
    Call<SnippetModel> GetSnippetPicturePost(@Header("AccessToken") String token,
                                             @Url String url,
                                             @Part MultipartBody.Part image,
                                             @Part("SnippetsCaption") RequestBody SnippetsCaption);

    @Multipart
    @POST
    Call<SnippetModel> GetSnippetVideoPost(@Header("AccessToken") String token,
                                           @Url String url,
                                           @Part MultipartBody.Part video,
                                           @Part MultipartBody.Part image,
                                           @Part("SnippetsCaption") RequestBody SnippetsCaption);

    @Multipart
    @POST
    Call<SnippetModel> GetSnippetAudioPost(@Header("AccessToken") String token,
                                           @Url String url,
                                           @Part MultipartBody.Part audio,
                                           @Part MultipartBody.Part image,
                                           @Part("SnippetsCaption") RequestBody SnippetsCaption);


    @Multipart
    @POST
    Call<UserDetailModel> GetRegistration(@Url String url,
                                          @Part MultipartBody.Part video,
                                          @Part MultipartBody.Part image,
                                          @Part("model") RequestBody Customer);

    @Multipart
    @POST
    Call<LikeUnlikePostModel> GetUpdateProfile(@Header("AccessToken") String token,
                                               @Url String url,
                                               @Part MultipartBody.Part video,
                                               @Part MultipartBody.Part image,
                                               @Part("model") RequestBody Customer);

    @Multipart
    @POST
    Call<LikeUnlikePostModel> GetUpdateWithoutProfile(@Header("AccessToken") String token,
                                                      @Url String url,
                                                      @Part("model") RequestBody Customer);


    @Multipart
    @POST
    Call<LikeUnlikePostModel> GetAddBCLPost(@Header("AccessToken") String token,
                                            @Url String url,
                                            @Part MultipartBody.Part image,
                                            @Part("model") RequestBody BreastCancerLegacy);

    @Multipart
    @POST
    Call<LikeUnlikePostModel> GetEditBCLPost(@Header("AccessToken") String token,
                                             @Url String url,
                                             @Part("model") RequestBody BreastCancerLegacy);


    @Multipart
    @POST
    Call<LikeUnlikePostModel> AddNewAdsImagePost(@Header("AccessToken") String token,
                                                 @Url String url,
                                                 @Part MultipartBody.Part image,
                                                 @Part("model") RequestBody Ad);

    @Multipart
    @POST
    Call<LikeUnlikePostModel> AddNewAdsVideoPost(@Header("AccessToken") String token,
                                                 @Url String url,
                                                 @Part MultipartBody.Part video,
                                                 @Part("model") RequestBody Ad);


}
