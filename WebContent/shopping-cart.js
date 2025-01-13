/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleAddingMovieToCart(resultData) {
    console.log("entered handleAddingMovieToCart");
    console.log(resultData);
    console.log("handleAddingMovieToCart: populating the shopping cart table with movie info");

    // Populate the cart table
    // Find the empty table body by id "shopping_cart_table_body"
    let cartTableBodyElement = jQuery("#shopping_cart_table_body");
    cartTableBodyElement.empty(); //important to reset it bc of the quantity getting changed
    // Go through all movies in the cart and add them to the table

    //IMPORTANT: the last element in the list is the subtotal object not a cartmovie object
    for (let i = 0; i < resultData.length-1; i++) {
        let movieRowHTML = "<tr>";
        // append movie title and hyperlink it
        movieRowHTML +=
            "<th>" +
            // Add a link to single-movie.html with id passed with GET url parameter
            '<a href="single-movie.html?id=' + resultData[i]['id'] + '">'
            + resultData[i]["title"] +     // display movie_title for the link text
            '</a>' +
            "</th>";

        // add the quanity buttons and quantity of the movie to table
        movieRowHTML += "<th><button class='btn cart-button minus-quantity' "+
            "data-movieid='"+ resultData[i]['id'] + "' >-</button>"
            + resultData[i]["quantity"] +
            "<button class='btn cart-button add-quantity' " +
            "data-movieid='"+ resultData[i]['id'] + "' >+</button></th>";

        // add delete button
        movieRowHTML += "<th><button class='btn cart-button delete-from-cart' " +
            "data-movieid='"+ resultData[i]['id'] + "' >Delete</button></th>";
        // add price of each movie
        movieRowHTML += "<th>$" + resultData[i]["price"] + "</th>";
        // add total of all the movies (quantity * unit price)
        movieRowHTML += "<th>$" + resultData[i]["total"] + "</th>";

        movieRowHTML += "</tr>";

        // Append the row created to the table body, which will refresh the page
        cartTableBodyElement.append(movieRowHTML);
    }
    let subtotalElement = jQuery("#subtotal");
    // append to the element
    subtotalElement.empty() //important to reset it bc of the quantity getting changed
    subtotalElement.append("Subtotal: $"+ resultData[resultData.length-1]['subtotal']);
}


/**
 * Once this .js is loaded, following scripts will be executed by the browser\
 */
// Makes the HTTP GET request and registers on success callback function handleSingleMovieResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/shopping-cart", // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleAddingMovieToCart(resultData) // Setting callback function to handle data returned successfully by the SingleMovieServlet
});

// Runs when the trying to decrement the quantity and sends the movieId to the servlet
// which then changes the session data
jQuery(document).on("click", ".minus-quantity", function(){
    // Get the data from the clicked button
    console.log("Minus Quantity clicked"); // Check if this log message appears
    let movieId = jQuery(this).data("movieid");
    console.log(movieId);
    jQuery.ajax({
        url: "api/shopping-cart",
        method: "POST",
        data: {
            action: "minusQuantity",
            movieId: movieId
        },
        success:function(){
            refreshCartTable();
        }
    });
});

// Runs when the trying to increment the quantity and sends the movieId to the servlet
// which then changes the session data
jQuery(document).on("click", ".add-quantity", function(){
    // Get the data from the clicked button
    console.log("Minus Quantity clicked"); // Check if this log message appears
    let movieId = jQuery(this).data("movieid");
    console.log(movieId);
    jQuery.ajax({
        url: "api/shopping-cart",
        method: "POST",
        data: {
            action: "addQuantity",
            movieId: movieId
        },
        success:function(){
            refreshCartTable();
        }
    });
});

jQuery(document).on("click", ".delete-from-cart", function(){
    // Get the data from the clicked button
    console.log("Deleting movie from cart button is clicked"); // Check if this log message appears
    let movieId = jQuery(this).data("movieid");
    console.log(movieId);
    jQuery.ajax({
        url: "api/shopping-cart",
        method: "POST",
        data: {
            action: "deleteMovie",
            movieId: movieId
        },
        success:function(){
            refreshCartTable();
        }
    });
});

// Function to refresh the cart table
function refreshCartTable() {
    // Fetch the updated cart data
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "api/shopping-cart",
        success: function (resultData) {
            // Call the handleAddingMovieToCart function to update the cart table
            handleAddingMovieToCart(resultData);
        }
    });
}