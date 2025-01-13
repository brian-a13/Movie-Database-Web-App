import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;



// Declaring a WebServlet called Top20Servlet, which maps to url "/api/top-20"
@WebServlet(name = "Top20Servlet", urlPatterns = "/api/top-20")
public class Top20Servlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    public static final String TOP_20_QUERY = "SELECT m.id AS movieId, m.title AS movieTitle, m.year AS movieYear, m.director AS movieDirector, r.rating AS movieRating, " +
            "( " +
            "   SELECT SUBSTRING_INDEX(GROUP_CONCAT(CONCAT(s.id, ': ', s.name) " +
            "                  ORDER BY star_counts.num_movies_star_acted_in DESC, s.name ASC " +
            "                  SEPARATOR ', '), ', ', 3) " +
            "   FROM stars_in_movies AS sim " +
            "   LEFT JOIN stars AS s ON sim.starId = s.id " +
            "   LEFT JOIN ( " +
            "       SELECT sim1.starId, COUNT(sim1.movieId) AS num_movies_star_acted_in " +
            "       FROM stars_in_movies AS sim1 " +
            "       GROUP BY sim1.starId " +
            "   ) AS star_counts ON sim.starId = star_counts.starId " +
            "   WHERE sim.movieId = m.id " +
            ") AS movieStars, " +
            "SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT CONCAT(g.id, ': ', g.name) ORDER BY g.name ASC SEPARATOR ', '), ', ', 3) AS movieGenres " +
            "FROM (SELECT m1.id AS id, m1.title AS title, m1.year AS year, m1.director AS director, r1.rating AS rating " +
            "FROM movies AS m1, ratings AS r1 " +
            "WHERE m1.id = r1.movieId " +
            "ORDER BY r1.rating DESC, m1.title ASC " +
            "LIMIT 20) AS m " +
            "LEFT JOIN ratings AS r ON m.id = r.movieId " +
            "LEFT JOIN genres_in_movies AS gim ON m.id = gim.movieId " +
            "LEFT JOIN genres AS g ON gim.genreId = g.id " +
            "GROUP BY m.id, m.title, m.year, m.director, r.rating " +
            "ORDER BY r.rating DESC, m.title ASC";
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json"); // Response mime type

        HttpSession session = request.getSession();
        PreviousMovieListUrl previousMovieListUrl = (PreviousMovieListUrl) session.getAttribute("previous_movie_list_url");
        // it must've been search (only add the ones we actually used)
        if (previousMovieListUrl == null) { //basically if not yet initialized
            previousMovieListUrl = new PreviousMovieListUrl("top-20.html", new HashMap<String, String>());
        }
        else {
            previousMovieListUrl = (PreviousMovieListUrl) session.getAttribute("previous_movie_list_url");
            previousMovieListUrl.clearEverything();
            previousMovieListUrl.setPage("top-20.html");
        }
        // always put it back into the session:
        session.setAttribute("previous_movie_list_url", previousMovieListUrl);

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {
            Statement statement0 = conn.createStatement();
            String query0 = Top20Servlet.TOP_20_QUERY;
            ResultSet resultSet0 = statement0.executeQuery(query0);
            JsonArray moviesArray = new JsonArray();
            addTop20MoviesToArrayJsonResponse(resultSet0, moviesArray);

            resultSet0.close();
            statement0.close();
            request.getServletContext().log("getting top " + moviesArray.size() + " results");
            out.write(moviesArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    private void addTop20MoviesToArrayJsonResponse(ResultSet resultSet0, JsonArray moviesArray) throws SQLException {
        while (resultSet0.next()) {
            String movie_id = resultSet0.getString("movieId");
            String movie_title = resultSet0.getString("movieTitle");
            String movie_year = resultSet0.getString("movieYear");
            String movie_director = resultSet0.getString("movieDirector");
            String movie_rating = resultSet0.getString("movieRating");
            String movie_stars = resultSet0.getString("movieStars");
            String movie_genres = resultSet0.getString("movieGenres");

            JsonObject movieObject = new JsonObject();
            movieObject.addProperty("movie_id", movie_id);
            movieObject.addProperty("movie_title", movie_title);
            movieObject.addProperty("movie_year", movie_year);
            movieObject.addProperty("movie_director", movie_director);
            movieObject.addProperty("movie_rating", movie_rating);
            movieObject.addProperty("movie_stars", movie_stars);
            movieObject.addProperty("movie_genres", movie_genres);

            // Add the movie object with all the information to the moviesArray
            moviesArray.add(movieObject);
        }
    }
}