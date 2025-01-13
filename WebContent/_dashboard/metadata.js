/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleMetadataResult(resultData) {
    console.log("entered handleMetadataResult");
    console.log("size of metadata:", resultData.length);
    console.log(resultData);

    let databaseTableInfoBody = jQuery("#database_table_info_body");

    // each object will be a table of {tableName: "", attributes: [{name: "", type: ""}]
    for (let i = 0; i < resultData.length; i++) {
        let rowHTML = "<tr>";
        rowHTML += "<th>" + resultData[i]["tableName"] + "</th>";
        for (let j = 0; j < resultData[i]["attributes"].length; j++) {
            if (j !== 0) {
                rowHTML = "<tr><th></th>";
            }
            // add the name of the attribute
            rowHTML += "<th>" + resultData[i]["attributes"][j]["name"] + "</th>";
            // add the type of the attribute
            rowHTML += "<th>" + resultData[i]["attributes"][j]["type"] + "</th>";
            rowHTML += "</tr>"
            // every attribute will have its own row (only one per table will have
            // the table name with it)
            databaseTableInfoBody.append(rowHTML);
        }
    }
}

/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */
// Makes the HTTP GET request and registers on success callback function handleMetadataResult
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/metadata", // Setting request url, which is mapped by Top20Servlet in MetadataServlet.java
    success: (resultData) => handleMetadataResult(resultData) // Setting callback function to handle data returned successfully by the MetadataServlet
});
