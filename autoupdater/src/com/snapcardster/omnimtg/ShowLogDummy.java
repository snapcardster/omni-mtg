package com.snapcardster.omnimtg;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class ShowLogDummy extends ShowLogBase {
    public static final boolean windows = String.valueOf(System.getProperty("os.name")).toLowerCase().contains("windows");
    // String.valueOf(System.getenv("os.name")).toLowerCase().contains("windows");

    @Override
    public void log(String s) {
        System.out.println(s);

        try {
            Path logs = Paths.get("/logs/updater.log");
            if (windows) {
                logs = Paths.get("logs", "updater.log");
            }

            try {
                logs.getParent().toFile().mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!logs.toFile().exists()) {
                try {
                    logs.toFile().createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Files.write(logs, Arrays.asList(s), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getText() {
        return "";
    }

    @Override
    public void setText(String s) {
        System.out.println(s);
    }

    @Override
    public void dispose() {
    }
}
