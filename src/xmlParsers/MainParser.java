package xmlParsers;

import java.util.HashMap;

public class MainParser {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        // 1. Parse the actors XML file
        SAXActorParser actorParser = new SAXActorParser();
        actorParser.runParser();
        HashMap<String, String> starNameToIds = actorParser.getStarNameToIds(); // star name to DATABASE star Ids (this is fine because casts.xml doesn't specify id or dob so just pick 1)


        // 2. Parse the movies XML file
        SAXMoviesParser moviesParser = new SAXMoviesParser();
        moviesParser.runParser();
        HashMap<String, String> movieFIDToDBIds = moviesParser.getMovieFIDToDBIds(); // dictionary of <main.xml FID>: DB Movie ID
          // this step above is super important or casts parser won't work.

        // 3. Parse the casts XML file
        SAXCastsParser castsParser = new SAXCastsParser(starNameToIds, movieFIDToDBIds);
        castsParser.runParser();
        int numMoviesWithStars = castsParser.getNumberOfMoviesWithStars();


        System.out.println("Number of Movies without Stars: " + (movieFIDToDBIds.size() - numMoviesWithStars));

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;
        System.out.println("Elapsed Time: " + elapsedTime + " milliseconds");



    }
}
