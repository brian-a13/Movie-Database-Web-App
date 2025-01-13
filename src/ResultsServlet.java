import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;




@WebServlet(name = "ResultsServlet", urlPatterns = "/api/results")
public class ResultsServlet extends HttpServlet {
    private static final long serialVersionUID = 6L;

    public static final String BROWSE_GENRE_QUERY = "SELECT m.id AS movieId, m.title AS movieTitle, m.year AS movieYear, m.director AS movieDirector, r.rating AS movieRating, " +
            "( " +
            "   SELECT SUBSTRING_INDEX(GROUP_CONCAT(CONCAT(s.id, ': ', s.name) " +
            "                  ORDER BY star_counts.num_movies_star_acted_in DESC, s.name ASC " +
            "                  SEPARATOR ', '), ', ', 3) " +
            "   FROM stars_in_movies AS sim " +
            "   LEFT JOIN stars AS s ON sim.starId = s.id " +
            "   LEFT JOIN ( " +
            "       SELECT sim1.starId, COUNT(sim1.movieId) AS num_movies_star_acted_in " +
            "       FROM stars_in_movies AS sim1 " +
            "       GROUP BY sim1.starId " +
            "   ) AS star_counts ON sim.starId = star_counts.starId " +
            "   WHERE sim.movieId = m.id " +
            ") AS movieStars, " +
            "SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT CONCAT(g.id, ': ', g.name) ORDER BY g.name ASC SEPARATOR ', '), ', ', 3) AS movieGenres " +
            "FROM movies AS m " +
            "LEFT JOIN ratings AS r ON m.id = r.movieId " +
            "LEFT JOIN genres_in_movies AS gim ON m.id = gim.movieId " +
            "LEFT JOIN genres AS g ON gim.genreId = g.id " +
            "WHERE m.id IN ( " +
            "   SELECT gim2.movieId " +
            "   FROM genres_in_movies AS gim2 " +
            "   WHERE gim2.genreId = ? " +
            ") " +
            "GROUP BY m.id, m.title, m.year, m.director, r.rating ";
    public static final String NON_ALPHANUMERIC_QUERY = "SELECT m.id AS movieId, m.title AS movieTitle, m.year AS movieYear, m.director AS movieDirector, r.rating AS movieRating, " +
            "( " +
            "   SELECT SUBSTRING_INDEX(GROUP_CONCAT(CONCAT(s.id, ': ', s.name) " +
            "                  ORDER BY star_counts.num_movies_star_acted_in DESC, s.name ASC " +
            "                  SEPARATOR ', '), ', ', 3) " +
            "   FROM stars_in_movies AS sim " +
            "   LEFT JOIN stars AS s ON sim.starId = s.id " +
            "   LEFT JOIN ( " +
            "       SELECT sim1.starId, COUNT(sim1.movieId) AS num_movies_star_acted_in " +
            "       FROM stars_in_movies AS sim1 " +
            "       GROUP BY sim1.starId " +
            "   ) AS star_counts ON sim.starId = star_counts.starId " +
            "   WHERE sim.movieId = m.id " +
            ") AS movieStars, " +
            "SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT CONCAT(g.id, ': ', g.name) ORDER BY g.name ASC SEPARATOR ', '), ', ', 3) AS movieGenres " +
            "FROM (SELECT * FROM movies AS m3 WHERE m3.title NOT REGEXP '^[0-9a-zA-Z]') AS m " +
            "LEFT JOIN ratings AS r ON m.id = r.movieId " +
            "LEFT JOIN genres_in_movies AS gim ON m.id = gim.movieId " +
            "LEFT JOIN genres AS g ON gim.genreId = g.id " +
            "GROUP BY m.id, m.title, m.year, m.director, r.rating ";
    public static final String START_WITH_QUERY = "SELECT m.id AS movieId, m.title AS movieTitle, m.year AS movieYear, m.director AS movieDirector, r.rating AS movieRating, " +
            "( " +
            "   SELECT SUBSTRING_INDEX(GROUP_CONCAT(CONCAT(s.id, ': ', s.name) " +
            "                  ORDER BY star_counts.num_movies_star_acted_in DESC, s.name ASC " +
            "                  SEPARATOR ', '), ', ', 3) " +
            "   FROM stars_in_movies AS sim " +
            "   LEFT JOIN stars AS s ON sim.starId = s.id " +
            "   LEFT JOIN ( " +
            "       SELECT sim1.starId, COUNT(sim1.movieId) AS num_movies_star_acted_in " +
            "       FROM stars_in_movies AS sim1 " +
            "       GROUP BY sim1.starId " +
            "   ) AS star_counts ON sim.starId = star_counts.starId " +
            "   WHERE sim.movieId = m.id " +
            ") AS movieStars, " +
            "SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT CONCAT(g.id, ': ', g.name) ORDER BY g.name ASC SEPARATOR ', '), ', ', 3) AS movieGenres " +
            "FROM (SELECT * FROM movies AS m3 WHERE LOWER(m3.title) LIKE ?) AS m " +
            "LEFT JOIN ratings AS r ON m.id = r.movieId " +
            "LEFT JOIN genres_in_movies AS gim ON m.id = gim.movieId " +
            "LEFT JOIN genres AS g ON gim.genreId = g.id " +
            "GROUP BY m.id, m.title, m.year, m.director, r.rating ";
    public static final String STARNAME_SEARCH_QUERY = "SELECT m.id AS movieId, m.title AS movieTitle, m.year AS movieYear, m.director AS movieDirector, r.rating AS movieRating, " +
            "( " +
            "   SELECT SUBSTRING_INDEX(GROUP_CONCAT(CONCAT(s.id, ': ', s.name) " +
            "                  ORDER BY star_counts.num_movies_star_acted_in DESC, s.name ASC " +
            "                  SEPARATOR ', '), ', ', 3) " +
            "   FROM stars_in_movies AS sim " +
            "   LEFT JOIN stars AS s ON sim.starId = s.id " +
            "   LEFT JOIN ( " +
            "       SELECT sim1.starId, COUNT(sim1.movieId) AS num_movies_star_acted_in " +
            "       FROM stars_in_movies AS sim1 " +
            "       GROUP BY sim1.starId " +
            "   ) AS star_counts ON sim.starId = star_counts.starId " +
            "   WHERE sim.movieId = m.id " +
            ") AS movieStars, " +
            "SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT CONCAT(g.id, ': ', g.name) ORDER BY g.name ASC SEPARATOR ', '), ', ', 3) AS movieGenres " +
            "FROM (SELECT * FROM movies AS m3" +
            "       WHERE (? IS NULL OR LOWER(m3.title) LIKE ?) " +
            "             AND (? IS NULL OR m3.year = ?) " +
            "             AND (? IS NULL OR LOWER(m3.director) LIKE ?) ) AS m " +
            "LEFT JOIN ratings AS r ON m.id = r.movieId " +
            "LEFT JOIN genres_in_movies AS gim ON m.id = gim.movieId " +
            "LEFT JOIN genres AS g ON gim.genreId = g.id " +
            "WHERE m.id IN ( " +
            "   SELECT sim2.movieId " +
            "   FROM stars_in_movies AS sim2 " +
            "   INNER JOIN stars AS s2 ON sim2.starId = s2.id " +
            "   WHERE LOWER(s2.name) LIKE ? " +
            ") " +
            "GROUP BY m.id, m.title, m.year, m.director, r.rating ";
    public static final String SEARCH_WITHOUT_STARNAME_QUERY = "SELECT m.id AS movieId, m.title AS movieTitle, m.year AS movieYear, m.director AS movieDirector, r.rating AS movieRating, " +
            "( " +
            "   SELECT SUBSTRING_INDEX(GROUP_CONCAT(CONCAT(s.id, ': ', s.name) " +
            "                  ORDER BY star_counts.num_movies_star_acted_in DESC, s.name ASC " +
            "                  SEPARATOR ', '), ', ', 3) " +
            "   FROM stars_in_movies AS sim " +
            "   LEFT JOIN stars AS s ON sim.starId = s.id " +
            "   LEFT JOIN ( " +
            "       SELECT sim1.starId, COUNT(sim1.movieId) AS num_movies_star_acted_in " +
            "       FROM stars_in_movies AS sim1 " +
            "       GROUP BY sim1.starId " +
            "   ) AS star_counts ON sim.starId = star_counts.starId " +
            "   WHERE sim.movieId = m.id " +
            ") AS movieStars, " +
            "SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT CONCAT(g.id, ': ', g.name) ORDER BY g.name ASC SEPARATOR ', '), ', ', 3) AS movieGenres " +
            "FROM (SELECT * FROM movies AS m3 " +
            "       WHERE (? IS NULL OR LOWER(m3.title) LIKE ?) " +
            "       AND (? IS NULL OR m3.year = ?) " +
            "       AND (? IS NULL OR LOWER(m3.director) LIKE ?) " +
            ") AS m " +
            "LEFT JOIN ratings AS r ON m.id = r.movieId " +
            "LEFT JOIN genres_in_movies AS gim ON m.id = gim.movieId " +
            "LEFT JOIN genres AS g ON gim.genreId = g.id " +
            "GROUP BY m.id, m.title, m.year, m.director, r.rating ";

    public static final String FULL_TEXT_SEARCH_QUERY = "SELECT m.id AS movieId, m.title AS movieTitle, m.year AS movieYear, m.director AS movieDirector, r.rating AS movieRating, " +
            "( " +
            "   SELECT SUBSTRING_INDEX(GROUP_CONCAT(CONCAT(s.id, ': ', s.name) " +
            "                  ORDER BY star_counts.num_movies_star_acted_in DESC, s.name ASC " +
            "                  SEPARATOR ', '), ', ', 3) " +
            "   FROM stars_in_movies AS sim " +
            "   LEFT JOIN stars AS s ON sim.starId = s.id " +
            "   LEFT JOIN ( " +
            "       SELECT sim1.starId, COUNT(sim1.movieId) AS num_movies_star_acted_in " +
            "       FROM stars_in_movies AS sim1 " +
            "       GROUP BY sim1.starId " +
            "   ) AS star_counts ON sim.starId = star_counts.starId " +
            "   WHERE sim.movieId = m.id " +
            ") AS movieStars, " +
            "SUBSTRING_INDEX(GROUP_CONCAT(DISTINCT CONCAT(g.id, ': ', g.name) ORDER BY g.name ASC SEPARATOR ', '), ', ', 3) AS movieGenres " +
            "FROM (SELECT * FROM movies AS m3 " +
            "       WHERE (? IS NULL OR MATCH (m3.title) AGAINST (? IN BOOLEAN MODE)) " +
            ") AS m " +
            "LEFT JOIN ratings AS r ON m.id = r.movieId " +
            "LEFT JOIN genres_in_movies AS gim ON m.id = gim.movieId " +
            "LEFT JOIN genres AS g ON gim.genreId = g.id " +
            "GROUP BY m.id, m.title, m.year, m.director, r.rating ";
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Convert into appropriate SQL pattern to format for LIKE operator
     */
    public String convertStringToPatternOrNull(String given_string, String pattern_wanted) {
        if (given_string == null) {
            return null;
        }
        else if (given_string.isEmpty()) {
            // return null if the length of it is zero/0
            return null;
        }
        else {
            if (pattern_wanted.compareTo("substring") == 0) {
                return "%" + given_string.toLowerCase() + "%";
            }
            else if (pattern_wanted.compareTo("start") == 0) {
                // method won't fail if string is number or letter
                return given_string.toLowerCase() + "%";
            }
            else if (pattern_wanted.compareTo("full-text") == 0) {
                String[] words = given_string.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    words[i] = "+" + words[i] + "*";
                }
                return String.join(" ", words);
            }
            else {
                // default return substring (if pattern_wanted is unrecognized)
                return "%" + given_string.toLowerCase() + "%";
            }
        }
    }

    public String convertIntegerToStringOrNull(Integer given_integer) {
        if (given_integer == null) {
            return null;
        }
        else {
            return String.valueOf(given_integer);
        }
    }

    /**
     * Return string associated with order id passed through from web browser
     */
    public String setupOrdering(int orderingUsed) {
        String orderResult;

        switch (orderingUsed) {
            case 0:
                orderResult = "m.title ASC, r.rating DESC";
                break;
            case 1:
                orderResult = "m.title ASC, r.rating ASC";
                break;
            case 2:
                orderResult = "m.title DESC, r.rating DESC";
                break;
            case 3:
                orderResult = "m.title DESC, r.rating ASC";
                break;
            case 4:
                orderResult = "r.rating DESC, m.title ASC";
                break;
            case 5:
                orderResult = "r.rating DESC, m.title DESC";
                break;
            case 6:
                orderResult = "r.rating ASC, m.title ASC";
                break;
            case 7:
                orderResult = "r.rating ASC, m.title DESC";
                break;
            default:
                orderResult = "";
                break;
        }
        return orderResult;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long servletStartTime = System.nanoTime();
        response.setContentType("application/json"); // Response mime type
        request.getServletContext().log("ResultsServlet: Entered GET search results");
        // Retrieve parameter from full text search
        String fullTextSearch = request.getParameter("full_text_search") == null ? null : request.getParameter("full_text_search");
        request.getServletContext().log("ResultsServlet: " +
                "got fullTextSearch: " + fullTextSearch);
        // Retrieve parameters from search
        String title = request.getParameter("title");
        Integer year = request.getParameter("year") == null ? null : Integer.valueOf(request.getParameter("year"));
        String director = request.getParameter("director");
        String starName = request.getParameter("star_name");
        request.getServletContext().log("ResultsServlet: " +
                "got title: " + title + ", " +
                "year: " + year + ", " +
                "director: " + director + ", " +
                "starName: " + starName);
        // Retrieve either browse parameter
        Integer genreId = request.getParameter("genre_id") == null ? null : Integer.valueOf(request.getParameter("genre_id"));
        String startWith = request.getParameter("start_with");
        request.getServletContext().log("ResultsServlet: " +
                "got genreId: " + genreId + ", " +
                "startWith: " + startWith);
        // Retrieve display parameters
        int orderUsed = 0;
        if (request.getParameter("order_used") != null) {
            orderUsed  = Integer.parseInt(request.getParameter("order_used"));
        }
        String orderByString = setupOrdering(orderUsed);
        // Retrieve pagination parameters
        int numResultsPerPage  = 25;
        int page = 1;
        if (request.getParameter("num_results_per_page") != null) {
            numResultsPerPage  = Integer.parseInt(request.getParameter("num_results_per_page"));
        }
        if (request.getParameter("page") != null) {
            page  = Integer.parseInt(request.getParameter("page"));
        }
        request.getServletContext().log("ResultsServlet: " +
                "got orderUsed: " + orderUsed + ", " +
                "numResultsPerPage: " + numResultsPerPage + ", " +
                "page: " + page);

        HttpSession session = request.getSession();
        PreviousMovieListUrl previousMovieListUrl = (PreviousMovieListUrl) session.getAttribute("previous_movie_list_url");
        if (previousMovieListUrl == null) {
            previousMovieListUrl = new PreviousMovieListUrl("results.html", new HashMap<String, String>());
        }
        else {
            previousMovieListUrl = (PreviousMovieListUrl) session.getAttribute("previous_movie_list_url");
            previousMovieListUrl.clearEverything();
            previousMovieListUrl.setPage("results.html");
        }
        setPreviousMovieListUrlAttributesIntoSession(previousMovieListUrl, orderUsed, numResultsPerPage,
                page,fullTextSearch, genreId, startWith, title, year, director, starName, session);

        PrintWriter out = response.getWriter();
        long jdbcTotalTime;
        long jdbcStartTime = System.nanoTime();
        try (
                Connection conn = dataSource.getConnection();
        ) {
            jdbcTotalTime = System.nanoTime() - jdbcStartTime;
            String query;
            PreparedStatement statement;
            // first check the full text search parameter
            if (fullTextSearch != null) {
                request.getServletContext().log("ResultsServlet: entered fullTextSearch query");
                query = ResultsServlet.FULL_TEXT_SEARCH_QUERY +
                        "ORDER BY " + orderByString + " " +
                        "LIMIT " + numResultsPerPage + " " +
                        "OFFSET " + ((page - 1) * numResultsPerPage);
                jdbcStartTime = System.nanoTime();
                statement = conn.prepareStatement(query);
                jdbcTotalTime += System.nanoTime() - jdbcStartTime;
                statement.setString(1, convertStringToPatternOrNull(fullTextSearch, "full-text"));
                statement.setString(2, convertStringToPatternOrNull(fullTextSearch, "full-text"));
            }
            // then check the browse parameters (if they exist, then we aren't 'searching')
            else if (genreId != null) {
                request.getServletContext().log("ResultsServlet: entered genreId query");
                query = ResultsServlet.BROWSE_GENRE_QUERY +
                        "ORDER BY " + orderByString + " " +
                        "LIMIT " + numResultsPerPage + " " +
                        "OFFSET " + ((page - 1) * numResultsPerPage);
                jdbcStartTime = System.nanoTime();
                statement = conn.prepareStatement(query);
                jdbcTotalTime += System.nanoTime() - jdbcStartTime;
                statement.setString(1, convertIntegerToStringOrNull(genreId));
            }
            else if (startWith != null) {
                request.getServletContext().log("ResultsServlet: entered startWith query");
                if (startWith.compareTo("*") == 0) {
                    request.getServletContext().log("ResultsServlet: entered * query");
                    query = ResultsServlet.NON_ALPHANUMERIC_QUERY +
                            "ORDER BY " + orderByString + " " +
                            "LIMIT " + numResultsPerPage + " " +
                            "OFFSET " + ((page - 1) * numResultsPerPage);
                    jdbcStartTime = System.nanoTime();
                    statement = conn.prepareStatement(query);
                    jdbcTotalTime += System.nanoTime() - jdbcStartTime;
                }
                else {
                    request.getServletContext().log("ResultsServlet: entered " + startWith + " query");
                    // use the parameter
                    query = ResultsServlet.START_WITH_QUERY +
                            "ORDER BY " + orderByString + " " +
                            "LIMIT " + numResultsPerPage + " " +
                            "OFFSET " + ((page - 1) * numResultsPerPage);
                    jdbcStartTime = System.nanoTime();
                    statement = conn.prepareStatement(query);
                    jdbcTotalTime += System.nanoTime() - jdbcStartTime;
                    statement.setString(1, convertStringToPatternOrNull(startWith, "start"));
                }
            }
            else if (starName != null) {
                request.getServletContext().log("ResultsServlet: entered (with starName) query");
                query = ResultsServlet.STARNAME_SEARCH_QUERY +
                        "ORDER BY " + orderByString + " " +
                        "LIMIT " + numResultsPerPage + " " +
                        "OFFSET " + ((page - 1) * numResultsPerPage);
                jdbcStartTime = System.nanoTime();
                statement = conn.prepareStatement(query);
                jdbcTotalTime += System.nanoTime() - jdbcStartTime;
                statement.setString(1, convertStringToPatternOrNull(title, "substring"));
                statement.setString(2, convertStringToPatternOrNull(title, "substring"));
                statement.setString(3, convertIntegerToStringOrNull(year));
                statement.setString(4, convertIntegerToStringOrNull(year));
                statement.setString(5, convertStringToPatternOrNull(director, "substring"));
                statement.setString(6, convertStringToPatternOrNull(director, "substring"));
                statement.setString(7, convertStringToPatternOrNull(starName, "substring"));
            }
            else {
                request.getServletContext().log("ResultsServlet: entered search (without starName) query");
                query = SEARCH_WITHOUT_STARNAME_QUERY +
                        "ORDER BY " + orderByString + " " +
                        "LIMIT " + numResultsPerPage + " " +
                        "OFFSET " + ((page - 1) * numResultsPerPage);
                jdbcStartTime = System.nanoTime();
                statement = conn.prepareStatement(query);
                jdbcTotalTime += System.nanoTime() - jdbcStartTime;
                statement.setString(1, convertStringToPatternOrNull(title, "substring"));
                statement.setString(2, convertStringToPatternOrNull(title, "substring"));
                statement.setString(3, convertIntegerToStringOrNull(year));
                statement.setString(4, convertIntegerToStringOrNull(year));
                statement.setString(5, convertStringToPatternOrNull(director, "substring"));
                statement.setString(6, convertStringToPatternOrNull(director, "substring"));
            }
            request.getServletContext().log(
                    "ResultsServlet: about to execute the query: " +
                            query);
            jdbcStartTime = System.nanoTime();
            ResultSet resultSet = statement.executeQuery();
            jdbcTotalTime += System.nanoTime() - jdbcStartTime;
            request.getServletContext().log("ResultsServlet: executed it");

            JsonArray objectArray = new JsonArray();
            addMovieResultsToJsonArrayResponse(resultSet, objectArray);
            resultSet.close();
            statement.close();
            writeToFile(request, System.nanoTime() - servletStartTime, jdbcTotalTime);
            request.getServletContext().log("getting responseJsonObject results");
            out.write(objectArray.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject errorObject = new JsonObject();
            request.getServletContext().log("Error inside ResultsServlet was: " + e.getMessage());
            errorObject.addProperty("errorMessage", e.getMessage());
            out.write(errorObject.toString());
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    private void addMovieResultsToJsonArrayResponse(ResultSet resultSet, JsonArray objectArray) throws SQLException {
        while (resultSet.next()) {
            String movie_id = resultSet.getString("movieId");
            String movie_title = resultSet.getString("movieTitle");
            String movie_year = resultSet.getString("movieYear");
            String movie_director = resultSet.getString("movieDirector");
            String movie_rating = resultSet.getString("movieRating");
            String movie_stars = resultSet.getString("movieStars");
            String movie_genres = resultSet.getString("movieGenres");

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("movie_id", movie_id);
            jsonObject.addProperty("movie_title", movie_title);
            jsonObject.addProperty("movie_year", movie_year);
            jsonObject.addProperty("movie_director", movie_director);
            jsonObject.addProperty("movie_rating", movie_rating);
            jsonObject.addProperty("movie_stars", movie_stars);
            jsonObject.addProperty("movie_genres", movie_genres);
            // Add the movie object with all the information to the moviesArray
            objectArray.add(jsonObject);
        }
    }

    private void setPreviousMovieListUrlAttributesIntoSession(PreviousMovieListUrl previousMovieListUrl, int orderUsed, int numResultsPerPage, int page, String fullTextSearch, Integer genreId, String startWith, String title, Integer year, String director, String starName, HttpSession session) {
        previousMovieListUrl.setParam("order_used", String.valueOf(orderUsed));
        previousMovieListUrl.setParam("num_results_per_page", String.valueOf(numResultsPerPage));
        previousMovieListUrl.setParam("page", String.valueOf(page));
        if (fullTextSearch != null) {
            previousMovieListUrl.setParam("full_text_search", String.valueOf(fullTextSearch));
        }
        else if (genreId != null) {
            previousMovieListUrl.setParam("genre_id", String.valueOf(genreId));
        }
        else if (startWith != null) {
            previousMovieListUrl.setParam("start_with", startWith);
        }
        else {
            // it must've been search (only add the ones we actually used)
            if (title != null) { previousMovieListUrl.setParam("title", title); }
            if (year != null) { previousMovieListUrl.setParam("year", String.valueOf(year)); }
            if (director != null) { previousMovieListUrl.setParam("director", director); }
            if (starName != null) { previousMovieListUrl.setParam("star_name", starName); }
        }
        session.setAttribute("previous_movie_list_url", previousMovieListUrl);
    }

    private void writeToFile(HttpServletRequest request, long servletTotalTime, long jdbcTotalTime) {
        try {
            String contextPath = request.getServletContext().getRealPath("/");
            String timeTextFilePath = contextPath + "\\timing.txt";
            File file = new File(timeTextFilePath);
            file.createNewFile();
            synchronized (this) {
                try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
                    writer.println((servletTotalTime) + " " + (jdbcTotalTime));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
