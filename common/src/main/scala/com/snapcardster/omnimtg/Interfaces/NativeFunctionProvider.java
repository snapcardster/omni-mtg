package com.snapcardster.omnimtg.Interfaces;

import java.util.Properties;

public interface NativeFunctionProvider {
    void openLink(String url);
    Throwable save(Properties prop, Object nativeBase);
    Throwable readProperties(Properties prop, MainControllerInterface controller, Object nativeBase);
    byte[] decodeBase64(String str);
    Throwable saveToFile(String path, String contents, Object nativeBase);
}
