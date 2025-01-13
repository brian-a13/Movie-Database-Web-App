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
import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * Declaring a WebServlet called SingleStarServlet, which maps to url "/api/single-star"
 */
@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;
    public static final String SINGLE_STAR_QUERY = "SELECT s.id AS starId, s.name AS starName, s.birthYear AS starBirthYear, " +
            "m.id AS movieId, m.title AS movieTitle, m.year AS movieYear, m.director AS movieDirector " +
            "FROM stars AS s, stars_in_movies AS sim, movies AS m " +
            "WHERE s.id = ? AND sim.starId = s.id AND sim.movieId = m.id " +
            "ORDER BY m.year DESC, m.title ASC";
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
        request.getServletContext().log("getting star id: " + id);
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            String query = SingleStarServlet.SINGLE_STAR_QUERY;
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, id);
            ResultSet resultSet = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();

            // first object of the array will be a "relative_url" key with String value of relative url
            HttpSession session = request.getSession();
            // this can't be null because you can't reach a single page without having seen a movie list
            PreviousMovieListUrl previousMovieListUrl = (PreviousMovieListUrl) session.getAttribute("previous_movie_list_url");
            String relativeUrl = previousMovieListUrl.buildRelativeUrl();
            // the first object of the response will be the relative_url
            JsonObject relativeUrlObject = new JsonObject();
            relativeUrlObject.addProperty("relative_url", relativeUrl.toString());
            jsonArray.add(relativeUrlObject);
            addMoviesToJsonFormattedArrayForResponse(resultSet, jsonArray);
            resultSet.close();
            statement.close();
            out.write(jsonArray.toString());
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

    private void addMoviesToJsonFormattedArrayForResponse(ResultSet resultSet, JsonArray jsonArray) throws SQLException {
        // Iterate through each row of result set
        while (resultSet.next()) {
            String starId = resultSet.getString("starId");
            String starName = resultSet.getString("starName");
            String starBirthYear = resultSet.getString("starBirthYear");

            String movieId = resultSet.getString("movieId");
            String movieTitle = resultSet.getString("movieTitle");
            String movieYear = resultSet.getString("movieYear");
            String movieDirector = resultSet.getString("movieDirector");

            // Create JsonObjects based on the data we retrieved
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("star_id", starId);
            jsonObject.addProperty("star_name", starName);
            jsonObject.addProperty("star_birth_year", starBirthYear);
            jsonObject.addProperty("movie_id", movieId);
            jsonObject.addProperty("movie_title", movieTitle);
            jsonObject.addProperty("movie_year", movieYear);
            jsonObject.addProperty("movie_director", movieDirector);

            jsonArray.add(jsonObject);
        }
    }
}