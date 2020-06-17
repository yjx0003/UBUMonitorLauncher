package es.ubu.lsi.ubumonitorlauncher.controller;


import java.text.MessageFormat;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.StageStyle;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class DownloadConfirmationController {
	@FXML
	private DialogPane dialogPane;
	@FXML
	private Label label;
	@FXML
	private CheckBox checkBox;

	public boolean isUserConfirmed(String version, String body) {
		TextArea textArea = new TextArea(body);
		textArea.setWrapText(true);
		textArea.setEditable(false);
		dialogPane.setExpandableContent(textArea);
		label.setText(MessageFormat.format(label.getText(), version));
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.initStyle(StageStyle.UNDECORATED);
		alert.setDialogPane(dialogPane);
		Optional<ButtonType> buttonType = alert.showAndWait();
		
		return buttonType.isPresent() && buttonType.get() == ButtonType.OK;
	}
	
	public boolean askAgain() {
		return !checkBox.isSelected();
	}

}
