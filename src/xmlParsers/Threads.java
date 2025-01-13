package xmlParsers;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Threads {
    public static int starsAlreadyExistCount;

    public static int moviesAlreadyExistCount;
    public static int genresAddedCount;
    public static int genresLinked;

    public static int genresAndMovieLinkAlreadyExistsCount;
    public static int starAndMovieLinkAlreadyExistsCount;
    public static int starDoesNotExistCount;
    public static int movieDoesNotExistCount;

    public static int getStarsAlreadyExistCount() {
        return starsAlreadyExistCount;
    }
    public static int getGenresAddedCount() {return genresAddedCount;}

    public static int genresLinkedToMoviesCount() {return genresLinked;}

    public static int getStarAndMovieLinkAlreadyExistsCount() {
        return starAndMovieLinkAlreadyExistsCount;
    }
    public static int getGenreAndMovieLinkAlreadyExistsCount() {
        return genresAndMovieLinkAlreadyExistsCount;
    }
    public static int getStarDoesNotExistCount() {
        return starDoesNotExistCount;
    }
    public static int getMovieDoesNotExistCount() {
        return movieDoesNotExistCount;
    }

    public static void executeThreads(ArrayList<Stars> starsToBeAdded,
                                      HashMap<String, HashSet<String>> genreNamesToMovieId,
                                      ArrayList<Movies> moviesToBeAdded,
                                      HashSet<String> genresToBeAdded,
                                      HashMap<String, HashSet<String>> starIdToMovieIds) {
        // IF YOU ADD TO THIS, CONTROL+SHIFT+F THROUGH ALL FILES and add null arguments

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try (Connection sharedConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/moviedb", "primaryuser", "mypassword")) {
            sharedConnection.setAutoCommit(false);
            ExecutorService executor = Executors.newFixedThreadPool(3);

            starsAlreadyExistCount = 0;
            if (starsToBeAdded != null) {
                for (Stars star : starsToBeAdded) {
                    QueryWorkerStars worker = new QueryWorkerStars(star, sharedConnection);
                    executor.execute(worker);
                }
            }

            moviesAlreadyExistCount = 0;
            if (moviesToBeAdded != null) {
                for (Movies movie : moviesToBeAdded) {
                    QueryWorkerMovies worker = new QueryWorkerMovies(movie, sharedConnection);
                    executor.execute(worker);
                }
            }

            genresAddedCount= 0;
            if (genresToBeAdded != null) {
                for (String genre : genresToBeAdded) {
                    QueryWorkerGenres worker = new QueryWorkerGenres(genre, sharedConnection);
                    executor.execute(worker);
                }
            }

            //CHANGE THIS TO ITERATE THROUGH GENRE MOVIE LINKS
            genresLinked= 0;
            if (genreNamesToMovieId != null) {
                for (HashMap.Entry<String, HashSet<String>> entry : genreNamesToMovieId.entrySet()) {
                    String movieId = entry.getKey();
                    HashSet<String> genres = entry.getValue();

                    // Iterate through the HashSet (nested loop)
                    for (String genre : genres) {
                        QueryWorkerGenresToMovies worker = new QueryWorkerGenresToMovies(movieId, genre, sharedConnection);
                        executor.execute(worker);
                    }
                }
            }


            starAndMovieLinkAlreadyExistsCount = 0;
            starDoesNotExistCount = 0;
            movieDoesNotExistCount = 0;
            if (starIdToMovieIds != null) {
                for (HashMap.Entry<String, HashSet<String>> entry : starIdToMovieIds.entrySet()) {
                    String movieId = entry.getKey();
                    HashSet<String> starIds = entry.getValue();

                    // Iterate through the HashSet (nested loop)
                    for (String starId : starIds) {
                        QueryWorkerStarToMovies worker = new QueryWorkerStarToMovies(starId, movieId, sharedConnection);
                        executor.execute(worker);
                    }
                }
            }
            sharedConnection.commit();
            sharedConnection.setAutoCommit(true);

            executor.shutdown();
            while (!executor.isTerminated()) {}
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static class QueryWorkerStars implements Runnable {
        Stars star;
        Connection sharedConnection;

        QueryWorkerStars(Stars star, Connection sharedConnection) {
            this.star = star;
            this.sharedConnection = sharedConnection;
        }

        @Override
        public void run() {
            try {
                String addStarProcedure = "{CALL add_star_with_id(?, ?, ?, ?)}";
                try (CallableStatement callableStatement = sharedConnection.prepareCall(addStarProcedure)) {
                    callableStatement.setString(1, star.getId());
                    callableStatement.setString(2, star.getName());
                    if (star.getBirthYear() != -1) {
                        callableStatement.setInt(3, star.getBirthYear());
                    }
                    else {
                        callableStatement.setNull(3, java.sql.Types.INTEGER);
                    }
                    callableStatement.registerOutParameter(4, java.sql.Types.VARCHAR);

                    synchronized (callableStatement) {
                        callableStatement.execute();
                    }

                    String errorMessage = callableStatement.getString(4);
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        starsAlreadyExistCount++;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static class QueryWorkerGenres implements Runnable {
        // genre name and adds it to the genres table (add_genre stored procedure won't add if duplicate)
        String genreName;
        Connection sharedConnection;

        QueryWorkerGenres(String genreName, Connection sharedConnection) {
            this.genreName = genreName;
            this.sharedConnection = sharedConnection;
        }
        @Override
        public void run() {
            try{
                String addStarProcedure = "{CALL add_genre(?,?,?)}";
                try (CallableStatement callableStatement = sharedConnection.prepareCall(addStarProcedure)) {
                    callableStatement.setString(1, genreName);

                    callableStatement.registerOutParameter(3, Types.BOOLEAN);

                    synchronized (callableStatement) {
                        callableStatement.execute();
                    }

                    boolean result = callableStatement.getBoolean(3);
                    if (!result) {
                       genresAddedCount++;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static class QueryWorkerMovies implements Runnable {
        Movies movie;

        Connection sharedConnection;

        QueryWorkerMovies(Movies movie, Connection sharedConnection){
            this.movie = movie;
            this.sharedConnection = sharedConnection;
        }

        @Override
        public void run() {
            try {
                String addStarProcedure = "{CALL add_movie_alone_with_id(?, ?, ?, ?, ?)}";
                try (CallableStatement callableStatement = sharedConnection.prepareCall(addStarProcedure)) {
                    callableStatement.setString(1, movie.getId());
                    callableStatement.setString(2, movie.getTitle());
                    callableStatement.setInt(3, movie.getYear());
                    callableStatement.setString(4, movie.getDirector());

                    callableStatement.registerOutParameter(5, java.sql.Types.VARCHAR);

                    synchronized (callableStatement) {
                        callableStatement.execute();
                    }

                    String errorMessage = callableStatement.getString(5);
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        moviesAlreadyExistCount++;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static class QueryWorkerGenresToMovies implements Runnable {
        String movieId;
        String genre;
        Connection sharedConnection;

        QueryWorkerGenresToMovies(String movieId, String genre, Connection sharedConnection) {
            this.movieId = movieId;
            this.genre = genre;
            this.sharedConnection = sharedConnection;
        }

        @Override
        public void run() {
            try {
                String callProcedure = "{call link_genre_and_movie_id(?,?,?,?)}";

                try (CallableStatement callableStatement = sharedConnection.prepareCall(callProcedure)) {
                    callableStatement.setString(1, this.genre);
                    callableStatement.setString(2,  this.movieId);
                    callableStatement.registerOutParameter(4, Types.VARCHAR);
                    synchronized (callableStatement) {
                        callableStatement.execute();
                    }
                    String errorMessage = callableStatement.getString(4);

                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        switch (errorMessage) {
                            case "Error: Pair exists already.":
                                genresAndMovieLinkAlreadyExistsCount++;
                                break;
                            case "Error: Movie does not exist.":
                                movieDoesNotExistCount++;
                                break;
                        }
                    }else{
                        genresLinked++;
                    }
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static class QueryWorkerStarToMovies implements Runnable {
        String starId;
        String movieId;
        Connection sharedConnection;

        QueryWorkerStarToMovies(String starId, String movieId, Connection sharedConnection) {
            this.starId = starId;
            this.movieId = movieId;
            this.sharedConnection = sharedConnection;
        }

        @Override
        public void run() {
            try {
                String callProcedure = "{call link_star_id_and_movie_id(?, ?, ?)}";

                try (CallableStatement callableStatement = sharedConnection.prepareCall(callProcedure)) {
                    callableStatement.setString(1, this.starId);
                    callableStatement.setString(2,  this.movieId);
                    callableStatement.registerOutParameter(3, Types.VARCHAR);
                    synchronized (callableStatement) {
                        callableStatement.execute();
                    }
                    String errorMessage = callableStatement.getString(3);

                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        switch (errorMessage) {
                            case "Error: Pair exists already.":
                                starAndMovieLinkAlreadyExistsCount++;
                                break;
                            case "Error: Star does not exist.":
                                starDoesNotExistCount++;
                                break;
                            case "Error: Movie does not exist.":
                                movieDoesNotExistCount++;
                                break;
                        }
                    }
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
