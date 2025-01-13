package edu.uci.ics.fabflixmobile.ui.singlemovie;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import edu.uci.ics.fabflixmobile.data.NetworkManager;
import edu.uci.ics.fabflixmobile.databinding.SingleMovieBinding;
import edu.uci.ics.fabflixmobile.ui.search.searchActivity;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;
import java.util.HashMap;
import java.util.Map;



public class SingleMovieActivity extends AppCompatActivity {
    private String movieId;
    private TextView title;
    private TextView rating;
    private TextView year;
    private TextView director;
    private TextView genres;
    private TextView stars;

    /*
      In Android, localhost is the address of the device or the emulator.
      To connect to your machine, you need to use the below IP address
     */
    private final String host = "3.145.141.199";
    private final String port = "8443";
    private final String domain = "2023-fall-cs122b-brogle";
    private final String baseURL = "https://" + host + ":" + port + "/" + domain;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SingleMovieBinding binding = SingleMovieBinding.inflate(getLayoutInflater());
        // upon creation, inflate and initialize the layout
        setContentView(binding.getRoot());

        title = binding.title;
        rating = binding.rating;
        year = binding.year;
        director = binding.director;
        genres = binding.genres;
        stars = binding.stars;
        final Button backButton = binding.back;
        backButton.setOnClickListener(view -> goToSearchPage());

        Intent intent = getIntent();
        // Retrieve the data using the keys specified in putExtra()
        if (intent != null) {
            movieId = intent.getStringExtra("movieId");
        }
        // get the movie info
        getMovieInfo();
    }

    public void goToSearchPage() {
        Intent goToIntent = new Intent(this, searchActivity.class);
        startActivity(goToIntent);
    }

    @SuppressLint("SetTextI18n")
    public void getMovieInfo() {
        // Use the same network queue across our application
        final RequestQueue queue = NetworkManager.sharedManager(this).queue;
        // now make that StringRequest with the description given
        final StringRequest singleMovieRequest = new StringRequest(
                Request.Method.GET,
                baseURL + "/api/single-movie?id=" + movieId,
                response -> {
                    Log.d("single-movie.success", response);
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response);
                        String movieTitle = jsonObject.getString("movie_title");
                        String movieRating = jsonObject.getString("movie_rating");
                        String movieYear = jsonObject.getString("movie_year");
                        String movieDirector = jsonObject.getString("movie_director");
                        String movieStars = jsonObject.getString("movie_stars");
                        String movieGenres = jsonObject.getString("movie_genres");
                        title.setText("Title: " + movieTitle);
                        // check if rating is null and if it is set it to "N/A" instead
                        if (movieRating.equals("null")) {
                            movieRating = "N/A";
                        }
                        rating.setText("Rating: " + movieRating);
                        // check if year is null and if it is set it to "N/A" instead
                        if (movieYear.equals("null")) {
                            movieYear = "N/A";
                        }
                        year.setText("Year: " + movieYear);
                        director.setText("Director: " + movieDirector);
                        // parse the stars and genres
                        // stars
                        String[] starsArray = movieStars.split(", ");
                        StringBuilder starsString = new StringBuilder();
                        for (String star : starsArray) {
                            String[] starInfo = star.split(": ");
                            starsString.append(starInfo[1]).append(", ");
                        }
                        starsString.deleteCharAt(starsString.length() - 1);
                        starsString.deleteCharAt(starsString.length() - 1);
                        stars.setText("Stars: " + starsString);
                        // genres
                        String[] genresArray = movieGenres.split(", ");
                        StringBuilder genresString = new StringBuilder();
                        for (String genre : genresArray) {
                            String[] genreInfo = genre.split(": ");
                            genresString.append(genreInfo[1]).append(", ");
                        }
                        genresString.deleteCharAt(genresString.length() - 1);
                        genresString.deleteCharAt(genresString.length() - 1);
                        genres.setText("Genres: " + genresString);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    // error
                    Log.d("single-movie.error", error.toString());
                }) {
            @Override
            public Map<String, String> getHeaders() {
                // Pass the token for authentication
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + "nuncaSerMadridsta10");
                return headers;
            }
        };
        // important: queue.add is where the login request is actually sent
        queue.add(singleMovieRequest);
    }
}
