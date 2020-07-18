package GUI;

import Indexing.Index.IndexEntry;
import Querying.QueryResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.util.*;

/**
 * MVC controller
 */
public class Controller {

    Model model;
    View view;

    public Controller(Model model, View view) {
        this.model = model;
        this.view = view;
    }

    public void reset() {
        model.reset(view.getOutputLocation().toString());
    }

    public Alert generateIndex(boolean memorySaver) {
        String corpusLocation = view.getCorpusLocation().toString();
        String stopwordsLocation = view.getStopwordsLocation().toString();
        String outputLocation = view.getOutputLocation().toString();
        if(corpusLocation.isEmpty() ) return new Alert(Alert.AlertType.ERROR, "Please specify corpus location.");
        else if (stopwordsLocation.isEmpty()) return new Alert(Alert.AlertType.ERROR, "Please specify stopwords file location.");
        else if (outputLocation.isEmpty()) return new Alert(Alert.AlertType.ERROR, "Please specify output location.");
        else{
            try {

                String information;
                if(memorySaver){
                    information = model.generateIndexTwoPhase(view.isUseStemming() , corpusLocation, outputLocation, stopwordsLocation );
                }
                else
                    information = model.generateIndex(view.isUseStemming() , corpusLocation, outputLocation, stopwordsLocation );


                ObservableList<String> languages = FXCollections.observableArrayList(model.getLanguages());
                view.setLanguages(languages);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, information);
                alert.setHeaderText("Index Generated!");
                return alert;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return new Alert(Alert.AlertType.ERROR, "IO error. Please check the paths and try again.");
            }
            catch (Exception e){
                e.printStackTrace();
                return new Alert(Alert.AlertType.ERROR, "Fatal error encountered during index generation: " + e.getMessage());
            }
        }
    }


    public List<View.ObservableTuple> getDictionary() {
        Map<String, IndexEntry> dictionary = model.getDictionary(view.isUseStemming());
        List<View.ObservableTuple> res = new ArrayList<>();
        if(null == dictionary){
           return res;
        }
        else{
            Object[] keysAsObject = (dictionary.keySet().toArray());
            String[] keys = new String[keysAsObject.length];
            for (int i = 0; i < keysAsObject.length ; i++) {
                keys[i] = (String) keysAsObject[i];
            }

            Object[] values = dictionary.values().toArray();
            String[] valuesAsStrings = new String[values.length];
            for (int i = 0; i < values.length ; i++) {
                valuesAsStrings[i] = values[i].toString();
            }

            for (int i = 0; i < keys.length ; i++) {
                res.add(new View.ObservableTuple(new SimpleStringProperty(keys[i]), new SimpleStringProperty(valuesAsStrings[i])));
            }

            ObservableList<String> languages = FXCollections.observableArrayList(model.getLanguages());
            view.setLanguages(languages);

            return res;
        }
    }

    public Alert loadDictionary() {
        String outputLocation = view.getOutputLocation().toString();

        if(outputLocation.isEmpty() ) return new Alert(Alert.AlertType.ERROR, "Please specify output folder location.");
        try {
            model.loadDictionary(view.isUseStemming(), outputLocation);
        } catch (Exception e) {
            e.printStackTrace();
            return new Alert(Alert.AlertType.ERROR, "No valid dictionary file found in the given output folder.");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Dictionary loaded successfully.");
        alert.setHeaderText("Dictionary Loaded");

        ObservableList<String> languages = FXCollections.observableArrayList(model.getLanguages());
        view.setLanguages(languages);

        return alert;
    }

    /**
     * this method runs a function on the model that retrieve  a set with all of the cites in the corpus
     * @return
     */
    public Set<String> getAllCities(){
      return  model.getAllCities();
    }

    public List<QueryResult> aswerSingelQuery(String query , Set<String> citiesFilter , boolean useSemantic , boolean isUsedStemming , String pathToOutpotFolder){
        return model.aswerSingelQuery(query,citiesFilter,useSemantic,isUsedStemming ,pathToOutpotFolder);
    }

    public List<QueryResult> answerMultipleQueries(String pathToQueryFile , Set<String> citiesFilter , boolean useSemantic , boolean isUsedStemming, String pathToOutpotFolder){
        return model.answerMultipleQueries(pathToQueryFile,citiesFilter,useSemantic,isUsedStemming,pathToOutpotFolder);
    }

    public void saveQueryResults(String resultOutputPath , List<QueryResult> results){
        model.saveQueryResults(resultOutputPath , results);
    }
}
