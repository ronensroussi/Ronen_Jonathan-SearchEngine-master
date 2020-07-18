package Indexing.Index;

import Indexing.DocumentProcessing.Term;
import Indexing.DocumentProcessing.TermDocument;
import Indexing.Index.IO.*;

import java.io.*;
import java.util.*;

import static javafx.application.Platform.exit;

/**
 * creates the main index.
 * holds both dictionaries as fields. They can be retrieved with {@link #getMainDictionary getMainDictionary} and {@link #getDocsDictionary getDocsDictionary}.
 * every {@value partialGroupSize} documents, dumps postings into a temporary file.
 */
public class MainIndexMaker extends AIndexMaker {

    //the size of the group of documents that will be indexed every time.
    private static final short partialGroupSize = 10000;
    private Map<String, TempIndexEntry> tempDictionary;
    private Map <String, IndexEntry> mainDictionary;
    private List<DocIndexEntery> docsDictionary;
    private int numOfDocs;
    private short tempFileNumber;
    private String path="";
    private Set<String> languages;

    public MainIndexMaker (String path ){
        super();
        this.tempDictionary=new HashMap<>();
        this.mainDictionary = new HashMap<>();
        this.docsDictionary = new ArrayList<>();
        languages=new HashSet<>();
        numOfDocs=0;
        tempFileNumber=0;
        this.path=path;

    }

    @Override
    /**
     * this method receive a docs of Terms and adding them to the index.
     * at first it adding terms to the temporary index and finally adds all to the main dictionary
     * after  #partialGroupSize docs that ware indexed we dump the temporary postings to the disk
     */
    public void addToIndex(TermDocument doc) {
        if(doc.getSerialID() != -1){

            Set<String> uniqueWords=new HashSet<>();// set of all unique words in a doc
            Map<String,Integer> tfMap=new HashMap<>(); // map  term to his tf value in this doc
            Set<String>entities =new HashSet<>();
            List<Term> title = doc.getTitle();
            List<Term> text = doc.getText();
            int docDateAsInt=0;
            if(doc.date!=null) {
                Date docDate = doc.date;
                docDateAsInt = convertDateToInt(docDate);
            }

            int docLength = text.size();
            int maxTf = getMaxTf(uniqueWords,tfMap,title,text);
            int numOfUniqueWords = uniqueWords.size();
            String docId = doc.getDocId();
            String city ="";
            String language = "";
            if(doc.getCity()!=null) {
                city = doc.getCity().toString();
            }
            if(doc.getLanguage()!=null) {
                language = doc.getLanguage();
            }

            if(!language.equals("")){
                languages.add(language);
            }

// add a document to the DocIndex
            DocIndexEntery docIndexEntery = new DocIndexEntery(docId,numOfUniqueWords,maxTf,city,language,docLength,docDateAsInt);
            docsDictionary.add(docIndexEntery);
            docIndexEntery=null;


// create the posting to the doc and add is to the index entery or creat a new index entery if not exist
            for(String term : uniqueWords){
                short tf =  tfMap.get(term).shortValue();
                Posting posting = new Posting(doc.getSerialID(), tf);

                int beginning=0;
                try {
                    beginning = (int)(text.size()*0.1);
                }catch (NullPointerException e ){
                    beginning = 0;
                    e.printStackTrace();
                }

                if (title.contains(new Term(term))){
                    posting.setInTitle(true);
                }

                boolean isInBeginning=false;
                for (int i = 0; i <beginning && !isInBeginning ; i++) {
                    if(term.equals(text.get(i).toString()))
                        isInBeginning=true;
                }

                posting.setInBeginning(isInBeginning);

                if(!tempDictionary.containsKey(term)) {
                    TempIndexEntry tmp = new TempIndexEntry();
                    tmp.addPosting(posting);
                    tmp.increaseTfByN(tfMap.get(term).shortValue());
                    posting=null;
                    tempDictionary.put(term,tmp);
                }else {
                    TempIndexEntry tmp = tempDictionary.get(term);
                    tmp.increaseTfByN(tfMap.get(term).shortValue());
                    tmp.addPosting(posting);
                    posting = null;
                }

                if(tfMap.containsKey(term.toUpperCase())){
                    if (!tfMap.containsKey((term.toLowerCase()))){
                        entities.add(term);
                    }
                }

            }
            numOfDocs++;

            if(entities.size()>0) {
                String[] top5 = getTopEntities(tfMap, entities);
                float[] ranking = new float[top5.length];
                for (int i = 0; i < top5.length; i++) {
                    int a =tfMap.get(top5[i]);
                    float b = (float)a/maxTf;
                    ranking[i] = (b);
                }
                docsDictionary.get(doc.getSerialID()).setEntities(top5);
                docsDictionary.get(doc.getSerialID()).setRanking(ranking);
            }
            else {
                docsDictionary.get(doc.getSerialID()).setEntities(new String[0]);
                docsDictionary.get(doc.getSerialID()).setRanking(new float[0]);
            }

            if(numOfDocs==partialGroupSize){
                dumpToDisk();
            }

            tfMap=null;
        }else {
            dumpToDisk();
        }


    }

    /**
     * this method converts a Date object to int that represent the same date
     * @param docDate
     * @return
     */
    private int convertDateToInt(Date docDate) {

        String year = String.valueOf(1900+docDate.getYear());
        String month = String.valueOf(docDate.getMonth()+1);
        String day = String.valueOf(docDate.getDate());

    while (month.length()<2){
        month="0"+month;

    }
    while (day.length()<2){
        day="0"+day;
    }

    return Integer.valueOf(year+month+day);
    }

    /**
     * this method return the top 5 entities in the doc
     * @param tfMap - map of terms and their total frequency in the document
     * @param entities - a list of all the entities in the document
     * @return array of at most 5 entities in order of their relevance
     */
    private String[] getTopEntities(Map<String, Integer> tfMap,  Set<String> entities) {
        String [] toReturn =null;
        List<String> ent=new ArrayList<>();
        for (String term:entities) {
            ent.add(term);
        }
        Collections.sort(ent, (o1, o2) -> {
            int a= tfMap.get(o1),b =tfMap.get(o2);
            if(a>b){
                return -1;
            }
            else if (a<b){
                return 1;
            }else {
                return 0;
            }
        });
        if (ent.size()>=5){
            toReturn=new String[5];
        }
        else {
            toReturn= new String[ent.size()];
        }

        for (int i = 0; i <toReturn.length ; i++) {
            toReturn[i]=ent.get(i);
        }
        return toReturn;

    }


    /**
     * this function go over every term in a list and calculate the maxTF for the doc
     * also it filling the set of unique words with every unique word that in the document
     * and also fillingg the map of terms that saves the tf for every word in the doc
     * and fiinaly check if a term is in the title or in the first 20% of a document and update the spacial map
     * @param uniqueWords - an empty Set that will contain every unique term in the doc
     * @param tfMap - an empty Hash map that counts , for each Term it's tf in the doc
     * @return the maxTF of a term in the Document
     */
    private int getMaxTf( Set<String> uniqueWords , Map<String,Integer> tfMap, List<Term> title , List<Term> text){
        int maxTf=0;
        int beginning=0;
        try {
            beginning = (int)(text.size()*0.1);
        }catch (NullPointerException e ){
            beginning=0;
        }

        for(Term term : title){
            String t = term.toString();
            uniqueWords.add(t);
            if(tfMap.containsKey(t)){
                tfMap.put(t, tfMap.get(t)+1);
            }
            else{
                tfMap.put(t, 1);
            }

            int value = tfMap.get(t);
            if(value > maxTf){
                maxTf = value;
            }

        }
        int count =0;
        for(Term term : text){
            String t = term.toString();
            uniqueWords.add(t);
            if(tfMap.containsKey(t)){
                tfMap.put(t, tfMap.get(t)+1);
            }
            else{
                tfMap.put(t, 1);
            }

            int value = tfMap.get(t);
            if(value > maxTf){
                maxTf = value;
            }
            count++;
        }
        return maxTf;
    }



    public Map<String , TempIndexEntry> getTempDictionary(){

        return tempDictionary;
    }

    public Map<String , IndexEntry> getMainDictionary(){
        return mainDictionary;
    }


    public List<DocIndexEntery> getDocsDictionary(){
        return docsDictionary;
    }


    /**
     * this method creates a temporary posting file and write them to disk
     */
    public void  dumpToDisk()
    {

        try {
            IPostingOutputStream outputStream = new PostingOutputStream(path+"\\temp"+tempFileNumber+".txt");
            TempIndexEntry tmp =null;
            numOfDocs=0;
            for (String term : tempDictionary.keySet()) {
                tmp = tempDictionary.get(term);
                if (tmp.getPostingSize() > 0) {
                    tmp.sortPosting();
                    int pointer = (int)outputStream.write(tmp.getPosting());
                    tmp.addPointer(tempFileNumber, pointer);
                    tmp.deletePostingList();
                }
            }
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tempFileNumber++;

    }


    /**
     * this method merge all temporary postings files to a single and final posting file
     * by iterating over the key of the temporary dictionary and getting its posting from all of the temp postings files,
     * sort them , write them to the disk and save the pointer to them in the main dictionary.
     * finally it deleting the term from the temp dictionary.
     */
    public void mergeIndex()
    {
        Set<String> uniqueWords = tempDictionary.keySet();
        String[] allTerms = new String[uniqueWords.size()];
        uniqueWords.toArray(allTerms);
        try {
            IPostingOutputStream postingOutputStream=new PostingOutputStream(path+"\\Postings");
            for (String term: allTerms ) {
                if(!tempDictionary.containsKey(term)){
                    continue;
                }
                List<Posting> postingToWrite = new ArrayList<>();
                String finalTerm = addTermToDictionary(term , postingToWrite);
                if (finalTerm.equals("")){
                    continue;
                }
                Collections.sort(postingToWrite, (o1, o2) -> {
                    if(o1.getTf()>o2.getTf()){
                        return -1;
                    }else if(o1.getTf()==o2.getTf()){
                        return 0;
                    }else
                        return 1;
                });
                int pointer =(int)postingOutputStream.write(postingToWrite);
                mainDictionary.get(finalTerm).setPostingPointer(pointer);

            }
            postingOutputStream.flush();
            postingOutputStream.close();

        } catch (NullPointerException e){
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
        tempDictionary=null;



        try {
            File file = new File(path);
            for (File fi : file.listFiles()) {
                String name = fi.getName().substring(0,4);
                if (name.equals("temp")) {
                    fi.delete();
                }
            }
        }catch (NullPointerException e){
            e.printStackTrace();

        }


    }


    private String addTermToDictionary(String term , List<Posting> finalPosting) {
        String termToWrite=term;

        try {

            int totalTF = tempDictionary.get(term).getTfTotal();
            char c = term.charAt(0);
            List<Posting> postingList = getTermPostings(term);
            if (c >= 'a' && c <= 'z') { //if first letter is lower case
                termToWrite = term;

                String newTerm =new String(term.toUpperCase());
                if (tempDictionary.containsKey(newTerm)) { // if there is also the same term with upper case in the corpus
                    List<Posting> newTermPostings = getTermPostings(newTerm);
                    totalTF += tempDictionary.get(newTerm).getTfTotal();
                    finalPosting.addAll(mergePostings(postingList, newTermPostings));
                    tempDictionary.remove(newTerm);
                }else{
                    finalPosting.addAll(postingList);
                }


            } else if (c >= 'A' && c <= 'Z') { // if firs letter is a upper case

                String newTerm =new String(term.toLowerCase());
                if (tempDictionary.containsKey(newTerm)) { // if there is also the same term with LOWER case in the corpus
                    List<Posting> newTermPostings = getTermPostings(newTerm);
                    totalTF += tempDictionary.get(newTerm).getTfTotal();
                    finalPosting.addAll(mergePostings(postingList, newTermPostings));
                    tempDictionary.remove(newTerm);
                    termToWrite = term.toLowerCase();
                } else {
                    termToWrite = term;
                    finalPosting.addAll(postingList);
                }
            } else {
                termToWrite = term;
                finalPosting.addAll(postingList);
            }

            tempDictionary.remove(term);
            IndexEntry indexEntry = new IndexEntry(totalTF, finalPosting.size());
//            if(indexEntry.getTotalTF()>1) {
//                mainDictionary.put(termToWrite, indexEntry);
//            }
//            else {
//                return "";
//            }

            mainDictionary.put(termToWrite, indexEntry);


        }catch (NullPointerException e){
            e.printStackTrace();
        }
        return termToWrite;
    }


    private List<Posting>getTermPostings(String term)
    {
        List<Posting> postingList = new ArrayList<>();
        int[] pointers = tempDictionary.get(term).getPointerList();
        int length = pointers.length;
        for (int i = 0; i < length; i++) {
            if (pointers[i] != -1) {
                IPostingInputStream inputStream = null;
                try {
                    inputStream = new PostingInputStream(path + "\\temp" + i + ".txt");
                    List<Posting> tempPostings = inputStream.readTermPostings((long)pointers[i]);
                    ((PostingInputStream) inputStream).close();

                    postingList.addAll(tempPostings);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Throwable e){
                    e.printStackTrace();
                }

            }
        }
        return postingList;
    }

    private List<Posting> mergePostings(List<Posting>postingList , List<Posting>newTermPostings )
    {
        int maxTF;
        List<Posting> finalList = new ArrayList<>();
        Map<Integer,Posting> newTermPostingMap = new HashMap<>();

        for (Posting post : newTermPostings) {
            newTermPostingMap.put(post.getDocSerialID(),post);
        }

        for (Posting posting: postingList) {
            int docID=posting.getDocSerialID();
            int tf = posting.getTf();

            if(!newTermPostingMap.containsKey(docID)){
                finalList.add(posting);
            }
            else { // if there is the same doc in two different lists of the same term
                Posting samePosting = newTermPostingMap.get(docID);
                tf+=samePosting.getTf();
                Posting newPosting = new Posting(posting.getDocSerialID() ,(short)tf ,posting.isInTitle()||samePosting.isInTitle() ,posting.isInBeginning() || samePosting.isInBeginning());
                finalList.add(newPosting);

                DocIndexEntery docIndexEntery = docsDictionary.get(docID);
                maxTF = docIndexEntery.getMaxTF();
                if(tf>maxTF){
                    docIndexEntery.setMaxTF(tf);
                }
                docIndexEntery.setNumOfUniqueWords(docIndexEntery.getNumOfUniqueWords()-1);
                newTermPostingMap.remove(docID);
            }
        }
        for (Integer key : newTermPostingMap.keySet()) {
            finalList.add(newTermPostingMap.get(key));
        }
        return finalList;
    }

    public Set<String> getLanguages(){
        return languages;
    }


}
