package omnimtg.Interfaces;

import java.util.Properties;

public interface PropertyFactory {
    BooleanProperty newBooleanProperty(Boolean initialValue);

    StringProperty newStringProperty(String initialValue);

    StringProperty newStringProperty(String name, String initialValue, Properties prop);

    IntegerProperty newIntegerProperty(Integer initialValue);

    IntegerProperty newIntegerProperty(String name, Integer initialValue, Properties prop);

    ObjectProperty newObjectProperty(Object value);
}
