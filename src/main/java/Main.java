import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.controllers.InterfaceController;
import ui.controllers.TranscriptionHistoryController;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        UserAgentBuilder.builder()
			.themes(JavaFXThemes.MODENA)
			.themes(MaterialFXStylesheets.forAssemble(true))
			.setDeploy(true)
			.setResolveAssets(true)
			.build()
			.setGlobal();

		FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/Interface.fxml"));
		loader.setControllerFactory(_ -> new InterfaceController(stage));

		stage.setScene(new Scene(loader.load()));		
		stage.setResizable(false);

		stage.setOnCloseRequest(_ -> System.exit(0));

		stage.setTitle("Live Captioning System");
		stage.show();

		Stage transcriptionHistoryStage = new Stage();

		FXMLLoader transcriptionHistoryLoader = new FXMLLoader(Main.class.getResource("/fxml/TranscriptionHistory.fxml"));
		transcriptionHistoryLoader.setControllerFactory(_ -> new TranscriptionHistoryController());

		transcriptionHistoryStage.setScene(new Scene(transcriptionHistoryLoader.load()));
		transcriptionHistoryStage.setResizable(false);

		transcriptionHistoryStage.setOnCloseRequest(_ -> System.exit(0));

		transcriptionHistoryStage.setTitle("Transcription History");
		transcriptionHistoryStage.show();
    }    
}
