package com.snapcardster.omnimtg.Interfaces;

public interface IntegerProperty {
    void setValue(Integer value);
    Integer getValue();
    Object getNativeBase();
    void addListener(Object listener);
}
