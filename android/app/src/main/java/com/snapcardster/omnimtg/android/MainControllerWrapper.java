package com.snapcardster.omnimtg.android;

import com.snapcardster.omnimtg.Interfaces.BooleanProperty;
import com.snapcardster.omnimtg.Interfaces.IntegerProperty;
import com.snapcardster.omnimtg.Interfaces.MainControllerInterface;
import com.snapcardster.omnimtg.Interfaces.StringProperty;
import com.snapcardster.omnimtg.MainController;

import java.util.Properties;

public class MainControllerWrapper implements MainControllerInterface {
    AndroidPropertyFactory propFactory = new AndroidPropertyFactory();
    AndroidNativeFunctionProvider nativeProvider = new AndroidNativeFunctionProvider();
    MainController controller = new MainController(propFactory, nativeProvider);

    public void loginSnap() {
        controller.loginSnap();
    }

    @Override
    public Thread getThread() {
        return null;
    }

    @Override
    public Properties getProperties() {
        return null;
    }

    @Override
    public BooleanProperty getAborted() {
        return null;
    }

    @Override
    public BooleanProperty getRunning() {
        return null;
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
        return null;
    }

    @Override
    public IntegerProperty getInterval() {
        return null;
    }

    @Override
    public IntegerProperty getnextSync() {
        return null;
    }
}
