import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;


// Declaring a WebServlet called MovieSuggestionsServlet, which maps to url "/api/movie-suggestions"
@WebServlet(name = "MovieSuggestionsServlet", urlPatterns = "/api/movie-suggestions")
public class MovieSuggestionsServlet extends HttpServlet {
    private static final long serialVersionUID = 3L;

    public static final String MOVIE_SUGGESTIONS_QUERY = "SELECT m.id AS movieId, m.title AS movieTitle " +
            "FROM movies AS m " +
            "WHERE MATCH (m.title) AGAINST (? IN BOOLEAN MODE) " +
            "LIMIT 10";
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
        String fullTextQuery = request.getParameter("query");
        request.getServletContext().log("getting suggestions for query: " + fullTextQuery);
        PrintWriter out = response.getWriter();
        try (Connection connection = dataSource.getConnection()) {
            // create query so that all words will be lowercase and searched as prefixes using "AND" boolean mode
            String[] words = fullTextQuery.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                words[i] = "+" + words[i] + "*";
            }
            String fixedFullTextQuery = String.join(" ", words);
            request.getServletContext().log("after lowercasing, splitting, adding + and *, and rejoining with spaces (query that will be inserted into prepared statement): " + fixedFullTextQuery);
            PreparedStatement statement = connection.prepareStatement(MovieSuggestionsServlet.MOVIE_SUGGESTIONS_QUERY);
            statement.setString(1, fixedFullTextQuery);
            request.getServletContext().log("finished preparing, about to execute the query");
            ResultSet resultSet = statement.executeQuery();
            request.getServletContext().log("executed the query");
            JsonArray jsonArray = new JsonArray();
            while (resultSet.next()) {
                jsonArray.add(writeMovieSuggestionsToJsonFormattedResponse(resultSet));
            }
            request.getServletContext().log("The result array: " + jsonArray.toString());
            resultSet.close();
            statement.close();
            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject errorObject = new JsonObject();
            request.getServletContext().log("Error inside MovieSuggestionsServlet was: " + e.getMessage());
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    private JsonObject writeMovieSuggestionsToJsonFormattedResponse(ResultSet resultSet) throws SQLException {
        String movieId = resultSet.getString("movieId");
        String movieTitle = resultSet.getString("movieTitle");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("value", movieTitle);

        JsonObject additionalDataJsonObject = new JsonObject();
        additionalDataJsonObject.addProperty("movieId", movieId);

        jsonObject.add("data", additionalDataJsonObject);
        return jsonObject;
    }
}
