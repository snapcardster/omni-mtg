package com.snapcardster.omnimtg;

import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AutoUpdaterTest {

    private AutoUpdater obj;

    @Before
    public void setUp() {
        obj = new AutoUpdater(new DummyShowLog(), new DummyReportException());
    }

    @Test
    public void downloadBinaryFile() throws Exception {
        File f = createTempFile("test", "html");
        long res = obj.downloadBinaryFile("https://google.com", f);

        Assert.assertTrue(f.exists());
        Assert.assertTrue(f.length() > 0);
        Assert.assertTrue(res > 0);
    }

    @Test
    public void readFromGitApi() throws Exception {
        String json = obj.readFromGitApi();

        Assert.assertTrue(json.length() > 0);
        Assert.assertTrue(json.contains("published_at"));
        Assert.assertTrue(json.contains("browser_download_url"));
    }

    @Test
    public void parseJson() throws Exception {
        Map<Date, String> items = getMap();

        Assert.assertTrue(items.size() > 0);
    }

    @Test
    public void readTime() throws Exception {
        long millis = obj.readTime("2018-11-12T10:54:30Z");
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+0:00"));
        calendar.set(2018, Calendar.NOVEMBER, 12, 10, 54, 30);
        calendar.set(Calendar.MILLISECOND, 0);

        Assert.assertEquals(calendar.getTimeInMillis(), millis);
    }

    @Test
    public void unzipFile() throws Exception {
        File tempFile = createTempFile("test", "zip");

        Files.write(tempFile.toPath(), Base64.getDecoder().decode(zipB64()));
        File tempTarget = Paths.get(tempFile.getParentFile().getAbsolutePath(), "tempDir" + System.currentTimeMillis()).toFile();
        Assert.assertTrue(tempTarget.mkdirs());
        obj.unzipFile(tempFile, tempTarget);

        String[] list = tempTarget.list();
        Assert.assertNotNull(list);
        Assert.assertEquals(list.length, 1);
        Path path = Paths.get(tempTarget.getAbsolutePath(), list[0]);
        Assert.assertEquals(path.toFile().getName(), "X.txt");
        Assert.assertEquals(Arrays.toString(Files.readAllLines(path).toArray()), "[Y]");
    }

    @Test
    public void runJar() throws Exception {
        File tempFile = createTempFile("test", "jar");
        Files.write(tempFile.toPath(), Base64.getDecoder().decode(jarB64()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        obj.startJar(tempFile);

        Assert.assertThat(out.toString(), new CustomTypeSafeMatcher<>("contains") {
            @Override
            protected boolean matchesSafely(String s) {
                return s.contains("Hello World");
            }
        });
    }

    @Test
    public void validImage() throws IOException {
        Image image = obj.getImage();
        Assert.assertEquals(image.getHeight(null), 600);
        Assert.assertEquals(image.getWidth(null), 800);
    }

    @Test
    public void testMB() {
        Assert.assertEquals(obj.mb(0), 0.000, 0.01);
        Assert.assertEquals(obj.mb(1024), 0.001, 0.01);
        Assert.assertEquals(obj.mb(64000), 0.064, 0.01);
        Assert.assertEquals(obj.mb(64000000), 64.000, 0.01);
    }

    private Map<Date, String> getMap() throws Exception {
        Map<Date, String> items = new HashMap<>();
        obj.parseJson(items, obj.readFromGitApi());
        return items;
    }

    private File createTempFile(String pref, String suf) throws IOException {
        File tempFile = File.createTempFile(pref, suf);
        tempFile.deleteOnExit();
        return tempFile;
    }

    private String zipB64() {
        return "UEsDBAoAAAAAAMSYRE7dBrXAAQAAAAEAAAAFAAAAWC50eHRZUEsBAj8ACgAAAAAAxJhETt0GtcABAAAAAQAAAAUAJAAAAAAAAAAgCAAAAAAAAFgudHh0CgAgAAAAAAABABgAORAkTbS81AGgDJhKtLzUAaAMmEq0vNQBUEsFBgAAAAABAAEAVwAAACQAAAAAAA==";
    }

    private String jarB64() {
        return "UEsDBBQACAgIAPNURU4AAAAAAAAAAAAAAAAUAAQATUVUQS1JTkYvTUFOSUZFU1QuTUb+ygAA803My0xLLS7RDUstKs7Mz7NSMNQz4OXyTczM03XOSSwutlIAsXm5eLkAUEsHCHwCO+cqAAAAKwAAAFBLAwQKAAAIAADzVEVOAAAAAAAAAAAAAAAACQAAAE1FVEEtSU5GL1BLAwQUAAgICABrVEVOAAAAAAAAAAAAAAAACgAAAE1haW4uY2xhc3NtkM9OwkAQxr+FQmmtgiD4X8ETerAXEw8YLybGQ1ETDB48LbDBJdvWlGLiY+lBEw8+gA9lnF1I0IQ9zJf5ZvY3s/v98/kF4AQHLvJYc1BFrYB1FxvYtLFlY5shfyYjmZ4zZJuHXQbrIh4IhmIgI3E9CXsiueM9RU45iPtcdXkidT4zrfRRjokRtLmMWpSHpAy15kMw4s/cVzwa+p00kdGwZeg8GVJ/ZUGZwe3Ek6QvLqUmO5p4rLs82CjY2PGwiz0G70ooFdfv40QNGjb2PdTRIHLbTC7NuTe9kein/6zOyzgVIT01nlChOl1Cxv4tbZDSHoKHtEdlgc1gP+lM0Yxqc9Hj0ECOPlmfDJjemaJDmU/KSHNHH2CvpuxSzBuTYYmiN20gXSZ1sILi7PKpgZH3hkw5+w5rDnBJAYvm5P5AHJSwasBl01n5BVBLBwhtsEyQSQEAAAQCAABQSwECFAAUAAgICADzVEVOfAI75yoAAAArAAAAFAAEAAAAAAAAAAAAAAAAAAAATUVUQS1JTkYvTUFOSUZFU1QuTUb+ygAAUEsBAgoACgAACAAA81RFTgAAAAAAAAAAAAAAAAkAAAAAAAAAAAAAAAAAcAAAAE1FVEEtSU5GL1BLAQIUABQACAgIAGtURU5tsEyQSQEAAAQCAAAKAAAAAAAAAAAAAAAAAJcAAABNYWluLmNsYXNzUEsFBgAAAAADAAMAtQAAABgCAAAAAA==";
    }
}
