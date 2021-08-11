package com.stimednp.javasamplemvvm.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.stimednp.javasamplemvvm.databinding.ItemMovieBinding;
import com.stimednp.javasamplemvvm.model.MovieList;

import java.util.ArrayList;

/**
 * Created by rivaldy on Aug/03/2020.
 * Find me on my lol Github :D -> https://github.com/im-o
 */

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {
    private ArrayList<MovieList> movieList = new ArrayList<>();

    MovieAdapter() {
    }

    private ArrayList<MovieList> getMovieList() {
        return movieList;
    }

    void setMovieList(ArrayList<MovieList> items) {
        movieList.clear();
        movieList.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieAdapter.MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMovieBinding view = ItemMovieBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieAdapter.MovieViewHolder holder, int position) {
        holder.bindItem(getMovieList().get(position));
    }

    @Override
    public int getItemCount() {
        return getMovieList().size();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        private ItemMovieBinding binding;

        MovieViewHolder(ItemMovieBinding movieBinding) {
            super(movieBinding.getRoot());
            binding = movieBinding;
        }

        void bindItem(MovieList movieList) {
            String voteValue = String.valueOf(movieList.getVoteAverage());
            String urlImg = "https://image.tmdb.org/t/p/w220_and_h330_face" + movieList.getPosterPath();

            binding.tvTitle.setText(movieList.getOriginalTitle());
            binding.tvOverview.setText(movieList.getOverview());
            binding.tvVote.setText(voteValue);
            Picasso.get()
                    .load(urlImg)
                    .into(binding.imgPoster);
        }
    }
}
