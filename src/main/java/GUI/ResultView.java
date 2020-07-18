package GUI;

import Querying.QueryResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.omg.CORBA.PUBLIC_MEMBER;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.ResourceBundle;

public class ResultView  {

    @FXML
    public TableColumn<SearchView.ObservableCell, String> result_docsCol;
    @FXML
    public TableColumn<SearchView.ObservableCell, String> result_queryCol;
    @FXML
    public TableView<SearchView.ObservableCell> result_Querytbl;
    @FXML
    public TableView<SearchView.ObservableCell> result_Docstbl;

    public Button result_viewEntities;

    private List<QueryResult> result;


    @FXML
    public void initialize() {
        result_queryCol.setCellValueFactory(cellData -> cellData.getValue().data);
        result_Querytbl.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            QueryResult res = result.get(result_Querytbl.getSelectionModel().selectedIndexProperty().getValue());
            List<SearchView.ObservableCell> listOfdocs=new ArrayList<>();
            for (String s : res.getRelevantDocs()) {
                listOfdocs.add(new SearchView.ObservableCell(new SimpleStringProperty(s)));
            }
            result_Docstbl.setItems(FXCollections.observableArrayList(listOfdocs));
            result_docsCol.setCellValueFactory(cellData -> cellData.getValue().data);
        });

    }


    public void setTableData(List<SearchView.ObservableCell> queryList){
        result_Querytbl.setItems(FXCollections.observableArrayList(queryList));
        result_Querytbl.getSelectionModel().selectFirst();
    }


    public void onViewEntitiesClicket()
    {
        if(result_Docstbl.getSelectionModel().selectedIndexProperty().getValue()  <0){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("No doc selected");
            alert.setContentText("Please choose a document");
            alert.showAndWait();
    }
    else {
            int Queryindex = result_Querytbl.getSelectionModel().selectedIndexProperty().getValue() ;
            int docindex= result_Docstbl.getSelectionModel().selectedIndexProperty().getValue();
            String [] entites = result.get(Queryindex).getEntities().get(docindex);
            float [] rank = result.get(Queryindex).getEntRanking().get(docindex);

            List<View.ObservableTuple> entieisList= new ArrayList<>();

            if (entites.length>0) {
                for (int i = 0; i < entites.length; i++) {
                    entieisList.add(new View.ObservableTuple(new SimpleStringProperty(entites[i]), new SimpleStringProperty(String.valueOf(rank[i]))));
                }
            }else {
                entieisList.add(new View.ObservableTuple(new SimpleStringProperty(""),new SimpleStringProperty("")));
            }


            Stage stage = new Stage();
            stage.setTitle("Results");
            FXMLLoader fxmlLoader = new FXMLLoader();
            Parent root = null;
            try {
                root = fxmlLoader.load(getClass().getResource("EntitiesView.fxml").openStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Scene scene = new Scene(root, 400, 258);
            stage.setScene(scene);
            stage.setMaxWidth(400);
            stage.setMaxHeight(258);
            stage.setMinWidth(400);
            stage.setMinHeight(258);

            EnititiesView enititiesView = fxmlLoader.getController();
            enititiesView.setTableData(entieisList);
            stage.show();

        }


    }
    //public void

    public void setResult(List<QueryResult> result) {
        this.result = result;
    }


    public List<QueryResult> getResult() {
        return result;
    }

}
