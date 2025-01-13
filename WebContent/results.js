
// Get possible parameters from URL
let nameOfPossibleParams = ["full_text_search", "title", "year", "director", "star_name", "genre_id", "start_with", "order_used", "num_results_per_page", "page"];
let params = {"order_used": "0", "num_results_per_page": "25", "page": "1"};
for (let i = 0; i < nameOfPossibleParams.length; i++) {
    if (getParameterByName(nameOfPossibleParams[i]) != null) {
        // also check it isn't empty
        if (getParameterByName(nameOfPossibleParams[i]).length > 0) {
            params[nameOfPossibleParams[i]] = getParameterByName(nameOfPossibleParams[i]);
        }
    }
}


function handleResult(resultData) {
    console.log("result.js: handleResult: entered function");
    // Find the empty table body by id "movie_list_table_body"
    let movieTableBodyElement = jQuery("#movie_list_table_body");
    // empty it out in case it was previously filled with past pages
    movieTableBodyElement.empty();
    console.log("Length of resultData = " + resultData.length);
    // Concatenate the html tags with resultData jsonObject to create table rows
    for (let i = 0; i < resultData.length; i++) {
        let rowHTML = "";
        rowHTML += "<tr>";
        // movie_id with movie_title
        rowHTML +=
            "<th>" +
            // Add a link to single-movie.html with id passed with GET url parameter
            '<a href="single-movie.html?id=' + resultData[i]['movie_id'] + '">'
            + resultData[i]["movie_title"] +     // display movie_title for the link text
            '</a>' +
            "</th>";
        // display movie_year
        rowHTML += "<th>" + resultData[i]["movie_year"] + "</th>";
        // display movie_director
        rowHTML += "<th>" + resultData[i]["movie_director"] + "</th>";
        // display the genres of the movie
        rowHTML += "<th>";
        if (resultData[i]["movie_genres"] !== null) {
            let genreIdAndNames = resultData[i]["movie_genres"].split(", "); // list of genre (id: name)
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
        }
        rowHTML += "</th>";

        // display the stars of the movie
        rowHTML += "<th>";
        if (resultData[i]["movie_stars"] !== null) {
            let starIdAndNames = resultData[i]["movie_stars"].split(", "); // list of stars (id: name)
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
        }
        rowHTML += "</th>";
        // display movie_rating
        rowHTML += "<th>" + (resultData[i]["movie_rating"] == null ? "N/A" : resultData[i]["movie_rating"]) + "</th>";

        rowHTML += "<th><button class='btn cart-button add-to-cart' " +
            "data-movieid='" + resultData[i]["movie_id"] + "' " +
            "data-movietitle='" + resultData[i]["movie_title"] + "' " +
            "data-movierating='" + resultData[i]["movie_rating"] + "'>Add</button></th>";

        rowHTML += "</tr>";

        // Append the row created to the table body, which will refresh the page
        movieTableBodyElement.append(rowHTML);
    }
    // now update the selected dropdowns
    jQuery("#order-types").val(params["order_used"]);
    jQuery("#num-results-per-page").val(params["num_results_per_page"]);
    // previous button
    // check page = 0
    if (parseInt(params["page"]) === 1) {
        jQuery("#previous-button").hide();
    }
    else {
        jQuery("#previous-button").show();
    }
    // next button
    // check result . length < num results per page
    if (resultData.length < parseInt(params["num_results_per_page"])) {
        jQuery("#next-button").hide();
    }
    else {
        jQuery("#next-button").show();
    }
    // current page value
    jQuery(".current-page-number").html("Page " + params["page"]);
}

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


let queryString = jQuery.param(params);
// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/results?" + queryString,
    success: (resultData) => handleResult(resultData)
});

let displayForm = jQuery("#displayForm");
let previousForm = jQuery("#previousForm");
let nextForm = jQuery("#nextForm");

function submitDisplayForm(formSubmitEvent) {
    console.log("submit display form");
    formSubmitEvent.preventDefault();
    // go back to 1 page
    params["page"] = "1";
    // update the order_used and the num_results_per_page
    params["order_used"] = $("#order-types").val();
    params["num_results_per_page"] = $("#num-results-per-page").val();

    let queryString = jQuery.param(params);

    console.log("current params are:");
    console.log(params);
    jQuery.ajax({
        dataType: "json",  // Setting return data type
        method: "GET",// Setting request method
        url: "api/results?" + queryString,
        success: (resultData) => handleResult(resultData)
    });
}

function submitPreviousForm(formSubmitEvent) {
    console.log("submit previous form");
    formSubmitEvent.preventDefault();
    params["page"] = (parseInt(params["page"]) - 1).toString();
    let queryString = jQuery.param(params);
    console.log("current params are:");
    console.log(params);
    jQuery.ajax({
        dataType: "json",  // Setting return data type
        method: "GET",// Setting request method
        url: "api/results?" + queryString,
        success: (resultData) => handleResult(resultData)
    });
}

function submitNextForm(formSubmitEvent) {
    console.log("submit next form");
    formSubmitEvent.preventDefault();
    params["page"] = (parseInt(params["page"]) + 1).toString();
    let queryString = jQuery.param(params);
    console.log("current params are:");
    console.log(params);
    jQuery.ajax({
        dataType: "json",  // Setting return data type
        method: "GET",// Setting request method
        url: "api/results?" + queryString,
        success: (resultData) => handleResult(resultData)
    });
}


// Bind the submit action of the previous and next pages to a handler function
previousForm.submit(submitPreviousForm);
nextForm.submit(submitNextForm);
displayForm.submit(submitDisplayForm);

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

// Function to get or fetch suggestions (returns null if no prior similar queries)
function getCachedResultIfExists(query) {
    let cachedQueryStringResult = localStorage.getItem(query.toLowerCase());
    if (cachedQueryStringResult) {
        // If there is cached data, parse into JSON and return it
        return JSON.parse(cachedQueryStringResult);
    } else {
        return null;
    }
}

function setCachedResult(query, jsonResult) {
    // Update the cache with the new suggestions
    localStorage.setItem(query.toLowerCase(), JSON.stringify(jsonResult));
}

/*
 * This function is called by the library when it needs to lookup a query.
 *
 * The parameter query is the query string.
 * The doneCallback is a callback function provided by the library, after you get the
 *   suggestion list from AJAX, you need to call this function to let the library know.
 */
function handleLookup(query, doneCallback) {
    console.log("autocomplete initiated");

    let cachedResult = getCachedResultIfExists(query);
    if (cachedResult !== null) {
        console.log("found query in cache / frontend");
        console.log("cached suggestions:", cachedResult);
        doneCallback( { suggestions: cachedResult });
        return;
    }

    console.log("sending AJAX request to backend Java Servlet");
    // sending the HTTP GET request to the Java Servlet endpoint hero-suggestion
    // with the query data
    jQuery.ajax({
        "method": "GET",
        // generate the request url from the query.
        // escape the query string to avoid errors caused by special characters
        // but "escape" function is deprecated, so we use "encodeURI" instead
        "url": "api/movie-suggestions?query=" + encodeURI(query),
        "success": function(data) {
            // pass the data, query, and doneCallback function into the success handler
            handleLookupAjaxSuccess(data, query, doneCallback)
        },
        "error": function(errorData) {
            console.log("lookup ajax error");
            console.log(errorData);
        }
    })
}

/*
 * This function is used to handle the ajax success callback function.
 * It is called by our own code upon the success of the AJAX request
 *
 * data is the JSON data string you get from your Java Servlet
 *
 */
function handleLookupAjaxSuccess(data, query, doneCallback) {
    console.log("backend suggestions:", data);
    // add the query and the results to the cache
    setCachedResult(query, data);
    // call the callback function provided by the autocomplete library
    // add "{suggestions: jsonData}" to satisfy the library response format according to
    //   the "Response Format" section in documentation
    doneCallback( { suggestions: data } );
}

/*
 * This function is the select suggestion handler function.
 * When a suggestion is selected, this function is called by the library.
 *
 * You can redirect to the page you want using the suggestion data.
 */
function handleSelectSuggestion(suggestion) {
    console.log("User selected " + suggestion["value"] + " with ID " + suggestion["data"]["movieId"]);
    // jump to the single-move.html page with the movie ID from suggestion["data"] as a parameter
    window.location.replace("single-movie.html?id=" + suggestion["data"]["movieId"]);
}

/*
 * This statement binds the autocomplete library with the input box element and
 *   sets necessary parameters of the library.
 *
 * The library documentation can be find here:
 *   https://github.com/devbridge/jQuery-Autocomplete
 *   https://www.devbridge.com/sourcery/components/jquery-autocomplete/
 *
 */
// $('#autocomplete') is to find element by the ID "autocomplete"
$('#autocomplete').autocomplete({
    // documentation of the lookup function can be found under the "Custom lookup function" section
    lookup: function (query, doneCallback) {
        handleLookup(query, doneCallback)
    },
    onSelect: function(suggestion) {
        handleSelectSuggestion(suggestion)
    },
    // set delay time
    deferRequestBy: 300,
    lookuplimit: 10,
    minChars: 3
    // there are some other parameters that you might want to use to satisfy all the requirements
});

/*
 * do normal full text search if no suggestion is selected
 */
function handleNormalSearch(query) {
    console.log("doing normal search with query: " + query);
    window.location.replace("results.html?full_text_query=" + query);
}

// bind pressing enter key to a handler function
$('#autocomplete').keypress(function(event) {
    // keyCode 13 is the enter key
    if (event.keyCode == 13) {
        // pass the value of the input box to the handler function
        handleNormalSearch($('#autocomplete').val())
    }
})
