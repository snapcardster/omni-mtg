package com.snapcardster.omnimtg.android;

import com.snapcardster.omnimtg.Interfaces.BooleanProperty;
import com.snapcardster.omnimtg.Interfaces.IntegerProperty;
import com.snapcardster.omnimtg.Interfaces.PropertyFactory;
import com.snapcardster.omnimtg.Interfaces.StringProperty;

import java.util.ArrayList;
import java.util.List;
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
        prop.put(name, initialValue);
        s.addListener((AndroidSrtingPropertyListener) (oldValue, newValue, callListener) -> prop.put(name, newValue));
        return s;
    }

    @Override
    public IntegerProperty newIntegerProperty(Integer initialValue) {
        return new AndroidIntegerProperty(initialValue);
    }

    @Override
    public IntegerProperty newIntegerProperty(String name, Integer initialValue, Properties prop) {
        IntegerProperty i = new AndroidIntegerProperty(initialValue);
        prop.put(name, initialValue.toString());
        i.addListener((AndroidIntegerPropertyListener) (oldValue, newValue, callListener) -> prop.put(name, newValue.toString()));
        return i;
    }
}

class AndroidBooleanProperty implements BooleanProperty {
    boolean value;

    AndroidBooleanProperty(boolean initialValue) {
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

class AndroidStringProperty implements StringProperty {
    String value;
    List<AndroidSrtingPropertyListener> listener = new ArrayList<>();

    AndroidStringProperty(String initialValue) {
        this.value = initialValue;
    }

    @Override
    public void setValue(String value) {
        for (AndroidSrtingPropertyListener l : listener) {
            l.onChanged(this.value, value, true);
        }
        this.value = value;
    }

    @Override
    public void setValue(String value, Boolean callListener) {
        for (AndroidSrtingPropertyListener l : listener) {
            l.onChanged(this.value, value, callListener);
        }
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
        this.listener.add((AndroidSrtingPropertyListener) listener);
    }
}

class AndroidIntegerProperty implements IntegerProperty {
    Integer value;
    List<AndroidIntegerPropertyListener> listener = new ArrayList<>();

    AndroidIntegerProperty(Integer initialValue) {
        this.value = initialValue;
    }

    @Override
    public void setValue(Integer value) {
        for (AndroidIntegerPropertyListener l : listener) {
            l.onChanged(this.value, value, true);
        }
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

    @Override
    public void addListener(Object listener) {
        this.listener.add((AndroidIntegerPropertyListener) listener);
    }
}
