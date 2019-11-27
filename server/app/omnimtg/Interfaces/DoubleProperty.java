package omnimtg.Interfaces;

public interface DoubleProperty {
    void setValue(Double value);
    Double getValue();
    Object getNativeBase();
    void addListener(Object listener);
}
