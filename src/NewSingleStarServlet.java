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






@WebServlet(name = "NewSingleStarServlet", urlPatterns = "/_dashboard/api/new-single-star")
public class NewSingleStarServlet extends HttpServlet {
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
            String starName = request.getParameter("star_name");
            Integer birthYear = (request.getParameter("birth_year") == null || request.getParameter("birth_year").isEmpty()) ? null : Integer.valueOf(request.getParameter("birth_year"));

            String storedProcedureCall = "{call add_star(?, ?, ?)}";
            CallableStatement callableStatement = conn.prepareCall(storedProcedureCall);
            callableStatement.setString(1, starName);
            if (birthYear != null) {
                callableStatement.setInt(2, birthYear);
            }
            else {
                callableStatement.setNull(2, java.sql.Types.INTEGER);
            }
            // register the output parameter
            callableStatement.registerOutParameter(3, Types.VARCHAR);
            callableStatement.execute();

            String starId = callableStatement.getString(3);
            JsonObject responseJsonObject = new JsonObject();
            responseJsonObject.addProperty("status", "success");
            responseJsonObject.addProperty("starId", starId);

            callableStatement.close();
            request.getServletContext().log("getting New Single Star responseJsonObject results");
            out.write(responseJsonObject.toString());
            response.setStatus(200);
        } catch (Exception e) {
            request.getServletContext().log("New Single Star Servlet: Exception thrown:" + e.getMessage());
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
