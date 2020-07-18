package Indexing.DocumentProcessing;

import com.sun.scenario.effect.impl.prism.PrImage;

/**
 * this class is a data type that represents a document.
 */
public class Document {
    private  String docId;
    private String title = "";
    private String text = "";
    private  String date = "";
    private String city="";
    private String language ="";


    public Document(/*int serialID*/){
        //this.serialID = serialID;
    }

    public Document(String docId, String title, String text, String date ) {
        this.docId = docId;
        this.title = title;
        this.text = text;
        this.date = date;

    }

    public Document(String docId, String title, String text, String date , String city , String language) {
        this.docId = docId;
        this.title = title;
        this.text = text;
        this.date = date;
        this.city=city;
        this.language=language;
    }

    public void setTitle(String header) {
        this.title = header;
    }
    public String[] getAllParsableFields(){
        String[] fields = new String[3];
        fields[0] = title;
        fields[1] = text;
        fields[2] = city;
        return fields;
    }


    public void setText(String text) {
        this.text = text;
    }
    public void setDate(String date){
        this.date = date;
    }
    public void setCity(String city){this.city=city;}
    public void setLanguage(String language){this.language=language;}

    public void setDocId(String id){
        docId=id;
    }

    public String getDocId() {
        return docId;
    }

    public String getHeader() {
        return title;
    }

    public String getText() {
        return text;
    }
    public String getCity(){return city;}

    public String getDate(){
        return date;
    }
    public String getLanguage(){return language;}


    @Override
    public String toString(){
        return "docID : " + docId+"\n" + "title : " + title+"\n" +"date : "+date+"\n" + "text : " + text + "city : "+ city;
    }
}

