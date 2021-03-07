package com.github.engatec.vdl.controller;

import java.nio.file.Files;
import java.util.List;

import com.github.engatec.vdl.controller.components.DownloadableItemsComponentController;
import com.github.engatec.vdl.core.ApplicationContext;
import com.github.engatec.vdl.core.I18n;
import com.github.engatec.vdl.core.UiComponent;
import com.github.engatec.vdl.core.UiManager;
import com.github.engatec.vdl.core.UpdateManager;
import com.github.engatec.vdl.core.command.DownloadCommand;
import com.github.engatec.vdl.core.handler.CopyUrlFromClipboardOnFocusChangeListener;
import com.github.engatec.vdl.core.preferences.ConfigManager;
import com.github.engatec.vdl.model.Language;
import com.github.engatec.vdl.model.downloadable.CustomFormatDownloadable;
import com.github.engatec.vdl.model.downloadable.Downloadable;
import com.github.engatec.vdl.model.downloadable.MultiFormatDownloadable;
import com.github.engatec.vdl.model.preferences.general.AutoDownloadConfigItem;
import com.github.engatec.vdl.model.preferences.general.AutoDownloadFormatConfigItem;
import com.github.engatec.vdl.model.preferences.general.LanguageConfigItem;
import com.github.engatec.vdl.model.preferences.general.SkipDownloadableDetailsSearchConfigItem;
import com.github.engatec.vdl.stage.AboutStage;
import com.github.engatec.vdl.stage.PreferencesStage;
import com.github.engatec.vdl.stage.QueueStage;
import com.github.engatec.vdl.ui.Dialogs;
import com.github.engatec.vdl.util.AppUtils;
import com.github.engatec.vdl.worker.service.DownloadableSearchService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainController extends StageAwareController {

    private static final Logger LOGGER = LogManager.getLogger(MainController.class);

    private final ApplicationContext appCtx = ApplicationContext.INSTANCE;
    private final ConfigManager cfgMgr = ConfigManager.INSTANCE;
    private final DownloadableSearchService downloadableSearchService = new DownloadableSearchService();

    @FXML private VBox rootControlVBox;

    @FXML private ScrollPane contentScrollPane;

    @FXML private Menu fileMenu;
    @FXML private MenuItem downloadQueueMenuItem;
    @FXML private MenuItem preferencesMenuItem;
    @FXML private MenuItem exitMenuItem;

    @FXML private Menu languageMenu;
    @FXML private MenuItem langEnMenuItem;
    @FXML private MenuItem langRuMenuItem;
    @FXML private MenuItem langUkMenuItem;

    @FXML private Menu helpMenu;
    @FXML private MenuItem checkYoutubeDlUpdatesMenuItem;
    @FXML private MenuItem aboutMenuItem;

    @FXML private TextField videoUrlTextField;
    @FXML private Button searchBtn;
    @FXML private ProgressIndicator searchProgressIndicator;

    private MainController() {
    }

    public MainController(Stage stage) {
        super(stage);
    }

    @FXML
    public void initialize() {
        initLocaleBindings();
        initSearchBindings();
        initMenuItems();
        initDragAndDrop();

        stage.focusedProperty().addListener(new CopyUrlFromClipboardOnFocusChangeListener(videoUrlTextField, searchBtn));
    }

    private void initLocaleBindings() {
        I18n.bindLocaleProperty(fileMenu.textProperty(), "menu.file");
        I18n.bindLocaleProperty(downloadQueueMenuItem.textProperty(), "menu.file.queue");
        I18n.bindLocaleProperty(preferencesMenuItem.textProperty(), "menu.file.preferences");
        I18n.bindLocaleProperty(exitMenuItem.textProperty(), "menu.file.exit");
        I18n.bindLocaleProperty(languageMenu.textProperty(), "menu.language");
        I18n.bindLocaleProperty(helpMenu.textProperty(), "menu.help");
        I18n.bindLocaleProperty(checkYoutubeDlUpdatesMenuItem.textProperty(), "menu.help.update.youtubedl");
        I18n.bindLocaleProperty(aboutMenuItem.textProperty(), "menu.help.about");
        I18n.bindLocaleProperty(searchBtn.textProperty(), "search");
    }

    private void initSearchBindings() {
        searchBtn.managedProperty().bind(searchProgressIndicator.visibleProperty().not());
        searchBtn.visibleProperty().bind(searchProgressIndicator.visibleProperty().not());
        searchProgressIndicator.prefHeightProperty().bind(searchBtn.heightProperty());
        searchProgressIndicator.prefWidthProperty().bind(searchBtn.widthProperty());
        searchProgressIndicator.managedProperty().bind(searchProgressIndicator.visibleProperty());
        searchProgressIndicator.setVisible(false);
        searchBtn.setOnAction(this::handleSearchEvent);
        videoUrlTextField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSearchEvent(event);
            }
        });
    }

    private void initMenuItems() {
        exitMenuItem.setOnAction(this::handleExitMenuItemClick);
        preferencesMenuItem.setOnAction(this::handlePreferencesMenuItemClick);
        checkYoutubeDlUpdatesMenuItem.setOnAction(this::handleYoutubeDlUpdatesMenuItemClick);
        aboutMenuItem.setOnAction(this::handleAboutMenuItemClick);
        downloadQueueMenuItem.setOnAction(this::handleDownloadQueueMenuItemClick);

        langEnMenuItem.setOnAction(event -> handleLanguageChange(event, Language.ENGLISH));
        langRuMenuItem.setOnAction(event -> handleLanguageChange(event, Language.RUSSIAN));
        langUkMenuItem.setOnAction(event -> handleLanguageChange(event, Language.UKRAINIAN));
    }

    private void handleExitMenuItemClick(ActionEvent event) {
        event.consume();
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void handlePreferencesMenuItemClick(ActionEvent event) {
        new PreferencesStage().modal(stage).showAndWait();
        event.consume();
    }

    private void handleLanguageChange(ActionEvent event, Language language) {
        appCtx.setLanguage(language);
        cfgMgr.setValue(new LanguageConfigItem(), language.getLocaleLanguage());
        event.consume();
    }

    private void handleYoutubeDlUpdatesMenuItemClick(ActionEvent event) {
        if (Files.isWritable(appCtx.getYoutubeDlPath())) {
            UpdateManager.updateYoutubeDl(stage);
        } else {
            Dialogs.error("update.youtubedl.nopermissions");
        }
        event.consume();
    }

    private void handleAboutMenuItemClick(ActionEvent event) {
        new AboutStage().modal(stage).showAndWait();
        event.consume();
    }

    private void handleDownloadQueueMenuItemClick(ActionEvent event) {
        new QueueStage().modal(stage).showAndWait();
        event.consume();
    }

    private void handleSearchEvent(Event event) {
        contentScrollPane.setContent(null);

        boolean autodownloadEnabled = cfgMgr.getValue(new AutoDownloadConfigItem());
        boolean skipDownloadableDetailsSearch = cfgMgr.getValue(new SkipDownloadableDetailsSearchConfigItem());
        if (autodownloadEnabled && skipDownloadableDetailsSearch) {
            performAutoDownload();
        } else {
            searchDownloadables();
        }

        event.consume();
    }

    private void performAutoDownload() {
        final String format = cfgMgr.getValue(new AutoDownloadFormatConfigItem());
        Downloadable downloadable = new CustomFormatDownloadable(videoUrlTextField.getText(), format);
        AppUtils.executeCommandResolvingPath(stage, new DownloadCommand(stage, downloadable), downloadable::setDownloadPath);
    }

    private void searchDownloadables() {
        downloadableSearchService.setUrl(videoUrlTextField.getText());

        downloadableSearchService.setOnSucceeded(it -> {
            List<MultiFormatDownloadable> downloadables = (List<MultiFormatDownloadable>) it.getSource().getValue();
            loadContentPane(downloadables);

            boolean autodownloadEnabled = cfgMgr.getValue(new AutoDownloadConfigItem());
            if (autodownloadEnabled && downloadables.size() == 1) {
                Platform.runLater(this::performAutoDownload); // runLater is to release the service and trigger runningProperty to be false
            }
        });

        downloadableSearchService.setOnFailed(it -> {
            Throwable ex = it.getSource().getException();
            LOGGER.error(ex.getMessage(), ex);
            Dialogs.info("video.search.error");
        });

        searchProgressIndicator.visibleProperty().bind(downloadableSearchService.runningProperty());

        downloadableSearchService.restart();
    }

    private void loadContentPane(List<MultiFormatDownloadable> downloadables) {
        boolean hasVideos = false;
        for (MultiFormatDownloadable item : downloadables) {
            if (CollectionUtils.isNotEmpty(item.getVideos())) {
                hasVideos = true;
                break;
            }
        }

        if (hasVideos) {
            Parent videoComponent = UiManager.loadComponent(
                    UiComponent.DOWNLOADABLE_ITEMS_COMPONENT,
                    param -> new DownloadableItemsComponentController(
                            stage,
                            downloadables,
                            (downloadable) -> UiManager.loadComponent(UiComponent.VIDEO_DOWNLOAD_GRID, param1 -> new VideoDownloadGridController(stage, downloadable))
                    )
            );
            contentScrollPane.setContent(videoComponent);
        }
    }

    private void initDragAndDrop() {
        rootControlVBox.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        rootControlVBox.setOnDragDropped(e -> {
            Dragboard dragboard = e.getDragboard();
            if (e.getTransferMode() == TransferMode.COPY && dragboard.hasString()) {
                videoUrlTextField.setText(dragboard.getString());
                searchBtn.fire();
                e.setDropCompleted(true);
            }
            e.consume();
        });
    }
}
