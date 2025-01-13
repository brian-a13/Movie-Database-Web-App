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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jasypt.util.password.StrongPasswordEncryptor;




@WebServlet(name = "AdminLoginServlet", urlPatterns = "/_dashboard/api/admin-login")
public class AdminLoginServlet extends HttpServlet {
    public static final String QUERY = "SELECT * " +
            "FROM employees AS e " +
            "WHERE e.email = ?";
    private static final long serialVersionUID = 4L;
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

        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
        request.getServletContext().log("Admin Login Servlet: getting recaptcha response");
        try {
            RecaptchaVerifyUtils.verify(gRecaptchaResponse);
            request.getServletContext().log("Admin Login Servlet: Successfully passed reCAPTCHA");
        }
        catch (Exception e) {
            decorateReCAPTCHAErrorObject(request, response, out);
            request.getServletContext().log("Admin Login Servlet: FAILED reCAPTCHA (in catch)");
            return;
        }

        request.getServletContext().log("Admin Login Servlet: about to connect to datasource");
        try (Connection conn = dataSource.getConnection()) {
            String QUERY = AdminLoginServlet.QUERY;
            request.getServletContext().log("Admin Login Servlet: about to prepare the statement");
            PreparedStatement statement = conn.prepareStatement(QUERY);
            String email = request.getParameter("email");
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();
            JsonObject responseJsonObject = new JsonObject();
            if (resultSet.next()) {
                // an employee with this email exists
                // get the *encrypted password* from the database
                String encryptedPassword = resultSet.getString("password");
                // get the unencrypted password that the user submitted
                String password = request.getParameter("password");
                // use the same encryptor to compare the user input password with encrypted password stored in DB
                boolean successOrFailure = new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);
                request.getServletContext().log("With encryption, login... " + successOrFailure);
                if (successOrFailure) {
                    decorateLoginSuccess(request, resultSet, email, responseJsonObject);
                }
                else { // password incorrect
                    decorateLoginErrorJsonObject(request, responseJsonObject);
                }
            }
            else { // email not associated with any customer
                decorateLoginErrorJsonObject(request, responseJsonObject);
            }
            resultSet.close();
            statement.close();

            request.getServletContext().log("getting Admin responseJsonObject results");
            out.write(responseJsonObject.toString());
            response.setStatus(200);
        } catch (Exception e) {
            // Write error message JSON object to output
            request.getServletContext().log("Admin Login Servlet: Exception thrown when trying to connect to datasource");
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    private void decorateReCAPTCHAErrorObject(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        JsonObject reCAPTCHAErrorJsonObject = new JsonObject();
        reCAPTCHAErrorJsonObject.addProperty("status", "fail");
        reCAPTCHAErrorJsonObject.addProperty("message", "reCAPTCHA check not passed.");
        request.getServletContext().log("Admin Login failed due to reCAPTCHA.");
        response.setStatus(200);
        out.write(reCAPTCHAErrorJsonObject.toString());
        out.close();
    }

    private void decorateLoginSuccess(HttpServletRequest request, ResultSet resultSet, String email, JsonObject responseJsonObject) throws SQLException {
        request.getSession().setAttribute("user", new User(email, "-1", true));
        responseJsonObject.addProperty("status", "success");
        responseJsonObject.addProperty("message", "success");
        request.getServletContext().log("Admin Login succeeded");
    }

    private void decorateLoginErrorJsonObject(HttpServletRequest request, JsonObject responseJsonObject) {
        responseJsonObject.addProperty("status", "fail");
        responseJsonObject.addProperty("message", "Email/Password combination incorrect.");
        request.getServletContext().log("Admin Login failed");
    }
}
