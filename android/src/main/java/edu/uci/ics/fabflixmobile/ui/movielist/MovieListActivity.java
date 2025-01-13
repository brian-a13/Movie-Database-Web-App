package edu.uci.ics.fabflixmobile.ui.movielist;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;
import android.widget.*;
import android.view.View;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import edu.uci.ics.fabflixmobile.R;
import edu.uci.ics.fabflixmobile.data.NetworkManager;
import edu.uci.ics.fabflixmobile.data.model.Movie;

import edu.uci.ics.fabflixmobile.ui.singlemovie.SingleMovieActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MovieListActivity extends AppCompatActivity {
    private final String host = "3.145.141.199";
    private final String port = "8443";
    private final String domain = "2023-fall-cs122b-brogle";
    private final String baseURL = "https://" + host + ":" + port + "/" + domain;
    private String pageNumber;
    private String movieTitle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movielist);
        final RequestQueue queue = NetworkManager.sharedManager(this).queue;

        Intent intent = getIntent();
        movieTitle = intent.getStringExtra("movieTitle");
        pageNumber = intent.getStringExtra("pageNumber");

        TextView textViewTitle = findViewById(R.id.pageNumber);
        textViewTitle.setText(pageNumber);

        Log.d("movieTitle: ", movieTitle);
        Log.d("pageNumber: ", pageNumber);
        Log.d("hitting endpoint of:", "/api/results?full_text_search=" + movieTitle + "&num_results_page=10&order_used=0&page=" + pageNumber);
        final StringRequest searchRequest = new StringRequest(
                Request.Method.GET,
                baseURL + "/api/results?full_text_search=" + movieTitle + "&num_results_per_page=10&order_used=0&page=" + pageNumber,
                response -> {
                    Log.d("movies.success", "Movie list returned");
                    Log.d("movies.returned", response);
                    //Complete and destroy login activity once successful

                    try {
                        final ArrayList<Movie> movies = new ArrayList<>();
                        JSONArray jsonArray = new JSONArray(response);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String movieId = jsonObject.getString("movie_id");
                            String moviesListedTitle = jsonObject.getString("movie_title");
                            String moviesListedYear = jsonObject.getString("movie_year");
                            String movieDirector = jsonObject.getString("movie_director");
                            String movieRating = jsonObject.getString("movie_rating");
                            String movieGenres = parseMovieStarsAndGenres(jsonObject.getString("movie_genres"));
                            String movieStars = parseMovieStarsAndGenres(jsonObject.getString("movie_stars"));
                            movies.add(new Movie(movieId, moviesListedTitle, moviesListedYear, movieRating, movieDirector, movieGenres, movieStars));

                        }
                        Log.d("movies", String.valueOf(movies.size()));
                        MovieListViewAdapter adapter = new MovieListViewAdapter(this, movies);
                        ListView listView = findViewById(R.id.list);
                        listView.setAdapter(adapter);
                        listView.setOnItemClickListener((parent, view, position, id) -> {
                            Movie movie = movies.get(position);
                            // if a movie is clicked on, create a new intent that goes to the single movie
                            // activity and passing through the movieId
                            Intent goToIntent = new Intent(this, SingleMovieActivity.class);
                            goToIntent.putExtra("movieId", movie.getId());
                            startActivity(goToIntent);
                        });

                        Button prevButton = findViewById(R.id.prevButton);
                        Button nextButton = findViewById(R.id.nextButton);

                        // Hide prevButton if the pageNumber string is equal to "1"
                        if (pageNumber.equals("1")) {
                            prevButton.setVisibility(View.GONE);
                        } else {
                            prevButton.setVisibility(View.VISIBLE);
                        }

                        // Hide nextButton if there are less than 10 movies
                        if (movies.size() < 10) {
                            nextButton.setVisibility(View.GONE);
                        } else {
                            nextButton.setVisibility(View.VISIBLE);
                        }

                        // Set click listeners for the buttons
                        prevButton.setOnClickListener(v -> onPrevButtonClick());
                        nextButton.setOnClickListener(v -> onNextButtonClick());


                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    // error
                    Log.d("search.error", error.toString());
                }) {
            @Override
            public Map<String, String> getHeaders() {
                // Pass the token for authentication
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + "nuncaSerMadridsta10");
                return headers;
            }
        };
        queue.add(searchRequest);
    }

    public static String parseMovieStarsAndGenres(String input) {
        if (input.equals("null")) {
            return "null";
        }
        // Splitting the string based on comma and colon
        String[] parts = input.split(", ");

        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            // Splitting each part to get the name after the colon
            String[] name = part.split(": ");
            if (name.length == 2) {
                result.append(name[1]);
                result.append(", ");
            }
        }

        // Removing the extra comma and space at the end
        if (result.length() > 0) {
            result.delete(result.length() - 2, result.length());
        }

        return result.toString();
    }

    public void onPrevButtonClick() {
        int page = Integer.parseInt(pageNumber);
        if (page > 1) {
            page--;
        }
        pageNumber = String.valueOf(page);
        finish();
        Intent MovieListPage = new Intent(MovieListActivity.this, MovieListActivity.class);
        MovieListPage.putExtra("movieTitle", movieTitle);
        MovieListPage.putExtra("pageNumber", pageNumber);
        startActivity(MovieListPage);
    }

    public void onNextButtonClick() {
        int page = Integer.parseInt(pageNumber);
        page++;
        pageNumber = String.valueOf(page);
        finish();
        Intent MovieListPage = new Intent(MovieListActivity.this, MovieListActivity.class);
        MovieListPage.putExtra("movieTitle", movieTitle);
        MovieListPage.putExtra("pageNumber", pageNumber);
        startActivity(MovieListPage);
    }
}