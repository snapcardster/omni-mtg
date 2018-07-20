package com.snapcardster.omnimtg.Interfaces;

import java.util.Properties;

public interface PropertyFactory {
    BooleanProperty newBooleanProperty(Boolean initialValue);

    StringProperty newStringProperty(String initialValue);

    IntegerProperty newIntegerProperty(Integer initialValue);

    Object newListener(Properties prop, String name);
}
