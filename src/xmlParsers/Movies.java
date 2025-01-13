package xmlParsers;

public class Movies {

    private String id;
    private String XMLId;
    private String title;

    private int year;

    private String director;

    private int numDirectors;


    public Movies() {
        this.id = null;
        this.XMLId = null;
        this.title = null;
        this.year = 0;
        this.director = null;
        this.numDirectors = 0;
    }

    public String getId(){
        return this.id;
    }
    public String getXMLId() { return this.XMLId; }
    public String getTitle(){
        return this.title;
    }

    public int getYear(){
        return this.year;
    }
    public String getDirector(){
        return this.director;
    }

    public void setId(String id){
        this.id = id;
    }
    public void setXMLId(String XMLId){
        this.XMLId = XMLId;
    }
    public void setTitle(String title){
        this.title = title;
    }
    public void setYear(int year){
        this.year = year;
    }

    public void setDirector(String director){
        this.director = director;
    }

    public int getNumDirectors(){
        return this.numDirectors;
    }
    public void addDirectorCount(){
        this.numDirectors++;
    }


}