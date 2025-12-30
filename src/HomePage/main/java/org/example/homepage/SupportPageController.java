package HomePage.main.java.org.example.homepage;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;

public class SupportPageController {

    @FXML private TextArea detailsArea;
    @FXML private ComboBox<String> questionsCombo;

    @FXML
    public void initialize() {
        questionsCombo.setItems(FXCollections.observableArrayList(
                "I can’t log in",
                "Payment issue",
                "Subscription problem",
                "Bug / app not working", "When my membership expires?", "I am ready to worship our only true god Catulhu!",
                "Other"
        ));

        questionsCombo.getSelectionModel().selectFirst();
        detailsArea.setDisable(true);

        questionsCombo.valueProperty().addListener((obs, oldV, newV) -> {
            boolean isOther = "Other".equals(newV);
            detailsArea.setDisable(!isOther);
            if (!isOther) detailsArea.clear();
        });
    }

    @FXML
    private void handleBack() {
        System.out.println("Back clicked");
    }

    @FXML
    private void handleSend() {
        String selected = questionsCombo.getValue();
        if ("Other".equals(selected)) {
            System.out.println("SEND: Other -> " + detailsArea.getText().trim());
        } else {
            System.out.println("SEND: " + selected);
        }
    }
}