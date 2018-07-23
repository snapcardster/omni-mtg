package com.snapcardster.omnimtg.android;

import com.snapcardster.omnimtg.Interfaces.BooleanProperty;
import com.snapcardster.omnimtg.Interfaces.IntegerProperty;
import com.snapcardster.omnimtg.Interfaces.PropertyFactory;
import com.snapcardster.omnimtg.Interfaces.StringProperty;

import java.util.Properties;

public class AndroidPropertyFactory implements PropertyFactory {
    @Override
    public BooleanProperty newBooleanProperty(Boolean initialValue) {
        return null;
    }

    @Override
    public StringProperty newStringProperty(String initialValue) {
        return null;
    }

    @Override
    public IntegerProperty newIntegerProperty(Integer initialValue) {
        return null;
    }

    @Override
    public Object newListener(Properties prop, String name) {
        return null;
    }
}
