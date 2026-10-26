package com.awesomeshot5051.edl2chapters.app.gui;

import com.awesomeshot5051.edl2chapters.model.ChapterOptions;
import com.awesomeshot5051.edl2chapters.model.Marker;
import com.awesomeshot5051.edl2chapters.model.TimelineInfo;
import com.awesomeshot5051.edl2chapters.utils.resolve.ResolveClient;
import com.awesomeshot5051.edl2chapters.utils.chapters.ChapterBuilder;
import com.awesomeshot5051.edl2chapters.utils.edl.EdlParser;
import com.awesomeshot5051.edl2chapters.utils.io.FileReaderUtil;
import com.awesomeshot5051.edl2chapters.utils.io.FileWriterUtil;
import com.awesomeshot5051.edl2chapters.utils.time.TimecodeParser;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class MainView extends VBox {

    private static final String DROP_ACTIVE_CLASS = "drop-active";
    private static final String STATUS_OK_CLASS = "status-ok";
    private static final String STATUS_ERROR_CLASS = "status-error";

    private final Stage stage;
    private final Label fileLabel = new Label("No EDL loaded (open one or drop it here)");
    private final TextField offsetField = new TextField("00:00:00");
    private final CheckBox introBox = new CheckBox("Add intro at 00:00");
    private final TextField introTitleField = new TextField(ChapterOptions.DEFAULT_INTRO_TITLE);
    private final TextArea outputArea = new TextArea();
    private final Label statusLabel = new Label();
    private final ResolveClient resolveClient = new ResolveClient();
    private List<Marker> loadedMarkers;

    public MainView(Stage stage) {
        this.stage = stage;
        setSpacing(10);
        setPadding(new Insets(16));
        getStyleClass().add("main-view");

        Label titleLabel = new Label("EDL to YouTube Chapters");
        titleLabel.getStyleClass().add("title-label");
        fileLabel.getStyleClass().add("file-label");

        Button openButton = new Button("Open EDL...");
        openButton.setOnAction(e -> chooseFile());
        introBox.setSelected(true);
        introTitleField.disableProperty().bind(introBox.selectedProperty().not());
        offsetField.setPrefColumnCount(8);
        introTitleField.setPrefColumnCount(14);

        HBox options = new HBox(8, new Label("Timeline start offset:"), offsetField, introBox, introTitleField);
        options.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        outputArea.setEditable(false);
        outputArea.getStyleClass().add("output-area");
        VBox.setVgrow(outputArea, Priority.ALWAYS);

        Button copyButton = new Button("Copy");
        copyButton.setOnAction(e -> copyOutput());
        Button saveButton = new Button("Save...");
        saveButton.setOnAction(e -> saveOutput());

        HBox optionsCard = options;
        optionsCard.getStyleClass().add("card");

        Button importButton = new Button("Import from DaVinci");
        importButton.setOnAction(e -> importFromResolve());

        getChildren().addAll(titleLabel, new HBox(8, openButton, importButton, fileLabel), optionsCard, outputArea,
                new HBox(8, copyButton, saveButton, statusLabel));

        introBox.setOnAction(e -> refresh());
        offsetField.setOnAction(e -> refresh());
        offsetField.focusedProperty().addListener((obs, was, now) -> { if (!now) refresh(); });
        introTitleField.textProperty().addListener((obs, old, now) -> refresh());

        setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                if (!getStyleClass().contains(DROP_ACTIVE_CLASS)) {
                    getStyleClass().add(DROP_ACTIVE_CLASS);
                }
            }
            e.consume();
        });
        setOnDragExited(e -> getStyleClass().remove(DROP_ACTIVE_CLASS));
        setOnDragDropped(e -> {
            if (e.getDragboard().hasFiles()) {
                loadFile(e.getDragboard().getFiles().getFirst().toPath());
            }
            e.setDropCompleted(true);
            e.consume();
        });
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("EDL files", "*.edl"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            loadFile(file.toPath());
        }
    }

    private void loadFile(Path file) {
        try {
            loadedMarkers = EdlParser.parse(FileReaderUtil.readText(file));
            fileLabel.setText(file.getFileName().toString());
            refresh();
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), false);
        }
    }

    private void importFromResolve() {
        showStatus("Contacting DaVinci Resolve...", true);
        Thread worker = new Thread(() -> {
            try {
                List<TimelineInfo> timelines = resolveClient.fetchTimelines();
                javafx.application.Platform.runLater(() -> pickTimeline(timelines));
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> showStatus(
                        "Can't reach DaVinci. In Resolve choose Workspace > Scripts > Edl2Chapters, then try again.", false));
            }
        }, "resolve-import");
        worker.setDaemon(true);
        worker.start();
    }

    private void pickTimeline(List<TimelineInfo> timelines) {
        if (timelines.isEmpty()) {
            showStatus("DaVinci has no timelines in this project", false);
            return;
        }
        TimelineInfo defaultChoice = timelines.stream().filter(TimelineInfo::current).findFirst().orElse(timelines.getFirst());
        ChoiceDialog<TimelineInfo> dialog = new ChoiceDialog<>(defaultChoice, timelines);
        dialog.setTitle("Import from DaVinci");
        dialog.setHeaderText("Choose a timeline");
        dialog.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        dialog.initOwner(stage);
        dialog.showAndWait().ifPresent(this::loadTimeline);
    }

    private void loadTimeline(TimelineInfo timeline) {
        Thread worker = new Thread(() -> {
            try {
                List<Marker> markers = resolveClient.fetchMarkers(timeline.index());
                javafx.application.Platform.runLater(() -> {
                    loadedMarkers = markers;
                    fileLabel.setText("DaVinci: " + timeline.name());
                    refresh();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> showStatus("Error: " + e.getMessage(), false));
            }
        }, "resolve-markers");
        worker.setDaemon(true);
        worker.start();
    }

    private void refresh() {
        if (loadedMarkers == null) {
            return;
        }
        try {
            ChapterOptions options = new ChapterOptions(
                    TimecodeParser.parseClock(offsetField.getText()), introBox.isSelected(), introTitleField.getText());
            outputArea.setText(ChapterBuilder.build(loadedMarkers, options));
            statusLabel.setText("");
        } catch (Exception e) {
            outputArea.clear();
            showStatus("Error: " + e.getMessage(), false);
        }
    }

    private void copyOutput() {
        ClipboardContent content = new ClipboardContent();
        content.putString(outputArea.getText());
        Clipboard.getSystemClipboard().setContent(content);
        showStatus("Copied", true);
    }

    private void saveOutput() {
        File file = new FileChooser().showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            FileWriterUtil.writeText(file.toPath(), outputArea.getText());
            showStatus("Saved " + file.getName(), true);
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), false);
        }
    }

    private void showStatus(String message, boolean ok) {
        statusLabel.getStyleClass().removeAll(STATUS_OK_CLASS, STATUS_ERROR_CLASS);
        statusLabel.getStyleClass().add(ok ? STATUS_OK_CLASS : STATUS_ERROR_CLASS);
        statusLabel.setText(message);
    }
}
