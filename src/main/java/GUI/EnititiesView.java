package GUI;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

public class EnititiesView {

    // DICTIONARY_VIEW
    @FXML
    public TableColumn<View.ObservableTuple, String> entities_entCol;
    @FXML
    public TableColumn<View.ObservableTuple, String> entities_rankCol;
    @FXML
    public TableView<View.ObservableTuple> entitites_tbl;


    @FXML
    private void initialize(){
        entities_entCol.setCellValueFactory(cellData -> cellData.getValue().term);
        entities_rankCol.setCellValueFactory(cellData -> cellData.getValue().temInformation);

    }

    public void setTableData(List<View.ObservableTuple> dictionaryAsObservableTuples){

        entitites_tbl.setItems(FXCollections.observableArrayList(dictionaryAsObservableTuples));

    }
}
