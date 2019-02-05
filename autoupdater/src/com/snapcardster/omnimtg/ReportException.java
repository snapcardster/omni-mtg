package com.snapcardster.omnimtg;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

class ReportException {
    void sendEmail(String content) throws Exception {
        String auth = "_ANONYMOUS_WEB_cbea1255fd40c800,8dfe272393798e53c07d34bcaf90de9ae4d440fdbcbc36dd9465a8f1c4b3a59c";
        long time = System.currentTimeMillis();
        String replace = (System.getProperty("user.name") + "@" + new Date() + "\n" + content).replace("\\", "\\\\").replace('"', '\'').replace("\n", "\\n");
        String rawData = "{ \"id\": 0, \"from\": \"_ANONYMOUS_WEB_cbea1255fd40c800\", \"to\": \"Karsten P\", \"timestamp\": " + time + ", \"message\": \"" + replace + "\" }";

        String type = "application/json";
        // String encodedData = URLEncoder.encode(rawData, "UTF-8");
        URL u = new URL("https://api.snapcardster.com/messages");
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", type);
        conn.setRequestProperty("Authorization", auth);
        OutputStream os = conn.getOutputStream();
        os.write(rawData.getBytes());
        os.flush();
        os.close();
        System.out.println(conn.getResponseCode() + ": " + conn.getResponseMessage());
        conn.disconnect();
    }
}
