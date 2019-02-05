package com.snapcardster.omnimtg;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;

import static javax.swing.text.DefaultCaret.ALWAYS_UPDATE;

public class ShowLog {
    private JTextArea label;
    private JFrame win;

    public void log(String s) {

        if (label == null) {
            label = new JTextArea();
            // label.setEditable(false);
            label.setLineWrap(true);
            label.setAutoscrolls(true);
            label.setWrapStyleWord(true);
            label.setForeground(Color.BLACK);
            DefaultCaret caret = (DefaultCaret) label.getCaret();
            caret.setUpdatePolicy(ALWAYS_UPDATE);
            /*JButton b = new JButton();
            b.setEnabled(false);
            b.setText("OK");*/

            //  ImageIcon image = new ImageIcon(Base64.getDecoder().decode(Gif.B64()));
            //     JLabel background = new JLabel(image);

            win = new JFrame();
            win.setTitle("Omni MTG Auto Updater");
            win.setContentPane(label);
            win.setLocationRelativeTo(null);
            win.setSize(400, 300);
            win.setVisible(true);
        }
        String t = label.getText() + "\n" + s;
        label.setText(t);
        label.setCaretPosition(t.length());

        System.out.println(s);
    }

    public String getText() {
        return label == null ? "" : label.getText();
    }

    public void setText(String s) {
        if (label != null) {
            label.setText(s);
        }
    }

    public void dispose() {
        if (win != null) {
            win.dispose();
        }
    }
}
