package es.ubu.lsi.ubumonitorlauncher;

import java.io.File;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.ubu.lsi.ubumonitorlauncher.configuration.ConfigHelper;
import es.ubu.lsi.ubumonitorlauncher.controller.DownloadController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Loader extends Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(Loader.class);
	private Stage stage;
	private ResourceBundle resourceBundle;

	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		resourceBundle = ResourceBundle.getBundle("messages/Messages");

		ConfigHelper.initialize(AppInfo.CONFIGURATION_FILE);

		boolean askAgain = ConfigHelper.getProperty("askAgain", true);
		boolean betaTester = ConfigHelper.getProperty("betaTester", false);

		if (askAgain || destFolderIsEmpty(new File(AppInfo.DEFAULT_VERSION_DIR))) {
			primaryStage.initStyle(StageStyle.UNDECORATED);
			primaryStage.centerOnScreen();

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Download.fxml"), resourceBundle);
			primaryStage.setScene(new Scene(loader.load()));
			DownloadController downloadController = loader.getController();

			String lastCheck = ConfigHelper.getProperty("lastUpdateCheck", null);

			LOGGER.info("Last check time {}", lastCheck);
			ZonedDateTime lasCheckZonedDateTime = lastCheck == null ? ZonedDateTime.now(ZoneOffset.UTC)
					: ZonedDateTime.parse(lastCheck);

			downloadController.init(this, ConfigHelper.getProperty("checkUrlUpdate", AppInfo.DEFAULT_CHECK_URL),
					lasCheckZonedDateTime, AppInfo.DEFAULT_VERSION_DIR, askAgain, betaTester);
			ConfigHelper.setProperty(AppInfo.ASK_AGAIN, downloadController.isAskAgain());

		} else {
			executeFile();
			close();
		}

	}

	public void executeFile() {
		try {
			String defaultPath = getDefaultPath();
			File file = new File(
					AppInfo.DEFAULT_VERSION_DIR + ConfigHelper.getProperty("applicationPath", defaultPath));
			
			if (!file.isFile()) {
				file = new File(AppInfo.DEFAULT_VERSION_DIR + defaultPath);
			}
			String[] command = { System.getProperty("java.home") + "/bin/java", "-jar", file.getName() };
			
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Executting command app {}", Arrays.toString(command));
			}

			ProcessBuilder builder = new ProcessBuilder(command).directory(new File(AppInfo.DEFAULT_VERSION_DIR).getAbsoluteFile());

			builder.start();

		} catch (Exception e) {
			LOGGER.error("Cannot execute application", e);
			
		}
	}

	private String getDefaultPath() {
		LOGGER.info("Executtin using the default path");
		File directory = new File(AppInfo.DEFAULT_VERSION_DIR);
		String[] jars = directory.list((dir, name) -> name.matches(AppInfo.PATTERN_FILE));
		if (jars != null && jars.length != 0) {
			return Collections.max(Arrays.asList(jars));
		}
		throw new IllegalStateException("Not found jars in the directory " + AppInfo.DEFAULT_VERSION_DIR);
	}

	public void close() {
		stage.close();

		Platform.exit();
	}

	public static void initialize(String[] args) {
		launch(args);
	}

	public Stage getStage() {
		return stage;
	}

	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	public boolean destFolderIsEmpty(File directory) {

		String[] jars = directory.list((dir, name) -> name.matches(AppInfo.PATTERN_FILE));

		return jars == null || jars.length == 0;

	}

}
