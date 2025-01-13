import java.util.HashMap;



public class PreviousMovieListUrl {
    private String page;
    private HashMap<String, String> params;
    private int size;
    public PreviousMovieListUrl(String page, HashMap<String, String> params) {
        this.page = page;
        this.params = params;
        this.size = params.size();
    }

    public void clearEverything() {
        this.page = "";
        this.params.clear();
        this.size = 0;
    }

    public String getPage() {
        return this.page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public HashMap<String, String> getParams() {
        return this.params;
    }

    public void setParam(String key, String value) {
        this.params.put(key, value);
        this.size++;
    }

    public int getSize() {
        return this.size;
    }

    public String buildRelativeUrl() {
        // get the page
        StringBuilder relativeUrl = new StringBuilder();
        relativeUrl.append(this.getPage());
        // add all parameters to the url
        if (this.getSize() != 0) {
            relativeUrl.append("?");
            this.getParams().forEach((key, value) -> {
                relativeUrl.append(key).append("=").append(value).append("&");
            });
            // remove the unnecessary trailing ampersand (&)
            relativeUrl.deleteCharAt(relativeUrl.length() - 1);
        }
        return relativeUrl.toString();
    }
}
