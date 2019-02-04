package com.snapcardster.omnimtg;

public class DummyReportException extends ReportException {
    @Override
    public void sendEmail(String s) {
        System.out.println("MAIL: " + s);
    }
}
