package es.ubu.lsi.ubumonitorlauncher.controller;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.web.WebView;
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
		
		WebView webView = new WebView();
		webView.getEngine().loadContent(parseMarkDown(body), "text/html");
		dialogPane.setExpandableContent(webView);
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
	
	private String parseMarkDown(String markDown) {
		Parser parser = Parser.builder().build();
		Node document = parser.parse(markDown);
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		return renderer.render(document); 
	}

	public boolean askAgain() {
		return !checkBox.isSelected();

	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		resourceBundle = arg1;

	}

}
