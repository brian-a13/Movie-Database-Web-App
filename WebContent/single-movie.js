/**
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
function getParameterByName(target) {
    // Get request URL
    let url = window.location.href;
    // Encode target parameter name to url encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Ues regular expression to find matched parameter value
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}


/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleSingleMovieResult(resultData) {
    console.log("entered handleSingleMovieResult");

    // print out the json result object we got as response
    console.log(resultData);
    let previousPageElement = jQuery("#previous-page");
    previousPageElement.attr("href", resultData["relative_url"]);


    console.log("handleSingleMovieResult: populating movie info (title and release year) from resultData");
    // populate the star info h3
    // find the empty h3 body by id "star_info"
    let movieTitleElement = jQuery("#movieName");
    // append to the element
    movieTitleElement.append(resultData["movie_title"]);

    let releaseYearElement = jQuery("#movieYear");
    // append to the element
    releaseYearElement.append("Release Year: " + (resultData["movie_year"] ? resultData["movie_year"] : "N/A"));

    console.log("handleSingleMovieResult: populating movie table from resultData");

    // Populate the movie information "row"
    // Find the empty table body by id "movie_info_table_body"
    let movieInfoTableBodyElement = jQuery("#movie_info_table_body");

    let movieRowHTML = "<tr>";
    movieRowHTML += "<th>" + resultData["movie_director"] + "</th>";

    // display the genres of the movie
    movieRowHTML += "<th>";
    if (resultData["movie_genres"] !== null) {
        let genreIdAndNames = resultData["movie_genres"].split(", "); // list of genre (id: name)
        for (let i = 0; i < genreIdAndNames.length; i++) {
            let singleGenreIdAndName = genreIdAndNames[i].split(": ");
            movieRowHTML += '<a href="results.html?genre_id=' + singleGenreIdAndName[0] + '">'
                + singleGenreIdAndName[1] +     // display genre name for the link text
                '</a>';
            if (i !== (genreIdAndNames.length - 1)) {
                // then add a comma
                movieRowHTML += ", ";
            }
        }
    }
    movieRowHTML += "</th>";

// display the stars of the movie
    movieRowHTML += "<th>";
    if (resultData["movie_stars"] !== null) {
        let starIdAndNames = resultData["movie_stars"].split(", "); // list of stars (id: name)
        for (let i = 0; i < starIdAndNames.length; i++) {
            let singleStarIdAndName = starIdAndNames[i].split(": ");
            movieRowHTML += '<a href="single-star.html?id=' + singleStarIdAndName[0] + '">'
                + singleStarIdAndName[1] +     // display star name for the link text
                '</a>';
            if (i !== (starIdAndNames.length - 1)) {
                // then add a comma
                movieRowHTML += ", ";
            }
        }
    }
    movieRowHTML += "</th>";

    movieRowHTML += "<th>" + (resultData["movie_rating"] == null ? "N/A" : resultData["movie_rating"]) + "</th>";

    // displays 'Add' button to add movie to cart
    // send the data we need with the clicked button (kinda as if they were parameters)
    // BRO MAKE SURE THAT WHEN DOING "data-*" ANYTHING THAT YOU INPUT IN THE FOR * IS
    // ALL LOWERCASE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    movieRowHTML += "<th><button class='btn cart-button add-to-cart' " +
        "data-movieid='" + resultData["movie_id"] + "' " +
        "data-movietitle='" + resultData["movie_title"] + "' " +
        "data-movierating='" + resultData["movie_rating"] + "'>Add</button></th>";

    movieRowHTML += "</tr>";

    // Append the row created to the table body, which will refresh the page
    movieInfoTableBodyElement.append(movieRowHTML);
}

/**
 * Once this .js is loaded, following scripts will be executed by the browser\
 */

// Get id from URL
let movieId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleSingleMovieResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/single-movie?id=" + movieId, // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleSingleMovieResult(resultData) // Setting callback function to handle data returned successfully by the SingleMovieServlet
});

//this function will occur when a button with the ".add-to-cart" class is clicked
//it sends over data to the shopping-cart servlet
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
            // Display an error message to the user
            alert("Your movie has been added to the cart!");
        },
        error: function(jqXHR, textStatus, errorThrown){
            console.log("Error: " + textStatus);
            // Display an error message to the user
            alert("An error occurred while adding the movie to the cart.");
        }
    });
});