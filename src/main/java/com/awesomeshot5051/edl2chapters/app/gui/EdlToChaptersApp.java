package com.awesomeshot5051.edl2chapters.app.gui;

import com.awesomeshot5051.edl2chapters.utils.resolve.AppPresenceMarker;
import com.awesomeshot5051.edl2chapters.utils.resolve.ResolveBridgePaths;
import com.awesomeshot5051.edl2chapters.utils.resolve.ResolveScriptInstaller;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class EdlToChaptersApp extends Application {

    private static final String STYLESHEET = "/css/app.css";
    private static final String ICON = "/icons/ETIcon.png";
    private static final String TITLE = "EDL to YouTube Chapters";

    @Override
    public void start(Stage stage) {
        ResolveScriptInstaller.installIfResolvePresent();
        AppPresenceMarker.createUntilExit(ResolveBridgePaths.mailboxDir());
        stage.setTitle(TITLE);
        stage.getIcons().add(new Image(getClass().getResource(ICON).toExternalForm()));
        Scene scene = new Scene(new MainView(stage), 680, 540);
        scene.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
        stage.setScene(scene);
        stage.show();
    }
}
