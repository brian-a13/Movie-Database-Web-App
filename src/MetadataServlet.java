import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Connection;



// Declaring a WebServlet called MetadataServlet, which maps to url "/api/metadata"
@WebServlet(name = "MetadataServlet", urlPatterns = "/_dashboard/api/metadata")
public class MetadataServlet extends HttpServlet {
    private static final long serialVersionUID = 11L;
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json"); // Response mime type
        // Output stream to STDOUT
        PrintWriter out = response.getWriter();
        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection connection = dataSource.getConnection()) {
            CallableStatement callableStatement = connection.prepareCall("{call get_table_metadata}");
            callableStatement.execute();
            ResultSet resultSet = callableStatement.getResultSet();

            JsonArray allTableObjects = new JsonArray();
            JsonObject tableObject = new JsonObject();
            tableObject.addProperty("tableName", "");
            JsonArray tableAttributes = new JsonArray();
            while (resultSet.next()) {
                String tableName = resultSet.getString("tableName");
                String columnName = resultSet.getString("columnName");
                String dataType = resultSet.getString("dataType");
                if (tableName.compareTo(tableObject.get("tableName").getAsString()) != 0) {
                    if (!tableAttributes.isEmpty()) {
                        request.getServletContext().log("Creating table " + tableObject.get("tableName").getAsString() + " object with all information");
                        tableObject.add("attributes", tableAttributes);
                        allTableObjects.add(tableObject);
                    }
                    tableObject = new JsonObject();
                    tableObject.addProperty("tableName", tableName);
                    tableAttributes = new JsonArray();
                }
                JsonObject attribute = new JsonObject();
                attribute.addProperty("name", columnName);
                attribute.addProperty("type", dataType);
                tableAttributes.add(attribute);
            }
            if (!tableAttributes.isEmpty()) {
                request.getServletContext().log("Last one: Creating table " + tableObject.get("tableName").getAsString() + " object with all information");
                tableObject.add("attributes", tableAttributes);
                allTableObjects.add(tableObject);
            }

            // Here is where we would start building the objects
            // we want to send back in the array

            resultSet.close();
            callableStatement.close();
            request.getServletContext().log("Returning " + allTableObjects.size() + " table info results");
            out.write(allTableObjects.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject errorObject = new JsonObject();
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}