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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Date;



@WebServlet(name = "PaymentServlet", urlPatterns = "/api/payment")
public class PaymentServlet extends HttpServlet {
    public static final String PAYMENT_QUERY = "SELECT * " +
            "FROM creditcards " +
            "WHERE id = ? AND firstName = ?  AND lastName = ? AND expiration = ? ";
    public static final String INSERT_SALE_QUERY = "INSERT INTO sales (customerId, saleDate) VALUES (?, ?)";
    public static final String INSERT_MOVIE_QUERY = "INSERT INTO sales_movies (saleId, movieId, quantity) VALUES (?, ?, ?)";
    private static final long serialVersionUID = 8L;
    private DataSource dataSource;

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
        // if they are an admin user,
        // they aren't allowed to make purchases
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();
        User theUser = (User) session.getAttribute("user");
        if (theUser.isAdmin()) {
            JsonObject adminErrorJsonObject = new JsonObject();
            adminErrorJsonObject.addProperty("status", "fail");
            adminErrorJsonObject.addProperty("message", "Cannot purchase cart as admin user.");
            request.getServletContext().log("Payment failed because they are an admin");
            out.write(adminErrorJsonObject.toString());
            response.setStatus(200);
            out.close();
            return;
        }

        String card_num = request.getParameter("card_num");
        String first_name = request.getParameter("first_name");
        String last_name = request.getParameter("last_name");
        String expire_date = request.getParameter("expire_date");
        try (Connection conn = dataSource.getConnection()) {
            request.getServletContext().log("We are in the try");
            String PAYMENT_QUERY = PaymentServlet.PAYMENT_QUERY;
            PreparedStatement statement = conn.prepareStatement(PAYMENT_QUERY);
            statement.setString(1, card_num);
            statement.setString(2, first_name);
            statement.setString(3, last_name);
            statement.setString(4, expire_date);
            request.getServletContext().log("before Query");
            JsonObject responseJsonObject = new JsonObject();
            ResultSet resultSet;
            // Handle date formatting in case SQL can't compare with date given
            try {
                resultSet = statement.executeQuery();
            } catch (Exception e) {
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Please check/re-enter your billing details.");
                request.getServletContext().log("Payment failed");
                statement.close();
                request.getServletContext().log("getting responseJsonObject results");
                out.write(responseJsonObject.toString());
                response.setStatus(200);
                out.close();
                return;
            }
            request.getServletContext().log("after Query");
            if (resultSet.next()) {
                // if enters here then it found a match and good to go
                responseJsonObject.addProperty("status", "success");
                responseJsonObject.addProperty("message", "success");
                request.getServletContext().log("Payment succeeded");
                // adding the transaction to the sales table
                String customerId = theUser.getCustomerId();
                List<CartMovie> cart = (List<CartMovie>) session.getAttribute("cart");
                request.getServletContext().log("Attempting to add into sales table");
                // Insert a new sale record
                String INSERT_SALE_QUERY = PaymentServlet.INSERT_SALE_QUERY;
                try (PreparedStatement saleStatement =
                             conn.prepareStatement(INSERT_SALE_QUERY,
                             PreparedStatement.RETURN_GENERATED_KEYS)) {
                    saleStatement.setString(1, customerId);
                    java.sql.Date saleDate = new java.sql.Date(new Date().getTime()); // Current date
                    saleStatement.setDate(2, saleDate);
                    int rowsAffected = saleStatement.executeUpdate();
                    int generatedId = -1; // Initialize to a default value
                    generatedId = getGeneratedSaleId(request, rowsAffected, saleStatement, generatedId);
                    // Insert movie IDs associated with the sale
                    if (generatedId != -1) {
                        String INSERT_MOVIE_QUERY = PaymentServlet.INSERT_MOVIE_QUERY;
                        try (PreparedStatement movieStatement = conn.prepareStatement(INSERT_MOVIE_QUERY)) {
                            updateTheSalesTable(cart, movieStatement, generatedId);
                            request.getServletContext().log("Movie IDs are associated with the sale.");
                            setInformationForCompletedPaymentInSession(session, generatedId, cart);
                        }
                    }
                } catch (SQLException e){
                    e.printStackTrace();
                    String errorMessage = e.getMessage();
                    request.getServletContext().log("SQL Error Message: " + errorMessage);
                }
            } else {
                // incorrect info
                responseJsonObject.addProperty("status", "fail");
                responseJsonObject.addProperty("message", "Please check/re-enter your billing details.");
                request.getServletContext().log("Payment failed");
            }
            resultSet.close();
            statement.close();
            request.getServletContext().log("getting responseJsonObject results");
            out.write(responseJsonObject.toString());
            response.setStatus(200);
        } catch (Exception e) {
            request.getServletContext().log("Went straight to exception");
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    private void setInformationForCompletedPaymentInSession(HttpSession session, int generatedId, List<CartMovie> cart) {
        // Write information to session
        session.setAttribute("saleId", generatedId);
        session.setAttribute("boughtCart", cart);
        session.setAttribute("cart", null); //resetting the actual cart
        Integer subtotal = (Integer) session.getAttribute("subtotal");
        session.setAttribute("boughtSubtotal", subtotal);
        session.setAttribute("subtotal", null ); // resetting the actual subtotal
    }

    private void updateTheSalesTable(List<CartMovie> cart, PreparedStatement movieStatement, int generatedId) throws SQLException {
        for (CartMovie movie : cart) {
            movieStatement.setInt(1, generatedId); // Use the generated sale ID
            movieStatement.setString(2, movie.getMovieId());
            movieStatement.setInt(3, movie.getQuantity());
            movieStatement.executeUpdate();
        }
    }

    private int getGeneratedSaleId(HttpServletRequest request, int rowsAffected, PreparedStatement saleStatement, int generatedId) throws SQLException {
        if (rowsAffected > 0) {
            try (ResultSet generatedKeys = saleStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    generatedId = generatedKeys.getInt(1);
                }
            }
            request.getServletContext().log("Sale record inserted successfully with ID: " + generatedId);
        } else {
            request.getServletContext().log("Sale record insertion failed.");
        }
        return generatedId;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
     * doGet receives the subtotal information from the server session
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json"); // Response mime type
        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();
        Integer subtotal = (Integer) session.getAttribute("subtotal");
        JsonObject responseJsonObject = new JsonObject();
        if (subtotal == null) {
            responseJsonObject.addProperty("subtotal", 0);
        }
        else {
            responseJsonObject.addProperty("subtotal", subtotal);
        }

        out.write(responseJsonObject.toString());
        response.setStatus(200);
        out.close();
    }
}
