package es.ubu.lsi.ubumonitorlauncher.controller;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class DownloadConfirmationController implements Initializable {

	@FXML
	private DialogPane dialogPane;
	@FXML
	private Label label;
	@FXML
	private CheckBox checkBox;
	private ResourceBundle resourceBundle;

	public boolean isUserConfirmed(String version, String body) {
		TextArea textArea = new TextArea(body);
		textArea.setWrapText(true);
		textArea.setEditable(false);
		dialogPane.setExpandableContent(textArea);
		label.setText(MessageFormat.format(label.getText(), version));
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(resourceBundle.getString("label.updateavailable"));
		Stage stageAlert = (Stage) alert.getDialogPane()
				.getScene()
				.getWindow();
		stageAlert.getIcons()
				.add(new Image("/img/download.png"));
		alert.setDialogPane(dialogPane);
		Optional<ButtonType> buttonType = alert.showAndWait();

		return buttonType.isPresent() && buttonType.get() == ButtonType.OK;
	}

	public boolean askAgain() {
		return !checkBox.isSelected();

	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		resourceBundle = arg1;

	}

}
