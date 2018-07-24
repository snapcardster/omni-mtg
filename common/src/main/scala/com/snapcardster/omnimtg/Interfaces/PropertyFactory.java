package com.snapcardster.omnimtg.Interfaces;

import java.util.Properties;

public interface PropertyFactory {
    BooleanProperty newBooleanProperty(Boolean initialValue);

    StringProperty newStringProperty(String initialValue);

    StringProperty newStringProperty(String name, String initialValue, Properties prop);

    IntegerProperty newIntegerProperty(Integer initialValue);
}
