import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;


// Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-movie"
@WebServlet(name = "SingleMovieServlet", urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 3L;
    public static final String SINGLE_MOVIE_QUERY = "SELECT m.id AS movieId, m.title AS movieTitle, m.year AS movieYear, m.director AS movieDirector, r.rating AS movieRating, " +
            "( " +
            "   SELECT GROUP_CONCAT(CONCAT(s.id, ': ', s.name) " +
            "                  ORDER BY star_counts.num_movies_star_acted_in DESC, s.name ASC " +
            "                  SEPARATOR ', ') " +
            "   FROM stars_in_movies AS sim " +
            "   LEFT JOIN stars AS s ON sim.starId = s.id " +
            "   LEFT JOIN ( " +
            "       SELECT sim1.starId, COUNT(sim1.movieId) AS num_movies_star_acted_in " +
            "       FROM stars_in_movies AS sim1 " +
            "       GROUP BY sim1.starId " +
            "   ) AS star_counts ON sim.starId = star_counts.starId " +
            "   WHERE sim.movieId = m.id " +
            ") AS movieStars, " +
            "GROUP_CONCAT(DISTINCT CONCAT(g.id, ': ', g.name) ORDER BY g.name ASC SEPARATOR ', ') AS movieGenres " +
            "FROM movies AS m " +
            "LEFT JOIN ratings AS r ON m.id = r.movieId " +
            "LEFT JOIN genres_in_movies AS gim ON m.id = gim.movieId " +
            "LEFT JOIN genres AS g ON gim.genreId = g.id " +
            "WHERE m.id = ? " +
            "GROUP BY m.id, m.title, m.year, m.director, r.rating ";
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
        String id = request.getParameter("id");
        request.getServletContext().log("getting movieId: " + id);
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String query = SingleMovieServlet.SINGLE_MOVIE_QUERY;
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                // also make sure to add the previous page button url
                // first object of the array will be a "relative_url" key with String value of relative url
                HttpSession session = request.getSession();
                PreviousMovieListUrl previousMovieListUrl = (PreviousMovieListUrl) session.getAttribute("previous_movie_list_url");
                // check if it is null (in case it is mobile)
                if (previousMovieListUrl == null) {
                    writeMovieInformationToJsonFormattedResponse(resultSet, null, out);
                }
                else {
                    String relativeUrl = previousMovieListUrl.buildRelativeUrl();
                    writeMovieInformationToJsonFormattedResponse(resultSet, relativeUrl, out);
                }
            }
            resultSet.close();
            statement.close();
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject errorObject = new JsonObject();
            request.getServletContext().log("error it got was: " + e.getMessage());
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    private void writeMovieInformationToJsonFormattedResponse(ResultSet resultSet, String relativeUrl, PrintWriter out) throws SQLException {
        String movie_id = resultSet.getString("movieId");
        String movie_title = resultSet.getString("movieTitle");
        String movie_year = resultSet.getString("movieYear");
        String movie_director = resultSet.getString("movieDirector");
        String movie_rating = resultSet.getString("movieRating");
        String movie_stars = resultSet.getString("movieStars");
        String movie_genres = resultSet.getString("movieGenres");

        JsonObject jsonObject = new JsonObject();
        // check if relativeUrl is null
        if (relativeUrl != null) {
            jsonObject.addProperty("relative_url", relativeUrl);
        }
        else {
            // occurs when it is mobile
            jsonObject.addProperty("relative_url", "");
        }
        jsonObject.addProperty("movie_id", movie_id);
        jsonObject.addProperty("movie_title", movie_title);
        jsonObject.addProperty("movie_year", movie_year);
        jsonObject.addProperty("movie_director", movie_director);
        jsonObject.addProperty("movie_rating", movie_rating);
        jsonObject.addProperty("movie_stars", movie_stars);
        jsonObject.addProperty("movie_genres", movie_genres);

        out.write(jsonObject.toString());
    }

    private void buildPreviousUrlFromSessionObject(StringBuilder relativeUrl, PreviousMovieListUrl previousMovieListUrl) {
        // get the page
        relativeUrl.append(previousMovieListUrl.getPage());
        if (previousMovieListUrl.getSize() != 0) {
            relativeUrl.append("?");
            previousMovieListUrl.getParams().forEach((key, value) -> {
                relativeUrl.append(key).append("=").append(value).append("&");
            });
            // remove the unnecessary trailing ampersand (&)
            relativeUrl.deleteCharAt(relativeUrl.length() - 1);
        }
    }
}
