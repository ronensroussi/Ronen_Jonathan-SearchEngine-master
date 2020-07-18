package Querying.Ranking;

import Indexing.Index.CityIndexEntry;
import Indexing.Index.DocIndexEntery;
import Indexing.Index.IndexEntry;
import Indexing.Index.Indexer;
import Querying.QueryResult;
import Querying.Searcher;
import Querying.Semantics.SemanticEngine;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * contains a static function for optimizing the ranking parameters, using a hill climbing algorithm.
 */
public class HillClimbingOptimizer {

    private static String pathToPostingsFolder = "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\indexing";
    private static String pathToResultsOutputFolder = "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\queryResults";
    private static String pathToGloVeFilesFolder = "C:\\Users\\John\\Downloads\\infoRetrieval\\GloVe\\customVectors";


    static Ranker ranker;
    static Searcher searcher;
    static Map<String, IndexEntry> mainDictionaryWithStemming;
    static ArrayList<DocIndexEntery> docDictionaryWithStemming;
    static Map<String, IndexEntry> mainDictionaryNoStemming;
    static ArrayList<DocIndexEntery> docDictionaryNoStemming;
    static Map<String , CityIndexEntry> cityDictionary;
    static Set<String> languages;

    public static void main(String[] args){
        try {
            hillClimbing();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * initializes fields in preparation of optimization
     * @param useStemming
     * @param kNeighbors
     * @param cityList
     * @param rankingParameters
     * @throws IOException
     * @throws ClassNotFoundException
     */
    static void initialize(boolean useStemming, int kNeighbors, HashSet<String> cityList, RankingParameters rankingParameters) throws IOException, ClassNotFoundException {
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


    private static  List<String> convertFromSerialIDtoDocID(List<String> docsSerialID){
        List<String> toReturn = new ArrayList<>();
        for (String st: docsSerialID) {
            toReturn.add(docDictionaryWithStemming.get(Integer.parseInt(st)).getDocID());
        }
        return toReturn;
    }

    /**
     * starts the hill climbing optimization
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    static void hillClimbing() throws IOException, ClassNotFoundException, InterruptedException {
        Scanner sc = new Scanner(System.in);
        String input;
        System.out.println("Enter pathToPostingsFolder, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) pathToPostingsFolder = input;
        System.out.println("Enter pathToResultsOutputFolder, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) pathToResultsOutputFolder = input;
        System.out.println("Enter pathToGloVeFilesFolder, or press enter to use default path (not recommended)");
        input = sc.nextLine();
        if(!input.isEmpty()) pathToGloVeFilesFolder = input;

        boolean saveResults = true;
        Calendar cal = Calendar.getInstance();
        Date date=cal.getTime();
        DateFormat dateFormat = new SimpleDateFormat("YY_MM_DD_HH_mm");
        String formattedDate=dateFormat.format(date);
        String outputPath = "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\paramOptimization " + formattedDate + ".txt";

        double[] inputVector = {1.3, 0.1, 1, 0.3, 0, 1.5, 0.6};
//        double[] inputVector = {1.2, 0.2, 1, 0.35, 0, 1.6, 0.75};
        int numParameters = inputVector.length;
        double[] paramVector = Arrays.copyOf(inputVector, numParameters);
        initialize(true, 5, new HashSet<>(),
                new RankingParameters(paramVector[0], paramVector[1], paramVector[2], paramVector[3], paramVector[4], paramVector[5], paramVector[6]));
        boolean useSemantics = true;
        int numIterations = 5;
        double initialStepSize = 0.1;
        int maxNoGrowthSteps = 2;
        double stepSize = initialStepSize;
        Random rnd = new Random();

        double originalFitness = BM25Run(useSemantics);
        System.out.println("starting hill climbing with " + numIterations + " iterations, with initial step size of " + initialStepSize);
        System.out.println("starting fitness is: " + originalFitness + "\n");
        initialStepSize /= 0.75;
        for (int i = 0; i < numIterations; i++) {
            System.out.println("starting iteration " + i);
            stepSize = initialStepSize*0.75;
            System.out.println("step size is: " + stepSize);
            boolean[] paramAlreadyOptimized = new boolean[numParameters];
            //for each optimization parameter
            for (int j = 0; j < numParameters ; j++) {
                //choose a random parameter to optimize
                int paramIndex = rnd.nextInt(7);
                while(paramAlreadyOptimized[paramIndex]) paramIndex = rnd.nextInt(7);
                paramAlreadyOptimized[paramIndex] = true;

                System.out.println("    optimizing param " + paramIndex);
                // at most, one of the phases should end with improved values (according to the gradient of the fitness function).

                double newFitness;
                double currFitness = originalFitness;
                double[] originalParamVector = Arrays.copyOf(paramVector, numParameters);
                //up phase
                newFitness = currFitness;
                //while fitness doesn't decrease, increase this parameter
                for (int k = 0; newFitness >= currFitness && k < maxNoGrowthSteps  ; k++) {
                    currFitness = newFitness;
                    paramVector[paramIndex] = paramVector[paramIndex]+stepSize;
                    ranker.setRankingParameters(new RankingParameters(paramVector[0], paramVector[1], paramVector[2], paramVector[3], paramVector[4], paramVector[5], paramVector[6]));
                    newFitness = BM25Run(useSemantics);
                    if(newFitness > currFitness) k=0; // if there was growth, reset counter of steps taken without growth
                }
                //revert the step that caused fitness to decrease or stagnate
                paramVector[paramIndex] = paramVector[paramIndex]-stepSize;
                // currFitness is still set to the last good value (before fitness decreased).

                //reset for down phase
                //revert to original vector if no improvement was registered
                paramVector = (currFitness <= originalFitness ? Arrays.copyOf(originalParamVector , numParameters) : paramVector);
                //set original to self if no improvement, or to the new vector, if it resulted in an improvement.
                originalParamVector = (currFitness <= originalFitness ? originalParamVector : Arrays.copyOf(paramVector , numParameters));
                //set original to self if no improvement, or to the new fitness, if it was an improvement.
                originalFitness = Math.max(originalFitness, currFitness);

                //down phase
                newFitness = currFitness;
                //while fitness doesn't decrease, decrease this parameter
                for (int k = 0; newFitness >= currFitness && k < maxNoGrowthSteps  ; k++) {
                    currFitness = newFitness;
                    paramVector[paramIndex] = paramVector[paramIndex]-stepSize;
                    ranker.setRankingParameters(new RankingParameters(paramVector[0], paramVector[1], paramVector[2], paramVector[3], paramVector[4], paramVector[5], paramVector[6]));
                    newFitness = BM25Run(useSemantics);
                    if(newFitness > currFitness) k=0; // if there was growth, reset counter of steps taken without growth
                }
                //revert the step that caused fitness to decrease or stagnate
                paramVector[paramIndex] = paramVector[paramIndex]+stepSize;
                //revert to original vector if no improvement was registered
                paramVector = (currFitness <= originalFitness ? Arrays.copyOf(originalParamVector , numParameters) : paramVector);
                //set original to self if no improvement, or to the new fitness, if it was an improvement.
                originalFitness = Math.max(originalFitness, currFitness);
            }
            System.out.println("    finished iteration " + i);
            System.out.print("    current vector: [");
            for (int k = 0; k < numParameters ; k++) {
                System.out.print(paramVector[k] + ((k < numParameters -1) ? ", " : "]\r\n"));
            }
            System.out.println("current fitness: " + originalFitness);
        }

        StringBuilder result = new StringBuilder();
        result.append("Finished " + numIterations + " iterations, initial step size: " + initialStepSize + ", final step size: " + stepSize + "\r\n");
        result.append("original vector: [");
        for (int i = 0; i < paramVector.length ; i++) {
            result.append(inputVector[i] + ((i < numParameters -1) ? ", " : "]\r\n"));
        }
        result.append("Fittest vector : [");
        for (int i = 0; i < paramVector.length ; i++) {
            result.append(paramVector[i] + ((i < numParameters -1) ? ", " : "]\r\n"));
        }
        result.append("Fitness reached: " + originalFitness + "\r\n");
        System.out.println(result.toString());


        if(saveResults){
            try (PrintWriter out = new PrintWriter(outputPath, "UTF-8")){
                out.print(result);
            } catch (FileNotFoundException e) {

            }
        }

    }

    /**
     * run the ranker on a set of queries and returns the RPrecision value
     * @param withSemantics
     * @return the RPrecision value
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     */
    static double BM25Run(boolean withSemantics) throws IOException, ClassNotFoundException, InterruptedException {
        List<QueryResult> qRes = new ArrayList<>();

        qRes.add(new QueryResult("351", convertFromSerialIDtoDocID( searcher.answerquery("Falkland petroleum exploration", withSemantics))));
        qRes.add(new QueryResult("352" , convertFromSerialIDtoDocID(searcher.answerquery("British Chunnel impact", withSemantics))));
        qRes.add(new QueryResult("358" ,convertFromSerialIDtoDocID( searcher.answerquery("blood-alcohol fatalities", withSemantics))));
        qRes.add(new QueryResult("359" , convertFromSerialIDtoDocID( searcher.answerquery("mutual fund predictors ", withSemantics))));
        qRes.add(new QueryResult("362" , convertFromSerialIDtoDocID(searcher.answerquery("human smuggling ", withSemantics))));
        qRes.add(new QueryResult("367" , convertFromSerialIDtoDocID(searcher.answerquery("piracy ", withSemantics))));
        qRes.add(new QueryResult("373" , convertFromSerialIDtoDocID (searcher.answerquery("encryption equipment export ", withSemantics))));
        qRes.add(new QueryResult("374" , convertFromSerialIDtoDocID (searcher.answerquery("Nobel prize winners ", withSemantics))));
        qRes.add(new QueryResult("377" , convertFromSerialIDtoDocID(searcher.answerquery("cigar smoking ", withSemantics))));
        qRes.add(new QueryResult("380" , convertFromSerialIDtoDocID(searcher.answerquery("obesity medical treatment ", withSemantics))));
        qRes.add(new QueryResult("384" , convertFromSerialIDtoDocID(searcher.answerquery("space station moon ", withSemantics))));
        qRes.add(new QueryResult("385" , convertFromSerialIDtoDocID(searcher.answerquery("hybrid fuel cars ", withSemantics))));
        qRes.add(new QueryResult("387" , convertFromSerialIDtoDocID(searcher.answerquery("radioactive waste ", withSemantics))));
        qRes.add(new QueryResult("388" , convertFromSerialIDtoDocID(searcher.answerquery("organic soil enhancement ", withSemantics))));
        qRes.add(new QueryResult("390" , convertFromSerialIDtoDocID(searcher.answerquery("orphan drugs ", withSemantics))));
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
        double RPrecision = 0.0;
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            if(s.equals("R-Precision (precision after R (= num_rel for a query) docs retrieved):")){
                //get rPrecision result
                s = stdInput.readLine();
                s = s.replace("    Exact:        ", "");
                RPrecision = Double.parseDouble(s);
            }
        }

        // read any errors from the attempted command
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
        }

        return RPrecision;
    }

    /**
     * loads the dictionaries
     * @param useStemming
     * @param outputFolder
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws ClassCastException
     */
    public static void loadDictionaries(boolean useStemming, String outputFolder) throws IOException, ClassNotFoundException, ClassCastException {
        ObjectInputStream inDictionary = new ObjectInputStream(new BufferedInputStream(new FileInputStream(outputFolder + '/' + (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.dictionarySaveName )));
        ObjectInputStream inDocDictionary = new ObjectInputStream(new BufferedInputStream(new FileInputStream(outputFolder + '/' +
                (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.docsDictionaryName )));
        ObjectInputStream inCityDictionay = new ObjectInputStream(new BufferedInputStream(new FileInputStream(outputFolder + '/' +
                (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.cityDictionaryName)));
        ObjectInputStream inLanguages = new ObjectInputStream(new BufferedInputStream(new FileInputStream(outputFolder + '/' +
                (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.languages)));

        if(useStemming){
            HillClimbingOptimizer.mainDictionaryWithStemming = (Map<String, IndexEntry>) inDictionary.readObject();
            HillClimbingOptimizer.docDictionaryWithStemming = (ArrayList)inDocDictionary.readObject();
        }
        else{
            HillClimbingOptimizer.mainDictionaryNoStemming = (Map<String, IndexEntry>) inDictionary.readObject();
            HillClimbingOptimizer.docDictionaryNoStemming = (ArrayList) inDocDictionary.readObject();
        }
        HillClimbingOptimizer.cityDictionary = (Map<String , CityIndexEntry>) inCityDictionay.readObject();
        HillClimbingOptimizer.languages = (Set<String>) inLanguages.readObject();

        inDictionary.close();
        inDocDictionary.close();
        inCityDictionay.close();
        inLanguages.close();
    }

}
