/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleAddingMovieToConfirmation(resultData) {
    console.log("entered handleAddingMovieToConfirmation");
    console.log(resultData);
    console.log("handleAddingMovieToConfirmation: populating the confirmation table with sale info");

    // Populate the confirmation table
    // Find the empty table body by id "confirmation_table_body"
    let cartTableBodyElement = jQuery("#confirmation_table_body");
    cartTableBodyElement.empty(); //important to reset it bc of the quantity getting changed
    // Go through all movies in the cart and add them to the table

    //IMPORTANT: the last element in the list is the subtotal object not a cartmovie object
    for (let i = 0; i < resultData.length-1; i++) {
        let movieRowHTML = "<tr>";
        //add the sale Id to the table
        //ensures that saleId is given only once in the table to show that all the movies purchased are under
        //the same saleId
        if(i === 0){
            movieRowHTML += "<th>" + resultData[i]["saleId"] + "</th>";
        }
        else{
            movieRowHTML += "<th>" + "</th>";
        }
        // append movie title and hyperlink it
        movieRowHTML +=
            "<th>" +
            // Add a link to single-movie.html with id passed with GET url parameter
            '<a href="single-movie.html?id=' + resultData[i]['movieId'] + '">'
            + resultData[i]["title"] +     // display movie_title for the link text
            '</a>' +
            "</th>";

        // add the quantity of the movie to table
        movieRowHTML += "<th>" + resultData[i]["quantity"] + "</th>";

        // add price of each movie
        movieRowHTML += "<th>$" + resultData[i]["price"] + "</th>";
        // add total of all the movies
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
    url: "api/confirmation", // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) =>handleAddingMovieToConfirmation(resultData) // Setting callback function to handle data returned successfully by the SingleMovieServlet
});
