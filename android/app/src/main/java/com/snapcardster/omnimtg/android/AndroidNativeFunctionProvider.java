package com.snapcardster.omnimtg.android;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.snapcardster.omnimtg.Interfaces.IntegerProperty;
import com.snapcardster.omnimtg.Interfaces.MainControllerInterface;
import com.snapcardster.omnimtg.Interfaces.NativeFunctionProvider;
import com.snapcardster.omnimtg.Interfaces.StringProperty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Properties;

import scala.Int;

public class AndroidNativeFunctionProvider implements NativeFunctionProvider {

    AndroidNativeFunctionProvider() {

    }

    @Override
    public void openLink(String url) {

    }

    public byte[] decodeBase64(String str) {
        return android.util.Base64.decode(str, Base64.DEFAULT);
    }

    @Override
    public Throwable saveToFile(String path, String contents, Object nativeBase) {
        try (OutputStream out = new FileOutputStream(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), path))) {
            PrintWriter pw = new PrintWriter(out);
            pw.write(contents);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
        return null;
    }

    @Override
    public Throwable save(Properties prop, Object nativeBase) {
        try (OutputStream out = ((Context) nativeBase).openFileOutput("config.txt", Context.MODE_PRIVATE)) {
            prop.store(out, null);
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
        return null;
    }

    private void updateProp(String str, StringProperty property, Properties prop) {
        Log.d("LoadProps", str + ": " + prop.get(str));
        Object value = prop.get(str);
        if (value != null && value != "") {
            property.setValue(String.valueOf(value));
        }
    }

    private void updateProp(String str, IntegerProperty property, Properties prop) {
        Log.d("LoadProps", str + ": " + prop.get(str));
        Object value = prop.get(str);
        if (value != null && value != "") {
            property.setValue(Integer.valueOf(String.valueOf(value)));
        }
    }

    @Override
    public Throwable readProperties(Properties prop, MainControllerInterface controller, Object nativeBase) {
        try (InputStream in = ((Activity) nativeBase).openFileInput("config.txt")) {
            prop.load(in);

            updateProp("mkmApp", controller.getMkmAppToken(), prop);
            updateProp("mkmAppSecret", controller.getMkmAppSecret(), prop);
            updateProp("mkmAccessToken", controller.getMkmAccessToken(), prop);
            updateProp("mkmAccessTokenSecret", controller.getMkmAccessTokenSecret(), prop);
            updateProp("snapUser", controller.getSnapUser(), prop);
            updateProp("snapToken", controller.getSnapToken(), prop);
            updateProp("interval", controller.getInterval(), prop);
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
        return null;
    }
}
