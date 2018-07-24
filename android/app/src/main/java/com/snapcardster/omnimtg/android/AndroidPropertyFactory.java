package com.snapcardster.omnimtg.android;

import com.snapcardster.omnimtg.Interfaces.BooleanProperty;
import com.snapcardster.omnimtg.Interfaces.IntegerProperty;
import com.snapcardster.omnimtg.Interfaces.PropertyFactory;
import com.snapcardster.omnimtg.Interfaces.StringProperty;

import java.util.Properties;

public class AndroidPropertyFactory implements PropertyFactory {
    @Override
    public BooleanProperty newBooleanProperty(Boolean initialValue) {
        return new AndroidBooleanProperty(initialValue);
    }

    @Override
    public StringProperty newStringProperty(String initialValue) {
        return new AndroidStringProperty(initialValue);
    }

    @Override
    public StringProperty newStringProperty(String name, String initialValue, Properties prop) {
        StringProperty s = new AndroidStringProperty(initialValue);
        // TODO add listener to s
        return s;
    }

    @Override
    public IntegerProperty newIntegerProperty(Integer initialValue) {
        return new AndroidIntegerProperty(initialValue);
    }
}

class AndroidBooleanProperty implements  BooleanProperty{
    boolean value;

    AndroidBooleanProperty(boolean initialValue){
        this.value = initialValue;
    }

    @Override
    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public Boolean getValue() {
        return this.value;
    }

    @Override
    public Object getNativeBase() {
        return this;
    }
}

class AndroidStringProperty implements  StringProperty{
    String value;

    AndroidStringProperty(String initialValue){
        this.value = initialValue;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public Object getNativeBase() {
        return this;
    }

    @Override
    public void addListener(Object listener) {

    }
}

class AndroidIntegerProperty implements  IntegerProperty{
    Integer value;

    AndroidIntegerProperty(Integer initialValue){
        this.value = initialValue;
    }

    @Override
    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }

    @Override
    public Object getNativeBase() {
        return this;
    }
}
