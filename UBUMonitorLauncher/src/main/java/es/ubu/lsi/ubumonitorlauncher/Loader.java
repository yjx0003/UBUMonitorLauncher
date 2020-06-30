package es.ubu.lsi.ubumonitorlauncher;

import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.ubu.lsi.ubumonitorlauncher.configuration.ConfigHelper;
import es.ubu.lsi.ubumonitorlauncher.controller.DownloadController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Load resources as internationalizarion messages and the configuration file. Download and load the main application.
 * @author Yi Peng Ji
 *
 */
public class Loader extends Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(Loader.class);
	private Stage stage;
	private ResourceBundle resourceBundle;

	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		resourceBundle = ResourceBundle.getBundle("messages/Messages");

		ConfigHelper.initialize(AppInfo.CONFIGURATION_FILE);

		boolean askAgain = ConfigHelper.getProperty(AppInfo.ASK_AGAIN, true);
		boolean betaTester = ConfigHelper.getProperty(AppInfo.BETA_TESTER, true);

		if (askAgain || destFolderIsEmpty(new File(AppInfo.DEFAULT_VERSION_DIR))) {
			primaryStage.initStyle(StageStyle.UNDECORATED); 
			primaryStage.centerOnScreen();

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/Download.fxml"), resourceBundle);
			primaryStage.setScene(new Scene(loader.load()));
			primaryStage.setTitle(resourceBundle.getString("label.updateavailable"));
			primaryStage.getIcons()
					.add(new Image("/img/download.png"));
			DownloadController downloadController = loader.getController();

			String lastCheck = ConfigHelper.getProperty(AppInfo.LAST_UPDATE_DOWNLOAD, null);

			LOGGER.info("Last check time {}", lastCheck);
			ZonedDateTime lasCheckZonedDateTime = lastCheck == null ? Instant.EPOCH.atZone(ZoneOffset.UTC)
					: ZonedDateTime.parse(lastCheck);

			downloadController.init(this, ConfigHelper.getProperty("checkUrlUpdate", AppInfo.DEFAULT_CHECK_URL),
					lasCheckZonedDateTime, AppInfo.DEFAULT_VERSION_DIR, askAgain, betaTester);
			ConfigHelper.setProperty(AppInfo.ASK_AGAIN, downloadController.isAskAgain());

		} else {
			executeFile(convertJSONArrayToList(ConfigHelper.getArray(AppInfo.VM_ARGS)),
					convertJSONArrayToList(ConfigHelper.getArray(AppInfo.ARGS)));
			close();
		}

	}

	public void executeFile(List<String> vmArgs, List<String> args) {
		try {
			String defaultPath = getDefaultPath();
			File file = new File(
					AppInfo.DEFAULT_VERSION_DIR + ConfigHelper.getProperty("applicationPath", defaultPath));

			if (!file.isFile()) {
				file = new File(AppInfo.DEFAULT_VERSION_DIR + defaultPath);
			}
			List<String> command = new ArrayList<>();
			command.add(System.getProperty("java.home") + "/bin/java");
			if (vmArgs != null && !vmArgs.isEmpty()) {
				command.addAll(vmArgs);
			}
			command.add("-jar");
			command.add(file.getName());
			if (args != null && !args.isEmpty()) {
				command.addAll(args);
			}
			LOGGER.info("Executting command app {}", command);

			ProcessBuilder builder = new ProcessBuilder(command)
					.directory(new File(AppInfo.DEFAULT_VERSION_DIR).getAbsoluteFile());

			builder.start();

		} catch (Exception e) {
			LOGGER.error("Cannot execute application", e);

		}
	}

	private String getDefaultPath() {
		LOGGER.info("Executting using the default path");
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

	public static List<String> convertJSONArrayToList(JSONArray optJSONArray) {
		if (optJSONArray == null) {
			return Collections.emptyList();

		}

		List<String> list = new ArrayList<>(optJSONArray.length());
		for (int i = 0; i < optJSONArray.length(); ++i) {
			list.add(optJSONArray.getString(i));
		}
		return list;
	}

}
