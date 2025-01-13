package edu.uci.ics.fabflixmobile.ui.search;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import edu.uci.ics.fabflixmobile.data.NetworkManager;
import edu.uci.ics.fabflixmobile.databinding.SearchActivityBinding;
import edu.uci.ics.fabflixmobile.ui.movielist.MovieListActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class searchActivity extends AppCompatActivity {

    private EditText movieTitle;

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

        SearchActivityBinding binding = SearchActivityBinding.inflate(getLayoutInflater());
        // upon creation, inflate and initialize the layout
        setContentView(binding.getRoot());

        movieTitle = binding.movieSearch;
        final Button searchButton = binding.search;

        //assign a listener to call a function to handle the user request when clicking a button
        searchButton.setOnClickListener(view -> search());
    }

    @SuppressLint("SetTextI18n")
    public void search() {
        // use the same network queue across our application
        final RequestQueue queue = NetworkManager.sharedManager(this).queue;
        // request type is GET
        // No actual request to the server needs to be made but just have to pass in
        // the search query to the next activity
        final StringRequest searchRequest = new StringRequest(
                Request.Method.GET,
                //baseURL + "/api/results?full_text_search=" + movieTitle.getText().toString(),
                baseURL + "/api/results?full_text_search=" + movieTitle.getText().toString(),
                response -> {
                    Log.d("search.success", "they hit search");
                    Log.d("movieName", movieTitle.getText().toString());
                    finish();
                    Intent MovieListPage = new Intent(searchActivity.this, MovieListActivity.class);
                    MovieListPage.putExtra("movieTitle", movieTitle.getText().toString());
                    MovieListPage.putExtra("pageNumber", "1");
                    startActivity(MovieListPage);
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
        // important: queue.add is where the login request is actually sent
        queue.add(searchRequest);
    }
}