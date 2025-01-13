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
function handleSingleStarResult(resultData) {
    console.log("entered handleSingleStarResult");

    // first deal with the "previous page" button
    let previousPageElement = jQuery("#previous-page");
    previousPageElement.attr("href", resultData[0]["relative_url"]);

    console.log("handleSingleStarResult: populating star info (name and birth year) from resultData");
    // populate the star info h3
    // find the empty h3 body by id "star_info"
    let starNameElement = jQuery("#star_name");
    // append to the element
    // Removed "Star Name:" to make the website look cleaner (and it was obvious)
    starNameElement.append(resultData[1]["star_name"]);
    let starBirthYearElement = jQuery("#star_birth_year");
    // append to the element
    starBirthYearElement.append("Birth Year: " + (resultData[1]["star_birth_year"] ? resultData[1]["star_birth_year"] : "N/A"));

    console.log("handleSingleStarResult: populating movie table from resultData");

    // Populate the star table
    // Find the empty table body by id "movie_table_body"
    let movieTableBodyElement = jQuery("#movie_table_body");
    // Go through all rows (different movies of that actor and populate movie table)
    for (let i = 1; i < resultData.length; i++) {
        let movieRowHTML = "<tr>";
        // append movie title and hyperlink it
        movieRowHTML +=
            "<th>" +
            // Add a link to single-movie.html with id passed with GET url parameter
            '<a href="single-movie.html?id=' + resultData[i]['movie_id'] + '">'
            + resultData[i]["movie_title"] +     // display movie_title for the link text
            '</a>' +
            "</th>";
        // add movie year
        movieRowHTML += "<th>" + resultData[i]["movie_year"] + "</th>";
        // add movie director
        movieRowHTML += "<th>" + resultData[i]["movie_director"] + "</th>";
        movieRowHTML += "</tr>";

        // Append the row created to the table body, which will refresh the page
        movieTableBodyElement.append(movieRowHTML);
    }
}


/**
 * Once this .js is loaded, following scripts will be executed by the browser\
 */

// Get id from URL
let starId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleSingleStarResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/single-star?id=" + starId, // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleSingleStarResult(resultData) // Setting callback function to handle data returned successfully by the SingleStarServlet
});
