package com.snapcardster.omnimtg.Interfaces;

import java.util.Properties;

public interface MainControllerInterface {

    void sync(Object nativeBase);

    void save(Object nativeBase);

    void readProperties(Object nativeBase);

    Thread getThread();

    Properties getProperties();

    BooleanProperty getAborted();

    BooleanProperty getRunning();

    StringProperty getMkmAppToken();

    StringProperty getMkmAppSecret();

    StringProperty getMkmAccessToken();

    StringProperty getMkmAccessTokenSecret();

    StringProperty getSnapUser();

    StringProperty getSnapPassword();

    StringProperty getSnapToken();

    StringProperty getOutput();

    IntegerProperty getInterval();

    IntegerProperty getnextSync();

    void loginSnap();

    void insertFromClip(String mode,String data);

    void start(Object nativeBase);
}
