package xmlParsers;

public class Stars {

    private String starId;

    private String starName;

    private int birthYear;


    public Stars() {
        starId = null;
        starName = null;
        birthYear = -1;
    }

    public String getId(){
        return this.starId;
    }

    public String getName(){
        return this.starName;
    }

    public int getBirthYear(){
        return this.birthYear;
    }

    public void setId(String id){
        this.starId = id;
    }
    public void setName(String name){
        this.starName = name;
    }
    public void setBirthYear(int year){
        this.birthYear = year;
    }


}