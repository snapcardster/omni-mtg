package sample;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("unchecked")
public class Main
//        extends Application
{
    private static final String URL = "https://api.github.com/repos/snapcardster/omni-mtg/releases";
    //  private Label label = null;

    private void log(String s) {
        System.out.println(s);
    /*    if (label != null) {
            Platform.runLater(() ->
                    label.setText(label.getText() + "\n" + s)
            );
        }*/
    }

    /*@Override
    public void start(Stage primaryStage) {
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setSpacing(10);
        primaryStage.setTitle("Checking for updates...");
        label = new Label("\n\n\n\n\n\n\n\n\n\n\nChecking network...");
        label.setStyle("-fx-font-size: 150%");
        label.setTextFill(Color.web("#ffffff"));


        // root.setStyle("-fx-background-color: #050555");
        add(root, label);
        Image img = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(Gif.B64())));
        BackgroundPosition pos = new BackgroundPosition(Side.LEFT, 0.5, false, Side.TOP, 0, false);
        Background bg = new Background(
                Collections.singletonList(new BackgroundFill(new Color(25 / 255.0, 31 / 255.0, 38 / 255.0, 1), null, null)),
                Collections.singletonList(new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, pos, BackgroundSize.DEFAULT))
        );
        root.setBackground(bg);

        checkAndRun();
        primaryStage.setScene(new Scene(root, 675, 800));
        primaryStage.show();
    }*/

    private void checkAndRun() {
        try {
            Map<Date, String> items = new HashMap<>();
            try {
                StringBuilder sb = readFromGitApi();

                String json = sb.toString();
                log("Code Repository found");
                for (Map<String, ScriptObjectMirror> x : readJson(json)) {
                    Collection<Object> assets1 = x.get("assets").values();

                    Map<String, Object> assets = (Map<String, Object>) assets1.iterator().next();
                    String published_at = x.get("published_at") + "";

                    Date d = new Date(readTime(published_at));
                    items.put(d, assets.get("browser_download_url") + "");
                }
            } catch (Exception e) {
                log(e.toString());
            }

            if (items.isEmpty()) {
                log("No releases found, offline? Retrying in 10 sec.");
                new Thread(() -> {
                    try {
                        Thread.sleep(10 * 1000);
                        //label.setText("");
                        checkAndRun();
                    } catch (InterruptedException e) {
                        log(e.toString());
                    }
                }).start();
            } else {
                Date max = items.keySet().stream().max(Comparator.naturalOrder()).orElse(new Date());
                log("Newest release: " + max);

                String currentDir = new File("").getAbsolutePath();
                File targetFile = Paths.get(currentDir, "downloaded" + max.getTime() + ".zip").toFile();

                File unzip = Paths.get(currentDir, "unzip" + max.getTime()).toFile();

                String downloadUrl = items.get(max);
                if (targetFile.exists()) {
                    if (unzip.exists()) {
                        log("Up to date and unzipped! Starting...");
                        startJar(unzip);
                    } else {
                        log("Downloaded found, unzipping...");
                        unzipAndStart(unzip, targetFile);
                    }
                } else {
                    /*Button downloadIt = new Button("Download it");
                    downloadIt.setOnMouseClicked(x -> {*/
                    try {

                        log("Downloading... (this may may take around 5 minutes)\n" + downloadUrl);
                        new Thread(() -> {
                            try {
                                Thread.sleep(100);
                                long len = downloadBinaryFile(downloadUrl, targetFile);
                                log("Done: approx " + Math.round(len / 1000.0 / 1000.0 * 100) / 100.0 + " MB loaded, unzipping...");

                                unzipAndStart(unzip, targetFile);

                            } catch (Exception e) {
                                log(e.toString());
                            }
                        }).start();
                        /*if(!f.createNewFile()) {
                            throw new Exception("could not create file " + f);
                        }*/
                    } catch (Exception e) {
                        log(e.toString());
                    }
                   /* });
                    add(root, downloadIt);*/
                }
            }
        } catch (Exception e) {
            log(e.toString());
        }
    }

    private void unzipAndStart(File unzip, File downloadedZip) throws Exception {
        if (!unzip.exists() || unzip.delete()) {
            if (unzip.mkdirs()) {
                unzipFile(downloadedZip, unzip);
                log("Done. Starting...");
                startJar(unzip);
            } else {
                throw new Exception("Could not create folders");
            }
        } else {
            throw new Exception("Could not empty unzip folder");
        }
    }

    private void startJar(File unzip) {
        String o = Paths.get(unzip.getAbsolutePath(), "omni-mtg").toFile().getAbsolutePath();
        // String p = Paths.get(o, "start omni-mtg.bat").toFile().getAbsolutePath();
        try {
            // new ProcessBuilder("cd " + o).start().waitFor();
            System.setProperty("user.dir", o);
            //ProcessBuilder process = new ProcessBuilder();
            log("Ready!"); // Please start \"start omni-mtg.bat\" :) You may close this program");
            Desktop.getDesktop().open(Paths.get(o, "start omni-mtg.bat").toFile());
            //Runtime.getRuntime().exec(new String[]{"explorer", o});
            // new String[]{"jPortable/bin/java", "-jar", "omni-mtg-java-archive/omni-mtg.jar"});
            //Runtime.getRuntime().exec("\"jPortable/bin/java\" -jar \"omni-mtg-java-archive/omni-mtg.jar\"");
            //process.start().waitFor();

            // Of course it's possible you don't know the main class from the JAR file. Using java.util.jar.JarFile and java.util.jar.Manifest you can retrieve it:

            File file = Paths.get(o, "omni-mtg-java-archive", "omni-mtg.jar").toFile();
            JarFile jarFile = new JarFile(file);
            Manifest manifest = jarFile.getManifest(); // warning: can be null
            Attributes attributes = manifest.getMainAttributes();
            String className = attributes.getValue(Attributes.Name.MAIN_CLASS);

            URL[] urls = {file.toURI().toURL()};
            URLClassLoader loader = new URLClassLoader(urls);
            Class<?> cls = loader.loadClass(className); // replace the complete class name with the actual main class
            System.out.println(className);
            Method main = cls.getDeclaredMethod("main", String[].class); // get the main method using reflection
            System.out.println(main);
            String[] args = {};
            main.invoke(null, new Object[]{args}); // static methods are invoked with null as first argument

            /*
            final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            final File currentJar = file;

            // Build command: java -jar application.jar
            final ArrayList<String> command = new ArrayList<String>();
            command.add(javaBin);
            command.add("-jar");
            command.add(currentJar.getPath());

            final ProcessBuilder builder = new ProcessBuilder(command);
            builder.start();
            System.exit(0);
            */

        } catch (Exception e) {
            log(e.toString());
        }
    }

    private long downloadBinaryFile(String downloadUrl, File targetFile) throws IOException {
        URL website = new URL(downloadUrl);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(targetFile);
        return fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }

    private StringBuilder readFromGitApi() throws IOException {
        URL url = new URL(URL);
        URLConnection connection = url.openConnection();
        connection.connect();
        ReadableByteChannel channel = Channels.newChannel(connection.getInputStream());

        Reader reader = Channels.newReader(channel, "UTF-8");

        BufferedReader reader2 = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader2.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb;
    }

    /*private static void add(Pane root, Node label) {
        root.getChildren().addAll(label);
    }*/

    public static void main(String[] args) {
        /*VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setSpacing(10);

        Stage primaryStage = new Stage();
        primaryStage.setTitle("Checking for updates...");
        Label label = new Label("\n\n\n\n\n\n\n\n\n\n\nChecking network...");
        label.setStyle("-fx-font-size: 150%");
        label.setTextFill(Color.web("#ffffff"));


        // root.setStyle("-fx-background-color: #050555");
        add(root, label);
        Image img = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(Gif.B64())));
        BackgroundPosition pos = new BackgroundPosition(Side.LEFT, 0.5, false, Side.TOP, 0, false);
        Background bg = new Background(
                Collections.singletonList(new BackgroundFill(new Color(25 / 255.0, 31 / 255.0, 38 / 255.0, 1), null, null)),
                Collections.singletonList(new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, pos, BackgroundSize.DEFAULT))
        );
        root.setBackground(bg);

        //checkAndRun();


        primaryStage.setScene(new Scene(root, 675, 800));
        primaryStage.show();*/
        new Main().checkAndRun();
        // launch(args);
    }

    private List<Map<String, ScriptObjectMirror>> readJson(String json) throws Exception {
        String EXTRACTOR_SCRIPT =
                "var fun = function(raw) { var json = JSON.parse(raw); return json; }; ";
        // .map(x => [x.published_at, (x.assets[0]||{}).browser_download_url]); };";

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.eval(EXTRACTOR_SCRIPT);
        Invocable invocable = (Invocable) engine;
        JSObject result = (JSObject) invocable.invokeFunction("fun", json);

        return result.values().stream()
                .map(x -> new HashMap<>((Map<String, ScriptObjectMirror>) x))
                .collect(Collectors.toList());
    }

    private long readTime(String jsTime) throws Exception {
        String EXTRACTOR_SCRIPT =
                "var fun = function(raw) { return new Date(raw).getTime(); }; ";
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        engine.eval(EXTRACTOR_SCRIPT);
        Invocable invocable = (Invocable) engine;
        Double result = (Double) invocable.invokeFunction("fun", jsTime);
        return result.longValue();
    }

    private void unzipFile(File zipFile, File extractFolder) {
        try {
            int BUFFER = 2048;
            File file = new File(zipFile.getAbsolutePath());

            ZipFile zip = new ZipFile(file);
            String newPath = extractFolder.getAbsolutePath();

            new File(newPath).mkdir();
            Enumeration zipFileEntries = zip.entries();

            // Process each entry
            while (zipFileEntries.hasMoreElements()) {
                // grab a zip file entry
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                String currentEntry = entry.getName();

                File destFile = new File(newPath, currentEntry);
                //destFile = new File(newPath, destFile.getName());
                File destinationParent = destFile.getParentFile();

                // create the parent directory structure if needed
                destinationParent.mkdirs();

                if (!entry.isDirectory()) {
                    BufferedInputStream is = new BufferedInputStream(zip
                            .getInputStream(entry));
                    int currentByte;
                    // establish buffer for writing file
                    byte data[] = new byte[BUFFER];

                    // write the current file to disk
                    FileOutputStream fos = new FileOutputStream(destFile);
                    BufferedOutputStream dest = new BufferedOutputStream(fos,
                            BUFFER);

                    // read and write until last byte is encountered
                    while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, currentByte);
                    }
                    dest.flush();
                    dest.close();
                    is.close();
                }


            }
        } catch (Exception e) {
            log(e.toString());
        }
    }

    /*private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        destFile.mkdirs();

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }*/
}
