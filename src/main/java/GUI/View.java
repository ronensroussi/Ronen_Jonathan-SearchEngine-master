package GUI;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

/**
 * MVC view
 */
public class View {

    private Controller controller;
    @FXML
    public TextField txtfld_corpus_location;
    @FXML
    public TextField txtfld_stopwords_location;
    @FXML
    public TextField txtfld_output_location;

    public Button btn_corpus_browse;
    public Button btn_stopwords_browse;
    public Button btn_output_browse;
    public Button btn_reset;
    public Button btn_load_dictionary;
    public Button btn_display_dictionary;
    public Button view_search;

    public ChoiceBox choiceBox_languages;


    public CheckBox chkbox_use_stemming;
    public CheckBox chkbox_memory_saver;

    public BorderPane root_pane;

    @FXML
    private void initialize(){
        Tooltip tooltip = new Tooltip();
        tooltip.setText("Checking this will prioritize using less system memory, but greatly increase runtime");
        chkbox_memory_saver.setTooltip(tooltip);
        view_search.setDisable(true);
    }

    public CharSequence getOutputLocation(){
        return txtfld_output_location.getCharacters();
    }

    public CharSequence getCorpusLocation() {
        return txtfld_corpus_location.getCharacters();
    }

    public boolean isUseStemming() {
        return chkbox_use_stemming.isSelected();
    }

    public void browseCorpusLocation(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Corpus Location");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File corpusDir = directoryChooser.showDialog( btn_corpus_browse.getScene().getWindow());
        if(null != corpusDir){ //directory chosen
            txtfld_corpus_location.textProperty().setValue(corpusDir.getAbsolutePath());
        }
    }

    public void browseOutputLocation(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Output Location");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File corpusDir = directoryChooser.showDialog( btn_output_browse.getScene().getWindow());
        if(null != corpusDir){ //directory chosen
            txtfld_output_location.setText(corpusDir.getAbsolutePath());
        }
    }

    public void browseStopwordsLocation(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Output Location");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
        File corpusDir = fileChooser.showOpenDialog( btn_stopwords_browse.getScene().getWindow());
        if(null != corpusDir){ //directory chosen
            txtfld_stopwords_location.textProperty().setValue(corpusDir.getAbsolutePath());
        }
    }

    public void reset(ActionEvent actionEvent) {
        btn_display_dictionary.setDisable(true);
        choiceBox_languages.setDisable(true);
        controller.reset();
    }

    public void generateIndex(ActionEvent actionEvent) {
        Alert generatingAlert = new Alert(Alert.AlertType.INFORMATION, "Generating Index");
        generatingAlert.setHeaderText("Generating Index");
        generatingAlert.setTitle("Generating Index");
        generatingAlert.show();

        Alert alert = controller.generateIndex(chkbox_memory_saver.isSelected());

        generatingAlert.close();

        handleNewDictionary(alert);
    }

    public void displayDictionary(ActionEvent actionEvent) {
        Stage stage = new Stage();
        stage.setTitle("Dictionary");
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = null;
        try {
            root = fxmlLoader.load(getClass().getResource("DictionaryView.fxml").openStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scene scene = new Scene(root, 400, 700);
        stage.setScene(scene);

        DictionaryView dictionaryView = fxmlLoader.getController();
        dictionaryView.setTableData(controller.getDictionary());

        stage.show();
    }

    public CharSequence getStopwordsLocation() {
        return txtfld_stopwords_location.getCharacters();
    }

    public void setLanguages(ObservableList languages) {
        choiceBox_languages.setDisable(false);
        choiceBox_languages.setItems(languages);
    }


    static class ObservableTuple{
        StringProperty term;
        StringProperty temInformation;

        public ObservableTuple(StringProperty term, StringProperty temInformation) {
            this.term = term;
            this.temInformation = temInformation;
        }
    }

    public void loadDictionary(ActionEvent actionEvent) {
        Alert result = controller.loadDictionary();
        handleNewDictionary(result);
    }

    private void handleNewDictionary(Alert result) {
        if(result.getAlertType() == Alert.AlertType.ERROR){
            result.showAndWait();
        }
        else{
            result.show();
            btn_reset.setDisable(false);
            btn_display_dictionary.setDisable(false);
            view_search.setDisable(false);
        }
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }


    public void onSearchButtonClicked(){
        try{

//            FXMLLoader loader = new FXMLLoader();
//            AnchorPane pane = loader.load(getClass().getResource("SearchView.fxml").openStream());
//            root_pane.getChildren().setAll(pane);
//            SearchView searchView = loader.getController();
//            searchView.setController(controller);
//            searchView.setUseStemming(chkbox_use_stemming.isSelected());
//            searchView.setpathToOutpotFolder(txtfld_output_location.getText());
//            txtfld_output_location.clear();
//            searchView.setView();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("SearchView.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene((Pane) loader.load()));
            SearchView searchView = loader.<SearchView>getController();
            searchView.setController(controller);
            searchView.setUseStemming(chkbox_use_stemming.isSelected());
            searchView.setpathToOutpotFolder(txtfld_output_location.getText());
            searchView.setView();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

