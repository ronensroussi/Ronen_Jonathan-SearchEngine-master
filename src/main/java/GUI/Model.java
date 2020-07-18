package GUI;

import Indexing.DocumentProcessing.Document;
import Indexing.DocumentProcessing.TermDocument;
import Indexing.Index.CityIndexEntry;
import Indexing.Index.DocIndexEntery;
import Indexing.Index.IndexEntry;
import Indexing.Index.Indexer;
import Indexing.DocumentProcessing.Parse;
import Indexing.DocumentProcessing.ReadFile;
import Querying.*;
import Querying.Ranking.Ranker;
import Querying.Ranking.RankingParameters;
import Querying.Ranking.WeightedBM25Ranker;
import Querying.Semantics.SemanticEngine;
import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * MVC model.
 * holds program state and logic.
 */
public class Model {

    // GLOBAL PARAMETERS
    private static final int documentBufferSize = 3;
    private static final int termBufferSize = 3;

    private Controller controller;
    private Map<String, IndexEntry> mainDictionaryWithStemming;
    private List<DocIndexEntery> docDictionaryWithStemming;
    private Map<String, IndexEntry> mainDictionaryNoStemming;
    private List <DocIndexEntery> docDictionaryNoStemming;
    private Map<String , CityIndexEntry> cityDictionary;
    private Set<String> languages;
    private ExecutorService threadPool;
    private Thread.UncaughtExceptionHandler exceptionHandler;
    private boolean isExceptionThrownDuringGeneration = false;
    private Exception exceptionThrownDuringGeneration;
    private double avargeDocSize;
    private SemanticEngine semanticEngine;
    private RankingParameters rankingParameters;


    public Model(){
        try {
            semanticEngine=new SemanticEngine(System.getProperty("user.dir")+"\\resources\\Word2Vec",5);
            rankingParameters = new RankingParameters(1.2, 0.2, 1, 0.35, 0, 1.6, 0.75);
        } catch (IOException e) {
            e.printStackTrace();
        }
        exceptionHandler = new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread th, Throwable ex) {
                isExceptionThrownDuringGeneration = true;
                exceptionThrownDuringGeneration = (Exception) ex;
                threadPool.shutdownNow();
            }
        };


    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public Set<String> getLanguages(){
        return languages;
    }

    public Map<String, IndexEntry> getDictionary(boolean useStemming) {
        return useStemming ? mainDictionaryWithStemming : mainDictionaryNoStemming;
    }

    public void loadDictionary(boolean useStemming, String outputFolder) throws IOException, ClassNotFoundException, ClassCastException {
        ObjectInputStream inDictionary = new ObjectInputStream(new BufferedInputStream(new FileInputStream(outputFolder + '/' + (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.dictionarySaveName )));
        ObjectInputStream inDocDictionary = new ObjectInputStream(new BufferedInputStream(new FileInputStream(outputFolder + '/' +
                (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.docsDictionaryName )));
        ObjectInputStream inCityDictionay = new ObjectInputStream(new BufferedInputStream(new FileInputStream(outputFolder + '/' +
                (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.cityDictionaryName)));
        ObjectInputStream inLanguages = new ObjectInputStream(new BufferedInputStream(new FileInputStream(outputFolder + '/' +
                (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.languages)));

        if(useStemming){
            this.mainDictionaryWithStemming = (Map<String, IndexEntry>) inDictionary.readObject();
            this.docDictionaryWithStemming = (ArrayList)inDocDictionary.readObject();
            double DocLength = 0.0;
            for (DocIndexEntery doc: docDictionaryWithStemming) {
                DocLength += doc.getLength();
            }
            avargeDocSize=DocLength/docDictionaryWithStemming.size();
        }
        else{
            this.mainDictionaryNoStemming = (Map<String, IndexEntry>) inDictionary.readObject();
            this.docDictionaryNoStemming = (ArrayList) inDocDictionary.readObject();
            double DocLength = 0.0;
            for (DocIndexEntery doc: docDictionaryNoStemming) {
                DocLength += doc.getLength();
            }
            avargeDocSize=DocLength/docDictionaryNoStemming.size();
        }
        this.cityDictionary = (Map<String , CityIndexEntry>) inCityDictionay.readObject();
        this.languages = (Set<String>) inLanguages.readObject();

        inDictionary.close();
        inDocDictionary.close();
        inCityDictionay.close();
        inLanguages.close();
    }

    public void reset(String outputFolder) {
        mainDictionaryWithStemming = null;
        docDictionaryWithStemming = null;
        mainDictionaryNoStemming = null;
        docDictionaryNoStemming  = null;
        cityDictionary = null;
        languages = null;

        cleanOutputFiles(outputFolder);
    }


    /**
     * within the given directory, deletes everything within folders created by this program, and the folders themselves.
     * @param outputFolder - the folder where program output was written, and is now to be deleted.
     */
    private void cleanOutputFiles(String outputFolder) {
        File dir1 = new File(outputFolder + '/' + Indexer.withStemmingOutputFolderName);
        File dir2 = new File(outputFolder + '/' + Indexer.noStemmingOutputFolderName);
        if(dir1.exists()){
            for(File file : dir1.listFiles()){
                file.delete();
            }
            dir1.delete();
        }
        if(dir2.exists()){
            for(File file : dir2.listFiles()){
                file.delete();
            }
            dir2.delete();
        }
    }

    /**
     * generate a new index, including all sub indexes.
     * @param useStemming
     * @param corpusLocation
     * @param outputLocation
     * @param stopwordsLocation
     * @return - a String containing statistics about the generation process.
     * @throws Exception
     */
    public String generateIndex(boolean useStemming, String corpusLocation, String outputLocation, String stopwordsLocation) throws Exception {
        //setup
        setupToGenerateIndex();
        /*  Concurrent buffers:
        Thread safe. blocks if empty or full.
        Remember it is imperative that the user manually synchronize on the returned list when iterating over it */
        BlockingQueue<Document> documentBuffer = new ArrayBlockingQueue<Document>(documentBufferSize);
        BlockingQueue<TermDocument> termDocumentsBuffer = new ArrayBlockingQueue<>(termBufferSize);

        //  Worker Threads:
        //reading
        threadPool.submit(new Thread(new ReadFile(corpusLocation, documentBuffer)));
        HashSet<String> stopwords = Parse.getStopWords(stopwordsLocation);
        //parsing
        threadPool.submit(new Thread(new Parse(stopwords, documentBuffer, termDocumentsBuffer, useStemming)));
        //indexing
        Indexer indexer = new Indexer(outputLocation, termDocumentsBuffer,useStemming);
        threadPool.submit(new Thread(indexer));

        long time = System.currentTimeMillis();

        //wait for indexing to finish
        threadPool.shutdown();
        boolean timeoutReached = !(threadPool.awaitTermination(60, TimeUnit.MINUTES));

        time = (System.currentTimeMillis() - time)/1000;

        return handleNewIndexGeneration(indexer, useStemming, time, timeoutReached);
    }

    /**
     * Experimental!
     * similar to {@link #generateIndex(boolean, String, String, String)}, but separates indexing to a parsing and an indexing phase to avoid high memory use.
     * @param useStemming
     * @param corpusLocation
     * @param outputLocation
     * @param stopwordsLocation
     * @return
     * @throws Exception
     */
    public String generateIndexTwoPhase(final boolean useStemming, final String corpusLocation, final String outputLocation, String stopwordsLocation) throws Exception {
        //setup
        setupToGenerateIndex();
        /*  Concurrent buffers:
        Thread safe. blocks if empty or full.
        Remember it is imperative that the user manually synchronize on the returned list when iterating over it */
        BlockingQueue<Document> documentBuffer = new ArrayBlockingQueue<Document>(documentBufferSize);
        BlockingQueue<TermDocument> termDocumentsBuffer = new ArrayBlockingQueue<>(termBufferSize);

        long time = System.currentTimeMillis();

        //  Worker Threads:
        //reading
        threadPool.submit(new Thread(new ReadFile(corpusLocation, documentBuffer)));
        HashSet<String> stopwords = Parse.getStopWords(stopwordsLocation);
        //parsing
        threadPool.submit(new Thread(new Parse(stopwords, documentBuffer, termDocumentsBuffer, useStemming)));

        // saving termDocs to file
        Thread termDocsToFile = new Thread(() -> {
            final String filePath = outputLocation +"\\tmp_termDocuments";
            try {
                ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath, true )));
                Boolean done = false;
                while (!done) { //extract from buffer until poison element is encountered
                    TermDocument currDoc = null;
                    try {
                        currDoc = termDocumentsBuffer.take();
                        if (null == currDoc.getText()) done=true; //end of files (poison element)
                        else {
                            out.writeObject(currDoc);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        threadPool.submit(termDocsToFile);

        // wait until all docs are parsed and written to file
        threadPool.shutdown();
        threadPool.awaitTermination(60, TimeUnit.MINUTES);
        threadPool = null;
        //clear memory
        System.gc();

        // read TermDocuments and feed them to indexer
        threadPool = Executors.newFixedThreadPool(4);
        Thread fileToTermDocs = new Thread(() -> {
            final String filePath = outputLocation +"\\tmp_termDocuments";
            try {
                ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath )));
                TermDocument doc = (TermDocument) in.readObject();
                while (doc != null) { //insert to buffer until file end
                    try {
                        doc = (TermDocument) in.readObject();
                        doc.language = doc.language.intern();
                        termDocumentsBuffer.put(doc);
                    }catch(Exception e){
                        doc = null;
                    }
                }
                // poison element at end
                TermDocument poison = new TermDocument(-1,null, null);
                termDocumentsBuffer.put(poison);
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        threadPool.submit(fileToTermDocs);

        //indexing
        Indexer indexer = new Indexer(outputLocation, termDocumentsBuffer,useStemming);
        threadPool.submit(new Thread(indexer));

        //wait for indexing to finish
        threadPool.shutdown();
        boolean timeoutReached = !(threadPool.awaitTermination(60, TimeUnit.MINUTES));

        time = (System.currentTimeMillis() - time)/1000;

        return handleNewIndexGeneration(indexer, useStemming, time, timeoutReached);
    }

    /**
     * creates a new thread pool and sets all other fields to null.
     */
    private void setupToGenerateIndex(){
        threadPool = Executors.newFixedThreadPool(4);
        cityDictionary = null;
        languages = null;
        mainDictionaryWithStemming = null;
        docDictionaryWithStemming = null;
        mainDictionaryNoStemming = null;
        docDictionaryNoStemming  = null;
    }

    /**
     * checks if the index was generated successfully, if not, throws and exception.
     * takes the output of an index generation and saves it to local fields.
     * sets thread pool to null to free system resources.
     * @param indexer - contains the results of the index generation (dictionaries).
     * @param useStemming
     * @param executionTime - execution time.
     * @param timeoutReached - indicates if the thread pool cosed prematurely because of a timeout.
     * @return - a String containing statistics about the generation process.
     * @throws Exception - if an exception was thrown by one of the worker threads.
     */
    private String handleNewIndexGeneration(Indexer indexer, boolean useStemming, long executionTime, boolean timeoutReached) throws Exception {
        if(timeoutReached) throw new Exception("Timeout reached during execution");
        else if(this.isExceptionThrownDuringGeneration){
            this.threadPool = null;
            isExceptionThrownDuringGeneration = false;
            throw exceptionThrownDuringGeneration;
        }

        if(useStemming){
            this.mainDictionaryWithStemming = indexer.getMainMap();
            this.docDictionaryWithStemming = indexer.getDocsMap();
            double DocLength = 0.0;
            for (DocIndexEntery doc: docDictionaryWithStemming) {
                DocLength += doc.getLength();
            }
            avargeDocSize=DocLength/docDictionaryWithStemming.size();

        }
        else{
            this.mainDictionaryNoStemming = indexer.getMainMap();
            this.docDictionaryNoStemming = indexer.getDocsMap();
            double DocLength = 0.0;
            for (DocIndexEntery doc: docDictionaryNoStemming) {
                DocLength += doc.getLength();
            }
            avargeDocSize=DocLength/docDictionaryNoStemming.size();
        }
        this.cityDictionary = indexer.getCityMap();
        this.languages = indexer.getLanguages();
        this.threadPool = null;

        int numIndexedDocs = indexer.getNumIndexedDocs();
        int numUniqueTerms = useStemming ? mainDictionaryWithStemming.size() : mainDictionaryNoStemming.size();

        return "Number of indexed documents = " + numIndexedDocs + "\n" +
                "Number of unique terms = " + numUniqueTerms + "\n" +
                "Total time (seconds) = " + executionTime ;
    }

    /**
     * creat a set of all of the cities in the corpus
     * @return
     */
    public Set<String> getAllCities(){
        Set<String> allCites = new HashSet<>();
        for (String city : cityDictionary.keySet() ) {
            allCites.add(city);
        }

        return allCites;
    }

    /**
     * this method returns the average doc size
     * @return
     */
    public double getAvargeDocSize(){
        return avargeDocSize;
    }

    /**
     * this method answer a singel query and return a list with one Qeury result object
     * to answer a query this method uses a Searcher object and sends to is al the relvant data for answering the query
     * @param query - the query needed tp be answer
     * @param citiesFilter -
     *  @param citiesFilter - a set of cities that the answers will be filtered by
     * @param useSemantic - a boolean variable the indicates if semantic analysis need to be use
     * @param isUsedStemming - a boolean variable the indicates if we using a dictionary the was stemmed or not
     * @param pathToOutpotFolder - path of the locations of the postings file
     * @return
     */
    public List<QueryResult> aswerSingelQuery(String query , Set<String> citiesFilter , boolean useSemantic ,boolean isUsedStemming , String pathToOutpotFolder){
        Searcher searcher;
        List<QueryResult> results = new ArrayList<>();
        List<String> res ;
        Ranker ranker;
        if(isUsedStemming){
            ranker = new WeightedBM25Ranker(rankingParameters,docDictionaryWithStemming.size(),getAvargeDocSize());
            searcher= new Searcher(mainDictionaryWithStemming,cityDictionary,docDictionaryWithStemming,true,pathToOutpotFolder+"\\"+Indexer.withStemmingOutputFolderName+"\\Postings",semanticEngine,ranker,(HashSet<String>)citiesFilter);

        }
        else {
            ranker = new WeightedBM25Ranker(rankingParameters,docDictionaryNoStemming.size(),getAvargeDocSize());
            searcher= new Searcher(mainDictionaryNoStemming,cityDictionary,docDictionaryNoStemming,false,pathToOutpotFolder+"\\"+Indexer.noStemmingOutputFolderName+"\\Postings",semanticEngine,ranker,(HashSet<String>)citiesFilter);
        }
        res=searcher.answerquery(query,useSemantic);
        QueryResult result = new QueryResult("001",res);
        List<String> docsID = new ArrayList<>();
        List<String[]> entities =new ArrayList<>();
        List<float[]>  rankedEnt = new ArrayList<>();
        for (String doc : result.getRelevantDocs()) {
            String docID="";
            String [] ent ;
            float [] rank ;
            if(isUsedStemming){
                docID=docDictionaryWithStemming.get(Integer.parseInt(doc)).getDocID();
                ent=(docDictionaryWithStemming.get(Integer.parseInt(doc)).getEntities());
                rank=(docDictionaryWithStemming.get(Integer.parseInt(doc)).getRanking());
            }else {
                docID=docDictionaryNoStemming.get(Integer.parseInt(doc)).getDocID();
                ent=(docDictionaryNoStemming.get(Integer.parseInt(doc)).getEntities());
                rank=(docDictionaryNoStemming.get(Integer.parseInt(doc)).getRanking());
            }
            docsID.add(docID);
            entities.add(ent);
            rankedEnt.add(rank);
        }

        result.setEntities(entities);
        result.setRelevantDocs(docsID);
        result.setEntRanking(rankedEnt);
        results.add(result);
        return results;

    }

    /**
     * this method answer a set of Queries one by one .
     * and add and creating a Query result object for each one of the queries and return a list of Query result object.
     * to answer a query this method uses a Searcher object and sends to is al the relvant data for answering the query
     * @param pathToQureyFile- path to a file contains a set of queries
     * @param citiesFilter - a set of cities that the answers will be filtered by
     * @param useSemantic - a boolean variable the indicates if semantic analysis need to be use
     * @param isUsedStemming - a boolean variable the indicates if we using a dictionary the was stemmed or not
     * @param pathToOutpotFolder - path of the locations of the postings file
     * @return - a list of Query result objects  that represent the results of the queries from the pathToQueryFile
     */
    public List<QueryResult> answerMultipleQueries (String pathToQureyFile , Set<String> citiesFilter , boolean useSemantic ,boolean isUsedStemming,String pathToOutpotFolder){
        Searcher searcher;
        List<QueryResult> results = new ArrayList<>();
        List<String> res ;
        Ranker ranker;
        if(isUsedStemming){
            ranker = new WeightedBM25Ranker(rankingParameters,docDictionaryWithStemming.size(),getAvargeDocSize());
            searcher= new Searcher(mainDictionaryWithStemming,cityDictionary,docDictionaryWithStemming,true,pathToOutpotFolder+"\\"+Indexer.withStemmingOutputFolderName+"\\Postings",semanticEngine,ranker,(HashSet<String>)citiesFilter);

        }
        else {
            ranker = new WeightedBM25Ranker(rankingParameters,docDictionaryNoStemming.size(),getAvargeDocSize());
            searcher= new Searcher(mainDictionaryNoStemming,cityDictionary,docDictionaryNoStemming,false,pathToOutpotFolder+"\\"+Indexer.noStemmingOutputFolderName+"\\Postings",semanticEngine,ranker,(HashSet<String>)citiesFilter);
        }

        List<Pair<String,String>> allQuerys = parstQuerysFromQueryFile(pathToQureyFile);

        for (Pair<String,String> pair : allQuerys ) {
            QueryResult tmp = new QueryResult(pair.getKey(),searcher.answerquery(pair.getValue(),useSemantic));
            List<String> docsID = new ArrayList<>();
            List<String[]> entities =new ArrayList<>();
            List<float[]>  rankedEnt = new ArrayList<>();
            for (String doc : tmp.getRelevantDocs()) {
                String docID="";
                String [] ent ;
                float [] rank ;
                if(isUsedStemming){
                    docID=docDictionaryWithStemming.get(Integer.parseInt(doc)).getDocID();
                    ent=(docDictionaryWithStemming.get(Integer.parseInt(doc)).getEntities());
                    rank=(docDictionaryWithStemming.get(Integer.parseInt(doc)).getRanking());
                }else {
                    docID=docDictionaryNoStemming.get(Integer.parseInt(doc)).getDocID();
                    ent=(docDictionaryNoStemming.get(Integer.parseInt(doc)).getEntities());
                    rank=(docDictionaryNoStemming.get(Integer.parseInt(doc)).getRanking());
                }
                docsID.add(docID);
                entities.add(ent);
                rankedEnt.add(rank);
            }

            tmp.setEntities(entities);
            tmp.setRelevantDocs(docsID);
            tmp.setEntRanking(rankedEnt);
            results.add(tmp);
        }

        return results;
    }


    /**
     * this method  read the query file and parses it into a pair of <queryNum , Query>
     * and  adding it to a list
     * @param pathToQureyFile - the path to the queryy file needed to be parsed
     * @return - a list of pairs <queryNum , Query>
     */
    private List<Pair<String,String>> parstQuerysFromQueryFile(String pathToQureyFile) {
        File file = new File(pathToQureyFile);
        FileInputStream fi ;
        Elements elements=null;
        List<Pair<String,String>> parsResult= new ArrayList<>();
        try {
            fi = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fi, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line = null;
            line = br.readLine();
            while (line != null) {
                sb.append(line+"\n");
                line = br.readLine();
            }
            org.jsoup.nodes.Document doc = Jsoup.parse(sb.toString());
            elements = doc.select("top");

        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Element element : elements) {

            org.jsoup.nodes.Document titleDoc =Jsoup.parse(element.toString());
            //int startIDX = element.toString().indexOf("<num>",0) ;
            String QueryNum = element.toString().substring(element.toString().indexOf(":" ,0),element.toString().indexOf("\n" ,element.toString().indexOf(":" ,0)));
            QueryNum=QueryNum.replace(": ","").trim();
            String Query =titleDoc.select("title").text().trim();
            parsResult.add(new Pair<>(QueryNum,Query));

        }

        return parsResult;


    }

    /**
     * this method save the Query results into a File
     * @param resultOutputPath - the path for the file
     * @param results - the results that need to be saved
     */
    public void saveQueryResults(String resultOutputPath , List<QueryResult> results){

        try {
            Searcher.outputResults(results,resultOutputPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


}
