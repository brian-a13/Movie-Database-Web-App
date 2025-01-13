let payment_form = jQuery("#payment_form");

/**
 * Handle the data returned by PaymentServlet
 * @param resultDataString jsonObject
 */
function handlePaymentResult(resultDataString) {
    console.log(resultDataString)
    let resultDataJson = JSON.parse(resultDataString);

    console.log("handle payment response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);

    // If payment succeeds, it will redirect the user to confirmation.html
    if (resultDataJson["status"] === "success") {
        window.location.replace("confirmation.html");
        console.log("The thing works correctly.");
    } else {
        // If login fails, the web page will display
        // error messages on <div> with id "login_error_message"
        console.log("show error message");
        console.log(resultDataJson["message"]);
        jQuery("#payment_error_message").text(resultDataJson["message"]);
    }

    // NEED TO RECORD SALE IN SALES TABLE IF SUCCESSFUL, HERE OR MAYBE IN SERVLET
}

function handleSubtotal(resultData) {
    console.log("receiving subtotal info");

    let subtotalElement = jQuery("#subtotal");
    // append to the element
    subtotalElement.empty() //important to reset it bc of the quantity getting changed
    subtotalElement.append("Subtotal: $"+ resultData['subtotal'] );
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitPaymentForm(formSubmitEvent) {
    console.log("submit payment form");
    /**
     * When users click the submit button, the browser will not direct
     * users to the url defined in HTML form. Instead, it will call this
     * event handler when the event is triggered.
     */
    formSubmitEvent.preventDefault();

    jQuery.ajax(
        "api/payment", {
            method: "POST",
            // Serialize the login form to the data sent by POST request
            // This method goes through all the input fields within the form and gathers their names and values.
            // It then constructs a query string in the format name1=value1&name2=value2&...,
            data: payment_form.serialize(),
            success: handlePaymentResult
        }
    );
}

jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/payment", // Setting request url,
    success: (resultData) => handleSubtotal(resultData) // Setting callback function to handle data returned successfully by the SingleMovieServlet
});
// Bind the submit action of the form to a handler function
payment_form.submit(submitPaymentForm);

