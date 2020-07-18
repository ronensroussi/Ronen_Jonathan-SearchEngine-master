package Indexing.Index;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data type containing the information pertaining to a single city in the city dictionary.
 */
public class CityIndexEntry implements Serializable {

    private String countryName;
    private String currency;
    private String population;
    private boolean isPartOfCorpus=false;
    private Map<Integer , int[]> docsMap;
    private int pointer;


    public CityIndexEntry(String countryName , String currency , String population , boolean isPartOfCorpus){
        this.countryName=countryName;
        this.currency=currency;
        this.population=population;
        this.isPartOfCorpus=isPartOfCorpus;
        docsMap=new HashMap<>();
    }

    public CityIndexEntry(String countryName , String currency , String population ){
        this.countryName=countryName;
        this.currency=currency;
        this.population=population;
        docsMap=new HashMap<>();
    }

    /**
     * this method add a new doc and positions array to the docsMap
     * and change the #isPartOfCorpus flag if it is a new city
     * @param docNum - the doc id that we want to add
     * @param positions - int array  of positions of this doc
     */
    public void addDocToMap(int docNum , int [] positions ){
        if(!isPartOfCorpus){
            isPartOfCorpus=true;
        }
        docsMap.put(docNum,positions);
    }

    /**
     * this  method adds a new position of a city in the text  to a specific doc in #docsMap
     * @param docNum- the doc id that we want to add a  value to it
     * @param index - the index of the city in the text
     * @param numLeft - number of appearances of the city that we didnt insert yet (helps to know in which index of the array to insert the data)
     */
    public void addDocToMap(int docNum , int index , int numLeft ){

        int [] arr =docsMap.get(docNum);
        int pointer = arr.length-numLeft;
        arr[pointer]=index;

    }

    public void setDocsMap(Map<Integer , int[]> map){
        this.docsMap=map;
    }

    public Map<Integer , int[]> getDocsMap (){
        return docsMap;
    }



    public String getCountryName() {
        return countryName;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPopulation() {
        return population;
    }
    public Boolean isInCorpus(){
        return isPartOfCorpus;
    }

    public int getPointer(){return pointer;}

    public void setPointer(int newPointer){
        pointer=newPointer;
    }
}
