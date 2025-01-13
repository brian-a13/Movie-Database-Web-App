package xmlParsers;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


// Possible inconsistencies:
//  - multiple directors
//  - duplicate actors
public class SAXCastsParser extends DefaultHandler {
    // In here no new movies as casts.xml states that the movies are a reference to those in main
    HashMap<String, String> knownStarNameToIds;
    HashMap<String, String> knownMovieFidToDBIds;
    private String previousMovieFidThatHadStar;
    private int moviesWithStars;
    private HashSet<String> castPerMovie;   // resets when movie changes (removed duplicate line entries)
    ArrayList<Stars> starsToBeAdded = new ArrayList<>();    // used by the threads
    HashMap<String, HashSet<String>> movieToStarsLinks; // movieId: {starid1, starid2...}
    HashSet<String> tempStarIds;
    private String tempVal;
    private String tempStarName;
    private String starIdToAdd;

    private String tempMovieFid;
    private int movie_does_not_exist;
    private int starsInMoviesCount;
    private int starsAlreadyInMovieCount;
    private int unknownActorsCount;

    public SAXCastsParser(HashMap<String, String> starNameToIds, HashMap<String, String> movieFidToDBIds) {
        knownStarNameToIds = starNameToIds;
        knownMovieFidToDBIds = movieFidToDBIds;
        movieToStarsLinks = new HashMap<String, HashSet<String>>();
        tempStarIds = new HashSet<String>();
        starsToBeAdded = new ArrayList<>();
        previousMovieFidThatHadStar = "";
        moviesWithStars = 0;
        castPerMovie = new HashSet<String>();
        movie_does_not_exist = 0;
        starsInMoviesCount = 0;
        starsAlreadyInMovieCount = 0;
        unknownActorsCount = 0;
    }


    public void runParser() {
        getHighestStarIdFromDatabase();
        parseDocument();
        Threads.executeThreads(starsToBeAdded, null, null, null, movieToStarsLinks);
        movie_does_not_exist += Threads.getMovieDoesNotExistCount();
        starsAlreadyInMovieCount += Threads.getStarAndMovieLinkAlreadyExistsCount();
        printData();
    }


    private void getHighestStarIdFromDatabase() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "primaryuser", "mypassword")) {
            String callProcedure = "{CALL get_highest_star_id(?)}";
            CallableStatement callableStatement = connection.prepareCall(callProcedure);
            callableStatement.registerOutParameter(1, java.sql.Types.VARCHAR);
            callableStatement.execute();
            starIdToAdd = callableStatement.getString(1);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Issue connecting to database");
        }
    }

    private void incrementAndFormatStarId() {
        int numberByItselfIncremented = Integer.parseInt(starIdToAdd.substring(2)) + 1;
        starIdToAdd = "nm" + String.format("%08d", numberByItselfIncremented);
    }


    private void parseDocument() {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            SAXParser sp = spf.newSAXParser();sp.parse("src/xmlParsers/stanford-movies/casts124.xml", this);
        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Iterate through the list and print
     * the contents
     */
    private void printData() {
        System.out.println("Number of added links of Star with Movie: " + starsInMoviesCount);
        System.out.println("Number of Movies not Found: " + movie_does_not_exist);
        System.out.println("Number of duplicate/already existing Star with Movie: " + starsAlreadyInMovieCount);
        System.out.println("Number of entries with unknown actors: " + unknownActorsCount);
    }

    //Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //resets
        tempVal = "";
        if (qName.equalsIgnoreCase("filmc")) {
            //reset cast to correctly check for duplicates (an actor can be in multiple movies but not in the same movie twice)
            castPerMovie.clear();
            tempStarIds = new HashSet<String>();
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (qName.equalsIgnoreCase("m")) {
            if (castPerMovie.contains(tempStarName) ||
                    tempStarName.isEmpty()) {
                // don't add the star if they already showed up for this same movie in this xml file
                starsAlreadyInMovieCount++;
            }
            else {
                // check if the star even exists, if not, add it to the list of stars we need to create
                // before we add it to the "stars_in_movies" table
                if (!knownStarNameToIds.containsKey(tempStarName)) {
                    Stars starToAdd = new Stars();
                    starToAdd.setId(starIdToAdd);
                    starToAdd.setName(tempStarName);
                    starsToBeAdded.add(starToAdd);
                    tempStarIds.add(starIdToAdd);
                    incrementAndFormatStarId();
                }
                else {
                    tempStarIds.add(knownStarNameToIds.get(tempStarName));
                }

                // add the star to the cast now that it is being added for this movie
                castPerMovie.add(tempStarName);
                // add the movie to the overall set of movies with at least 1 star
                if (!previousMovieFidThatHadStar.equals(tempMovieFid)) {
                    moviesWithStars++;
                    previousMovieFidThatHadStar = tempMovieFid;
                }
                // new star movie link
                starsInMoviesCount++;
            }
            // reset for next entry
            tempStarName = "";
//            tempMovieFid = "";
        }
        else if (qName.equalsIgnoreCase("a")) {
            tempStarName = tempVal.trim();
            if (tempStarName.equals("sa") || tempStarName.equals("s a")) {
                tempStarName = "";  // set to empty string and check when adding
                unknownActorsCount++;
            }
            // don't add their name to the hash set of castPerMovie
            // here because then no one will be inserted
        }
        else if (qName.equalsIgnoreCase("f")) {
            tempMovieFid = tempVal.trim();
        }
        else if (qName.equalsIgnoreCase("filmc")) {
            if (knownMovieFidToDBIds.containsKey(tempMovieFid)) {
                // only enter if the movie DOES exist
                movieToStarsLinks.put(knownMovieFidToDBIds.get(tempMovieFid), tempStarIds);
                tempMovieFid = "";
            }
            else {
                movie_does_not_exist++;
            }
        }
    }

    public int getNumberOfMoviesWithStars() {
        return moviesWithStars;
    }
}
