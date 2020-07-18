package Indexing.Index;

import Indexing.DocumentProcessing.Document;
import Indexing.DocumentProcessing.TermDocument;
import Indexing.DocumentProcessing.Parse;
import Indexing.DocumentProcessing.ReadFile;
import Indexing.Index.IO.IntToIntArrayMapInputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class CityIndexTest {

    private static final int documentBufferSize = 3;
    private static final int termBufferSize = 3;
    private static final int stemmedTermBufferSize = 3;


    private static final String pathToDocumentsFolder = "C:\\Users\\ronen\\Desktop\\FB396001";
    private static final String pathToStopWordRONEN ="C:\\Users\\ronen\\Desktop\\stopWords.txt";

    @Test
    public void testJASON()
    {
        JSONParser jp =  new JSONParser();
        Map<String, CityIndexEntry> cityMap =new HashMap<>();

        try {
            Object obj = jp.parse(new FileReader("C:\\Users\\ronen\\Desktop\\jason.txt"));
            JSONArray jasonArray = (JSONArray)obj;
            for (Object jo : jasonArray ) {
                JSONObject j = (JSONObject)jo;
                String capital = j.get("capital").toString();
                String pop = j.get("population").toString();
                String country = (String)j.get("name");
                JSONArray ja=  (JSONArray)j.get("currencies");
                String currancy =(String)((JSONObject)ja.get(0)).get("code");

                CityIndexEntry cityIndexEntry = new CityIndexEntry(country,currancy,pop);
                if(capital!=null && !capital.equals("")){
                    cityMap.put(capital.split(" ")[0].toUpperCase(),cityIndexEntry);
                }




                FileOutputStream fileOutputStream = new FileOutputStream("resources\\rec");
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(cityMap);

                fileOutputStream.flush();
                fileOutputStream.close();

                objectOutputStream.flush();
                objectOutputStream.close();





            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }


    }

    @Test
    public void readCitiesDictionary(){
        Map<String, CityIndexEntry> cityMap = null;

        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("resources\\rec");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
             cityMap= (Map<String, CityIndexEntry>) objectInputStream.readObject();
             
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        for (String string : cityMap.keySet() ) {
            System.out.println(string+","+cityMap.get(string).getCountryName());
        }

    }

    @Test
    public void testCityIndex() throws InterruptedException, FileNotFoundException {

        BlockingQueue<Document> documentBuffer = new ArrayBlockingQueue<Document>(documentBufferSize);
        BlockingQueue<TermDocument> termDocumentsBuffer = new ArrayBlockingQueue<>(termBufferSize);



        //  Worker Threads:

        Thread tReader = new Thread(new ReadFile(pathToDocumentsFolder, documentBuffer));

        HashSet<String> stopwords = Parse.getStopWords(pathToStopWordRONEN);
        Thread tParser = new Thread(new Parse(stopwords, documentBuffer, termDocumentsBuffer, true));
        Indexer indexer =new Indexer("C:\\Users\\ronen\\Desktop\\test",termDocumentsBuffer,true);
        Thread tIndexer = new Thread(indexer);

        long start=System.currentTimeMillis();


        tReader.start();

        tParser.start();

        tIndexer.start();
        tIndexer.join();



    }

    @Test
    public void testForReport(){
        try {
            IntToIntArrayMapInputStream intToIntArrayMapInputStream = new IntToIntArrayMapInputStream("C:\\Users\\ronen\\Desktop\\test\\postingWithStemming\\CitiesPosting");


            ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream( new FileInputStream("C:\\Users\\ronen\\Desktop\\test\\postingWithStemming\\CityDictionary")));

            Map<String , CityIndexEntry> cityDictionary =(Map<String , CityIndexEntry>) objectInputStream.readObject();
            ObjectInputStream docsObjectStram = new ObjectInputStream(new BufferedInputStream( new FileInputStream("C:\\Users\\ronen\\Desktop\\test\\postingWithStemming\\DocsIndex")));
            List<DocIndexEntery> docList = (List<DocIndexEntery>) docsObjectStram.readObject();
            docsObjectStram.close();
            int max=0;
            int [] maxPositions=null;
            String city="";
            int docId=0;

            int numOfciteis=0;
            int numOfCapital=0;

            for (String key : cityDictionary.keySet()) {
                CityIndexEntry cityIndexEntry= cityDictionary.get(key);
                numOfciteis++;

                if (cityIndexEntry.getCountryName()!=null){
                    numOfCapital++;
                }
                int pointer = cityIndexEntry.getPointer();
                Map<Integer , int []> docsMap = intToIntArrayMapInputStream.readIntegerArraysMap(pointer);
                cityIndexEntry.setDocsMap(docsMap);
                for (Integer id : docsMap.keySet()) {
                    int [] positions = docsMap.get(id);
                    if(positions.length>max){
                        max=positions.length;
                        maxPositions=positions;
                        city=key;
                        docId=id;

                    }
                }
//                System.out.println(key+"->"+cityIndexEntry.getDocsMap().size()+"\n");
            }
            intToIntArrayMapInputStream.close();
            objectInputStream.close();

            System.out.println("NUMBER OF CITIES : "+ numOfciteis);
            System.out.println("NUMBER OF NON CAPITAL : "+(numOfciteis-numOfCapital));
            System.out.println("DOC ID : "+ docList.get(docId).getDocID()+"\nCITY : "+city+"\nNUM OF APPEARANCES :  "+max);
            System.out.print("POSITIONS : [");
            for (int i = 0; i <maxPositions.length ; i++) {
                System.out.print(maxPositions[i]+" ");
            }
            System.out.println("]");







        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }

}
