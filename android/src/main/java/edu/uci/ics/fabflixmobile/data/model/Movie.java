package edu.uci.ics.fabflixmobile.data.model;

/**
 * Movie class that captures movie information for movies retrieved from MovieListActivity
 */
public class Movie {
    //These will store the movies info already in strings, ready to be displayed
    // Ex: rating = "Rating: 8.3"

    private final String id;
    private final String nameYear;
    private final String rating;
    private final String director;
    private final String genres;
    private final String stars;

    public Movie(String id, String name, String year, String rating, String director, String genres, String stars) {
        this.id = id;
        this.nameYear = name + " (" + year + ")";
        if (rating.equals("null")) this.rating = "Rating: N/A";
        else {
            this.rating = "Rating: " + rating;
        }

        this.director = "Director: " + director;
        if (genres.equals("null")) this.genres = "Genres: N/A";
        else this.genres = "Genres: " + genres;
        if (stars.equals("null")) this.stars = "Stars: N/A";
        else this.stars = "Stars: " + stars;
    }

    public String getId() {
        return id;
    }
    public String getNameYear() {
        return nameYear;
    }

    public String getRating() {
        return rating;
    }

    public String getDirector() {
        return director;
    }

    public String getGenres() {
        return genres;
    }

    public String getStars() {
        return stars;
    }
}