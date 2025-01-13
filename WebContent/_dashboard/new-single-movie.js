/**
 * Handle the data returned by NewSingleMovieServlet
 * @param resultDataString jsonObject
 */
function handleNewMovieResult(resultDataString) {
    console.log(resultDataString)
    let resultDataJson = JSON.parse(resultDataString);

    console.log("handle new Movie response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);
    let successMessage = jQuery("#success_message")
    let errorMessage = jQuery("#error_message");
    successMessage.text("");
    errorMessage.text("");

    if (resultDataJson["status"] === "success") {
        console.log("Successfully added Movie");
        successMessage.text("Successfully added Movie: Movie ID of " + resultDataJson["movieId"] +
                        ", Star ID of " + resultDataJson["starId"] + ", Genre ID of " + resultDataJson["genreId"]);
    } else {
        console.log("Failed to add Movie");
        errorMessage.text(resultDataJson["errorMessage"]);
    }
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitNewMovieForm(formSubmitEvent) {
    console.log("submit new Movie form");
    formSubmitEvent.preventDefault();

    jQuery.ajax(
        "api/new-single-movie", {
            method: "POST",
            data: new_movie_form.serialize(),
            success: handleNewMovieResult
        }
    );
}

let new_movie_form = jQuery("#new_movie_form");
new_movie_form.submit(submitNewMovieForm);

