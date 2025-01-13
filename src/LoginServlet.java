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




@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    public static final String QUERY = "SELECT * " +
            "FROM customers AS c " +
            "WHERE c.email = ?";
    private static final long serialVersionUID = 4L;
    private DataSource dataSource;

    private boolean reCaptchaFailed = false;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();

//        Turned off for testing purposes
//        String gRecaptchaResponse = request.getParameter("g-recaptcha-response");
//        request.getServletContext().log("Login Servlet: getting recaptcha response");
//        try {
//            RecaptchaVerifyUtils.verify(gRecaptchaResponse);
//            request.getServletContext().log("Login Servlet: reCAPTCHA verified properly");
//        }
//        catch (Exception e) {
//            request.getServletContext().log("Login Servlet: reCAPTCHA FAILED (in catch)");
//            reCaptchaFailed = true;
//            //decorateReCAPTCHAErrorObject(request, response, out);
//            //return;
//        }


        try (Connection conn = dataSource.getConnection()) {
            String QUERY = LoginServlet.QUERY;
            PreparedStatement statement = conn.prepareStatement(QUERY);
            String email = request.getParameter("email");
            statement.setString(1, email);
            ResultSet resultSet = statement.executeQuery();
            JsonObject responseJsonObject = new JsonObject();
            if (resultSet.next()) {
                // a customer with this email exists
                // get the *encrypted password* from the database
                String encryptedPassword = resultSet.getString("password");
                // get the unencrypted password that the user submitted
                String password = request.getParameter("password");
                // use the same encryptor to compare the user input password with encrypted password stored in DB
                boolean successOrFailure = new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);
                request.getServletContext().log("With encryption, login... " + successOrFailure);
                if (successOrFailure) {
                    if(reCaptchaFailed) {
                        decorateReCAPTCHAErrorObject(request, response, out, "success");
                        return;
                    }else {
                        decorateLoginSuccess(request, resultSet, email, responseJsonObject);
                    };
                }
                else { // password incorrect
                    if(reCaptchaFailed) {
                        decorateReCAPTCHAErrorObject(request, response, out, "failure");
                        return;
                    }
                    else {
                        decorateLoginErrorJsonObject(request, responseJsonObject);
                    }
                }
            }
            else { // email not associated with any customer
                if(reCaptchaFailed) {
                    decorateReCAPTCHAErrorObject(request, response, out, "failure");
                    return;
                }
                else{
                    decorateLoginErrorJsonObject(request, responseJsonObject);
                }
            }
            resultSet.close();
            statement.close();

            request.getServletContext().log("getting responseJsonObject results");
            out.write(responseJsonObject.toString());
            response.setStatus(200);
        } catch (Exception e) {
            request.getServletContext().log("Login Servlet: Caught error when tryin to connect to database");
            // Write error message JSON object to output
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    private void decorateReCAPTCHAErrorObject(HttpServletRequest request,
                                              HttpServletResponse response,
                                              PrintWriter out,
                                              String loginResult) throws IOException {
        JsonObject reCAPTCHAErrorJsonObject = new JsonObject();
        reCAPTCHAErrorJsonObject.addProperty("status", "fail");
        reCAPTCHAErrorJsonObject.addProperty("message", "reCAPTCHA check not passed.");
        reCAPTCHAErrorJsonObject.addProperty("loginResult", loginResult);
        request.getServletContext().log("Login failed due to reCAPTCHA.");
        response.setStatus(200);
        out.write(reCAPTCHAErrorJsonObject.toString());
        out.close();
        reCaptchaFailed = false;
    }

    private void decorateLoginSuccess(HttpServletRequest request, ResultSet resultSet, String email, JsonObject responseJsonObject) throws SQLException {
        String customerId = resultSet.getString("id");
        request.getSession().setAttribute("user", new User(email, customerId, false));
        responseJsonObject.addProperty("status", "success");
        responseJsonObject.addProperty("message", "success");
        request.getServletContext().log("Login succeeded");
    }

    private void decorateLoginErrorJsonObject(HttpServletRequest request, JsonObject responseJsonObject) {
        responseJsonObject.addProperty("status", "fail");
        responseJsonObject.addProperty("message", "Email/Password combination incorrect.");
        request.getServletContext().log("Login failed");
    }
}
