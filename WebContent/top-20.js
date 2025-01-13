/**
 * Injects the exact text for one row in the table (movie information)
 * @param movieObject jsonObject
 * @param htmlElement jQueryElement
 */
function injectMovieRow(movieObject, htmlElement) {
    let rowHTML = "";
    rowHTML += "<tr>";
    // movie_id with movie_title
    rowHTML +=
        "<th>" +
        // Add a link to single-movie.html with id passed with GET url parameter
        '<a href="single-movie.html?id=' + movieObject['movie_id'] + '">'
        + movieObject["movie_title"] +     // display movie_title for the link text
        '</a>' +
        "</th>";
    // display movie_year
    rowHTML += "<th>" + movieObject["movie_year"] + "</th>";
    // display movie_director
    rowHTML += "<th>" + movieObject["movie_director"] + "</th>";
    // display the genres of the movie
    rowHTML += "<th>";
    let genreIdAndNames = movieObject["movie_genres"].split(", "); // list of genre (id: name)
    for (let i = 0; i < genreIdAndNames.length; i++) {
        let singleGenreIdAndName = genreIdAndNames[i].split(": ");
        rowHTML += '<a href="results.html?genre_id=' + singleGenreIdAndName[0] + '">'
            + singleGenreIdAndName[1] +     // display genre name for the link text
            '</a>';
        if (i !== (genreIdAndNames.length - 1)) {
            // then add a comma
            rowHTML += ", ";
        }
    }
    rowHTML += "</th>";

    // display the stars of the movie
    rowHTML += "<th>";
    let starIdAndNames = movieObject["movie_stars"].split(", "); // list of stars (id: name)
    for (let i = 0; i < starIdAndNames.length; i++) {
        let singleStarIdAndName = starIdAndNames[i].split(": ");
        rowHTML += '<a href="single-star.html?id=' + singleStarIdAndName[0] + '">'
            + singleStarIdAndName[1] +     // display star name for the link text
            '</a>';
        if (i !== (starIdAndNames.length - 1)) {
            // then add a comma
            rowHTML += ", ";
        }
    }
    rowHTML += "</th>";
    // display movie_rating
    rowHTML += "<th>" + (movieObject["movie_rating"] == null ? "N/A" : movieObject["movie_rating"]) + "</th>";

    // displays 'Add' button to add movie to cart
    rowHTML += "<th><button class='btn cart-button add-to-cart' " +
        "data-movieid='" + movieObject["movie_id"] + "' " +
        "data-movietitle='" + movieObject["movie_title"] + "' " +
        "data-movierating='" + movieObject["movie_rating"] + "'>Add</button></th>";

    rowHTML += "</tr>";

    // Append the row created to the table body, which will refresh the page
    htmlElement.append(rowHTML);
}

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleTop20Result(resultData) {
    console.log("handleTop20Result: populating movie list (top 20) table from resultData");
    // Populate the movie list table
    // Find the empty table body by id "movie_list_table_body"
    let movieListTableBodyElement = jQuery("#movie_list_table_body");

    // this has more than 20 rows so we need to parse it
    for (let i = 0; i < resultData.length; i++) {
        console.log(resultData[i]);
        injectMovieRow(resultData[i], movieListTableBodyElement);
    }
}

/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */
// Makes the HTTP GET request and registers on success callback function handleTop20Result
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/top-20", // Setting request url, which is mapped by Top20Servlet in Top20Servlet.java
    success: (resultData) => handleTop20Result(resultData) // Setting callback function to handle data returned successfully by the Top20Servlet
});

jQuery(document).on("click", ".add-to-cart", function(){
    // Get the data from the clicked button
    console.log("Button clicked"); // Check if this log message appears
    let movieId = jQuery(this).data("movieid");
    let movieTitle = jQuery(this).data("movietitle");
    let movieRating = jQuery(this).data("movierating");

    console.log(movieId);
    console.log(movieTitle);
    console.log(movieRating);

    jQuery.ajax({
        url: "api/shopping-cart",
        method: "POST",
        data: {
            action: "addMovieToCart",
            movieId: movieId,
            movieTitle: movieTitle,
            movieRating: movieRating
        },
        success: function(response){
            console.log("Success: " + response);
            alert("Your movie has been added to the cart!");
        },
        error: function(jqXHR, textStatus, errorThrown){
            console.log("Error: " + textStatus);
            alert("An error occurred while adding the movie to the cart.");
        }
    });
});
