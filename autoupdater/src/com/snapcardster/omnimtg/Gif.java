package com.snapcardster.omnimtg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * https://www.reddit.com/r/loadingicon/comments/7cwyib/beaker_loading_icon/
 */
public class Gif {

    public static String B64() {
        try {
            return new String(Files.readAllBytes(Paths.get("image.b64")));
        } catch (IOException e) {
            return "";
        }
    }
}
