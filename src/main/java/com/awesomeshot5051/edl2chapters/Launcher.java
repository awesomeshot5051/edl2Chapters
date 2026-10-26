package com.awesomeshot5051.edl2chapters;

import com.awesomeshot5051.edl2chapters.app.cli.CliMain;
import com.awesomeshot5051.edl2chapters.app.gui.EdlToChaptersApp;
import javafx.application.Application;

public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            Application.launch(EdlToChaptersApp.class, args);
        } else {
            System.exit(CliMain.run(args, System.out, System.err));
        }
    }
}
