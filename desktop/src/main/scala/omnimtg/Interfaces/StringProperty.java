package omnimtg.Interfaces;

public interface StringProperty {
    void setValue(String value);
    void setValue(String value, Boolean callListener);
    String getValue();
    Object getNativeBase();
    void addListener(Object listener);
}
