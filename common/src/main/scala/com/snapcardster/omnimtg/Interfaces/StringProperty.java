package com.snapcardster.omnimtg.Interfaces;

public interface StringProperty {
    void setValue(String value);
    String getValue();
    Object getNativeBase();
    void addListener(Object listener);
}
