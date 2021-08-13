package com.stimednp.javasamplemvvm.viewmodel;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.stimednp.javasamplemvvm.BuildConfig;
import com.stimednp.javasamplemvvm.model.MovieList;
import com.stimednp.javasamplemvvm.model.MovieResponse;
import com.stimednp.javasamplemvvm.remoteservice.ApiClient;
import com.stimednp.javasamplemvvm.remoteservice.ApiInterface;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by rivaldy on Aug/03/2020.
 * Find me on my lol Github :D -> https://github.com/im-o
 */

public class MovieViewModel extends ViewModel {
    private MutableLiveData<ArrayList<MovieList>> listMovie;

    public LiveData<ArrayList<MovieList>> getMovies() {
        if (listMovie == null) {
            listMovie = new MutableLiveData<>();
            loadMovies();
        }
        return listMovie;
    }

    private void loadMovies() { //do an asynchronous operation to fetch movies
        ApiInterface remoteService = ApiClient.getApiRemoteService();
        Call<MovieResponse> movieResponseCall = remoteService.getMovieList(BuildConfig.API_KEY, "en-US");
        movieResponseCall.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(@NonNull Call<MovieResponse> call, @NonNull Response<MovieResponse> response) {
                if (response.isSuccessful()){
                    if (response.body() != null) {
                        ArrayList<MovieList> movieList = response.body().getResults();
                        listMovie.postValue(movieList);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<MovieResponse> call, @NonNull Throwable t) {
                Log.e("INI", "ERROR -> " + t);
            }
        });
    }
}
