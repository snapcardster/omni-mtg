package omnimtg.Interfaces;

import java.util.Properties;

public interface MainControllerInterface {

    void sync(Object nativeBase);

    void readProperties(Object nativeBase);

    Thread getThread();

    Properties getProperties();

    /*
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

    DoubleProperty getMultiplier();

    DoubleProperty getMinBidPrice();

    DoubleProperty getMaxBidPrice();

    IntegerProperty getInterval();

    IntegerProperty getNextSync();

    BooleanProperty getInSync();

    ObjectProperty getRequest();
    */
    void loginSnap();

    void insertFromClip(String mode, String data);

    void start(Object nativeBase);
}
