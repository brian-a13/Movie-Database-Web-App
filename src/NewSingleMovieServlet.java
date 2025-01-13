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
import java.sql.CallableStatement;
import java.sql.Types;
import java.sql.SQLException;






@WebServlet(name = "NewSingleMovieServlet", urlPatterns = "/_dashboard/api/new-single-movie")
public class NewSingleMovieServlet extends HttpServlet {
    private static final long serialVersionUID = 15L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            System.out.println("Could not connect to datasource");
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        try (Connection conn = dataSource.getConnection()) {
            String movieTitle = request.getParameter("movie_title");
            Integer movieYear = Integer.valueOf(request.getParameter("movie_year"));
            String movieDirector = request.getParameter("movie_director");
            String starName = request.getParameter("star_name");
            Integer birthYear = (request.getParameter("birth_year") == null || request.getParameter("birth_year").isEmpty()) ? null : Integer.valueOf(request.getParameter("birth_year"));
            String genreName = request.getParameter("genre_name");
            request.getServletContext().log("got parameters and about to build procedure call");
            String storedProcedureCall = "{call add_movie(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
            CallableStatement callableStatement = conn.prepareCall(storedProcedureCall);

            callableStatement.setString(1, movieTitle);
            callableStatement.setInt(2, movieYear);
            callableStatement.setString(3, movieDirector);
            callableStatement.setString(4, starName);
            if (birthYear != null) {
                callableStatement.setInt(5, birthYear);
            }
            else {
                callableStatement.setNull(5, java.sql.Types.INTEGER);
            }
            callableStatement.setString(6, genreName);
            // Register the output parameters
            callableStatement.registerOutParameter(7, Types.VARCHAR); // movieId
            callableStatement.registerOutParameter(8, Types.VARCHAR); // starId
            callableStatement.registerOutParameter(9, Types.INTEGER);  // genreId
            callableStatement.registerOutParameter(10, Types.VARCHAR); // errorMessage
            callableStatement.execute();

            request.getServletContext().log("just executed procedure call");

            String movieId = callableStatement.getString(7);
            String starId = callableStatement.getString(8);
            int genreId = callableStatement.getInt(9);
            String errorMessage = callableStatement.getString(10);

            JsonObject responseJsonObject = new JsonObject();
            if (errorMessage == null) {
                request.getServletContext().log("entered if (error is null)");
                responseJsonObject.addProperty("status", "success");
                responseJsonObject.addProperty("movieId", movieId);
                responseJsonObject.addProperty("starId", starId);
                responseJsonObject.addProperty("genreId", genreId);
            }
            else {
                request.getServletContext().log("entered else (there is an error)");
                responseJsonObject.addProperty("status", "error");
                responseJsonObject.addProperty("errorMessage", errorMessage);
            }

            callableStatement.close();
            request.getServletContext().log("getting New Single Movie responseJsonObject results");
            out.write(responseJsonObject.toString());
            response.setStatus(200);
        } catch (Exception e) {
            request.getServletContext().log("New Single Movie Servlet: Exception thrown");
            request.getServletContext().log("New Single Movie Servlet: " + e.getMessage());
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
