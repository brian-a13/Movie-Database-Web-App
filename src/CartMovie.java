public class CartMovie {
    private String movieId;
    private String movieTitle;
    private double rating;
    private int price;
    private int quantity;
    private int total;

    public CartMovie(String movieId, String movieTitle, double rating) {
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.price = (int) ((rating*10)/2);
        this.quantity = 1;
        this.total = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartMovie otherMovie = (CartMovie) o;
        return movieId.equals(otherMovie.movieId);
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotal() {return total;}
}