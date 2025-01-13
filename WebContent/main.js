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
/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleMainResult(resultData) {
    console.log("handleMainResult: populating main page info from resultData");
    console.log("handleMainResult: size of resultData[0]: " + resultData[0].length);
    console.log("handleMainResult: size of resultData[1]: " + resultData[1].length);

    // resultData[0] should contain all genres and genreId's
    let allGenresElement = jQuery("#genre-options");
    for (let i = 0; i < resultData[0].length; i++) {
        // servlet should return these in alphabetical order
        let genreHTML = "";
        genreHTML +=
            '<div class="hyperlink-button">' +
            '<a href="results.html?genre_id=' + resultData[0][i]['genre_id'] + '">'
            + resultData[0][i]["genre_name"] +     // display movie_title for the link text
            '</a>' +
            '</div>';
        allGenresElement.append(genreHTML);
    }
    // resultData[1] should contain all "start_with" characters
    // <a href="results.html?start_with=0">0</a>
    let allStartWithOptionsElement = jQuery("#start-with-options");
    // add the "non alphanumeric" option
    allStartWithOptionsElement.append("<a href=\"results.html?start_with=*\">*</a>");
    for (let i = 0; i < resultData[1].length; i++) {
        // servlet should return these in alphanumerical order
        let startWithHtml = "";
        startWithHtml +=
            '<div class="hyperlink-button">' +
            '<a href="results.html?start_with=' + resultData[1][i] + '">'
            + resultData[1][i] +     // display movie_title for the link text
            '</a>' +
            '</div>';
        allStartWithOptionsElement.append(startWithHtml);
    }

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


/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */
// Makes the HTTP GET request and registers on success callback function handleMainResult
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/main", // Setting request url, which is mapped by MainServlet in MainServlet.java
    success: (resultData) => handleMainResult(resultData) // Setting callback function to handle data returned successfully by the MainServlet
});

