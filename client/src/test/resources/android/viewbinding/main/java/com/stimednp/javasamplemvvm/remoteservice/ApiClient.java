package com.stimednp.javasamplemvvm.remoteservice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stimednp.javasamplemvvm.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by rivaldy on Aug/03/2020.
 * Find me on my lol Github :D -> https://github.com/im-o
 */

public class ApiClient {
    public static ApiInterface getApiRemoteService() {
        final Gson gson = new GsonBuilder().create(); //init gson
        final HttpLoggingInterceptor httpInterceptor = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY); //to see result on log
        final OkHttpClient movieClient = new OkHttpClient().newBuilder().addInterceptor(httpInterceptor).build();

        Retrofit retrofit = new Retrofit
                    .Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(movieClient)
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        return retrofit.create(ApiInterface.class);
    }
}
