package es.ubu.lsi.ubumonitorlauncher.controller;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
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
import es.ubu.lsi.ubumonitorlauncher.model.DownloadInfo;
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

	private Service<DownloadInfo> createGetDownloadUrlService() {
		Service<DownloadInfo> service = new Service<DownloadInfo>() {

			@Override
			protected Task<DownloadInfo> createTask() {
				return new Task<DownloadInfo>() {

					@Override
					protected DownloadInfo call() throws Exception {
						String url = null;

						// Check in the online configuration file what server download the files, GitHub
						// or own server
						LOGGER.info("Connecting to {}", checkUrl);
						try (Response response = Connection.getResponse(checkUrl)) {
							JSONObject jsonObject = new JSONObject(response.body()
									.string());
							LOGGER.info("Response server check url: {}", jsonObject);
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

			DownloadInfo downloadInfo = service.getValue();
			LOGGER.info("{}", downloadInfo);
			if (downloadInfo != null && (versionsFiles.isEmpty() || userConfirmation(downloadInfo.getReleaseName(),
					downloadInfo.getReleaseDescription(), downloadInfo.isBeta()))) {
				ConfigHelper.setProperty(AppInfo.LAST_UPDATE_DOWNLOAD, downloadInfo.getUpdatedAt());
				new File(versionDir).mkdirs();
				label.setText(MessageFormat.format(label.getText(), downloadInfo.getReleaseName()));
				loader.getStage()
						.show();
				LOGGER.info("Downloading version {} in {}", downloadInfo.getDownloadUrl(), downloadInfo.getFileName());
				createDownloadService(downloadInfo.getDownloadUrl(), versionDir + downloadInfo.getFileName()).start();

			} else {
				onFinalize();
			}
		});
		service.setOnFailed(e -> onFinalize(e.getSource()
				.getException()));
		return service;
	}

	private boolean userConfirmation(String version, String body, boolean beta) {
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
			boolean isConfirmed = downloadConfirmationController.isUserConfirmed(version, body, beta);
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
	 * @param lastUpdate    time last checked for updates
	 * @param versionsFiles files in the destination forlder
	 * @return list with 3 values ( downloadUrl, filename and body of the release)
	 *         or empty list in case of the conditions is false
	 * @throws IOException
	 */
	public DownloadInfo getDownloadFile(String url, Pattern patternFile, ZonedDateTime lastUpdate) throws IOException {

		try (Response response = Connection.getResponse(url)) {
			JSONArray jsonArray = new JSONArray(response.body()
					.string());

			for (int i = 0; i < jsonArray.length(); ++i) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				if (betaTester || !jsonObject.optBoolean("prerelease", false)) {
					JSONArray assets = jsonObject.getJSONArray("assets");
					DownloadInfo info = getAsset(patternFile, lastUpdate, jsonObject, assets);
					if (info != null) {
						return info;
					}
				}

			}
		}

		return null;

	}

	private DownloadInfo getAsset(Pattern patternFile, ZonedDateTime lastUpdate, JSONObject jsonObject,
			JSONArray assets) {
		for (int j = 0; j < assets.length(); ++j) {
			JSONObject asset = assets.getJSONObject(j);
			String fileName = asset.getString("name");
			ZonedDateTime updatedAt = ZonedDateTime.parse(asset.getString("updated_at"));
			// file matches name
			LOGGER.info("Filename:{}", fileName);
			LOGGER.info("Updated at: {}", updatedAt);
			LOGGER.info("lastDownload {}", lastUpdate);
			if (patternFile.matcher(fileName)
					.matches()) {
				// lastcheck is before the last update
				if (updatedAt.isAfter(lastUpdate)) {
					DownloadInfo downloadInfo = new DownloadInfo();
					downloadInfo.setReleaseName(jsonObject.getString("name"));
					downloadInfo.setFileName(fileName);
					downloadInfo.setDownloadUrl(asset.getString("browser_download_url"));
					downloadInfo.setUpdatedAt(updatedAt);
					downloadInfo.setReleaseDescription(jsonObject.getString("body"));
					downloadInfo.setBeta(jsonObject.getBoolean("prerelease"));
					return downloadInfo;
				}
				return null;

			}
		}
		return null;
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
			ConfigHelper.setProperty("betaTester", betaTester);
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
