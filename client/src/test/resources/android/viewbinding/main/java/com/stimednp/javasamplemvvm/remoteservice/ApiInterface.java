package com.stimednp.javasamplemvvm.remoteservice;

import com.stimednp.javasamplemvvm.model.MovieResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by rivaldy on Aug/03/2020.
 * Find me on my lol Github :D -> https://github.com/im-o
 */

public interface ApiInterface {
    @GET("discover/movie")
    Call<MovieResponse> getMovieList(@Query("api_key") String apiKey, @Query("language") String language);
}
