package Querying;

import Indexing.DocumentProcessing.Term;
import Indexing.Index.CityIndexEntry;
import Indexing.Index.DocIndexEntery;
import Indexing.Index.IndexEntry;
import Indexing.Index.Indexer;
import Querying.Ranking.Ranker;
import Querying.Ranking.RankingParameters;
import Querying.Ranking.WeightedBM25Ranker;
import Querying.Semantics.SemanticEngine;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

class SearcherTest {

    private final String pathToPostingsFolder = "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\indexing";
    private final String pathToResultsOutputFolder = "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults";
    private final String pathToGloVeFilesFolder = "C:\\Users\\John\\Downloads\\infoRetrieval\\GloVe\\customVectors";


    Ranker ranker;
    Searcher searcher;
    Map<String, IndexEntry> mainDictionaryWithStemming;
    ArrayList<DocIndexEntery> docDictionaryWithStemming;
    Map<String, IndexEntry> mainDictionaryNoStemming;
    ArrayList<DocIndexEntery> docDictionaryNoStemming;
    Map<String , CityIndexEntry> cityDictionary;
    Set<String> languages;

    void initialize(boolean useStemming, int kNeighbors, HashSet<String> cityList, RankingParameters rankingParameters) throws IOException, ClassNotFoundException {
        loadDictionaries(useStemming, pathToPostingsFolder);
        int numDocsInCorpus = useStemming? docDictionaryWithStemming.size() : docDictionaryNoStemming.size();

        double averageDocLength = 0.0;
        for (DocIndexEntery doc: useStemming? docDictionaryWithStemming : docDictionaryNoStemming
                ) {
            averageDocLength += doc.getLength();
        }
        averageDocLength /= numDocsInCorpus;

        ranker = new WeightedBM25Ranker(rankingParameters, numDocsInCorpus, averageDocLength);

        searcher = new Searcher(useStemming? mainDictionaryWithStemming : mainDictionaryNoStemming, cityDictionary,
                useStemming? docDictionaryWithStemming : docDictionaryNoStemming, useStemming,
                pathToPostingsFolder + (useStemming? "/postingWithStemming/Postings" : "/postingWithOutStemming/Postings"),
                new SemanticEngine(pathToGloVeFilesFolder, kNeighbors), ranker, cityList);
    }

    @Test
    void EBM25Test() throws IOException, ClassNotFoundException, InterruptedException {
        boolean useStemming = true;
        boolean withSemantics = false;
        initialize(useStemming, 5, new HashSet<>(),
                new RankingParameters(1.2, 0.2, 1, 0.35, 0, 1.6, 0.75));

        List<QueryResult> qRes = new ArrayList<>();

        qRes.add(new QueryResult("351", convertFromSerialIDtoDocID( searcher.answerquery("Falkland petroleum exploration", withSemantics),useStemming)));
        qRes.add(new QueryResult("352" , convertFromSerialIDtoDocID(searcher.answerquery("British Chunnel impact", withSemantics),useStemming)));
        qRes.add(new QueryResult("358" ,convertFromSerialIDtoDocID( searcher.answerquery("blood-alcohol fatalities", withSemantics),useStemming)));
        qRes.add(new QueryResult("359" , convertFromSerialIDtoDocID( searcher.answerquery("mutual fund predictors ", withSemantics),useStemming)));
        qRes.add(new QueryResult("362" , convertFromSerialIDtoDocID(searcher.answerquery("human smuggling ", withSemantics),useStemming)));
        qRes.add(new QueryResult("367" , convertFromSerialIDtoDocID(searcher.answerquery("piracy ", withSemantics),useStemming)));
        qRes.add(new QueryResult("373" , convertFromSerialIDtoDocID (searcher.answerquery("encryption equipment export ", withSemantics),useStemming)));
        qRes.add(new QueryResult("374" , convertFromSerialIDtoDocID (searcher.answerquery("Nobel prize winners ", withSemantics),useStemming)));
        qRes.add(new QueryResult("377" , convertFromSerialIDtoDocID(searcher.answerquery("cigar smoking ", withSemantics),useStemming)));
        qRes.add(new QueryResult("380" , convertFromSerialIDtoDocID(searcher.answerquery("obesity medical treatment ", withSemantics),useStemming)));
        qRes.add(new QueryResult("384" , convertFromSerialIDtoDocID(searcher.answerquery("space station moon ", withSemantics),useStemming)));
        qRes.add(new QueryResult("385" , convertFromSerialIDtoDocID(searcher.answerquery("hybrid fuel cars ", withSemantics),useStemming)));
        qRes.add(new QueryResult("387" , convertFromSerialIDtoDocID(searcher.answerquery("radioactive waste ", withSemantics),useStemming)));
        qRes.add(new QueryResult("388" , convertFromSerialIDtoDocID(searcher.answerquery("organic soil enhancement ", withSemantics),useStemming)));
        qRes.add(new QueryResult("390" , convertFromSerialIDtoDocID(searcher.answerquery("orphan drugs ", withSemantics),useStemming)));
        Searcher.outputResults(qRes, pathToResultsOutputFolder);

        Runtime rt = Runtime.getRuntime();
//        rt.exec("cd C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\");
        String[] commands = {"C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\treceval.exe",
                "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\qrels",
                "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\results.txt"};
        Process proc = rt.exec(commands);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

        // read the output from the command
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        // read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }
    }

    @Test
    void statisticsForReport() throws IOException, ClassNotFoundException {
        boolean useStemming = true;
        boolean withSemantics = true;
        initialize(useStemming, 5, new HashSet<>(),
                new RankingParameters(1.2, 0.2, 1, 0.35, 0, 1.6, 0.75));

        List<QueryResult> qRes = new ArrayList<>();
        List<String> qTexts = new ArrayList<>();

        qRes.add(new QueryResult("351", convertFromSerialIDtoDocID( searcher.answerquery("Falkland petroleum exploration", withSemantics),useStemming)));
        qTexts.add("Falkland petroleum exploration");
        qRes.add(new QueryResult("352" , convertFromSerialIDtoDocID(searcher.answerquery("British Chunnel impact", withSemantics),useStemming)));
        qTexts.add("British Chunnel impact");
        qRes.add(new QueryResult("358" ,convertFromSerialIDtoDocID( searcher.answerquery("blood-alcohol fatalities", withSemantics),useStemming)));
        qTexts.add("blood-alcohol fatalities");
        qRes.add(new QueryResult("359" , convertFromSerialIDtoDocID( searcher.answerquery("mutual fund predictors ", withSemantics),useStemming)));
        qTexts.add("mutual fund predictors ");
        qRes.add(new QueryResult("362" , convertFromSerialIDtoDocID(searcher.answerquery("human smuggling ", withSemantics),useStemming)));
        qTexts.add("human smuggling ");
        qRes.add(new QueryResult("367" , convertFromSerialIDtoDocID(searcher.answerquery("piracy ", withSemantics),useStemming)));
        qTexts.add("piracy ");
        qRes.add(new QueryResult("373" , convertFromSerialIDtoDocID (searcher.answerquery("encryption equipment export ", withSemantics),useStemming)));
        qTexts.add("encryption equipment export ");
        qRes.add(new QueryResult("374" , convertFromSerialIDtoDocID (searcher.answerquery("Nobel prize winners ", withSemantics),useStemming)));
        qTexts.add("Nobel prize winners ");
        qRes.add(new QueryResult("377" , convertFromSerialIDtoDocID(searcher.answerquery("cigar smoking ", withSemantics),useStemming)));
        qTexts.add("cigar smoking ");
        qRes.add(new QueryResult("380" , convertFromSerialIDtoDocID(searcher.answerquery("obesity medical treatment ", withSemantics),useStemming)));
        qTexts.add("obesity medical treatment ");
        qRes.add(new QueryResult("384" , convertFromSerialIDtoDocID(searcher.answerquery("space station moon ", withSemantics),useStemming)));
        qTexts.add("space station moon ");
        qRes.add(new QueryResult("385" , convertFromSerialIDtoDocID(searcher.answerquery("hybrid fuel cars ", withSemantics),useStemming)));
        qTexts.add("hybrid fuel cars ");
        qRes.add(new QueryResult("387" , convertFromSerialIDtoDocID(searcher.answerquery("radioactive waste ", withSemantics),useStemming)));
        qTexts.add("radioactive waste ");
        qRes.add(new QueryResult("388" , convertFromSerialIDtoDocID(searcher.answerquery("organic soil enhancement ", withSemantics),useStemming)));
        qTexts.add("organic soil enhancement ");
        qRes.add(new QueryResult("390" , convertFromSerialIDtoDocID(searcher.answerquery("orphan drugs ", withSemantics),useStemming)));
        qTexts.add("orphan drugs ");



        PrintWriter cleaner = new PrintWriter("C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\statisticForReport"
                + (useStemming? "withStemming" : "") + (withSemantics? "withSemantics" : "") + ".txt");
        cleaner.close();

        int textsIndex = 0;
        PrintWriter csvOut = new PrintWriter(new FileOutputStream(
                new File("C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\statisticForReport"
                        + (useStemming? "withStemming" : "") + (withSemantics? "withSemantics" : "") + ".csv"),false));
        csvOut.println("qid,qtext,qPrecision,qRecall,pAt5,pAt15,pAt30,pAt50,Retrieved,Relevant,Rel_ret, MAP");
        for (QueryResult queryResult: qRes
             ) {
            List<QueryResult> queryInList = new ArrayList<>();
            queryInList.add(queryResult);
            Searcher.outputResults(queryInList, pathToResultsOutputFolder);

            Runtime rt = Runtime.getRuntime();
//        rt.exec("cd C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\");
            String[] commands = {"C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\treceval.exe",
                    "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\qrels",
                    "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\results.txt"};
            Process proc = rt.exec(commands);

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            // read the output from the command and write to file

            //                  qid,qtext,qPrecision,qRecall,pAt5,pAt15,pAt30,pAt50,Retrieved,Relevant,Rel_ret, map
            String[] csvLine = {",",",",",",",",",",",",",",",",",",",",",", ",", ""};
            PrintWriter out = new PrintWriter(new FileOutputStream(
                    new File("C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\statisticForReport"
                            + (useStemming? "withStemming" : "") + (withSemantics? "withSemantics" : "") + ".txt"),true));
            out.println("Actual Query ID = " + queryResult.getQueryNum());
            csvLine[0] = queryResult.getQueryNum()+",";
            csvLine[1] = qTexts.get(textsIndex);
            textsIndex++;
            System.out.println("Actual Query ID = " + queryResult.getQueryNum());
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                out.println(s);
                System.out.println(s);
                if(s.contains("At    5 docs:")){
                    s = s.replace("  At    5 docs:", "");
                    s = s.trim();
                    csvLine[5] =(s + ",");
                }
                if(s.contains("  At   15 docs:")){
                    s = s.replace("  At   15 docs:", "");
                    s = s.trim();
                    csvLine[6] = (s + ",");
                }
                if(s.contains("  At   30 docs:")){
                    s = s.replace("  At   30 docs:", "");
                    s = s.trim();
                    csvLine[7] = (s + ",");
                }
                if(s.contains("Retrieved:")){
                    s = s.replace("Retrieved:", "");
                    s = s.trim();
                    csvLine[9] = (s + ",");
                }
                if(s.contains("Relevant:")){
                    s = s.replace("Relevant:", "");
                    s = s.trim();
                    csvLine[10] = (s + ",");
                }
                if(s.contains("Rel_ret:")){
                    s = s.replace("Rel_ret:", "");
                    s = s.trim();
                    csvLine[11] = (s + ",");
                }
                if(s.contains("Average precision (non-interpolated) over all rel docs")){
                    s = stdInput.readLine();
                    s = s.trim();
                    csvLine[12] = (s);
                }
            }
            out.flush();
            out.close();
            csvOut.println(csvLine[0]+csvLine[1]+csvLine[2]+csvLine[3]+csvLine[4]+csvLine[5]+csvLine[6]+csvLine[7]+csvLine[8]+csvLine[9]+csvLine[10]+csvLine[11]+csvLine[12]);

            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        }

        //now the totals

        String[] csvLine = {",",",",",",",",",",",",",",",",",",",",",",",",",",",",""};
        csvOut.println(csvLine[0]+csvLine[1]+csvLine[2]+csvLine[3]+csvLine[4]+csvLine[5]+csvLine[6]+csvLine[7]+csvLine[8]+csvLine[9]+csvLine[10]+csvLine[11]+csvLine[12]);

        Searcher.outputResults(qRes, pathToResultsOutputFolder);

        Runtime rt = Runtime.getRuntime();
//        rt.exec("cd C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\");
        String[] commands = {"C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\treceval.exe",
                "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\qrels",
                "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults\\results.txt"};
        Process proc = rt.exec(commands);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

         //read the output from the command
        csvLine[0] = "totals"+",";
        csvLine[1] = "totals"+",";
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
            if(s.contains("At    5 docs:")){
                s = s.replace("  At    5 docs:", "");
                s = s.trim();
                csvLine[4] =(s + ",");
            }
            if(s.contains("  At   15 docs:")){
                s = s.replace("  At   15 docs:", "");
                s = s.trim();
                csvLine[5] = (s + ",");
            }
            if(s.contains("  At   30 docs:")){
                s = s.replace("  At   30 docs:", "");
                s = s.trim();
                csvLine[6] = (s + ",");
            }
            if(s.contains("Retrieved:")){
                s = s.replace("Retrieved:", "");
                s = s.trim();
                csvLine[8] = (s + ",");
            }
            if(s.contains("Relevant:")){
                s = s.replace("Relevant:", "");
                s = s.trim();
                csvLine[9] = (s + ",");
            }
            if(s.contains("Rel_ret:")){
                s = s.replace("Rel_ret:", "");
                s = s.trim();
                csvLine[10] = (s+ ",");
            }
            if(s.contains("Average precision (non-interpolated) over all rel docs")){
                s = stdInput.readLine();
                s = s.trim();
                csvLine[11] = (s);
                System.out.println(s);
            }
        }

        csvOut.println(csvLine[0]+csvLine[1]+csvLine[2]+csvLine[3]+csvLine[4]+csvLine[5]+csvLine[6]+csvLine[7]+csvLine[8]+csvLine[9]+csvLine[10]+csvLine[11]+csvLine[12]);

        csvOut.flush();
        csvOut.close();


    }

    private List<String> convertFromSerialIDtoDocID(List<String> docsSerialID ,boolean useStemming ){
        List<String> toReturn = new ArrayList<>();
        for (String st: docsSerialID) {
            if (useStemming) {
                toReturn.add(docDictionaryWithStemming.get(Integer.parseInt(st)).getDocID());
            }else {
                toReturn.add(docDictionaryNoStemming.get(Integer.parseInt(st)).getDocID());
            }
        }
        return toReturn;
    }


    public void loadDictionaries(boolean useStemming, String outputFolder) throws IOException, ClassNotFoundException, ClassCastException {
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
        }
        else{
            this.mainDictionaryNoStemming = (Map<String, IndexEntry>) inDictionary.readObject();
            this.docDictionaryNoStemming = (ArrayList) inDocDictionary.readObject();
        }
        this.cityDictionary = (Map<String , CityIndexEntry>) inCityDictionay.readObject();
        this.languages = (Set<String>) inLanguages.readObject();

        inDictionary.close();
        inDocDictionary.close();
        inCityDictionay.close();
        inLanguages.close();
    }
}