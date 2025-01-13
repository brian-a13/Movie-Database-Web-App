package xmlParsers;

import java.io.IOException;
import java.sql.*;
import java.util.*;


import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;
import java.sql.Connection;


// Possible inconsistencies:
//  - in the dob is said to be "" if it is not know but some have 'n.a'
//  - duplicate actors
public class SAXActorParser extends DefaultHandler {
    HashMap<String, HashSet<Integer>> stars;    // {starName: {dob1, dob2, ..., dobx}} used to avoid duplicates
    ArrayList<Stars> starsToBeAdded = new ArrayList<>();    // used by the threads
    private String starIdToAdd;
    private String tempVal;

    //to maintain context
    private Stars tempStar;

    private int inconsistencies;
    private int duplicateStars;
    private int starsAdded;

    public SAXActorParser() {
        stars = new HashMap<>();
        inconsistencies = 0;
        starsAdded = 0;
        duplicateStars = 0;
    }


    public void runParser() {
        getHighestStarIdFromDatabase();
        parseDocument();
        Threads.executeThreads(starsToBeAdded, null, null, null, null);
        duplicateStars += Threads.getStarsAlreadyExistCount();  // these ones were already in database but may have not been duplicates in xml
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

    public HashMap<String, String> getStarNameToIds() {
        HashMap<String, String> starNameToIds = new HashMap<>();
        for (Stars star : starsToBeAdded) {
            starNameToIds.put(star.getName(), star.getId());
        }
        return starNameToIds;
    }

    private void parseDocument() {
        //get a factory
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            //get a new instance of parser
            SAXParser sp = spf.newSAXParser();
            //parse the file and also register this class for call backs
            sp.parse("src/xmlParsers/stanford-movies/actors63.xml", this);

        } catch (SAXException se) {
            se.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }


    private void printData() {
        System.out.println("Number of Stars Inserted: " + starsAdded);
        System.out.println("Number of Duplicate Stars: " + duplicateStars);
    }

    private void incrementAndFormatStarId() {
        int numberByItselfIncremented = Integer.parseInt(starIdToAdd.substring(2)) + 1;
        starIdToAdd = "nm" + String.format("%08d", numberByItselfIncremented);
    }

    //Event Handlers
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //resets
        tempVal = "";
        if (qName.equalsIgnoreCase("actor")) {
            tempStar = new Stars();
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {

        if (qName.equalsIgnoreCase("actor")) {
            if (stars.containsKey(tempStar.getName()) && stars.get(tempStar.getName()).contains(tempStar.getBirthYear())) {
                inconsistencies++;
                duplicateStars++;
            }
            else {
                tempStar.setId(starIdToAdd);
                incrementAndFormatStarId();
                starsToBeAdded.add(tempStar);
                if (!stars.containsKey(tempStar.getName())) {
                    // if it doesn't contain the name, add it
                    stars.put(tempStar.getName(), new HashSet<>());
                }
                // add the date to the set associated with that name to make sure to not add it again
                stars.get(tempStar.getName()).add(tempStar.getBirthYear());
                starsAdded++;
            }
        }
        else if (qName.equalsIgnoreCase("stagename")) {
            tempStar.setName(tempVal.trim());
        }
        else if (qName.equalsIgnoreCase("dob")) {
            try {
                tempStar.setBirthYear(Integer.parseInt(tempVal.trim()));
            }
            catch (Exception e){
                tempStar.setBirthYear(-1);
            }
        }
    }

    public static void main(String[] args) {
        SAXActorParser spe = new SAXActorParser();
        spe.runParser();
    }
}
