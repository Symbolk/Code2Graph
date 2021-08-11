package com.stimednp.javasamplemvvm.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.stimednp.javasamplemvvm.databinding.ActivityMainBinding;
import com.stimednp.javasamplemvvm.model.MovieList;
import com.stimednp.javasamplemvvm.viewmodel.MovieViewModel;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        initViewModel();
    }

    private void initViewModel() {
        MovieViewModel viewModel = new ViewModelProvider(this).get(MovieViewModel.class);
        viewModel.getMovies().observe(this, this::initAdapter);
    }

    private void initAdapter(ArrayList<MovieList> movieLists) {
        MovieAdapter movieAdapter = new MovieAdapter();
        movieAdapter.setMovieList(movieLists);
        movieAdapter.notifyDataSetChanged();
        binding.rvMovie.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMovie.setAdapter(movieAdapter);
    }
}
