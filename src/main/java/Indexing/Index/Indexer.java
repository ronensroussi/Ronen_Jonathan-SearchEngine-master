package Indexing.Index;

import Indexing.DocumentProcessing.TermDocument;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * takes fully parsed and stemmed documents and indexes them.
 * Calculates tf, tags terms to indicate their importance...
 * will index {@value # partialGroupSize} documents at a time.
 */
public class Indexer implements Runnable {

    public static String withStemmingOutputFolderName = "postingWithStemming";
    public static String noStemmingOutputFolderName = "postingWithOutStemming";
    public static String dictionarySaveName = "Index";
    public static String docsDictionaryName="DocsIndex";
    public static String cityDictionaryName="CityDictionary";
    public static String languages="Languages";


    private String pathToOutputFolder;
    private BlockingQueue<TermDocument> stemmedTermDocumentsBuffer;
    private AIndexMaker mainIndex;
    private AIndexMaker cityIndex;
    private int numIndexedDocs;
    private String finalPath="";

    //private boolean withSteming=false;

    public Indexer(String pathToOutputFolder, BlockingQueue<TermDocument> stemmedTermDocumentsBuffer,boolean withSteming) throws FileNotFoundException {
        numIndexedDocs = 0;
        this.pathToOutputFolder = pathToOutputFolder;
        this.stemmedTermDocumentsBuffer = stemmedTermDocumentsBuffer;
        if(withSteming) {
            finalPath=pathToOutputFolder +"\\postingWithStemming";
            new File(finalPath).mkdir();
        }
        else {
            finalPath=pathToOutputFolder +"\\postingWithOutStemming";
            new File(finalPath).mkdir();
            mainIndex = new MainIndexMaker(finalPath);
        }

        mainIndex = new MainIndexMaker(finalPath);
        cityIndex = new CityIndexMaker(finalPath);
}

    /**
     * takes fully parsed and stemmed documents and indexes them.
     * Calculates tf, tags terms to indicate their importance...
     * will index {@value # partialGroupSize} documents at a time.
     */
    private void index(){
        Boolean done = false;
        try {
            while (!done) {
                TermDocument document = stemmedTermDocumentsBuffer.take();
                mainIndex.addToIndex(document);
                cityIndex.addToIndex(document);
                if(document.getSerialID()==-1){
                    done=true;
                }
                else numIndexedDocs++;
            }
            dumpCityDictionaryToDisk();
             mergeMainIndex();
            dumpDictionaryToDisk();



        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        stemmedTermDocumentsBuffer=null;
    }


    public void run() {
        index();
    }

    /**
     * get the index dictionary from MainIndexMaker
     * @return - View.Main index
     */
    public Map<String , IndexEntry> getMainMap(){

        return ((MainIndexMaker)mainIndex).getMainDictionary();
    }


    public Map<String , TempIndexEntry> getTempMap(){

        return ((MainIndexMaker)mainIndex).getTempDictionary();
    }

    public int getNumIndexedDocs(){
        return numIndexedDocs;
    }

    /**
     * get the Document dictionary from MainIndexMaker
     * @return - Doc dictionary
     */
    public List<DocIndexEntery> getDocsMap()
    {
        return ((MainIndexMaker)mainIndex).getDocsDictionary();
    }

    public Set<String> getLanguages(){
        return ((MainIndexMaker)mainIndex).getLanguages();
    }

    public void mergeMainIndex(){
            ((MainIndexMaker) mainIndex).mergeIndex();
    }

    /**
     * this method write the main Dictionary , the docs Dictionary and the languages set to the disk
     */
    public void dumpDictionaryToDisk(){
        try {
            OutputStream mainIndexFileOutputStream = new FileOutputStream(finalPath+"\\"+dictionarySaveName);
            BufferedOutputStream mainIndexBufferedOutputStream = new BufferedOutputStream(mainIndexFileOutputStream);
            ObjectOutputStream mainIndexObjectOutputStream  = new ObjectOutputStream(mainIndexBufferedOutputStream);


            OutputStream docsIndexFileOutputStream = new FileOutputStream(finalPath+"\\"+docsDictionaryName);
            BufferedOutputStream docsIndexBufferedOutputStream = new BufferedOutputStream(docsIndexFileOutputStream);
            ObjectOutputStream docsIndexObjectOutstream  = new ObjectOutputStream(docsIndexBufferedOutputStream);




            OutputStream languagesFileOutputStream = new FileOutputStream(finalPath+"\\"+languages);
            BufferedOutputStream languagesBufferedOutputStream = new BufferedOutputStream(languagesFileOutputStream);
            ObjectOutputStream languagesObjectOutputStream  = new ObjectOutputStream(languagesBufferedOutputStream);


            docsIndexObjectOutstream.writeObject(getDocsMap());
            mainIndexObjectOutputStream.writeObject(getMainMap());


            languagesObjectOutputStream.writeObject(getLanguages());

            mainIndexFileOutputStream.flush();
            mainIndexBufferedOutputStream.flush();
            mainIndexObjectOutputStream.flush();

            mainIndexFileOutputStream.close();
            mainIndexBufferedOutputStream.close();
            mainIndexObjectOutputStream.close();


            docsIndexFileOutputStream.flush();
            docsIndexBufferedOutputStream.flush();
            docsIndexObjectOutstream.flush();

            docsIndexFileOutputStream.close();
            docsIndexBufferedOutputStream.close();
            docsIndexObjectOutstream.close();



            languagesFileOutputStream.flush();
            languagesBufferedOutputStream.flush();
            languagesObjectOutputStream.flush();
            languagesFileOutputStream.close();
            languagesBufferedOutputStream.close();
            languagesObjectOutputStream.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     * this method writs the cities dictionary to the disk
     */
    private void dumpCityDictionaryToDisk() {


        try {
            OutputStream cityIndexFileOutputStream = new FileOutputStream(finalPath+"\\"+cityDictionaryName);
            BufferedOutputStream cityIndexBufferedOutputStream = new BufferedOutputStream(cityIndexFileOutputStream);
            ObjectOutputStream cityIndexObjectOutstream  = null;

            cityIndexObjectOutstream = new ObjectOutputStream(cityIndexBufferedOutputStream);


            cityIndexObjectOutstream.writeObject(getCityMap());

            cityIndexFileOutputStream.flush();
            cityIndexBufferedOutputStream.flush();
            cityIndexObjectOutstream.flush();
            cityIndexFileOutputStream.close();
            cityIndexBufferedOutputStream.close();
            cityIndexObjectOutstream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void pritDictionaryToFile(){
        try {
            OutputStream mainIndexFileOutputStream = new FileOutputStream(finalPath+"\\dictionaryFile.txt");
            OutputStreamWriter outputStream= new OutputStreamWriter(mainIndexFileOutputStream);
            Map<String , IndexEntry> dictionaryToPrint = getMainMap();
            outputStream.write("term,df,totalTF\n");
            for (String term : dictionaryToPrint.keySet() )  {
                outputStream.write(term+","+dictionaryToPrint.get(term).getDf()+','+dictionaryToPrint.get(term).getTotalTF()+"\n");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Map<String , CityIndexEntry> getCityMap(){
        return ((CityIndexMaker)cityIndex).getCityDictionary();
    }




}
