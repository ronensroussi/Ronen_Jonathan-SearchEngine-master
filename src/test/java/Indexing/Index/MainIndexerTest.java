package Indexing.Index;

import Indexing.DocumentProcessing.Document;
import Indexing.DocumentProcessing.TermDocument;
import Indexing.DocumentProcessing.Parse;
import Indexing.DocumentProcessing.ReadFile;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MainIndexerTest {

    private static final int documentBufferSize = 3;
    private static final int termBufferSize = 3;
    private static final int stemmedTermBufferSize = 3;


    private static final String pathToDocumentsFolder = "C:\\Users\\ronen\\Desktop\\FB396001";
    private static final String pathToStopWordRONEN ="C:\\Users\\ronen\\Desktop\\stopWords.txt";

    private static final String pathToDocumentsFolderAtJM = "C:/Users/John/Downloads/infoRetrieval/200 files";
    private static final String patToStopwordsFileAtJM = "C:/Users/John/Google Drive/Documents/1Uni/Semester E/information retrieval 37214406/Assignements/Ass1/stop_words.txt";
    private static final String pathToOutputFolderAtJM = "C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\indexing";

    @Test
    void testMainIndex() throws InterruptedException, FileNotFoundException {

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
        System.out.println(((double) System.currentTimeMillis()-start)/1000);
//        Map<String,TempIndexEntry> tempMap = indexer.getTempMap();
//
//        String path = "C:\\Users\\ronen\\Desktop\\test.txt";
//
//        try {
//            File file = new File(path);
//            OutputStream fo = new FileOutputStream(file);
//
//
//            for (String term : tempMap.keySet()) {
//                //(term+"->"+map.get(term).getPosting()+"\n");
//                int [] pointer=tempMap.get(term).getPointerList();
//                int df = tempMap.get(term).getDf();
//                fo.write((term+"->[").getBytes());
//                fo.write((df+"").getBytes());
//                fo.write(("] [").getBytes());
//                for (int i = 0; i <pointer.length ; i++) {
//                    fo.write((pointer[i]+",").getBytes());
//                }
//                fo.write(("]\n").getBytes());
//
//
//                //fo.write((term+"->"+map.get(term).getPointerList()[0]+"\n").getBytes());//term+"->"+map.get(term).getPosting()+"\n").getBytes());
//                //System.out.println(map.get(term).getPointerList()+"\n");
//            }
//            fo.flush();
//            fo.close();
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        //indexer.mergeMainIndex();
        //indexer.dumpDictionaryToDisk();
//        indexer.pritDictionaryToFile();

        System.out.println("Heap size (MBytes): " + toMB(Runtime.getRuntime().totalMemory()));
        System.out.println("Memory in use (MBytes): " + toMB(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

//        Map<String,IndexEntry> map = indexer.getMainMap();




//
//         path = "C:\\Users\\ronen\\Desktop\\a.txt";
//
//        try {
//            File file = new File(path);
//            OutputStream fo = new FileOutputStream(file);
//
//
//        for (String term : map.keySet()) {
//             //(term+"->"+map.get(term).getPosting()+"\n");
//            int  pointer=map.get(term).getPostingPointer();
//            int df = map.get(term).getDf();
//            int totaltf = map.get(term).getTotalTF();
//            fo.write((term+"->["+pointer+"],"+df+","+totaltf+"\n").getBytes());
//            //fo.write((term+"->"+map.get(term).getPointerList()[0]+"\n").getBytes());//term+"->"+map.get(term).getPosting()+"\n").getBytes());
//            //System.out.println(map.get(term).getPointerList()+"\n");
//        }
//        fo.flush();
//        fo.close();
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        path = "C:\\Users\\ronen\\Desktop\\docs.txt";
//
//         Map<Integer , DocIndexEntery> docsMap = indexer.getDocsMap();
//        indexer=null;
//        try {
//            File file = new File(path);
//            OutputStream fo = new FileOutputStream(file);
//
//
//            for (Integer docID : docsMap.keySet()) {
//                //(term+"->"+map.get(term).getPosting()+"\n");
//                String  pointer=docsMap.get(docID).getDocID();
//                fo.write((docID+"->["+pointer+"]\n").getBytes());
//                //fo.write((term+"->"+map.get(term).getPointerList()[0]+"\n").getBytes());//term+"->"+map.get(term).getPosting()+"\n").getBytes());
//                //System.out.println(map.get(term).getPointerList()+"\n");
//            }
//            fo.flush();
//            fo.close();
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//


    }

    @Test
    Indexer testMainIndexClone() throws InterruptedException, FileNotFoundException {

        BlockingQueue<Document> documentBuffer = new ArrayBlockingQueue<Document>(documentBufferSize);
        BlockingQueue<TermDocument> termDocumentsBuffer = new ArrayBlockingQueue<>(termBufferSize);


        //  Worker Threads:

        Thread tReader = new Thread(new ReadFile(pathToDocumentsFolderAtJM, documentBuffer));

        HashSet<String> stopwords = Parse.getStopWords(patToStopwordsFileAtJM);
        Thread tParser = new Thread(new Parse(stopwords, documentBuffer, termDocumentsBuffer, true));
        Indexer indexer =new Indexer(pathToOutputFolderAtJM,termDocumentsBuffer,true);
        Thread tIndexer = new Thread(indexer);

        long start=System.currentTimeMillis();

        Thread memoryReporter = new Thread(() -> {
            int seconds = 0;
            final int interval = 15;
            while (true){
                System.out.println("Seconds: " + seconds);
                seconds += interval;
                System.out.println("Heap size (MBytes): " + toMB(Runtime.getRuntime().totalMemory()));
                System.out.println("Memory in use (MBytes): " + toMB(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
                try {
                    Thread.sleep(interval*1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        memoryReporter.start();

        tReader.start();
        tParser.start();
        tIndexer.start();
        tIndexer.join();


        System.out.println("Total time: " + ((double) System.currentTimeMillis()-start)/1000);

        return indexer;

//        System.out.println("Heap size (MBytes): " + toMB(Runtime.getRuntime().totalMemory()));
//        System.out.println("Memory in use (MBytes): " + toMB(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
//
//        Map<String,TempIndexEntry> map = indexer.getMainMap();
///*
//        for (Term term : map.keySet())
//        {
//            System.out.println(map.get(term).getPointerList()+"\n");
//        }
//   */
//
//        String path = "C:\\Users\\ronen\\Desktop\\a.txt";
//
//        try {
//            File file = new File(path);
//            OutputStream fo = new FileOutputStream(file);
//
//
//            for (String term : map.keySet()) {
//                //(term+"->"+map.get(term).getPosting()+"\n");
//                fo.write((term+"->"+map.get(term).getPointerList()+"\n").getBytes());//term+"->"+map.get(term).getPosting()+"\n").getBytes());
//                //System.out.println(map.get(term).getPointerList()+"\n");
//            }
//            fo.flush();
//            fo.close();
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }


    @Test
    void statistics() throws InterruptedException, IOException, ClassNotFoundException {
        boolean useStemming = true;
        ObjectInputStream inDictionary = new ObjectInputStream(new BufferedInputStream(new FileInputStream(pathToOutputFolderAtJM + '/' +
                (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.dictionarySaveName )));
        ObjectInputStream inDocDictionary = new ObjectInputStream(new BufferedInputStream(new FileInputStream(pathToOutputFolderAtJM + '/' +
                (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.docsDictionaryName )));

        Map<String, IndexEntry> mainDic = (Map<String, IndexEntry>) inDictionary.readObject();
        Map<Integer, DocIndexEntery> docDic = (Map<Integer, DocIndexEntery>) inDocDictionary.readObject();

        useStemming = false;

        ObjectInputStream inDictionaryNS = new ObjectInputStream(new BufferedInputStream(new FileInputStream("C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\indexing" + '/' +
                (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.dictionarySaveName )));
        ObjectInputStream inDocDictionaryNS = new ObjectInputStream(new BufferedInputStream(new FileInputStream("C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\indexing" + '/' +
                (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.docsDictionaryName )));

        Map<String, IndexEntry> mainDicNS = (Map<String, IndexEntry>) inDictionaryNS.readObject();
        Map<Integer, DocIndexEntery> docDicNS = (Map<Integer, DocIndexEntery>) inDocDictionaryNS.readObject();

        // GENERAL

        System.out.println("Number of terms w/stemming: " + mainDic.size());
        System.out.println("Number of terms wo/stemming: " + mainDicNS.size());

         // COUNTRIES

        Set<String> countries = new HashSet<>();
        Set<String> termsThatAreCountries = new HashSet<>();
        BufferedReader countriesIn = new BufferedReader(new CharArrayReader(this.countries.toCharArray()));


        String line = countriesIn.readLine();
        while (line != null){
            countries.add(line);
            line = countriesIn.readLine();
        }

        for (String term: mainDicNS.keySet()
             ) {
            if(countries.contains(term.toUpperCase())) termsThatAreCountries.add(term.toUpperCase());
        }


        System.out.println("Number of unique terms that are countries: " + termsThatAreCountries.size());

        // top 10 totalTF and bottom 10

        Map.Entry<String, IndexEntry>[] entriesNS = new Map.Entry[mainDicNS.size()];
        mainDicNS.entrySet().toArray(entriesNS);

        Arrays.sort(entriesNS, (Comparator.comparingInt(e -> -1 * e.getValue().getTotalTF())));

        System.out.println("Ten most common terms: ");
        for (int i = 0; i < 10 ; i++) {
            System.out.print("term: " + entriesNS[i].getKey());
            System.out.println(", TotalTF: " + entriesNS[i].getValue().getTotalTF());
        }

        System.out.println("Ten least common terms: ");
        for (int i = entriesNS.length-1; entriesNS.length - i <= 10  ; i--) {
            System.out.print("term: " + entriesNS[i].getKey());
            System.out.println(", TotalTF: " + entriesNS[i].getValue().getTotalTF());
        }

        // dictionary to csv

//        PrintWriter csvWriterNS = new PrintWriter(new BufferedOutputStream(new FileOutputStream(pathToOutputFolderAtJM + "/ mainDicNS.csv")));
//
//        csvWriterNS.println("term,TotalTf,df");
//
//        for (Map.Entry<String, IndexEntry> entry: entriesNS
//             ) {
//            StringBuilder sb = new StringBuilder();
//            sb.append(entry.getKey());
//            sb.append(",");
//            sb.append(entry.getValue().getTotalTF());
//            sb.append(",");
//            sb.append(entry.getValue().getDf());
//            sb.append(",");
//            csvWriterNS.println(sb);
//        }
//
//        csvWriterNS.flush();
//        csvWriterNS.close();

        // cities

        ObjectInputStream inCityDictionaryNS = new ObjectInputStream(new BufferedInputStream(new FileInputStream("C:\\Users\\John\\Downloads\\infoRetrieval\\test results\\indexing" + '/' +
                (useStemming ? Indexer.withStemmingOutputFolderName : Indexer.noStemmingOutputFolderName) +'/'+ Indexer.cityDictionaryName )));

        Map<String , CityIndexEntry> citiesIndexNS = (Map<String , CityIndexEntry>) inCityDictionaryNS.readObject();

        Map.Entry<String , CityIndexEntry>[] citiesEntriesNS = new Map.Entry[citiesIndexNS.size()];
        citiesIndexNS.entrySet().toArray(citiesEntriesNS);

//        Arrays.sort(citiesEntriesNS, (Comparator.comparingInt(e -> -1 * e.getValue().())));



    }

    @Test
    void statisticsFBIS33366() throws InterruptedException, FileNotFoundException {
        BlockingQueue<Document> documentBuffer = new ArrayBlockingQueue<Document>(documentBufferSize);
        BlockingQueue<TermDocument> termDocumentsBuffer = new ArrayBlockingQueue<>(termBufferSize);


        //  Worker Threads:

        Thread tReader = new Thread(new ReadFile("C:\\Users\\John\\Downloads\\infoRetrieval\\just FBIS3-3366", documentBuffer));

        HashSet<String> stopwords = Parse.getStopWords(patToStopwordsFileAtJM);
        Thread tParser = new Thread(new Parse(stopwords, documentBuffer, termDocumentsBuffer, true));
        Indexer indexer =new Indexer(pathToOutputFolderAtJM,termDocumentsBuffer,true);
        Thread tIndexer = new Thread(indexer);


        tReader.start();
        tParser.start();
        tIndexer.start();
        tIndexer.join();

        Map<String, IndexEntry> mainDic = indexer.getMainMap();




    }

    String countries = "AFGHANISTAN\n" +
            "ALBANIA\n" +
            "ALGERIA\n" +
            "ANDORRA\n" +
            "ANGOLA\n" +
            "ANTIGUA & BARBUDA\n" +
            "ARGENTINA\n" +
            "ARMENIA\n" +
            "AUSTRALIA\n" +
            "AUSTRIA\n" +
            "AZERBAIJAN\n" +
            "BAHAMAS, THE\n" +
            "BAHRAIN\n" +
            "BANGLADESH\n" +
            "BARBADOS\n" +
            "BELARUS\n" +
            "BELGIUM\n" +
            "BELIZE\n" +
            "BENIN\n" +
            "BHUTAN\n" +
            "BOLIVIA\n" +
            "BOSNIA & HERZEGOVINA\n" +
            "BOTSWANA\n" +
            "BRAZIL\n" +
            "BRUNEI\n" +
            "BULGARIA\n" +
            "BURKINA FASO\n" +
            "BURUNDI\n" +
            "CABO VERDE\n" +
            "CAMBODIA\n" +
            "CAMEROON\n" +
            "CANADA\n" +
            "CENTRAL AFRICAN REPUBLIC\n" +
            "CHAD\n" +
            "CHILE\n" +
            "CHINA\n" +
            "COLOMBIA\n" +
            "COMOROS\n" +
            "CONGO, DEMOCRATIC REPUBLIC OF THE\n" +
            "COSTA RICA\n" +
            "CÔTE D'IVOIRE\n" +
            "CROATIA\n" +
            "CUBA\n" +
            "CYPRUS\n" +
            "CZECH REPUBLIC\n" +
            "DENMARK\n" +
            "DJIBOUTI\n" +
            "DOMINICA\n" +
            "DOMINICAN REPUBLIC\n" +
            "ECUADOR\n" +
            "EGYPT\n" +
            "EL SALVADOR\n" +
            "EQUATORIAL GUINEA\n" +
            "ERITREA\n" +
            "ESTONIA\n" +
            "ESWATINI\n" +
            "ETHIOPIA\n" +
            "FEDERATED STATES OF MICRONESIA\n" +
            "FIJI\n" +
            "FINLAND\n" +
            "FRANCE\n" +
            "GABON\n" +
            "GAMBIA, THE\n" +
            "GEORGIA\n" +
            "GERMANY\n" +
            "GHANA\n" +
            "GREECE\n" +
            "GRENADA\n" +
            "GUATEMALA\n" +
            "GUINEA\n" +
            "GUINEA-BISSAU\n" +
            "GUYANA\n" +
            "HAITI\n" +
            "HONDURAS\n" +
            "HUNGARY\n" +
            "ICELAND\n" +
            "INDIA\n" +
            "INDONESIA\n" +
            "IRAN\n" +
            "IRAQ\n" +
            "IRELAND\n" +
            "ISRAEL\n" +
            "ITALY\n" +
            "JAMAICA\n" +
            "JAPAN\n" +
            "JORDAN\n" +
            "KAZAKHSTAN\n" +
            "KENYA\n" +
            "KIRIBATI\n" +
            "KOSOVO\n" +
            "KUWAIT\n" +
            "KYRGYZSTAN\n" +
            "LAOS\n" +
            "LATVIA\n" +
            "LEBANON\n" +
            "LESOTHO\n" +
            "LIBERIA\n" +
            "LIBYA\n" +
            "LIECHTENSTEIN\n" +
            "LITHUANIA\n" +
            "LUXEMBOURG\n" +
            "MACEDONIA\n" +
            "MADAGASCAR\n" +
            "MALAWI\n" +
            "MALAYSIA\n" +
            "MALDIVES\n" +
            "MALI\n" +
            "MALTA\n" +
            "MARSHALL ISLANDS\n" +
            "MAURITANIA\n" +
            "MAURITIUS\n" +
            "MEXICO\n" +
            "MOLDOVA\n" +
            "MONACO\n" +
            "MONGOLIA\n" +
            "MONTENEGRO\n" +
            "MOROCCO\n" +
            "MOZAMBIQUE\n" +
            "MYANMAR\n" +
            "NAMIBIA\n" +
            "NAURU\n" +
            "NEPAL\n" +
            "NETHERLANDS\n" +
            "NEW ZEALAND\n" +
            "NICARAGUA\n" +
            "NIGER\n" +
            "NIGERIA\n" +
            "NORTH KOREA\n" +
            "NORWAY\n" +
            "OMAN\n" +
            "PAKISTAN\n" +
            "PALAU\n" +
            "PALESTINE\n" +
            "PANAMA\n" +
            "PAPUA NEW GUINEA\n" +
            "PARAGUAY\n" +
            "PERU\n" +
            "PHILIPPINES\n" +
            "POLAND\n" +
            "PORTUGAL\n" +
            "QATAR\n" +
            "REPUBLIC OF THE CONGO\n" +
            "ROMANIA\n" +
            "RUSSIA\n" +
            "RWANDA\n" +
            "SAINT KITTS & NEVIS\n" +
            "SAINT LUCIA\n" +
            "SAINT VINCENT & THE GRENADINES\n" +
            "SAMOA\n" +
            "SAN MARINO\n" +
            "SÃO TOMÉ & PRÍNCIPE\n" +
            "SAUDI ARABIA\n" +
            "SENEGAL\n" +
            "SERBIA\n" +
            "SEYCHELLES\n" +
            "SIERRA LEONE\n" +
            "SINGAPORE\n" +
            "SLOVAKIA\n" +
            "SLOVENIA\n" +
            "SOLOMON ISLANDS\n" +
            "SOMALIA\n" +
            "SOUTH AFRICA\n" +
            "SOUTH KOREA\n" +
            "SOUTH SUDAN\n" +
            "SPAIN\n" +
            "SRI LANKA\n" +
            "SUDAN\n" +
            "SURINAME\n" +
            "SWEDEN\n" +
            "SWITZERLAND\n" +
            "SYRIA\n" +
            "TAJIKISTAN\n" +
            "TANZANIA\n" +
            "THAILAND\n" +
            "TIMOR-LESTE\n" +
            "TOGO\n" +
            "TONGA\n" +
            "TRINIDAD & TOBAGO\n" +
            "TUNISIA\n" +
            "TURKEY\n" +
            "TURKMENISTAN\n" +
            "TUVALU\n" +
            "UGANDA\n" +
            "UKRAINE\n" +
            "UNITED ARAB EMIRATES\n" +
            "UNITED KINGDOM\n" +
            "UNITED STATES\n" +
            "URUGUAY\n" +
            "UZBEKISTAN\n" +
            "VANUATU\n" +
            "VATICAN CITY\n" +
            "VENEZUELA\n" +
            "VIETNAM\n" +
            "YEMEN\n" +
            "ZAMBIA\n" +
            "ZIMBABWE\n";

    public static double toMB(long bytes){
        return bytes/(Math.pow(2, 20));
    }


}
