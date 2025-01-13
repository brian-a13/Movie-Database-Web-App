/**
 * Handle the data returned by LoginServlet
 * @param resultDataString jsonObject
 */
function handleAdminLoginResult(resultDataString) {
    let resultDataJson = JSON.parse(resultDataString);

    console.log("handle admin login response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);

    // If login succeeds, it will redirect the user to main.html
    if (resultDataJson["status"] === "success") {
        // COME BACK TO THIS
        window.location.replace("metadata.html");
    } else {
        // If login fails, the web page will display
        // error messages on <div> with id "login_error_message"
        console.log("show error message");
        console.log(resultDataJson["message"]);
        jQuery("#login_error_message").text(resultDataJson["message"]);
    }
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitAdminLoginForm(formSubmitEvent) {
    console.log("submit admin login form");
    /**
     * When users click the submit button, the browser will not direct
     * users to the url defined in HTML form. Instead, it will call this
     * event handler when the event is triggered.
     */
    formSubmitEvent.preventDefault();
    console.log("about to hit api/admin-login")
    jQuery.ajax(
        "api/admin-login", {
            method: "POST",
            data: admin_login_form.serialize(),
            success: handleAdminLoginResult
        }
    );
}

// Bind the submit action of the form to a handler function
let admin_login_form = jQuery("#admin_login_form");
admin_login_form.submit(submitAdminLoginForm);

