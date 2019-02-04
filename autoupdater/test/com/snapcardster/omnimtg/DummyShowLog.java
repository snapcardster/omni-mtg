package com.snapcardster.omnimtg;

public class DummyShowLog extends ShowLog {
    @Override
    public void setText(String s) {
    }

    @Override
    public String getText() {
        return "";
    }

    @Override
    public void log(String s) {
        System.out.println(s);
    }

    @Override
    public void dispose() {
    }
}
