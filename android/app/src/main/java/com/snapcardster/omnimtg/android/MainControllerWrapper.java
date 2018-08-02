package com.snapcardster.omnimtg.android;

import android.app.Activity;
import android.content.Context;
import android.os.PowerManager;

import com.snapcardster.omnimtg.Interfaces.BooleanProperty;
import com.snapcardster.omnimtg.Interfaces.IntegerProperty;
import com.snapcardster.omnimtg.Interfaces.MainControllerInterface;
import com.snapcardster.omnimtg.Interfaces.StringProperty;
import com.snapcardster.omnimtg.MainController;

import java.util.Properties;

public class MainControllerWrapper implements MainControllerInterface {
    private AndroidPropertyFactory propFactory;
    private AndroidNativeFunctionProvider nativeProvider;
    private MainController controller;

    MainControllerWrapper() {
        propFactory = new AndroidPropertyFactory();
        nativeProvider = new AndroidNativeFunctionProvider();
        controller = new MainController(propFactory, nativeProvider);
    }

    public void loginSnap() {
        controller.loginSnap();
    }

    @Override
    public void insertFromClip(String mode, String data) {
        controller.insertFromClip(mode, data);
    }

    @Override
    public void start(Object nativeBase) {
        /*PowerManager pm = (PowerManager) ((Activity)nativeBase).getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "omnimtg:sync");
        wl.acquire();*/
        controller.start(nativeBase);
    }

    @Override
    public void sync(Object nativeBase) {
        controller.sync(nativeBase);
    }

    @Override
    public void save(Object nativeBase) {
        controller.save(nativeBase);
    }

    @Override
    public void readProperties(Object nativeBase) {
        controller.readProperties(nativeBase);
    }

    @Override
    public Thread getThread() {
        return controller.getThread();
    }

    @Override
    public Properties getProperties() {
        return controller.getProperties();
    }

    @Override
    public BooleanProperty getAborted() {
        return controller.getAborted();
    }

    @Override
    public BooleanProperty getRunning() {
        return controller.getRunning();
    }

    @Override
    public StringProperty getMkmAppToken() {
        return controller.getMkmAppToken();
    }

    @Override
    public StringProperty getMkmAppSecret() {
        return controller.getMkmAppSecret();
    }

    @Override
    public StringProperty getMkmAccessToken() {
        return controller.getMkmAccessToken();
    }

    @Override
    public StringProperty getMkmAccessTokenSecret() {
        return controller.getMkmAccessTokenSecret();
    }

    @Override
    public StringProperty getSnapUser() {
        return controller.getSnapUser();
    }

    @Override
    public StringProperty getSnapPassword() {
        return controller.getSnapPassword();
    }

    @Override
    public StringProperty getSnapToken() {
        return controller.getSnapToken();
    }

    @Override
    public StringProperty getOutput() {
        return controller.getOutput();
    }

    @Override
    public IntegerProperty getInterval() {
        return controller.getInterval();
    }

    @Override
    public IntegerProperty getnextSync() {
        return controller.getnextSync();
    }
}
