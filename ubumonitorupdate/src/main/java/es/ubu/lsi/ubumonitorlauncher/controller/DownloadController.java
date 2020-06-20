package es.ubu.lsi.ubumonitorlauncher.controller;

import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import es.ubu.lsi.ubumonitorlauncher.AppInfo;
import es.ubu.lsi.ubumonitorlauncher.Loader;
import es.ubu.lsi.ubumonitorlauncher.configuration.ConfigHelper;
import es.ubu.lsi.ubumonitorlauncher.connection.Connection;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class DownloadController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadController.class);

	@FXML
	private Label label;

	@FXML
	private ProgressIndicator progress;

	@FXML
	private GridPane gridPane;

	private Loader loader;
	private Pattern patternFile;
	private ZonedDateTime lastChecked;
	private String checkUrl;
	private String versionDir;
	private Set<String> versionsFiles;
	private ResourceBundle resourceBundle;
	private boolean askAgain;
	private boolean betaTester;
	private List<String> vmArgs;
	private List<String> args;

	private double xOffset = 0;
	private double yOffset = 0;

	public void init(Loader loader, String checkUrl, ZonedDateTime lastChecked, String versionDir, boolean askAgain,
			boolean betaTester) {
		this.askAgain = askAgain;
		this.betaTester = betaTester;
		this.loader = loader;
		this.resourceBundle = loader.getResourceBundle();
		this.lastChecked = lastChecked;
		this.checkUrl = checkUrl;
		this.versionDir = versionDir;
		String[] versionDirectory = new File(versionDir).list((dir, name) -> name.matches(AppInfo.PATTERN_FILE));
		versionsFiles = versionDirectory == null ? Collections.emptySet()
				: new HashSet<>(Arrays.asList(versionDirectory));
		setMovableNode(gridPane, loader.getStage());

		createGetDownloadUrlService().start();

	}

	private void setMovableNode(Node node, Stage stage) {
		node.setOnMousePressed(event -> {
			xOffset = event.getSceneX();
			yOffset = event.getSceneY();
		});

		node.setOnMouseDragged(event -> {
			stage.setX(event.getScreenX() - xOffset);
			stage.setY(event.getScreenY() - yOffset);
		});

	}

	private Service<List<String>> createGetDownloadUrlService() {
		Service<List<String>> service = new Service<List<String>>() {

			@Override
			protected Task<List<String>> createTask() {
				return new Task<List<String>>() {

					@Override
					protected List<String> call() throws Exception {
						String url = null;

						// Check in the online configuration file what server download the files, GitHub
						// or own server
						LOGGER.info("Connecting to {}", checkUrl);
						try (Response response = Connection.getResponse(checkUrl)) {
							JSONObject jsonObject = new JSONObject(response.body()
									.string());
							LOGGER.info("Response server check url: {}", jsonObject.toString(4));
							url = jsonObject.getString("checkUrl");

							patternFile = Pattern.compile(jsonObject.getString("pattern"));
							vmArgs = Loader.convertJSONArrayToList(jsonObject.optJSONArray(AppInfo.VM_ARGS));
							args = Loader.convertJSONArrayToList(jsonObject.optJSONArray(AppInfo.ARGS));

						}
						return getDownloadFile(url, patternFile, lastChecked);

					}

				};
			}
		};
		service.setOnSucceeded(e -> {

			List<String> list = service.getValue();
			LOGGER.info("{}", list);
			if (!list.isEmpty() && (versionsFiles.isEmpty() || userConfirmation(list.get(1), list.get(2)))) {
				ConfigHelper.setProperty("lastUpdateCheck", ZonedDateTime.now(ZoneOffset.UTC));
				new File(versionDir).mkdirs();
				loader.getStage()
						.show();
				LOGGER.info("Downloading version {} in {}", list.get(0), list.get(1));
				createDownloadService(list.get(0), versionDir + list.get(1)).start();

			} else {
				onFinalize();
			}
		});
		service.setOnFailed(e -> onFinalize(e.getSource()
				.getException()));
		return service;
	}

	private boolean userConfirmation(String version, String body) {
		if (askAgain) {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/DownloadConfirmation.fxml"),
					resourceBundle);
			try {
				fxmlLoader.load();
			} catch (IOException e) {
				LOGGER.error("cannot load /view/DownloadConfirmation.fxml", e);
				return false;
			}
			DownloadConfirmationController downloadConfirmationController = fxmlLoader.getController();
			boolean isConfirmed = downloadConfirmationController.isUserConfirmed(version, body);
			askAgain = downloadConfirmationController.askAgain();
			ConfigHelper.setProperty("askAgain", askAgain);
			LOGGER.info("Ask again {}", downloadConfirmationController.askAgain());
			return isConfirmed;

		}
		return false;

	}

	/**
	 * Get downdload info as string
	 * 
	 * @param url           information for download files
	 * @param patternFile   pattern matches filename
	 * @param lastChecked   time last checked for updates
	 * @param versionsFiles files in the destination forlder
	 * @return list with 3 values ( downloadUrl, filename and body of the release)
	 *         or empty list in case of the conditions is false
	 * @throws IOException
	 */
	public List<String> getDownloadFile(String url, Pattern patternFile, ZonedDateTime lastChecked) throws IOException {

		try (Response response = Connection.getResponse(url)) {
			JSONObject jsonObject = new JSONObject(response.body()
					.string());

			if (betaTester || !jsonObject.optBoolean("prerelease", false)) {
				JSONArray jsonArray = jsonObject.getJSONArray("assets");
				for (int i = 0; i < jsonArray.length(); ++i) {
					JSONObject asset = jsonArray.getJSONObject(i);
					String fileName = asset.getString("name");
					// file matches name
					if (patternFile.matcher(fileName)
							.matches()
							// lastcheck is before the last update
							&& (ZonedDateTime.parse(asset.getString("updated_at"))
									.isAfter(lastChecked) || versionsFiles.isEmpty())) {

						return Arrays.asList(asset.getString("browser_download_url"), fileName,
								jsonObject.getString("body"));
					}

				}
			}
		}
		return Collections.emptyList();
	}

	public Pattern getPatternFile() {
		return patternFile;
	}

	/**
	 * Download content of the url and save in file dest
	 * 
	 * @param downloadUrl file to download
	 * @param fileDest    path where save the file
	 * @return null
	 */
	private Service<Void> createDownloadService(String downloadUrl, String fileDest) {
		File file = new File(fileDest);
		Service<Void> service = new Service<Void>() {

			@Override
			protected Task<Void> createTask() {
				return new Task<Void>() {

					@Override
					protected Void call() throws Exception {

						try (Response response = Connection.getResponse(downloadUrl);
								BufferedSink sink = Okio.buffer(Okio.sink(file))) {
							sink.writeAll(response.body()
									.source());

						}

						return null;
					}
				};
			}

		};
		service.setOnSucceeded(e -> {
			ConfigHelper.setProperty("applicationPath", file.getName());
			ConfigHelper.setProperty("vmArgs", new JSONArray(vmArgs));
			ConfigHelper.setProperty("args", new JSONArray(args));
			onFinalize();
		});

		service.setOnFailed(e -> onFinalize(e.getSource()
				.getException()));

		return service;
	}

	private void onFinalize(Throwable exception) {
		LOGGER.error("Error:", exception);
		onFinalize();
	}

	private void onFinalize() {
		ConfigHelper.save();
		loader.executeFile(Loader.convertJSONArrayToList(ConfigHelper.getArray(AppInfo.VM_ARGS)),
				Loader.convertJSONArrayToList(ConfigHelper.getArray(AppInfo.ARGS)));
		loader.close();
	}

	public boolean isAskAgain() {
		return askAgain;
	}

	/**
	 * @return the vmArgs
	 */
	public List<String> getVmArgs() {
		return vmArgs;
	}

	/**
	 * @return the args
	 */
	public List<String> getArgs() {
		return args;
	}

	

}
