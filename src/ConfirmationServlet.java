import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;



@WebServlet(name = "ConfirmationServlet", urlPatterns = "/api/confirmation")
public class ConfirmationServlet extends HttpServlet {
    private static final long serialVersionUID = 9L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json"); // Response mime type
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession();
        List<CartMovie> boughtCart = (List<CartMovie>) session.getAttribute("boughtCart");
        if (boughtCart != null) {
            JsonArray entireCart = new JsonArray();
            Integer saleId = (Integer) session.getAttribute("saleId");
            convertMoviesInCartToJson(boughtCart, saleId, entireCart);
            // last object in resultData in javascript file will be the subtotal, not a movie
            JsonObject subtotalObj = new JsonObject();
            Integer subtotal = (Integer) session.getAttribute("boughtSubtotal");
            subtotalObj.addProperty("subtotal", subtotal);
            entireCart.add(subtotalObj);
            out.write(entireCart.toString());
            response.setStatus(200);
        } else {
            out.write("Bought nothing");
            response.setStatus(200);
        }
        out.close();
    }

    private void convertMoviesInCartToJson(List<CartMovie> boughtCart, Integer saleId, JsonArray entireCart) {
        // Iterate through the list and convert it to JSON in order to send as a response to the JavaScript file
        for (CartMovie cartMovie : boughtCart) {
            JsonObject movie = new JsonObject();
            movie.addProperty("saleId", saleId);
            movie.addProperty("movieId", cartMovie.getMovieId());
            movie.addProperty("title", cartMovie.getMovieTitle());
            movie.addProperty("price", cartMovie.getPrice());
            movie.addProperty("quantity", cartMovie.getQuantity());
            movie.addProperty("total", cartMovie.getTotal());
            entireCart.add(movie);
        }
    }
}