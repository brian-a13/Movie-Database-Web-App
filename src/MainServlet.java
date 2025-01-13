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


/**
 * Declaring a WebServlet called MainServlet, which maps to url "/api/main"
 * this gets all genres and their id's, as well as all alphanumeric
 * prefixes of Movie Titles
 */
@WebServlet(name = "MainServlet", urlPatterns = "/api/main")
public class MainServlet extends HttpServlet {
    public static final String GENRE_QUERY = "SELECT id AS genre_id, name AS genre_name FROM genres ORDER BY name ASC; ";
    public static final String START_WITH_QUERY = "SELECT DISTINCT SUBSTRING(m.title, 1, 1) AS start_with_char " +
            "FROM movies AS m " +
            "WHERE SUBSTRING(m.title, 1, 1) REGEXP '[A-Za-z0-9]' " +
            "ORDER BY start_with_char";
    private static final long serialVersionUID = 7L;
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
        request.getServletContext().log("MainServlet doGet: entered");

        HttpSession session = request.getSession();
        PreviousMovieListUrl previousMovieListUrl = (PreviousMovieListUrl) session.getAttribute("previous_movie_list_url");
        if (previousMovieListUrl == null) {
            previousMovieListUrl = new PreviousMovieListUrl("main.html", new HashMap<String, String>());
        }
        else {
            previousMovieListUrl = (PreviousMovieListUrl) session.getAttribute("previous_movie_list_url");
            previousMovieListUrl.clearEverything();
            previousMovieListUrl.setPage("main.html");
        }
        // always put it back into the session:
        session.setAttribute("previous_movie_list_url", previousMovieListUrl);

        PrintWriter out = response.getWriter();
        try (Connection conn = dataSource.getConnection()) {
            // first get Genre Information Array
            Statement statement = conn.createStatement();
            request.getServletContext().log("MainServlet: About to execute genre query");
            ResultSet resultSet = statement.executeQuery(MainServlet.GENRE_QUERY);
            request.getServletContext().log("MainServlet: Executed genre query");
            JsonArray alphabeticalGenresArray = new JsonArray();
            addAllGenresToJsonArray(request, resultSet, alphabeticalGenresArray);
            // second get Start With Information Array
            request.getServletContext().log("MainServlet: About to execute start with query");
            resultSet = statement.executeQuery(MainServlet.START_WITH_QUERY);
            request.getServletContext().log("MainServlet: Executed start with query");
            JsonArray startWithArray = new JsonArray();
            addCharactersStartWithToJsonArray(resultSet, startWithArray);

            resultSet.close();
            statement.close();

            JsonArray containsGenreArrayAndStartWithArray = new JsonArray();
            containsGenreArrayAndStartWithArray.add(alphabeticalGenresArray);
            containsGenreArrayAndStartWithArray.add(startWithArray);

            request.getServletContext().log("MainServlet: Sending back array");
            out.write(containsGenreArrayAndStartWithArray.toString());
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

    private void addAllGenresToJsonArray(HttpServletRequest request, ResultSet resultSet, JsonArray alphabeticalGenresArray) throws SQLException {
        while (resultSet.next()) {
            String genreId = resultSet.getString("genre_id");
            String genreName = resultSet.getString("genre_name");
            request.getServletContext().log("MainServlet: Object: " + genreId + ": " + genreName);
            JsonObject genreObject = new JsonObject();
            genreObject.addProperty("genre_id", genreId);
            genreObject.addProperty("genre_name", genreName);
            alphabeticalGenresArray.add(genreObject);
        }
    }

    private void addCharactersStartWithToJsonArray(ResultSet resultSet, JsonArray startWithArray) throws SQLException {
        while (resultSet.next()) {
            startWithArray.add(resultSet.getString("start_with_char"));
        }
    }
}