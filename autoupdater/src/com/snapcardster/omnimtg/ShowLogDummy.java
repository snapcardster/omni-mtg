package com.snapcardster.omnimtg;

public class ShowLogDummy extends ShowLogBase {
    @Override
    public void log(String s) {
        System.out.println(s);
    }

    @Override
    public String getText() {
        return "";
    }

    @Override
    public void setText(String s) {
        System.out.println(s);
    }

    @Override
    public void dispose() {
    }
}
