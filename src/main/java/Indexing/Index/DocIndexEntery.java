package Indexing.Index;

import org.omg.CORBA.PRIVATE_MEMBER;

import java.io.Serializable;

/**
 * Data type containing the information pertaining to a single Document in the document dictionary.
 */
public class DocIndexEntery implements Serializable {

    private String docID;
    private int numOfUniqueWords;
    private int maxTF;
    private String city;
    private String language;
    private int length;
    private String [] entities;
    private float[] ranking;
    private int date;



    public DocIndexEntery(String  docID, int numOfUniqueWords, int maxTF , String city, String language , int length , int date){

        this.docID=docID;
        this.numOfUniqueWords=numOfUniqueWords;
        this.maxTF=maxTF;
        this.city=city;
        this.language=language;
        this.length=length;
        this.date=date;
    }


    public String[] getEntities() {
        return entities;
    }

    public float[] getRanking() {
        return ranking;
    }

    public String getDocID() {
        return docID;
    }

    public int getNumOfUniqueWords() {
        return numOfUniqueWords;
    }

    public int getMaxTF() {
        return maxTF;
    }

    public String getCity() {
        return city;
    }

    public String getLanguage() {
        return language;
    }

    public int getLength(){
        return length;
    }

    public void setDocID(String docID) {
        this.docID = docID;
    }

    public void setNumOfUniqueWords(int numOfUniqueWords) {
        this.numOfUniqueWords = numOfUniqueWords;
    }

    public void setMaxTF(int maxTF) {
        this.maxTF = maxTF;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setEntities(String[] entities) {
        this.entities = entities;
    }

    public void setRanking(float[] ranking) {
        this.ranking = ranking;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }



}
