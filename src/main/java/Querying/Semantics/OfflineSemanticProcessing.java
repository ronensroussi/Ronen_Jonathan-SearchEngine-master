package Querying.Semantics;

import Indexing.DocumentProcessing.*;
import Indexing.Index.CityIndexEntry;
import Indexing.Index.IndexEntry;
import Indexing.Index.Indexer;
import de.jungblut.glove.impl.GloveBinaryWriter;
import de.jungblut.glove.impl.GloveTextReader;
import de.jungblut.glove.util.StringVectorPair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Stream;

/**
 * contains static methods for offline processing that is done to facilitate semantic analysis.
 */
public class OfflineSemanticProcessing {

    private static String textFilePath = "C:\\Users\\John\\Downloads\\infoRetrieval\\GloVe\\customVectors\\customVectors.txt";
    private static String pathToGloveFilesFolder = "C:\\Users\\John\\Downloads\\infoRetrieval\\GloVe\\customVectors";
    private static String pathToCorpus = "C:\\Users\\John\\Downloads\\infoRetrieval\\corpus";
    private static String pathToOutputParsedWordVectors = "C:\\Users\\John\\Downloads\\infoRetrieval\\GloVe\\corpus_as_word_vectors.txt";
    private static String pathToStopwords = "C:/Users/John/Google Drive/Documents/1Uni/Semester E/information retrieval 37214406/Assignements/Ass1/stop_words.txt";
    private static String pathToDictionary = "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\indexing\\postingWithOutStemming\\Index";

    public static void main(String[] args) {
        try {
            textGloVeToBinaryGloVe();
//            corpusToParsedWordVectors(true);
//            corpusToParsedWordVector(false);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    /**
     * converts parsed trained GloVe vectors to a binary file for faster real-time accessing.
     * @throws IOException
     */
    private static void textGloVeToBinaryGloVe() throws IOException {
        Scanner sc = new Scanner(System.in);
        String input;
        System.out.println("Enter textFilePath, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) textFilePath = input;
        System.out.println("Enter pathToGloveFilesFolder, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) pathToGloveFilesFolder = input;

        System.out.println("Running");

        GloveTextReader reader = new GloveTextReader();
        Stream<StringVectorPair> stream = reader.stream(Paths.get(textFilePath));
        GloveBinaryWriter writer = new GloveBinaryWriter();
        writer.writeStream(stream, Paths.get(pathToGloveFilesFolder));
    }


    /**
     * parses the corpus. Creates a vector of words (terms) out of each field of each document.
     * Outputs all the vectors as lines in a single file.
     */
    private static void corpusToParsedWordVectors(boolean includeTitles) throws IOException, ClassNotFoundException, InterruptedException {
        //get paths
        Scanner sc = new Scanner(System.in);
        String input;
        System.out.println("Enter pathToCorpus, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) pathToCorpus = input;
        System.out.println("Enter pathToOutputParsedWordVectors, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) pathToOutputParsedWordVectors = input;
        System.out.println("Enter pathToStopwords, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) pathToStopwords = input;
        System.out.println("Enter pathToDictionary, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) pathToDictionary = input;
        boolean isStemming = false;
        input = "";
        while(!(input.equals("Y") || input.equals("N"))) {
            System.out.println("is dictionary with stemming? (Y/N)");
            input = sc.nextLine();
            if(input.equals("Y")) isStemming = true;
            else if(input.equals("N")) isStemming = false;
        }

        System.out.println("Running");

        //load dictionary

        ObjectInputStream inDictionary = new ObjectInputStream(new BufferedInputStream(new FileInputStream(pathToDictionary)));
        Map<String, IndexEntry> dictionary = (Map<String, IndexEntry>) inDictionary.readObject();

        //start parsing

        ArrayBlockingQueue<Document> docs = new ArrayBlockingQueue<Document>(10);
        ArrayBlockingQueue<TermDocument> termDocs = new ArrayBlockingQueue<TermDocument>(10);
        Parse p = new Parse(Parse.getStopWords(pathToStopwords),
                docs, termDocs, isStemming);
        Parse.debug = false;
        Thread parser1 = new Thread(p);

        ReadFile rf = new ReadFile(pathToCorpus, docs);
        Thread reader = new Thread(rf);

        reader.start();
        parser1.start();

        //read parsed documents and append them to output file
        int docCounter = 1;
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(pathToOutputParsedWordVectors), StandardCharsets.UTF_8), true);
        TermDocument currDoc = termDocs.take();
        while(currDoc.getText() != null){
            if(!currDoc.getTitle().isEmpty() && includeTitles) printWriter.println(termListToString(filterTerms(currDoc.getTitle(), dictionary)));
            if(!currDoc.getText().isEmpty()) printWriter.println(termListToString(filterTerms(currDoc.getText(), dictionary)));
            if(docCounter%10000 == 0) System.out.println("Processed " + docCounter + " docs");
            currDoc = termDocs.take();
            docCounter++;
        }
        System.out.println("Processed " + docCounter + " docs");
        System.out.println("done");
    }


    /**
     * parses the corpus. Creates a vector of words (terms) out of all the documents.
     * Outputs all the vectors as lines in a single file.
     */
    private static void corpusToParsedWordVector(boolean includeTitles) throws IOException, ClassNotFoundException, InterruptedException {
        //get paths
        Scanner sc = new Scanner(System.in);
        String input;
        System.out.println("Enter pathToCorpus, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) pathToCorpus = input;
        System.out.println("Enter pathToOutputParsedWordVectors, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) pathToOutputParsedWordVectors = input;
        System.out.println("Enter pathToStopwords, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) pathToStopwords = input;
        System.out.println("Enter pathToDictionary, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) pathToDictionary = input;
        boolean isStemming = false;
        input = "";
        while(!(input.equals("Y") || input.equals("N"))) {
            System.out.println("is dictionary with stemming? (Y/N)");
            input = sc.nextLine();
            if(input.equals("Y")) isStemming = true;
            else if(input.equals("N")) isStemming = false;
        }

        System.out.println("Running");

        //load dictionary

        ObjectInputStream inDictionary = new ObjectInputStream(new BufferedInputStream(new FileInputStream(pathToDictionary)));
        Map<String, IndexEntry> dictionary = (Map<String, IndexEntry>) inDictionary.readObject();

        //start parsing

        ArrayBlockingQueue<Document> docs = new ArrayBlockingQueue<Document>(10);
        ArrayBlockingQueue<TermDocument> termDocs = new ArrayBlockingQueue<TermDocument>(10);
        Parse p = new Parse(Parse.getStopWords(pathToStopwords),
                docs, termDocs, isStemming);
        Parse.debug = false;
        Thread parser1 = new Thread(p);

        ReadFile rf = new ReadFile(pathToCorpus, docs);
        Thread reader = new Thread(rf);

        reader.start();
        parser1.start();

        //read parsed documents and append them to output file
        int docCounter = 1;
        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(pathToOutputParsedWordVectors), StandardCharsets.UTF_8), true);
        TermDocument currDoc = termDocs.take();
        while(currDoc.getText() != null){
            if(!currDoc.getTitle().isEmpty() && includeTitles) printWriter.print(termListToString(filterTerms(currDoc.getTitle(), dictionary)));
            if(!currDoc.getText().isEmpty()) printWriter.print(termListToString(filterTerms(currDoc.getText(), dictionary)));
            if(docCounter%10000 == 0) System.out.println("Processed " + docCounter + " docs");
            currDoc = termDocs.take();
            docCounter++;
        }
        System.out.println("Processed " + docCounter + " docs");
        System.out.println("done");
    }

    /**
     * converts the list of terms to one long string, where terms are separated by " ".
     * @param terms list of terms to turn into a string
     * @return a string containing all the terms, separated by " ".
     */
    private static String termListToString(List<Term> terms){
        StringBuilder sb = new StringBuilder();
        sb.append(' ');
        for (Term t: terms
             ) {
            sb.append(t.toString());
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * filters the terms through the dictionary (filterDictionary). If a term doesn't exist in the dictionary, it will not appear in the returned list.
     * @param terms list of terms to filter.
     * @param filterDictionary dictionary to filter through.
     */
    private static List<Term> filterTerms(List<Term> terms, Map<String, IndexEntry> filterDictionary){
        ArrayList<Term> res = new ArrayList<>(terms.size()/2);
        for (Term t: terms
             ) {
            if(filterDictionary.containsKey(t.toString())) res.add(t);
        }
        return res;
    }


}
