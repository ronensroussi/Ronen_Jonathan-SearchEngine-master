package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Model model = new Model();

        primaryStage.setTitle("¬GOOGLE");
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("View.fxml").openStream());
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);

        View view = fxmlLoader.getController();
        Controller controller = new Controller(model, view);

        model.setController(controller);
        view.setController(controller);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }


}
