package com.snapcardster.omnimtg;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

public class ExecServerTest {

    private AutoUpdater obj;

    @Before
    public void setUp() {
        obj = new AutoUpdater(new ShowLogDummy(), new DummyReportException(), true);
    }

    @Test
    public void execBinTest() throws Exception {
        File path = Paths.get("C:/Arbeit/omni-mtg/server/target/universal/omnimtg-0.1-SNAPSHOT").toFile();
        File path2 = Paths.get(path.getAbsolutePath(), "bin", "omnimtg.bat").toFile();
        obj.execBin(path, path2);
    }
}
