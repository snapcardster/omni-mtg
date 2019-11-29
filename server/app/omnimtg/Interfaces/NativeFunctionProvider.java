package omnimtg.Interfaces;

import java.util.Properties;

public interface NativeFunctionProvider {
    void openLink(String url);

    Throwable updatePropertiesFromPropsAndSaveToFile(Properties prop, Object controller, Object nativeBase);

    Throwable savePropertiesToFile(Properties prop, Object nativeBase);

    Throwable readProperties(Properties prop, Object controller, Object nativeBase);

    byte[] decodeBase64(String str);

    Throwable saveToFile(String path, String contents, Object nativeBase);

    String encodeBase64ToString(byte[] digest);

    void println(Object x);
}
