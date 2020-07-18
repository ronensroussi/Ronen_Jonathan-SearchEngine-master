package Indexing.Index;

import Indexing.DocumentProcessing.Term;
import Indexing.DocumentProcessing.TermDocument;
import Indexing.Index.IO.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

/**
 * creates the city index.
 * holds the dictionary as a field. It can be retrieved with {@link #getCityDictionary()}.
 */
public class CityIndexMaker extends AIndexMaker {

    Map<String , CityIndexEntry> cityDictionary=null;
    String path;

    public CityIndexMaker(String path) throws FileNotFoundException {
        super();
        FileNotFoundException e = getDictionaryFromDisk();
        if(e != null) throw e;
        this.path=path;

    }

    @Override
    /**
     *  this method receive a docs of Terms and adding the city that represent the doc to the #CityDictionary.
     *  its counts the number of times that the city that represent the doc is appears in the text and save the locations of them.
     *  finally  for each city , there is a City Index entry with all the information about the city and the docs  represented by this city
     *  and the positions of the city in the text
     *
     */
    public void addToIndex(TermDocument doc) {
        if (doc.getSerialID() != -1) {
            List<Term> title = doc.getTitle();
            List<Term> text = doc.getText();
            Map<String , Integer> counterMap = new LinkedHashMap<>();

            String cityDoc;
            try{
                cityDoc=doc.getCity().toString();
            }catch (NullPointerException e ){
                cityDoc="";
            }
            if(!cityDoc.equals("") && (cityDoc.charAt(0)>='A' && cityDoc.charAt(0)<='Z')) {
                countApearance( text, counterMap, cityDoc);
                if(!cityDictionary.containsKey(cityDoc)) {
                    CityIndexEntry cityIndexEntry = new CityIndexEntry(null, null, null);
                    cityDictionary.put(cityDoc,cityIndexEntry);
                }
                CityIndexEntry cityIndexEntry=cityDictionary.get(cityDoc);
                if(counterMap.containsKey(cityDoc)) {
                    cityIndexEntry.addDocToMap(doc.getSerialID(), new int[counterMap.get(cityDoc)]);
                }
                else {
                    cityIndexEntry.addDocToMap(doc.getSerialID(), new int[1]);
                    cityIndexEntry.addDocToMap(doc.getSerialID(),-1,1);
                }

                int index=0;
                for (Term term : text) {
                    if(!counterMap.containsKey(cityDoc)) {
                        continue;
                    }
                    String trm = term.toString().toUpperCase();
                    if (trm.equals(cityDoc)){
                        addToCityDictionary(trm , doc.getSerialID() , counterMap ,index);

                    }
                    index++;

                }

            }

        }
        else {
        dumpToDisk();
        }
    }

    private void dumpToDisk() {

        try {
            IntToIntArrayMapOutputStream out = new IntToIntArrayMapOutputStream (path + "\\CitiesPosting");
            Set<String> keySet =new HashSet<>(cityDictionary.keySet());
            for (String key : keySet) {
                if (!cityDictionary.get(key).isInCorpus()) {
                    cityDictionary.remove(key);
                    continue;
                }
                CityIndexEntry cityIndexEntry = cityDictionary.get(key);
                Map<Integer , int[]> docsMap = cityIndexEntry.getDocsMap();
                int pointer = (int) out.write(docsMap);
                cityIndexEntry.setPointer(pointer);
                docsMap.clear();

            }
            out.flush();
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToCityDictionary(String trm, int serialID, Map<String, Integer> counterMap ,int index) {
        CityIndexEntry cityIndexEntry = cityDictionary.get(trm);
        cityIndexEntry.addDocToMap(serialID,index,counterMap.get(trm));
        counterMap.put(trm,counterMap.get(trm)-1);

    }





    private void countApearance( List<Term> text , Map<String , Integer> apearanceMap , String cityDoc){

        for (Term term : text ) {
            String trm= term.toString().toUpperCase();
            if(cityDoc.equals(trm)) {
                if (cityDictionary.containsKey(trm)) {
                    if (!apearanceMap.containsKey(trm)) {
                        apearanceMap.put(trm, 1);
                    } else {
                        apearanceMap.put(trm, new Integer(apearanceMap.get(trm) + 1));
                    }

                }
            }
        }
    }




    private FileNotFoundException getDictionaryFromDisk(){
        FileInputStream fileInputStream = null;
        final String citiesDictionaryName = "rec";
        try {
            fileInputStream = new FileInputStream("resources\\" + citiesDictionaryName);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            cityDictionary= (Map<String, CityIndexEntry>) objectInputStream.readObject();
        } catch (FileNotFoundException e) {
            cityDictionary = new HashMap<>();
        } catch (IOException e) {
            cityDictionary = new HashMap<>();
        } catch (ClassNotFoundException e) {
            cityDictionary = new HashMap<>();
        }
        return null;
    }

    public Map<String, CityIndexEntry> getCityDictionary() {
        return cityDictionary;
    }
}
