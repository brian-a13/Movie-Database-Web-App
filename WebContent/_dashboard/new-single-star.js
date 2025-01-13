/**
 * Handle the data returned by NewSingleStarServlet
 * @param resultDataString jsonObject
 */
function handleNewStarResult(resultDataString) {
    console.log(resultDataString)
    let resultDataJson = JSON.parse(resultDataString);

    console.log("handle new star response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);
    let successMessage = jQuery("#success_message")
    let errorMessage = jQuery("#error_message");
    successMessage.text("");
    errorMessage.text("");

    if (resultDataJson["status"] === "success") {
        console.log("Successfully added star");
        successMessage.text("Successfully added star with id of " + resultDataJson["starId"]);
    } else {
        console.log("Failed to add star");
        errorMessage.text("Error: " + resultDataJson["errorMessage"]);
    }
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitNewStarForm(formSubmitEvent) {
    console.log("submit new star form");
    formSubmitEvent.preventDefault();

    jQuery.ajax(
        "api/new-single-star", {
            method: "POST",
            data: new_star_form.serialize(),
            success: handleNewStarResult
        }
    );
}

let new_star_form = jQuery("#new_star_form");
new_star_form.submit(submitNewStarForm);

