import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;



@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/api/shopping-cart")
public class ShoppingCartServlet extends HttpServlet {
    private static final long serialVersionUID = 5L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // get the data from the button
        String action = request.getParameter("action");
        request.getServletContext().log("getting action: " + action);
        PrintWriter out = response.getWriter();
        // Perform actions based on the data received
        // Modify session data based on action
        if ("addMovieToCart".equals(action)) {
            addMovieToCart(request);
            out.write("Movies successfully added to cart.");
        }
        else if ("minusQuantity".equals(action)) {
            deleteOneQuantityOfMovieInCart(request);
            out.write("Movie quantity decremented.");
        }
        else if ("addQuantity".equals(action)) {
            addMoreQuantityOfMovieInCart(request, out);
            out.write("Movie quantity incremented");
        }
        else if ("deleteMovie".equals(action)) {
            deleteMovieFromCart(request);
            out.write("Movie deleted from cart.");
        }
        out.close();
    }

    private void addMovieToCart(HttpServletRequest request) {
        String movieId = request.getParameter("movieId");
        String movieTitle = request.getParameter("movieTitle");
        String movieRating = request.getParameter("movieRating");
        HttpSession session = request.getSession();

        List<CartMovie> cart = (List<CartMovie>) session.getAttribute("cart");
        if(cart == null) {
            cart = new ArrayList<>();
            session.setAttribute("cart", cart);
        }

        double tempDouble;
        // should cover the issue if there is no rating
        try{
            tempDouble = Double.valueOf(movieRating);
        }
        catch(NumberFormatException e){
            tempDouble = 5.0; //default mid value
        }
        CartMovie newMovie = new CartMovie(movieId, movieTitle, tempDouble);

        // for the subtotal (will be another var in the session data)
        // RAW CASTED but should be safe since "price" and "total" values in CartMovie class are ints
        // needed to use Integer bc int is not nullable
        Integer subtotal = (Integer) session.getAttribute("subtotal");
        if(subtotal == null){
            subtotal = 0;
            session.setAttribute("subtotal", subtotal);
        }
        if (cart.contains(newMovie)){
            // just adds one to the quantity of the movie as it was already in the cart
            CartMovie existingMovie = cart.get(cart.indexOf(newMovie));
            existingMovie.setQuantity(existingMovie.getQuantity() + newMovie.getQuantity());
            //this casting below should be fine since quantity and price are both ints
            int newTotal = existingMovie.getQuantity()*existingMovie.getPrice();
            existingMovie.setTotal(newTotal);
            subtotal += newTotal;
        }
        else{
            // adds a new movie in
            cart.add(newMovie);
            subtotal += newMovie.getTotal();
        }
        session.setAttribute("cart", cart);
        session.setAttribute("subtotal", subtotal);
    }

    private void deleteOneQuantityOfMovieInCart(HttpServletRequest request) {
        String movieId = request.getParameter("movieId");
        HttpSession session = request.getSession();

        // ADDED CARTMOVIE CLASS
        List<CartMovie> cart = (List<CartMovie>) session.getAttribute("cart");
        Integer subtotal = (Integer) session.getAttribute("subtotal");

        // Find and update the movie with the matching ID (quantity and total price)
        for (int i = 0; i < cart.size(); i++) {
            if (cart.get(i).getMovieId().equals(movieId)) {
                cart.get(i).setQuantity(cart.get(i).getQuantity()-1);
                cart.get(i).setTotal(cart.get(i).getQuantity()*cart.get(i).getPrice());
                subtotal -= cart.get(i).getPrice();
                if(cart.get(i).getQuantity() == 0){cart.remove(i);}
                break; // Break the loop once the movie is found and updated
            }
        }
        session.setAttribute("subtotal", subtotal);
        session.setAttribute("cart", cart);
    }

    private void addMoreQuantityOfMovieInCart(HttpServletRequest request, PrintWriter out) {
        String movieId = request.getParameter("movieId");
        HttpSession session = request.getSession();

        // ADDED CARTMOVIE CLASS
        List<CartMovie> cart = (List<CartMovie>) session.getAttribute("cart");
        Integer subtotal = (Integer) session.getAttribute("subtotal");

        // Find and update the movie with the matching ID (quantity and total price)
        for (CartMovie movie : cart) {
            if (movie.getMovieId().equals(movieId)) {
                movie.setQuantity(movie.getQuantity()+1);
                movie.setTotal(movie.getQuantity()*movie.getPrice());
                subtotal += movie.getPrice();
                break; // Break the loop once the movie is found and updated
            }
        }
        session.setAttribute("cart", cart);
        session.setAttribute("subtotal", subtotal);

        // Return a response
        out.write("Movie quantity incremented.");
    }

    private void deleteMovieFromCart(HttpServletRequest request) {
        String movieId = request.getParameter("movieId");
        HttpSession session = request.getSession();

        // ADDED CARTMOVIE CLASS
        List<CartMovie> cart = (List<CartMovie>) session.getAttribute("cart");
        Integer subtotal = (Integer) session.getAttribute("subtotal");

        // Find and update the movie with the matching ID (quantity and total price)
        for (int i = 0; i < cart.size(); i++) {
            if (cart.get(i).getMovieId().equals(movieId)) {
                subtotal -= cart.get(i).getTotal();
                cart.remove(i);
                break; // Break the loop once the movie is found and updated
            }
        }
        session.setAttribute("cart", cart);
        session.setAttribute("subtotal", subtotal);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json"); // Response mime type
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession();
        List<CartMovie> cart = (List<CartMovie>) session.getAttribute("cart");
        Integer subtotal = (Integer) session.getAttribute("subtotal");
        //iterate through the list and convert it to json in order to send as response to js file
        if (cart != null) {
            JsonArray entireCart = new JsonArray();
            // Iterate through the list and convert it to JSON in order to send as a response to the JavaScript file
            getInformationOfMoviesInCartToJsonFormat(cart, entireCart);
            // the last object will be a subtotal, not a movie
            putSubtotalOfCartIntoJsonFormat(subtotal, entireCart);
            out.write(entireCart.toString());
            response.setStatus(200);
        } else {
            // Handle the case where cart is null (no items in the cart)
            out.write("Cart is empty.");
            response.setStatus(200);
        }
        out.close();

    }

    private void putSubtotalOfCartIntoJsonFormat(Integer subtotal, JsonArray entireCart) {
        JsonObject subtotalObj = new JsonObject();
        subtotalObj.addProperty("subtotal", subtotal);
        entireCart.add(subtotalObj);
    }

    private void getInformationOfMoviesInCartToJsonFormat(List<CartMovie> cart, JsonArray entireCart) {
        for (CartMovie cartMovie : cart) {
            JsonObject movie = new JsonObject();
            movie.addProperty("id", cartMovie.getMovieId());
            movie.addProperty("title", cartMovie.getMovieTitle());
            movie.addProperty("price", cartMovie.getPrice());
            movie.addProperty("quantity", cartMovie.getQuantity());
            movie.addProperty("total", cartMovie.getTotal());
            entireCart.add(movie);
        }
    }
}