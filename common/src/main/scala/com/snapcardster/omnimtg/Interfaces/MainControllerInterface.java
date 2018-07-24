package com.snapcardster.omnimtg.Interfaces;

import java.util.Properties;

public interface MainControllerInterface {

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
}
