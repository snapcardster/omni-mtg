package com.snapcardster.omnimtg;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AutoUpdaterTest {

    AutoUpdater obj;

    @Before
    public void setUp() {
        obj = new AutoUpdater(new DummyShowLog(), new DummyReportException());
    }

    @Test
    public void downloadBinaryFile() throws Exception {
        File f = File.createTempFile("test", "html");
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
        File tempFile = File.createTempFile("test", "zip");
        String zipB64 = "UEsDBAoAAAAAAMSYRE7dBrXAAQAAAAEAAAAFAAAAWC50eHRZUEsBAj8ACgAAAAAAxJhETt0GtcABAAAAAQAAAAUAJAAAAAAAAAAgCAAAAAAAAFgudHh0CgAgAAAAAAABABgAORAkTbS81AGgDJhKtLzUAaAMmEq0vNQBUEsFBgAAAAABAAEAVwAAACQAAAAAAA==";
        Files.write(tempFile.toPath(), Base64.getDecoder().decode(zipB64));
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

    private Map<Date, String> getMap() throws Exception {
        Map<Date, String> items = new HashMap<>();
        obj.parseJson(items, obj.readFromGitApi());
        return items;
    }
}
