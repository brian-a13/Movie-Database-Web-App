package xmlParsers;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;


// Possible inconsistencies:
//  - error in year given (19yy) - not added to DB
//  - error in the genres listed - still added to DB

public class SAXMoviesParser extends DefaultHandler {
    private HashSet<String> movies; //To check for duplicates in XML via FID

    private ArrayList<Movies> allMovies; //Contains entire movie objects
    String movieIdToAdd;

    private String tempVal;

    //to maintain context
    private Movies tempMovie;

    private int inconsistencies;

    private boolean consistent;

    private HashMap<String, HashSet<String>> genresToMovieLinks; // {moviedId : (genre, genre, ...)}

    private HashSet<String> tempSetOfGenres;
    private int genresAddedCount;
    private int genresInMoviesCount;

    private int genresAndMovieLinkAlreadyExistsCount;
    private HashSet<String> allGenres;
    private HashMap<String, String> genresParsed; // {genre



    public SAXMoviesParser() {
        movies = new HashSet<String>();
        allMovies = new ArrayList<Movies>();
        genresToMovieLinks = new HashMap<String, HashSet<String>>();
        allGenres = new HashSet<String>();

        genresParsed = new HashMap<>();
        genresParsed.put("dram", "Drama");
        genresParsed.put("susp", "Thriller");
        genresParsed.put("romt", "Romantic");
        genresParsed.put("myst", "Mystery");
        genresParsed.put("docu", "Documentary");
        genresParsed.put("advt", "Adventure");
        genresParsed.put("actn", "Violence");
        genresParsed.put("comd", "Comedy");
        genresParsed.put("musc", "Musical");
        genresParsed.put("avga", "Avant Garde");
        genresParsed.put("cart", "Cartoon");
        genresParsed.put("camp", "Camp");
        genresParsed.put("disa", "Disaster");
        genresParsed.put("cnr", "Cops and Robbers");
        genresParsed.put("epic", "Epic");
        genresParsed.put("faml", "Family");
        genresParsed.put("hist", "History");
        genresParsed.put("horr", "Horror");
        genresParsed.put("noir", "Black");
        genresParsed.put("porn", "Pornography");
        genresParsed.put("scfi", "Science Fiction");
        genresParsed.put("surl", "Sureal");
        genresParsed.put("west", "Western");




        inconsistencies = 0;
    }


    public void runParser() {
        getHighestMovieIdFromDatabase();
        parseDocument();
        Threads.executeThreads(null, genresToMovieLinks, allMovies, allGenres, null);
        genresAddedCount = Threads.getGenresAddedCount();
        genresInMoviesCount = Threads.genresLinkedToMoviesCount();
        genresAndMovieLinkAlreadyExistsCount = Threads.getGenreAndMovieLinkAlreadyExistsCount();
        printData();
    }

    private void getHighestMovieIdFromDatabase() {
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
            String callProcedure = "{CALL get_highest_movie_id(?)}";
            CallableStatement callableStatement = connection.prepareCall(callProcedure);
            callableStatement.registerOutParameter(1, java.sql.Types.VARCHAR);
            callableStatement.execute();
            movieIdToAdd = callableStatement.getString(1);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Issue connecting to database");
        }
    }

    private void incrementAndFormatMovieId() {
        int numberByItselfIncremented = Integer.parseInt(movieIdToAdd.substring(2)) + 1;
        movieIdToAdd = "tt" + String.format("%08d", numberByItselfIncremented);
    }

    private void parseDocument() {

        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {

            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();

            //parse the file and also register this class for call backs
            sp.parse("src/xmlParsers/stanford-movies/mains243.xml", this);

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
        System.out.println("Number of Movies with inconsistencies: "+ inconsistencies);
        System.out.println("Number of Movies added: "+ allMovies.size());
        System.out.println("Number of Genres added: "+ genresAddedCount);
        System.out.println("Number of added links of Genres with Movies: " + genresInMoviesCount);
    }

    /**
     * Builds table that Cast XML parser will need to function (uncomment when done)
     */
    public HashMap<String, String> getMovieFIDToDBIds() {
        HashMap<String, String> movieFIDToDBIds = new HashMap<>();
        for (Movies movie : allMovies) {    // assuming you end up having some container of movie objects that will be added (this should be one of the objects that should be passed to threads)
            movieFIDToDBIds.put(movie.getXMLId(), movie.getId());
        }
        return movieFIDToDBIds;
    }

    //Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //resets
        tempVal = "";
        if (qName.equalsIgnoreCase("film")) {
            //create a new instance of employee
            tempMovie = new Movies();
            consistent = true;
            tempSetOfGenres = new HashSet<String>();
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
//        if (tempVal.equals("George Abbott")){
//            throw new SAXException("Found the specific stagename, stopping parsing");
//        }
        //System.out.println(tempVal);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (qName.equalsIgnoreCase("film")) {

            // check if the movie already exists (not through database checking)
            // and if it had a valid year

            if(movies.contains(tempMovie.getId()) || tempMovie.getYear() == -1 ||
                    tempMovie.getDirector() == null || tempMovie.getXMLId() == null ){
                inconsistencies++;
            }else {

                // at this point the movie would technically be inserted
                // maybe have one set of all the genre names you encounter 'allGenreNames' (doesn't matter if they exist or not in DB)
                // maybe have one ArrayList of 'Movies' objects 'allMovies' (I don't think we need a numDirectors attribute)
                //                          movie DB ID, {set of Genre Name strings)    reason is i think it is easier if we don't do the genres by ID
                // then have perhaps a HashMap<String, HashSet<String>>


                genresToMovieLinks.put(tempMovie.getId(), tempSetOfGenres);
                allMovies.add(tempMovie);
                incrementAndFormatMovieId();
            }

        } else if (qName.equalsIgnoreCase("fid")) {
            tempMovie.setId(movieIdToAdd);
            tempMovie.setXMLId(tempVal);
            movies.add(tempVal);
        } else if (qName.equalsIgnoreCase("t")) {
            tempMovie.setTitle(tempVal.trim());
        }else if (qName.equalsIgnoreCase("year")) {
            try{
                tempMovie.setYear(Integer.parseInt(tempVal.trim()));
            }catch (Exception e){
                if(consistent) { // To prevent from double counting inconsistencies
                    consistent = false;
                    inconsistencies++;
                }
                tempMovie.setYear(-1);
            }
        }else if (qName.equalsIgnoreCase("dirn")) {
//                if(tempVal.trim().isEmpty()){
//                    System.out.println(tempMovie.getXMLId());
//                    throw new SAXException("Found the specific stagename, stopping parsing");
//                }
                tempMovie.setDirector(tempVal.trim());
                //tempMovie.addDirectorCount();
        }else if (qName.equalsIgnoreCase("cat")) {
            if(!genresParsed.containsKey(tempVal.trim().toLowerCase())){
                if(consistent) { // To prevent from double counting inconsistencies
                    consistent = false;
                    inconsistencies++;
                }
            }else{
                //genresToMovieLinks.putgenresParsed.get(tempVal.trim().toLowerCase())
                tempSetOfGenres.add(genresParsed.get(tempVal.trim().toLowerCase()));
                allGenres.add(genresParsed.get(tempVal.trim().toLowerCase()));
            }
        }

    }

    public static void main(String[] args) {
        SAXMoviesParser spe = new SAXMoviesParser();
        spe.runParser();
    }

}
