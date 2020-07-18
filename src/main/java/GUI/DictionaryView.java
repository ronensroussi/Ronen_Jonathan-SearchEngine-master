package GUI;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

/**
 * displays a dictionary in a table.
 */
public class DictionaryView {

    // DICTIONARY_VIEW
    @FXML
    public TableColumn<View.ObservableTuple, String> clmn_terms;
    @FXML
    public TableColumn<View.ObservableTuple, String> clmn_term_information;
    @FXML
    public TableView<View.ObservableTuple> tbl_dictionary;


    @FXML
    private void initialize(){
        clmn_terms.setCellValueFactory(cellData -> cellData.getValue().term);
        clmn_term_information.setCellValueFactory(cellData -> cellData.getValue().temInformation);

    }

    public void setTableData(List<View.ObservableTuple> dictionaryAsObservableTuples){
        clmn_terms.setSortType(TableColumn.SortType.ASCENDING);
        tbl_dictionary.setItems(FXCollections.observableArrayList(dictionaryAsObservableTuples));
        tbl_dictionary.getSortOrder().add(clmn_terms);
        //      stage.initModality(Modality.APPLICATION_MODAL); //Lock the window until it closes
    }


}
